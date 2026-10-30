package core

import (
	"context"
	"fmt"
	"net"
	"net/netip"
	"os"
	"path/filepath"
	"strings"
	"sync"

	boxadapter "github.com/sagernet/sing-box/adapter"
	"github.com/sagernet/sing-box/common/process"
	"github.com/sagernet/sing-box/constant"
	libplatform "github.com/sagernet/sing-box/experimental/libbox/platform"
	"github.com/sagernet/sing-box/option"
	"github.com/sagernet/sing-tun"
	"github.com/sagernet/sing/common/control"
	"github.com/sagernet/sing/common/logger"
)

// Platform is the no-JVM implementation of sing-box's platform seam. libbox normally implements
// it through gomobile callbacks into the app's Java code; YumeBox runs the core in a separate
// process, so everything is either a direct syscall or a launcher RPC.
type Platform struct {
	opts           Options
	tunFd          int
	rpc            *launcherRPC
	controllerAddr string
	monitor        *interfaceMonitor

	access           sync.Mutex
	networkManager   boxadapter.NetworkManager
	myTunName        string
	platformInitOnce sync.Once
}

var _ libplatform.Interface = (*Platform)(nil)

func newPlatform(opts Options, tunFd int, rpc *launcherRPC, controllerAddr string) *Platform {
	return &Platform{
		opts:           opts,
		tunFd:          tunFd,
		rpc:            rpc,
		controllerAddr: controllerAddr,
		monitor:        newInterfaceMonitor(),
	}
}

func (p *Platform) Initialize(networkManager boxadapter.NetworkManager) error {
	p.access.Lock()
	defer p.access.Unlock()
	p.networkManager = networkManager
	return nil
}

// UsePlatformAutoDetectInterfaceControl is true in the VpnService child: the process cannot bind
// an interface, but it can ask the launcher to protect each socket before it connects. The root
// daemon keeps the normal path (no RPC channel, no hook), exactly like the mihomo build did.
func (p *Platform) UsePlatformAutoDetectInterfaceControl() bool {
	return p.rpc != nil
}

func (p *Platform) AutoDetectInterfaceControl(fd int) error {
	if p.rpc == nil {
		return nil
	}
	return p.rpc.protect(fd)
}

// OpenTun is the technical pivot of the whole port: the VpnService fd arrives over the launcher
// channel, so the tun is built on top of it instead of being created here.
func (p *Platform) OpenTun(options *tun.Options, platformOptions option.TunPlatformOptions) (tun.Tun, error) {
	if p.tunFd < 0 {
		return nil, fmt.Errorf("no tun file descriptor was handed over by the launcher")
	}
	// Routes and addresses belong to VpnService.Builder; the core only drives the stack.
	options.AutoRoute = false
	options.FileDescriptor = p.tunFd
	if p.opts.TunName != "" {
		options.Name = p.opts.TunName
	}
	if p.opts.MTU > 0 {
		options.MTU = uint32(p.opts.MTU)
	}
	if options.InterfaceMonitor == nil {
		options.InterfaceMonitor = p.monitor
	}
	options.InterfaceMonitor.RegisterMyInterface(options.Name)

	p.access.Lock()
	p.myTunName = options.Name
	p.access.Unlock()

	return tun.New(*options)
}

func (p *Platform) CreateDefaultInterfaceMonitor(_ logger.Logger) tun.DefaultInterfaceMonitor {
	return p.monitor
}

func (p *Platform) Interfaces() ([]boxadapter.NetworkInterface, error) {
	systemInterfaces, err := net.Interfaces()
	if err != nil {
		return nil, err
	}
	interfaces := make([]boxadapter.NetworkInterface, 0, len(systemInterfaces))
	for _, systemInterface := range systemInterfaces {
		if systemInterface.Flags&net.FlagUp == 0 {
			continue
		}
		if systemInterface.Name == p.myTunName {
			continue
		}
		interfaces = append(interfaces, boxadapter.NetworkInterface{
			Interface: control.Interface{
				Index:     systemInterface.Index,
				MTU:       systemInterface.MTU,
				Name:      systemInterface.Name,
				Addresses: interfacePrefixes(systemInterface),
				Flags:     systemInterface.Flags,
			},
			Type: interfaceType(systemInterface.Name),
		})
	}
	return interfaces, nil
}

func (p *Platform) UnderNetworkExtension() bool { return false }

func (p *Platform) IncludeAllNetworks() bool { return false }

func (p *Platform) ClearDNSCache() {}

func (p *Platform) ReadWIFIState() boxadapter.WIFIState {
	return boxadapter.WIFIState{}
}

// SystemCertificates reads the platform trust store directly; the app process cannot export it,
// and sing-box needs it for TLS outbounds that honour user CAs.
func (p *Platform) SystemCertificates() []string {
	const directory = "/system/etc/security/cacerts"
	entries, err := os.ReadDir(directory)
	if err != nil {
		return nil
	}
	certificates := make([]string, 0, len(entries))
	for _, entry := range entries {
		if entry.IsDir() || strings.HasPrefix(entry.Name(), ".") {
			continue
		}
		content, readErr := os.ReadFile(filepath.Join(directory, entry.Name()))
		if readErr != nil {
			continue
		}
		certificates = append(certificates, string(content))
	}
	return certificates
}

// FindProcessInfo answers sing-box's process lookup with the launcher's socket-owner RPC, which
// is the only owner source that works for a VpnService child on API 29+.
func (p *Platform) FindProcessInfo(_ context.Context, network string, source netip.AddrPort, destination netip.AddrPort) (*process.Info, error) {
	protocol := protocolFor(network)
	if protocol < 0 || p.rpc == nil {
		return nil, process.ErrNotFound
	}
	uid, pkg := socketOwnerQuery(p.rpc)(protocol, source.String(), destination.String())
	if uid < 0 && pkg == "" {
		return nil, process.ErrNotFound
	}
	return &process.Info{
		PackageName: pkg,
		ProcessPath: pkg,
		UserId:      int32(uid),
	}, nil
}

func (p *Platform) SendNotification(_ *libplatform.Notification) error { return nil }

func protocolFor(network string) int {
	switch network {
	case "tcp", "tcp4", "tcp6":
		return 6 // syscall.IPPROTO_TCP
	case "udp", "udp4", "udp6":
		return 17 // syscall.IPPROTO_UDP
	default:
		return -1
	}
}

func interfacePrefixes(systemInterface net.Interface) []netip.Prefix {
	addresses, err := systemInterface.Addrs()
	if err != nil {
		return nil
	}
	prefixes := make([]netip.Prefix, 0, len(addresses))
	for _, address := range addresses {
		prefix, parseErr := netip.ParsePrefix(address.String())
		if parseErr != nil {
			continue
		}
		prefixes = append(prefixes, prefix)
	}
	return prefixes
}

func interfaceType(name string) constant.InterfaceType {
	switch {
	case strings.HasPrefix(name, "wlan"):
		return constant.InterfaceTypeWIFI
	case strings.HasPrefix(name, "rmnet"), strings.HasPrefix(name, "ccmni"), strings.HasPrefix(name, "pdp"):
		return constant.InterfaceTypeCellular
	case strings.HasPrefix(name, "eth"):
		return constant.InterfaceTypeEthernet
	default:
		return constant.InterfaceTypeOther
	}
}
