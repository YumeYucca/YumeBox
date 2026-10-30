package core

import (
	"bytes"
	"encoding/json"
	"fmt"

	"github.com/yumeyucca/yumebox/singbox/internal/clash"
)

// Prepare turns whatever the Rust compiler produced into the sing-box options document.
//
// The compile chain still emits Clash YAML — that is what keeps the override semantics, the
// built-in override templates and the JS override API unchanged — so the conversion happens here,
// at the last possible moment. A document that already is sing-box JSON passes through untouched,
// which is what the spike and the tests feed in.
func Prepare(raw []byte, conversion clash.Options) ([]byte, []string, error) {
	if len(raw) >= 2 {
		// UTF-16 looks like a plausible YAML document to the parser, which then yields an empty
		// map and a core with no outbounds instead of an error. Fail with the actual cause.
		if (raw[0] == 0xFF && raw[1] == 0xFE) || (raw[0] == 0xFE && raw[1] == 0xFF) {
			return nil, nil, fmt.Errorf("config is UTF-16; sing-box and Clash profiles must be UTF-8")
		}
	}
	if len(bytes.TrimSpace(raw)) == 0 {
		return nil, nil, fmt.Errorf("empty config")
	}
	if IsJSON(raw) {
		return raw, nil, nil
	}
	result, err := clash.Convert(raw, conversion)
	if err != nil {
		return nil, nil, err
	}
	encoded, err := json.Marshal(result.Options)
	if err != nil {
		return nil, nil, fmt.Errorf("encode converted options: %w", err)
	}
	return encoded, result.Warnings, nil
}

// IsJSON reports whether the payload starts like a JSON object. A BOM is tolerated because
// profiles edited on Windows carry one and sing-box itself would choke on it.
func IsJSON(raw []byte) bool {
	trimmed := bytes.TrimSpace(bytes.TrimPrefix(raw, []byte{0xEF, 0xBB, 0xBF}))
	return len(trimmed) > 0 && trimmed[0] == '{'
}
