package core

import (
	"context"
	"fmt"
	"os"
	"os/signal"
	"strconv"
	"syscall"

	"github.com/sagernet/sing-box"
	"github.com/sagernet/sing-box/experimental/libbox/platform"
	"github.com/sagernet/sing-box/include"
	"github.com/sagernet/sing-box/log"
	"github.com/sagernet/sing-box/option"
	sjson "github.com/sagernet/sing/common/json"
	"github.com/sagernet/sing/service"

	"github.com/yumeyucca/yumebox/singbox/internal/clash"
)

// Run is the c-shared entry point body: parse flags, pull the config (and the TUN fd) off the
// launcher channel, start the instance, and block until the launcher signals stop.
func Run(args []string, channel string) int {
	opts := ParseFlags(args)
	opts.Channel = channel

	realHome := opts.Home
	if realHome == "" {
		return Fatal("missing required --home")
	}

	rawConfig, tunFd, channelFd := readStartup(opts)
	if channelFd >= 0 {
		defer closeFd(channelFd)
	}
	if len(rawConfig) == 0 {
		return Fatal("empty config for mode %q", opts.Mode)
	}

	// rule_set/cache_file paths in the compiled config are relative to the profile directory tree.
	if err := os.Chdir(realHome); err != nil {
		return Fatal("chdir %q: %v", realHome, err)
	}

	var rpc *launcherRPC
	if opts.Mode == "vpn" {
		rpc = newLauncherRPC(channelFd)
	}

	ctx := include.Context(context.Background())

	payload, warnings, err := Prepare(rawConfig, clash.Options{
		Gateway: opts.Gateway,
		TunName: opts.TunName,
		MTU:     opts.MTU,
		// The app's mode switch is only real if the converted config declares the modes.
		EnableModeSwitching: true,
	})
	if err != nil {
		return Fatal("convert config: %v", err)
	}
	for _, warning := range warnings {
		fmt.Fprintf(os.Stderr, "singbox: conversion warning: %s\n", warning)
	}

	options, err := sjson.UnmarshalExtendedContext[option.Options](ctx, payload)
	if err != nil {
		return Fatal("decode config: %v", err)
	}

	controllerAddr, err := applyRuntime(&options, opts)
	if err != nil {
		return Fatal("apply runtime options: %v", err)
	}

	instancePlatform := newPlatform(opts, tunFd, rpc, controllerAddr)
	ctx = service.ContextWith[platform.Interface](ctx, instancePlatform)

	instance, err := box.New(box.Options{
		Context:           ctx,
		Options:           options,
		PlatformLogWriter: platformLog{},
	})
	if err != nil {
		return Fatal("create instance: %v", err)
	}

	if opts.Test {
		_ = instance.Close()
		fmt.Fprintf(os.Stdout, "configuration file %s test is successful\n", opts.ConfigPath)
		return 0
	}

	// Armed before Start so a stop arriving mid-startup is latched, not lost.
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGINT, syscall.SIGTERM)

	if err = instance.Start(); err != nil {
		return Fatal("start instance: %v", err)
	}

	// mihomo exposed the controller on a unix socket; sing-box only listens on TCP, so the socket
	// the app connects to is ours and forwards into the loopback clash_api listener.
	if opts.Controller != "" {
		go func() {
			if err := serveControllerSocket(opts.Controller, controllerAddr); err != nil {
				fmt.Fprintf(os.Stderr, "singbox: controller socket: %v\n", err)
			}
		}()
	}

	<-signals
	// Unwinds inbounds (including the tun), the fake-ip pool and the cache file.
	_ = instance.Close()
	return 0
}

// Check decodes and builds a config without starting it. This is the `--test` equivalent used by
// the build pipeline and by lib/singbox/cmd/check, so the compiler's output is validated by the
// same code path that will run it. Clash YAML is accepted and converted first, exactly like Run.
func Check(home string, rawConfig []byte) error {
	payload, _, err := Prepare(rawConfig, clash.Options{EnableModeSwitching: true})
	if err != nil {
		return err
	}
	if home != "" {
		if err := os.Chdir(home); err != nil {
			return fmt.Errorf("chdir %q: %w", home, err)
		}
	}
	ctx := include.Context(context.Background())
	options, err := sjson.UnmarshalExtendedContext[option.Options](ctx, payload)
	if err != nil {
		return fmt.Errorf("decode config: %w", err)
	}
	ctx = service.ContextWith[platform.Interface](ctx, newPlatform(Options{Mode: "check"}, -1, nil, ""))
	instance, err := box.New(box.Options{Context: ctx, Options: options, PlatformLogWriter: platformLog{}})
	if err != nil {
		return fmt.Errorf("create instance: %w", err)
	}
	return instance.Close()
}

// readStartup fetches the compiled config: vpn takes it 鈥?plus the TUN fd 鈥?off the CHANNEL
// socketpair, root modes read it from --config. tunFd/channelFd are -1 when the mode has none.
func readStartup(opts Options) (rawConfig []byte, tunFd, channelFd int) {
	tunFd, channelFd = -1, -1

	switch opts.Mode {
	case "tun", "ebpf":
		if opts.ConfigPath == "" {
			Fatal("mode %q requires --config", opts.Mode)
			return nil, -1, -1
		}
		rawConfig, _ = os.ReadFile(opts.ConfigPath)
		// Root tun mode normally owns the interface; the spike host instead passes the fd its
		// VpnService established, which keeps the OpenTun path identical to the VPN child.
		tunFd = opts.TunFD
	case "preview":
		fd, err := strconv.Atoi(opts.Channel)
		if err != nil || fd < 0 {
			Fatal("missing or malformed CHANNEL=%q: preview delivers config over the socketpair", opts.Channel)
			return nil, -1, -1
		}
		channelFd = fd
		var err2 error
		if rawConfig, err2 = readPreviewSetup(channelFd); err2 != nil {
			Fatal("read preview setup from channel: %v", err2)
			return nil, -1, -1
		}
	case "vpn":
		fd, err := strconv.Atoi(opts.Channel)
		if err != nil || fd < 0 {
			Fatal("missing or malformed CHANNEL=%q: vpn delivers config over the socketpair", opts.Channel)
			return nil, -1, -1
		}
		channelFd = fd
		var err2 error
		if rawConfig, tunFd, err2 = readSetup(channelFd); err2 != nil {
			Fatal("read setup from channel: %v", err2)
			return nil, -1, -1
		}
	default:
		Fatal("unsupported run mode %q", opts.Mode)
	}
	return rawConfig, tunFd, channelFd
}

// platformLog forwards the core's log stream to stderr, which the launcher captures into
// core.log. The same messages also reach /logs because a PlatformWriter forces the observable
// log factory.
type platformLog struct{}

func (platformLog) DisableColors() bool { return true }

func (platformLog) WriteMessage(_ log.Level, message string) {
	// The message already carries its own level and timestamp prefix; adding one would double it.
	fmt.Fprintln(os.Stderr, message)
}
