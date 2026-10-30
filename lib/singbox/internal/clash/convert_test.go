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
