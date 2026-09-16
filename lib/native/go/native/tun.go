package main

import (
	"net"
	"net/netip"
	"strings"

	"github.com/metacubex/mihomo/config"
	C "github.com/metacubex/mihomo/constant"
	LC "github.com/metacubex/mihomo/listener/config"
	"github.com/metacubex/mihomo/listener/sing_tun"
)

// configureTun injects the VpnService TUN fd into the parsed config before ApplyConfig. Routes
// and the portal address are the VpnService.Builder's business; the core only needs the gateway
// prefixes and the DNS hijack targets.
func configureTun(cfg *config.Config, fd int, gateway, dns string) error {
	prefix4, prefix6, err := splitGatewayPrefixes(gateway)
	if err != nil {
		return err
	}

	// Routes and addresses are the VpnService.Builder's business. The compiled YAML still owns
	// tun.stack: the compiler overlays the app's userspace choice (gVisor or MIPS) and forces
	// enable=false so Parse does not open /dev/net/tun. Copy that stack here instead of
	// replacing the whole block with a hardcoded gVisor TUN. System/Mixed need a kernel TUN,
	// which this fd path does not have, so they fall back to gVisor.
	stack := cfg.General.Tun.Stack
	switch stack {
	case C.TunSystem, C.TunMixed:
		stack = C.TunGvisor
	}

	cfg.General.Tun = LC.Tun{
		Enable:    true,
		Device:    sing_tun.InterfaceName,
		Stack:     stack,
		DNSHijack: splitDNSHijack(dns),
		AutoRoute: false, // routes are set by the VpnService.Builder
		// Core sockets are protected through the launcher, so the TUN must not pick an interface.
		AutoDetectInterface: false,
		Inet4Address:        prefix4,
		Inet6Address:        prefix6,
		MTU:                 1500,
		FileDescriptor:      fd,
	}

	return nil
}

// splitGatewayPrefixes parses "172.19.0.1/30" or "172.19.0.1/30,fdfe:dcba:9876::1/126" into
// per-family prefix lists.
func splitGatewayPrefixes(gateway string) (prefix4, prefix6 []netip.Prefix, err error) {
	for entry := range strings.SplitSeq(gateway, ",") {
		entry = strings.TrimSpace(entry)
		if entry == "" {
			continue
		}
		prefix, err := netip.ParsePrefix(entry)
		if err != nil {
			return nil, nil, err
		}
		if prefix.Addr().Is4() {
			prefix4 = append(prefix4, prefix)
		} else {
			prefix6 = append(prefix6, prefix)
		}
	}
	return prefix4, prefix6, nil
}

// splitDNSHijack turns "172.19.0.2" or "172.19.0.2,fdfe:dcba:9876::2" into host:53 targets.
func splitDNSHijack(dns string) []string {
	var targets []string
	for entry := range strings.SplitSeq(dns, ",") {
		entry = strings.TrimSpace(entry)
		if entry == "" {
			continue
		}
		targets = append(targets, net.JoinHostPort(entry, "53"))
	}
	return targets
}
