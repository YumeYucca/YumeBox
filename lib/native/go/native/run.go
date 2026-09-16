package main

import (
	"os"
	"os/signal"
	"strconv"
	"syscall"

	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/hub"
	"github.com/metacubex/mihomo/hub/executor"
	"github.com/metacubex/mihomo/listener"
	"github.com/metacubex/mihomo/log"
)

// run starts the core and blocks until the launcher signals it to stop.
func run(opts options) {
	if opts.home == "" {
		fatal("missing required --home")
	}

	// Armed before any kernel state is installed; buffered so a stop landing mid-startup is
	// latched for the next checkpoint instead of skipping teardown.
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGINT, syscall.SIGTERM)

	rawConfig, tunFd, channelFd := readStartup(opts)
	if channelFd >= 0 {
		defer syscall.Close(channelFd)
	}
	if stopped(signals) {
		return
	}

	// Preview receives config through a short-lived channel too, but is never a VpnService child.
	var rpc *launcherRPC
	if opts.mode == "vpn" {
		rpc = newLauncherRPC(channelFd)
	}
	installHooks(opts.sdk, opts.mode == "vpn", rpc)

	cfg, err := config.Parse(rawConfig)
	if err != nil {
		fatal("parse compiled config: %v", err)
	}
	// The profile's log-level must not reopen the floodgates: ApplyConfig re-applies it.
	cfg.General.LogLevel = log.ERROR
	if opts.controller != "" {
		cfg.Controller.ExternalControllerUnix = opts.controller
	}
	if tunFd >= 0 {
		if err := configureTun(cfg, tunFd, opts.gateway, opts.dns, opts.stack); err != nil {
			fatal("configure tun: %v", err)
		}
	}
	if stopped(signals) {
		return
	}

	hub.ApplyConfig(cfg)

	// ReCreateTun reports failure only by logging and flipping Enable off, so a core that asked
	// for a tun and did not get one would otherwise look healthy to the launcher.
	if cfg.General.Tun.Enable && !listener.GetTunConf().Enable {
		executor.Shutdown()
		fatal("tun listener failed to start; see core.log")
	}

	<-signals
	// Unwinds listeners (including root tun ip rules) and the fake-ip pool, each self-guarding.
	// The VpnService child also arrives here through SIGTERM so selector state can be persisted.
	executor.Shutdown()
}

// stopped reports a latched stop without blocking: blocking syscalls restart after a signal
// (SA_RESTART), so startup relies on checkpoints rather than EINTR.
func stopped(signals <-chan os.Signal) bool {
	select {
	case <-signals:
		return true
	default:
		return false
	}
}

// readStartup fetches the compiled config: vpn takes it — plus the TUN fd — off the CHANNEL
// socketpair, root modes read it from --config. tunFd/channelFd are -1 when the mode has none.
func readStartup(opts options) (rawConfig []byte, tunFd, channelFd int) {
	tunFd, channelFd = -1, -1

	switch opts.mode {
	case "tun", "ebpf":
		if opts.configPath == "" {
			fatal("mode %q requires --config", opts.mode)
		}
		var err error
		if rawConfig, err = os.ReadFile(opts.configPath); err != nil {
			fatal("read config %q: %v", opts.configPath, err)
		}
	case "preview":
		fd, err := strconv.Atoi(opts.channel)
		if err != nil || fd < 0 {
			fatal("missing or malformed CHANNEL=%q: preview mode delivers config over the socketpair", opts.channel)
		}
		channelFd = fd
		if rawConfig, err = readPreviewSetup(channelFd); err != nil {
			fatal("read preview setup from channel: %v", err)
		}
	case "vpn":
		value := opts.channel
		fd, err := strconv.Atoi(value)
		if err != nil || fd < 0 {
			fatal("missing or malformed CHANNEL=%q: vpn mode delivers config over the socketpair, not a file", value)
		}
		channelFd = fd
		if rawConfig, tunFd, err = readSetup(channelFd); err != nil {
			fatal("read setup from channel: %v", err)
		}
	default:
		fatal("unsupported run mode %q", opts.mode)
	}

	// config.Parse fills an empty document with defaults instead of erroring, bringing up a core
	// that routes nothing while looking healthy.
	if len(rawConfig) == 0 {
		fatal("empty config for mode %q", opts.mode)
	}
	return rawConfig, tunFd, channelFd
}
