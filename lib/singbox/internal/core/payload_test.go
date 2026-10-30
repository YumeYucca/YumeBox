//go:build with_utls && with_quic

package core_test

import (
	"os"
	"testing"

	"github.com/yumeyucca/yumebox/singbox/internal/core"
)

// realWorldFixture is a sanitized copy of a production sing-box profile: 89 outbounds
// (vless/hysteria2/selector/urltest/http/socks/vmess), 17 remote binary rule-sets, fake-ip DNS and
// logical rules. It is here to catch schema drift when the engine version moves — a config that
// real clients ship must keep passing box.New.
const realWorldFixture = "../../testdata/real-world-singbox.json"

func TestRealWorldSingBoxProfileIsAccepted(t *testing.T) {
	content, err := os.ReadFile(realWorldFixture)
	if err != nil {
		t.Fatalf("read fixture: %v", err)
	}
	if !core.IsJSON(content) {
		t.Fatal("the fixture must take the JSON passthrough path, not the Clash converter")
	}
	if err := core.Check("", content); err != nil {
		t.Fatalf("sing-box rejected a real-world profile: %v", err)
	}
}

func TestIsJSONToleratesBOM(t *testing.T) {
	if !core.IsJSON([]byte("\xef\xbb\xbf  {\"log\":{}}")) {
		t.Fatal("a BOM-prefixed JSON document must still take the passthrough path")
	}
	if core.IsJSON([]byte("mixed-port: 7890\n")) {
		t.Fatal("clash yaml must not be treated as json")
	}
}
