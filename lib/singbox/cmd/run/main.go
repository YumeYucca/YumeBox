// Command run executes the core as a standalone process.
//
// On Android it is cross-compiled as a PIE executable and pushed to /data/local/tmp, which makes
// the whole control plane testable over adb without building an APK: the config carries a SOCKS
// inbound instead of a TUN, so no VpnService is needed. The VpnService path (config + TUN fd over
// the launcher channel) is unchanged and still goes through SingboxMain.
package main

import (
	"os"

	"github.com/yumeyucca/yumebox/singbox/internal/core"
)

func main() {
	os.Exit(core.Run(os.Args, ""))
}
