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
func configureTun(cfg *config.Config, fd int, gateway, dns, stackName string) error {
	prefix4, prefix6, err := splitGatewayPrefixes(gateway)
	if err != nil {
		return err
	}

	cfg.General.Tun = LC.Tun{
		Enable:    true,
		Device:    sing_tun.InterfaceName,
		Stack:     vpnTunStack(stackName),
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

// vpnTunStack maps the launcher --stack flag onto a TUNStack after config.Parse. Unknown names
// (including mips on a core that has not added it yet) fall back to gVisor so YAML never has
// to carry a value Parse would reject. System/Mixed need a kernel TUN this fd path does not have.
func vpnTunStack(name string) C.TUNStack {
	var stack C.TUNStack
	if err := stack.UnmarshalText([]byte(strings.TrimSpace(name))); err != nil {
		return C.TunGvisor
	}
	switch stack {
	case C.TunSystem, C.TunMixed:
		return C.TunGvisor
	default:
		return stack
	}
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
