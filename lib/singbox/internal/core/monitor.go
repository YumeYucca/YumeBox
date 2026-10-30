package core

import (
	"sync"

	"github.com/sagernet/sing-tun"
	"github.com/sagernet/sing/common/control"
	"github.com/sagernet/sing/common/x/list"
)

// interfaceMonitor keeps the default-interface contract while the app protects sockets instead of
// binding an interface. It reports no default interface on purpose: with
// UsePlatformAutoDetectInterfaceControl the dialer never needs one, and guessing from
// /proc/net/route is exactly the kind of Android-version-dependent code this port avoids.
//
// P1 replaces the polling body with a launcher RPC that reads ConnectivityManager's default
// network, which is the only accurate source on API 29+.
type interfaceMonitor struct {
	access     sync.Mutex
	myName     string
	callbacks  list.List[tun.DefaultInterfaceUpdateCallback]
	started    bool
	androidVPN bool
}

func newInterfaceMonitor() *interfaceMonitor {
	return &interfaceMonitor{}
}

func (m *interfaceMonitor) Start() error {
	m.access.Lock()
	defer m.access.Unlock()
	m.started = true
	return nil
}

func (m *interfaceMonitor) Close() error {
	m.access.Lock()
	defer m.access.Unlock()
	m.started = false
	return nil
}

func (m *interfaceMonitor) DefaultInterface() *control.Interface { return nil }

func (m *interfaceMonitor) OverrideAndroidVPN() bool { return m.androidVPN }

func (m *interfaceMonitor) AndroidVPNEnabled() bool { return m.androidVPN }

func (m *interfaceMonitor) RegisterCallback(callback tun.DefaultInterfaceUpdateCallback) *list.Element[tun.DefaultInterfaceUpdateCallback] {
	m.access.Lock()
	defer m.access.Unlock()
	return m.callbacks.PushBack(callback)
}

func (m *interfaceMonitor) UnregisterCallback(element *list.Element[tun.DefaultInterfaceUpdateCallback]) {
	if element == nil {
		return
	}
	m.access.Lock()
	defer m.access.Unlock()
	m.callbacks.Remove(element)
}

func (m *interfaceMonitor) RegisterMyInterface(interfaceName string) {
	m.access.Lock()
	defer m.access.Unlock()
	m.myName = interfaceName
}

func (m *interfaceMonitor) MyInterface() string {
	m.access.Lock()
	defer m.access.Unlock()
	return m.myName
}
