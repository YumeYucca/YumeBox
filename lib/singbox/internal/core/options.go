package core

import (
	"flag"
	"fmt"
	"os"
)

// Options mirrors the flag vocabulary of the mihomo launcher (lib/native/go/native/entry.go) so
// the Kotlin side (CoreProcess.kt) can drive both cores with the same command line.
type Options struct {
	Home       string
	Controller string
	Secret     string
	Gateway    string
	DNS        string
	Mode       string
	Stack      string
	ConfigPath string
	Channel    string
	SDK        int
	Test       bool
	TunName    string
	MTU        int
	APIAddr    string
	// TunFD carries the VpnService fd directly instead of over the socketpair. Only the spike
	// host uses it: the production path hands the fd over the channel with SCM_RIGHTS.
	TunFD int
}

// ParseFlags keeps ContinueOnError so a bad invocation reports through Fatal instead of a panic.
func ParseFlags(args []string) Options {
	var opts Options
	flags := flag.NewFlagSet("singbox", flag.ContinueOnError)
	flags.SetOutput(os.Stderr)

	flags.StringVar(&opts.Home, "home", "", "core home directory")
	flags.StringVar(&opts.Controller, "controller", "", "external-controller-unix socket path")
	flags.StringVar(&opts.Secret, "secret", "", "external controller secret")
	flags.StringVar(&opts.Gateway, "gateway", "", "tun gateway CIDR(s)")
	flags.StringVar(&opts.DNS, "dns", "", "tun DNS hijack address(es)")
	flags.StringVar(&opts.Mode, "mode", "vpn", "run mode: vpn | tun | ebpf | preview")
	flags.StringVar(&opts.Stack, "stack", "gvisor", "vpn tun stack: gvisor | system")
	flags.StringVar(&opts.ConfigPath, "config", "", "compiled config path; root modes read it here")
	flags.StringVar(&opts.Channel, "channel", "", "socketpair fd carrying config and tun fd")
	flags.IntVar(&opts.SDK, "sdk", 0, "android platform SDK int")
	flags.BoolVar(&opts.Test, "test", false, "parse and build the config, then exit")
	flags.StringVar(&opts.TunName, "tun-name", "", "tun interface name supplied by the launcher")
	flags.IntVar(&opts.MTU, "mtu", 0, "tun MTU; 0 keeps the config value")
	flags.StringVar(&opts.APIAddr, "api-addr", "", "fixed clash_api listen address; empty picks a free loopback port")
	flags.IntVar(&opts.TunFD, "tun-fd", -1, "spike host only: inherit this tun fd instead of receiving it on the channel")

	if err := flags.Parse(args[1:]); err != nil {
		Fatal("parse arguments: %v", err)
	}
	return opts
}

// Fatal reports on stderr and returns a process exit code; callers on the c-shared path return it
// to the launcher instead of calling os.Exit, which would take the whole VpnService down silently.
func Fatal(format string, args ...any) int {
	fmt.Fprintf(os.Stderr, "singbox: "+format+"\n", args...)
	return 1
}
