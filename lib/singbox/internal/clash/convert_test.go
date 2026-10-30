package clash_test

import (
	"bytes"
	"encoding/json"
	"strings"
	"testing"

	"github.com/yumeyucca/yumebox/singbox/internal/clash"
	"github.com/yumeyucca/yumebox/singbox/internal/core"
)

// realisticProfile is the shape a normal subscription arrives in after the Rust compiler has applied
// the user's overrides. It exercises the constructs the converter must get right on day one.
const realisticProfile = `
mixed-port: 7890
allow-lan: false
mode: rule
log-level: info
dns:
  enable: true
  enhanced-mode: redir-host
  nameserver:
    - 223.5.5.5
    - https://doh.pub/dns-query
proxies:
  - name: "🇭🇰 HK-01"
    type: ss
    server: hk1.example.com
    port: 8388
    cipher: aes-256-gcm
    password: secret
  - name: "🇯🇵 JP-01"
    type: vmess
    server: jp1.example.com
    port: 443
    uuid: 11111111-2222-3333-4444-555555555555
    alterId: 0
    cipher: auto
    tls: true
    servername: jp1.example.com
    network: ws
    ws-opts:
      path: /ray
  - name: "🇸🇬 SG-01"
    type: trojan
    server: sg1.example.com
    port: 443
    password: trojan-pass
    sni: sg1.example.com
    skip-cert-verify: true
proxy-groups:
  - name: "🚀 节点选择"
    type: select
    proxies:
      - "♻️ 自动选择"
      - "🇭🇰 HK-01"
      - DIRECT
  - name: "♻️ 自动选择"
    type: url-test
    proxies:
      - "🇭🇰 HK-01"
      - "🇯🇵 JP-01"
    url: https://www.gstatic.com/generate_204
    interval: 300
  - name: "🐟 漏网之鱼"
    type: select
    proxies:
      - DIRECT
      - REJECT
rules:
  - DOMAIN-SUFFIX,google.com,🚀 节点选择
  - DOMAIN-KEYWORD,github,♻️ 自动选择
  - GEOIP,CN,DIRECT
  - DST-PORT,22,REJECT
  - MATCH,🐟 漏网之鱼
`

func TestConvertedProfileIsAcceptedBySingBox(t *testing.T) {
	result, err := clash.Convert([]byte(realisticProfile), clash.Options{
		Gateway:             "172.19.0.1/30",
		TunName:             "tun0",
		EnableModeSwitching: true,
	})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}

	encoded, err := json.Marshal(result.Options)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}

	// The gate that matters: the same decode + box.New path the Android core uses.
	if err := core.Check("", encoded); err != nil {
		t.Fatalf("sing-box rejected the converted config: %v\n%s", err, pretty(t, encoded))
	}
}

func TestProxyProtocolsMapToExpectedTypes(t *testing.T) {
	result, err := clash.Convert([]byte(realisticProfile), clash.Options{Gateway: "172.19.0.1/30"})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	types := map[string]string{}
	for _, entry := range result.Options["outbounds"].([]map[string]any) {
		types[entry["tag"].(string)] = entry["type"].(string)
	}
	expected := map[string]string{
		"🇭🇰 HK-01": "shadowsocks",
		"🇯🇵 JP-01": "vmess",
		"🇸🇬 SG-01": "trojan",
		"🚀 节点选择":   "selector",
		"♻️ 自动选择":  "urltest",
		"direct":   "direct",
		"block":    "block",
	}
	for tag, want := range expected {
		if got := types[tag]; got != want {
			t.Errorf("outbound %q: got type %q, want %q", tag, got, want)
		}
	}
}

func TestModeRulesExistWhenSwitchingIsEnabled(t *testing.T) {
	withModes, err := clash.Convert([]byte(realisticProfile), clash.Options{
		Gateway:             "172.19.0.1/30",
		EnableModeSwitching: true,
	})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	rules := withModes.Options["route"].(map[string]any)["rules"].([]any)
	modes := map[string]bool{}
	for _, entry := range rules {
		if mode, ok := entry.(map[string]any)["clash_mode"].(string); ok {
			modes[mode] = true
		}
	}
	if !modes["Global"] || !modes["Direct"] {
		t.Fatalf("mode switching needs both clash_mode rules, got %v", modes)
	}
}

func TestRouteFinalFollowsMatchRule(t *testing.T) {
	result, err := clash.Convert([]byte(realisticProfile), clash.Options{Gateway: "172.19.0.1/30"})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	final := result.Options["route"].(map[string]any)["final"]
	if final != "🐟 漏网之鱼" {
		t.Fatalf("final: got %v, want the MATCH target", final)
	}
}

func TestUnsupportedProxyTypeFailsLoudly(t *testing.T) {
	profile := `
proxies:
  - name: snell-node
    type: snell
    server: example.com
    port: 443
`
	_, err := clash.Convert([]byte(profile), clash.Options{})
	if err == nil {
		t.Fatal("expected an error for an unsupported proxy type")
	}
	if !strings.Contains(err.Error(), "snell") || !strings.Contains(err.Error(), "snell-node") {
		t.Fatalf("error should name the type and the node, got: %v", err)
	}
}

func TestProfileWithoutDNSHasNoDanglingResolver(t *testing.T) {
	profile := `
proxies:
  - name: node
    type: trojan
    server: example.com
    port: 443
    password: p
proxy-groups:
  - name: g
    type: select
    proxies: [node, DIRECT]
rules:
  - MATCH,g
`
	result, err := clash.Convert([]byte(profile), clash.Options{})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	// A profile without dns would make sing-box fall back to [::1]:53 and fail every domain
	// outbound on Android, so the converter injects reachable resolvers instead.
	if _, exists := result.Options["dns"]; !exists {
		t.Fatal("a profile without a dns block must get injected resolvers")
	}
	resolver, exists := result.Options["route"].(map[string]any)["default_domain_resolver"]
	if !exists {
		t.Fatal("default_domain_resolver must name the injected resolver")
	}
	warned := false
	for _, warning := range result.Warnings {
		if strings.Contains(warning, "injected default resolvers") {
			warned = true
		}
	}
	if !warned {
		t.Fatalf("the injected resolvers must be reported, warnings: %v", result.Warnings)
	}
	_ = resolver
	encoded, _ := json.Marshal(result.Options)
	if err := core.Check("", encoded); err != nil {
		t.Fatalf("sing-box rejected the converted config: %v\n%s", err, pretty(t, encoded))
	}
}

func TestFallbackGroupIsReportedAsDegraded(t *testing.T) {
	profile := `
proxies:
  - name: a
    type: trojan
    server: a.example.com
    port: 443
    password: p
  - name: b
    type: trojan
    server: b.example.com
    port: 443
    password: p
proxy-groups:
  - name: fb
    type: fallback
    proxies: [a, b]
rules:
  - MATCH,fb
`
	result, err := clash.Convert([]byte(profile), clash.Options{})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	found := false
	for _, warning := range result.Warnings {
		if strings.Contains(warning, "fallback") {
			found = true
		}
	}
	if !found {
		t.Fatalf("a fallback group must be reported, warnings: %v", result.Warnings)
	}
}

func pretty(t *testing.T, encoded []byte) string {
	t.Helper()
	var buffer bytes.Buffer
	if err := json.Indent(&buffer, encoded, "", "  "); err != nil {
		return string(encoded)
	}
	return buffer.String()
}

func TestLogicalRulesBecomeSingBoxLogicalRules(t *testing.T) {
	profile := `
proxies:
  - { name: n, type: trojan, server: n.example.com, port: 443, password: p }
proxy-groups:
  - { name: g, type: select, proxies: [n, DIRECT] }
rules:
  - AND,((DOMAIN-SUFFIX,google.com),(NETWORK,TCP)),g
  - OR,((DOMAIN,a.example.com),(DST-PORT,443)),DIRECT
  - NOT,((DOMAIN-SUFFIX,cn)),g
  - MATCH,g
`
	result, err := clash.Convert([]byte(profile), clash.Options{})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	modes := map[string]bool{}
	notInverted := false
	for _, entry := range result.Options["route"].(map[string]any)["rules"].([]any) {
		rule := entry.(map[string]any)
		if rule["invert"] == true && rule["domain_suffix"] != nil {
			// sing-box has no "not" logical mode; NOT becomes an inverted matcher.
			notInverted = true
		}
		if rule["type"] != "logical" {
			continue
		}
		mode, _ := rule["mode"].(string)
		modes[mode] = true
		clauses, _ := rule["rules"].([]any)
		if len(clauses) != 2 {
			t.Errorf("mode %s: expected two clauses, got %d", mode, len(clauses))
		}
		if rule["outbound"] == nil {
			t.Errorf("mode %s: logical rule lost its target", mode)
		}
	}
	for _, want := range []string{"and", "or"} {
		if !modes[want] {
			t.Fatalf("missing logical mode %q, got %v", want, modes)
		}
	}
	if !notInverted {
		t.Fatal("NOT rule must become an inverted matcher")
	}
	encoded, _ := json.Marshal(result.Options)
	if err := core.Check("", encoded); err != nil {
		t.Fatalf("sing-box rejected the converted logical rules: %v", err)
	}
}

func TestFakeIPProfileGetsFakeipServerAndRules(t *testing.T) {
	profile := `
mixed-port: 7890
dns:
  enable: true
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  fake-ip-filter: ["+.lan", "localhost", "+.stun.*.*"]
  nameserver: [223.5.5.5]
proxies:
  - { name: n, type: trojan, server: n.example.com, port: 443, password: p }
proxy-groups:
  - { name: g, type: select, proxies: [n, DIRECT] }
rules:
  - MATCH,g
`
	result, err := clash.Convert([]byte(profile), clash.Options{Gateway: "172.19.0.1/30"})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	dns := result.Options["dns"].(map[string]any)
	hasFakeIP := false
	for _, entry := range dns["servers"].([]any) {
		server := entry.(map[string]any)
		if server["type"] == "fakeip" {
			hasFakeIP = true
			if server["inet4_range"] != "198.18.0.1/16" {
				t.Errorf("fakeip range: got %v", server["inet4_range"])
			}
		}
	}
	if !hasFakeIP {
		t.Fatal("enhanced-mode fake-ip must emit a fakeip dns server")
	}
	if dns["final"] == "fakeip" {
		t.Fatal("final must stay on a real resolver, fakeip only answers A/AAAA")
	}
	rules, _ := dns["rules"].([]any)
	if len(rules) != 2 {
		t.Fatalf("expected a filter rule plus the A/AAAA catch-all, got %d", len(rules))
	}
	filter := rules[0].(map[string]any)
	if filter["server"] == "fakeip" {
		t.Fatal("the fake-ip-filter rule must point at a real resolver")
	}
	suffixes, _ := filter["domain_suffix"].([]string)
	found := false
	for _, suffix := range suffixes {
		if suffix == "lan" {
			found = true
		}
	}
	if !found {
		t.Fatalf("+.lan should map to domain_suffix lan, got %v", filter["domain_suffix"])
	}
	encoded, _ := json.Marshal(result.Options)
	if err := core.Check("", encoded); err != nil {
		t.Fatalf("sing-box rejected the fake-ip config: %v", err)
	}
}

// This mirrors the shapes a real paid subscription ships (observed live): port hopping on
// hysteria2, certificate pinning, implicit TLS on trojan, a 24h url-test interval, a fallback
// group, fake-ip DNS and a no-resolve option after the target. Each of those broke the converter
// once; the test exists so they cannot break it again.
const subscriptionShapes = `
mixed-port: 7890
mode: rule
log-level: info
dns:
  enable: true
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  nameserver: [223.5.5.5, 119.29.29.29]
  fallback: [1.1.1.1]
  fallback-filter: { geoip: true, geoip-code: CN }
proxies:
  - { name: hy2, type: hysteria2, server: hy2.example.com, port: 50000, ports: 50000-53000,
      password: p, sni: www.example.com, skip-cert-verify: true,
      fingerprint: 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef, udp: true }
  - { name: vless, type: vless, server: vless.example.com, port: 443,
      uuid: 11111111-2222-3333-4444-555555555555, tls: true, servername: vless.example.com,
      network: tcp, flow: xtls-rprx-vision }
  - { name: trojan, type: trojan, server: trojan.example.com, port: 443, password: p,
      sni: trojan.example.com, skip-cert-verify: true }
proxy-groups:
  - { name: PROXY, type: select, proxies: [AUTO, FAILOVER, hy2, DIRECT] }
  - { name: AUTO, type: url-test, proxies: [hy2, vless],
      url: 'http://www.gstatic.com/generate_204', interval: 86400 }
  - { name: FAILOVER, type: fallback, proxies: [hy2, vless], interval: 7200 }
rules:
  - IP-CIDR,1.1.1.1/32,PROXY,no-resolve
  - DOMAIN-SUFFIX,example.com,PROXY
  - GEOIP,CN,DIRECT
  - MATCH,PROXY
`

func TestRealSubscriptionShapesSurviveConversion(t *testing.T) {
	result, err := clash.Convert([]byte(subscriptionShapes), clash.Options{Gateway: "172.19.0.1/30"})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	outbounds := map[string]map[string]any{}
	for _, entry := range result.Options["outbounds"].([]map[string]any) {
		outbounds[entry["tag"].(string)] = entry
	}

	hy2 := outbounds["hy2"]
	ports, _ := hy2["server_ports"].([]string)
	if len(ports) != 1 || ports[0] != "50000:53000" {
		t.Fatalf("hysteria2 port hopping must become server_ports, got %v", hy2["server_ports"])
	}
	tls, _ := hy2["tls"].(map[string]any)
	if tls == nil || tls["enabled"] != true {
		t.Fatalf("hysteria2 must carry a tls block, got %v", hy2["tls"])
	}

	trojan := outbounds["trojan"]
	trojanTLS, _ := trojan["tls"].(map[string]any)
	if trojanTLS == nil || trojanTLS["enabled"] != true {
		t.Fatalf("trojan implies TLS even without tls: true, got %v", trojan["tls"])
	}

	auto := outbounds["AUTO"]
	if auto["interval"] != auto["idle_timeout"] {
		t.Fatalf("urltest needs interval <= idle_timeout, got interval=%v idle_timeout=%v",
			auto["interval"], auto["idle_timeout"])
	}
	if outbounds["FAILOVER"]["type"] != "urltest" {
		t.Fatal("fallback must degrade to urltest")
	}

	rules := result.Options["route"].(map[string]any)["rules"].([]any)
	foundCIDR := false
	for _, entry := range rules {
		rule := entry.(map[string]any)
		if cidrs, ok := rule["ip_cidr"].([]string); ok && len(cidrs) == 1 {
			if cidrs[0] != "1.1.1.1/32" {
				t.Fatalf("no-resolve must not leak into the CIDR, got %q", cidrs[0])
			}
			if rule["outbound"] != "PROXY" {
				t.Fatalf("target must stay PROXY, got %v", rule["outbound"])
			}
			foundCIDR = true
		}
	}
	if !foundCIDR {
		t.Fatal("the IP-CIDR rule with a trailing no-resolve was lost")
	}

	encoded, _ := json.Marshal(result.Options)
	if err := core.Check("", encoded); err != nil {
		t.Fatalf("sing-box rejected the converted subscription shapes: %v", err)
	}
}

func TestPreviewConversionBindsNothing(t *testing.T) {
	profile := `
mixed-port: 7890
socks-port: 7891
proxies:
  - { name: n, type: trojan, server: n.example.com, port: 443, password: p }
proxy-groups:
  - { name: g, type: select, proxies: [n, DIRECT] }
rules:
  - MATCH,g
`
	result, err := clash.Convert([]byte(profile), clash.Options{Preview: true})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}
	if inbounds, exists := result.Options["inbounds"]; exists {
		t.Fatalf("a preview config must not declare inbounds, got %v", inbounds)
	}
	encoded, _ := json.Marshal(result.Options)
	if err := core.Check("", encoded); err != nil {
		t.Fatalf("sing-box rejected the preview config: %v", err)
	}
}

// The app does not feed the core a raw subscription: the Rust compiler applies the runtime patch
// set first, which forces fake-ip DNS, adds a tun block, pins interface-name/routing-mark and
// installs the unix controller. This locks the converter against that exact shape.
const runtimePatchedProfile = `
mixed-port: 7890
allow-lan: false
mode: rule
log-level: info
ipv6: true
external-controller: 127.0.0.1:9090
internal-controller: unix:/data/user/0/com.github.yumeyucca.yumebox/files/core.sock
interface-name: wlan0
routing-mark: 0
tcp-concurrent: true
unified-delay: true
profile:
  store-selected: true
  store-fake-ip: true
dns:
  enable: true
  use-hosts: true
  default-nameserver: [223.5.5.5, 119.29.29.29]
  nameserver: [223.5.5.5, 119.29.29.29]
  enhanced-mode: fake-ip
  fake-ip-range: 198.18.0.1/16
  fake-ip-filter: ['+.stun.*.*', '+.stun.*.*.*', +.lan, localhost]
  ipv6: false
tun:
  enable: true
  stack: mixed
  auto-route: false
  auto-detect-interface: false
  mtu: 1500
proxies:
  - { name: hy2, type: hysteria2, server: hy2.example.com, port: 50000, ports: 50000-53000,
      password: p, sni: www.example.com, skip-cert-verify: true, udp: true }
proxy-groups:
  - { name: PROXY, type: select, proxies: [hy2, DIRECT] }
rules:
  - DOMAIN-SUFFIX,example.com,PROXY
  - GEOIP,CN,DIRECT
  - MATCH,PROXY
`

func TestRuntimePatchedProfileConverts(t *testing.T) {
	result, err := clash.Convert([]byte(runtimePatchedProfile), clash.Options{
		Gateway:             "172.19.0.1/30",
		TunName:             "tun0",
		EnableModeSwitching: true,
	})
	if err != nil {
		t.Fatalf("convert: %v", err)
	}

	inbounds := result.Options["inbounds"].([]any)
	if len(inbounds) != 1 {
		t.Fatalf("the VPN run must declare exactly the tun inbound, got %d", len(inbounds))
	}
	tun := inbounds[0].(map[string]any)
	if tun["type"] != "tun" || tun["interface_name"] != "tun0" || tun["auto_route"] != false {
		t.Fatalf("tun inbound is wrong: %v", tun)
	}
	// Clash "mixed" is not a sing-box stack; gvisor is the safe fallback the mihomo shim used.
	if tun["stack"] != "gvisor" {
		t.Fatalf("stack mixed must fall back to gvisor, got %v", tun["stack"])
	}

	encoded, _ := json.Marshal(result.Options)
	if err := core.Check("", encoded); err != nil {
		t.Fatalf("sing-box rejected the runtime-patched conversion: %v", err)
	}
}
