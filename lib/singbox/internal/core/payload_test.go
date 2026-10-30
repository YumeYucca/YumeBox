package core_test

import (
	"os"
	"strings"
	"testing"

	"github.com/yumeyucca/yumebox/singbox/internal/clash"
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

func TestUTF16PayloadIsRejectedWithTheRealCause(t *testing.T) {
	// A UTF-16 document is valid YAML to the parser but decodes to an empty map, which used to
	// start a core with no outbounds at all. The error has to name the encoding instead.
	payload := []byte{0xFF, 0xFE, '{', 0x00, '}', 0x00}
	_, _, err := core.Prepare(payload, clash.Options{})
	if err == nil {
		t.Fatal("expected an encoding error")
	}
	if !strings.Contains(err.Error(), "UTF-16") {
		t.Fatalf("error should name the encoding, got: %v", err)
	}
}
