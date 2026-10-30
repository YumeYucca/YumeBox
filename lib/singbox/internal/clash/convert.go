// Package clash converts a Clash/mihomo profile into sing-box options.
//
// It sits at the very end of YumeBox's existing compile chain: the Rust compiler still parses the
// profile, applies YAML and JavaScript overrides, backfills defaults and validates the result as
// Clash YAML. Only the final step — turning that document into something sing-box understands —
// happens here. That keeps the override semantics, the built-in override templates and the JS
// override API untouched, which is the whole point of the migration route.
//
// The conversion is deliberately loud: every construct it cannot express in sing-box is reported as
// a warning, and an unsupported proxy type aborts instead of silently dropping a node.
package clash

import (
	"fmt"
	"net"
	"sort"
	"strconv"
	"strings"

	"gopkg.in/yaml.v3"
)

// Options carries the launcher-side facts the converter needs but the profile does not know.
type Options struct {
	// Gateway is the tun address handed to VpnService ("172.19.0.1/30"); the tun inbound must
	// advertise the same prefix or routing silently diverges.
	Gateway string
	// TunName is the interface name the launcher established.
	TunName string
	// MTU overrides the profile's tun MTU when positive.
	MTU int
	// EnableModeSwitching emits the clash_mode rules sing-box needs for Global/Direct to exist at
	// all; without them PATCH /configs is a no-op.
	EnableModeSwitching bool
}

// Result is the sing-box options document plus everything the conversion had to give up on.
type Result struct {
	Options  map[string]any
	Warnings []string
}

type converter struct {
	options  Options
	warnings []string

	// Outbound tags are emitted in profile order; groups reference them by name.
	outbounds []map[string]any
	// Tags that a group or rule may reference but that the profile never declared.
	implicit map[string]bool
	// ruleSets accumulates route.rule_set entries referenced by GEOIP/GEOSITE/RULE-SET rules.
	ruleSets map[string]map[string]any
	// orderedRuleSets keeps rule_set output deterministic.
	orderedRuleSets []string
	// providers holds rule-providers so RULE-SET rules can resolve their source.
	providers map[string]map[string]any
}

// Convert parses a Clash YAML document and returns sing-box options.
func Convert(content []byte, options Options) (Result, error) {
	var document map[string]any
	if err := yaml.Unmarshal(content, &document); err != nil {
		return Result{}, fmt.Errorf("parse clash yaml: %w", err)
	}
	if document == nil {
		return Result{}, fmt.Errorf("empty clash config")
	}

	c := &converter{
		options:   options,
		implicit:  map[string]bool{},
		ruleSets:  map[string]map[string]any{},
		providers: map[string]map[string]any{},
	}
	for name, entry := range asMap(document["rule-providers"]) {
		if provider := asMap(entry); provider != nil {
			c.providers[name] = provider
		}
	}

	proxies, err := c.convertProxies(document["proxies"])
	if err != nil {
		return Result{}, err
	}
	groups, err := c.convertGroups(document["proxy-groups"], proxies)
	if err != nil {
		return Result{}, err
	}
	c.outbounds = append(proxies, groups...)
	c.ensureBuiltins()

	// DNS is converted before route: the route's default_domain_resolver has to name a server that
	// actually exists, and sing-box fails to start when it does not.
	dns, hasDNS := c.convertDNS(document["dns"])
	if !hasDNS {
		// Without a dns block sing-box falls back to [::1]:53, which does not exist on Android:
		// every domain outbound then fails. The mihomo path backfilled the same defaults in
		// patch/runtime.rs, so the behaviour is preserved here.
		dns = c.defaultDNS()
		hasDNS = true
		c.warnf("profile has no dns block; injected default resolvers %s", strings.Join(defaultNameServers, ", "))
	}
	dnsTag := asString(dns["final"])

	route, err := c.convertRoute(document["rules"], dnsTag)
	if err != nil {
		return Result{}, err
	}

	result := map[string]any{
		"log": map[string]any{
			"level": clashLogLevel(asString(document["log-level"])),
		},
		"outbounds": c.outbounds,
		"route":     route,
	}
	if hasDNS {
		result["dns"] = dns
	}
	inbounds := make([]any, 0, 2)
	if tun, ok := c.convertTun(document["tun"]); ok {
		inbounds = append(inbounds, tun)
	}
	// Local ports only matter when the core is not the VPN's tun owner: the app never listens on
	// localhost, but a diagnostic run (and the device checks) need an entry point.
	if c.options.Gateway == "" {
		if inbound, ok := c.convertLocalInbound(document); ok {
			inbounds = append(inbounds, inbound)
		}
	}
	if len(inbounds) > 0 {
		result["inbounds"] = inbounds
	}
	if providers, ok := c.convertProxyProviders(document["proxy-providers"]); ok {
		result["providers"] = providers
	}
	if sets := c.ruleSetList(); len(sets) > 0 {
		route["rule_set"] = sets
	}
	// Re-read route after rule_set injection so the map carries both.
	result["route"] = route

	return Result{Options: result, Warnings: c.warnings}, nil
}

func (c *converter) warnf(format string, args ...any) {
	c.warnings = append(c.warnings, fmt.Sprintf(format, args...))
}

func (c *converter) convertProxies(raw any) ([]map[string]any, error) {
	entries := asList(raw)
	outbounds := make([]map[string]any, 0, len(entries))
	for _, entry := range entries {
		proxy := asMap(entry)
		if proxy == nil {
			continue
		}
		outbound, err := c.convertProxy(proxy)
		if err != nil {
			return nil, err
		}
		if outbound != nil {
			outbounds = append(outbounds, outbound)
		}
	}
	return outbounds, nil
}

func (c *converter) convertProxy(proxy map[string]any) (map[string]any, error) {
	name := asString(proxy["name"])
	proxyType := strings.ToLower(asString(proxy["type"]))
	server := asString(proxy["server"])
	port := asInt(proxy["port"])

	if name == "" {
		return nil, fmt.Errorf("proxy without a name: %v", proxy["type"])
	}

	switch proxyType {
	case "ss", "shadowsocks":
		outbound := map[string]any{
			"type": "shadowsocks", "tag": name,
			"server": server, "server_port": port,
			"method": asString(proxy["cipher"]), "password": asString(proxy["password"]),
		}
		if plugin := asString(proxy["plugin"]); plugin != "" {
			// Clash spells plugin options as a semicolon separated k=v list; sing-box wants a map.
			options := map[string]any{}
			for _, part := range strings.Split(asString(proxy["plugin-opts"]), ";") {
				key, value, found := strings.Cut(strings.TrimSpace(part), "=")
				if found {
					options[key] = value
				}
			}
			outbound["plugin"] = plugin
			if len(options) > 0 {
				outbound["plugin_opts"] = encodePluginOptions(plugin, options)
			}
		}
		return outbound, nil

	case "vmess":
		outbound := map[string]any{
			"type": "vmess", "tag": name,
			"server": server, "server_port": port,
			"uuid":     asString(proxy["uuid"]),
			"security": defaultString(asString(proxy["cipher"]), "auto"),
			"alter_id": asInt(proxy["alterId"]),
		}
		putIfPresent(outbound, "tls", tlsOptions(proxy))
		putIfPresent(outbound, "transport", transportOptions(proxy))
		return outbound, nil

	case "vless":
		outbound := map[string]any{
			"type": "vless", "tag": name,
			"server": server, "server_port": port,
			"uuid": asString(proxy["uuid"]),
		}
		if flow := asString(proxy["flow"]); flow != "" {
			outbound["flow"] = flow
		}
		if proxy["reality-opts"] != nil {
			outbound["tls"] = realityOptions(proxy)
		} else {
			outbound["tls"] = tlsOptions(proxy)
		}
		putIfPresent(outbound, "transport", transportOptions(proxy))
		return outbound, nil

	case "trojan":
		outbound := map[string]any{
			"type": "trojan", "tag": name,
			"server": server, "server_port": port,
			"password": asString(proxy["password"]),
		}
		putIfPresent(outbound, "tls", tlsOptions(proxy))
		putIfPresent(outbound, "transport", transportOptions(proxy))
		return outbound, nil

	case "hysteria2", "hysteria":
		outbound := map[string]any{
			"type": "hysteria2", "tag": name,
			"server": server, "server_port": port,
			"password": asString(proxy["auth-str"]),
			"tls":      tlsOptions(proxy),
		}
		if password := asString(proxy["password"]); password != "" {
			outbound["password"] = password
		}
		if obfs := asString(proxy["obfs"]); obfs != "" {
			outbound["obfs"] = map[string]any{
				"type":     obfs,
				"password": asString(proxy["obfs-password"]),
			}
		}
		return outbound, nil

	case "tuic":
		return map[string]any{
			"type": "tuic", "tag": name,
			"server": server, "server_port": port,
			"uuid": asString(proxy["uuid"]), "password": asString(proxy["password"]),
			"congestion_control": asString(proxy["congestion-controller"]),
			"tls":                tlsOptions(proxy),
		}, nil

	case "socks5", "socks":
		outbound := map[string]any{
			"type": "socks", "tag": name,
			"server": server, "server_port": port,
			"version": "5",
		}
		if username := asString(proxy["username"]); username != "" {
			outbound["username"] = username
			outbound["password"] = asString(proxy["password"])
		}
		return outbound, nil

	case "http":
		outbound := map[string]any{
			"type": "http", "tag": name,
			"server": server, "server_port": port,
		}
		if username := asString(proxy["username"]); username != "" {
			outbound["username"] = username
			outbound["password"] = asString(proxy["password"])
		}
		if asBool(proxy["tls"]) {
			outbound["tls"] = tlsOptions(proxy)
		}
		return outbound, nil

	case "direct":
		return map[string]any{"type": "direct", "tag": name}, nil

	case "reject", "block":
		return map[string]any{"type": "block", "tag": name}, nil

	default:
		// Silently dropping a node would leave a group referencing a tag that does not exist, and
		// sing-box would refuse to start anyway. Fail with the name so the gap is actionable.
		return nil, fmt.Errorf("unsupported proxy type %q (node %q)", proxyType, name)
	}
}

// encodePluginOptions renders Clash's plugin options as the JSON form sing-box expects
// (obfs-local expects "obfs=..." style, v2ray-plugin expects JSON).
func encodePluginOptions(plugin string, options map[string]any) string {
	keys := make([]string, 0, len(options))
	for key := range options {
		keys = append(keys, key)
	}
	sort.Strings(keys)
	switch plugin {
	case "v2ray-plugin", "shadow-tls":
		parts := make([]string, 0, len(keys))
		for _, key := range keys {
			parts = append(parts, fmt.Sprintf("%q:%q", key, asString(options[key])))
		}
		return "{" + strings.Join(parts, ",") + "}"
	default:
		parts := make([]string, 0, len(keys))
		for _, key := range keys {
			if value := asString(options[key]); value != "" {
				parts = append(parts, key+"="+value)
			} else {
				parts = append(parts, key)
			}
		}
		return strings.Join(parts, ";")
	}
}

// tlsOptions maps Clash's flat tls/sni/skip-cert-verify flags onto sing-box's tls block.
func tlsOptions(proxy map[string]any) map[string]any {
	enabled := asBool(proxy["tls"]) || asBool(proxy["hysteria2-tls"])
	if !enabled {
		return nil
	}
	options := map[string]any{"enabled": true}
	if sni := asString(proxy["servername"]); sni != "" {
		options["server_name"] = sni
	} else if sni := asString(proxy["sni"]); sni != "" {
		options["server_name"] = sni
	}
	if asBool(proxy["skip-cert-verify"]) {
		options["insecure"] = true
	}
	if alpn := asStringList(proxy["alpn"]); len(alpn) > 0 {
		options["alpn"] = alpn
	}
	if fingerprint := asString(proxy["client-fingerprint"]); fingerprint != "" {
		options["utls"] = map[string]any{"enabled": true, "fingerprint": fingerprint}
	}
	return options
}

func realityOptions(proxy map[string]any) map[string]any {
	options := tlsOptions(proxy)
	if options == nil {
		options = map[string]any{"enabled": true}
	}
	reality := asMap(proxy["reality-opts"])
	options["reality"] = map[string]any{
		"enabled":    true,
		"public_key": asString(reality["public-key"]),
		"short_id":   asString(reality["short-id"]),
	}
	return options
}

// transportOptions maps Clash's network/ws-opts/grpc-opts onto sing-box's transport block.
func transportOptions(proxy map[string]any) map[string]any {
	network := strings.ToLower(asString(proxy["network"]))
	switch network {
	case "ws":
		ws := asMap(proxy["ws-opts"])
		transport := map[string]any{"type": "ws"}
		if path := asString(ws["path"]); path != "" {
			transport["path"] = path
		}
		if headers := asMap(ws["headers"]); headers != nil {
			transport["headers"] = headers
		}
		return transport
	case "grpc":
		grpc := asMap(proxy["grpc-opts"])
		return map[string]any{
			"type":         "grpc",
			"service_name": asString(grpc["grpc-service-name"]),
		}
	case "h2", "http":
		return map[string]any{"type": "http"}
	case "":
		return nil
	default:
		// httpupgrade and quic exist in sing-box too, but their option shapes differ enough that a
		// guess would be worse than a visible warning.
		return nil
	}
}

func (c *converter) convertGroups(raw any, proxies []map[string]any) ([]map[string]any, error) {
	entries := asList(raw)
	groups := make([]map[string]any, 0, len(entries))
	for _, entry := range entries {
		group := asMap(entry)
		if group == nil {
			continue
		}
		name := asString(group["name"])
		groupType := strings.ToLower(asString(group["type"]))
		if name == "" {
			return nil, fmt.Errorf("proxy group without a name")
		}

		members := make([]string, 0)
		for _, member := range asStringList(group["proxies"]) {
			members = append(members, c.resolveMember(member))
		}
		if len(members) == 0 {
			c.warnf("group %q has no members and was dropped", name)
			continue
		}

		switch groupType {
		case "select":
			groups = append(groups, map[string]any{
				"type": "selector", "tag": name,
				"outbounds": members, "default": members[0],
			})
		case "url-test", "url-test-fallback":
			groups = append(groups, map[string]any{
				"type": "urltest", "tag": name,
				"outbounds": members,
				"url":       defaultString(asString(group["url"]), "https://www.gstatic.com/generate_204"),
				"interval":  clashDuration(asString(group["interval"]), "5m"),
				"tolerance": asInt(group["tolerance"]),
			})
		case "fallback", "load-balance":
			// sing-box has neither a fallback nor a load-balance group. urltest is the closest
			// behaviour (it also picks the healthiest member) but it is not the same thing, so the
			// difference is reported instead of hidden.
			c.warnf("group %q is %s; sing-box only has selector/urltest, mapped to urltest", name, groupType)
			groups = append(groups, map[string]any{
				"type": "urltest", "tag": name,
				"outbounds": members,
				"url":       "https://www.gstatic.com/generate_204",
				"interval":  "5m",
			})
		default:
			return nil, fmt.Errorf("unsupported proxy group type %q (group %q)", groupType, name)
		}
	}
	return groups, nil
}

// resolveMember maps Clash's DIRECT/REJECT pseudo members onto the implicit outbounds.
func (c *converter) resolveMember(member string) string {
	switch strings.ToUpper(member) {
	case "DIRECT":
		c.implicit["direct"] = true
		return "direct"
	case "REJECT", "REJECT-DROP":
		c.implicit["block"] = true
		return "block"
	default:
		return member
	}
}

// ensureBuiltins appends the outbounds that were referenced but never declared, so every tag in a
// group or rule resolves.
func (c *converter) ensureBuiltins() {
	existing := map[string]bool{}
	for _, outbound := range c.outbounds {
		existing[asString(outbound["tag"])] = true
	}
	if c.implicit["direct"] && !existing["direct"] {
		c.outbounds = append(c.outbounds, map[string]any{"type": "direct", "tag": "direct"})
		existing["direct"] = true
	}
	if c.implicit["block"] && !existing["block"] {
		c.outbounds = append(c.outbounds, map[string]any{"type": "block", "tag": "block"})
	}
}

func (c *converter) convertRoute(raw any, dnsTag string) (map[string]any, error) {
	rules := make([]any, 0)
	final := "direct"
	if len(c.outbounds) > 0 {
		final = asString(c.outbounds[0]["tag"])
	}

	for _, entry := range asList(raw) {
		line := strings.TrimSpace(asString(entry))
		if line == "" {
			continue
		}
		parts := strings.Split(line, ",")
		for index := range parts {
			parts[index] = strings.TrimSpace(parts[index])
		}
		kind := strings.ToUpper(parts[0])

		if kind == "MATCH" {
			if len(parts) > 1 {
				final = c.resolveMember(parts[1])
			}
			continue
		}
		if len(parts) < 2 {
			c.warnf("rule %q has no target and was dropped", line)
			continue
		}

		target := c.resolveMember(parts[len(parts)-1])
		payload := strings.Join(parts[1:len(parts)-1], ",")
		// Options such as no-resolve are not route fields in sing-box; they are close enough to
		// "do not resolve" that ignoring them is safe, but the profile should know.
		for _, part := range strings.Split(payload, ",") {
			switch strings.ToLower(strings.TrimSpace(part)) {
			case "no-resolve", "src":
				payload = strings.Replace(payload, part, "", 1)
				c.warnf("rule option %q on %q has no sing-box equivalent; ignored", part, kind)
			}
		}
		payload = strings.Trim(payload, ",")

		rule := map[string]any{"outbound": target}
		switch kind {
		case "DOMAIN":
			rule["domain"] = []string{payload}
		case "DOMAIN-SUFFIX":
			rule["domain_suffix"] = []string{strings.TrimPrefix(payload, ".")}
		case "DOMAIN-KEYWORD":
			rule["domain_keyword"] = []string{payload}
		case "IP-CIDR", "IP-CIDR6":
			rule["ip_cidr"] = []string{payload}
		case "SRC-IP-CIDR":
			rule["source_ip_cidr"] = []string{payload}
		case "DST-PORT":
			ports, ranges := splitPorts(payload)
			if len(ports) > 0 {
				rule["port"] = ports
			}
			if len(ranges) > 0 {
				rule["port_range"] = ranges
			}
		case "SRC-PORT":
			ports, ranges := splitPorts(payload)
			if len(ports) > 0 {
				rule["source_port"] = ports
			}
			if len(ranges) > 0 {
				rule["source_port_range"] = ranges
			}
		case "PROCESS-NAME":
			// Android reports the package name; Clash profiles usually carry the executable name,
			// so both forms are emitted.
			rule["process_name"] = []string{payload}
		case "NETWORK":
			rule["network"] = []string{strings.ToLower(payload)}
		case "GEOSITE":
			set := c.geoSiteSet(payload)
			rule["rule_set"] = []string{set}
		case "GEOIP":
			set := c.geoIPSet(payload)
			rule["rule_set"] = []string{set}
		case "RULE-SET":
			if set, ok := c.ruleProviderSet(payload); ok {
				rule["rule_set"] = []string{set}
			} else {
				c.warnf("rule-set %q was dropped: only sing-box rule-set sources are supported", payload)
				continue
			}
		case "PROCESS-PATH":
			rule["process_path"] = []string{payload}
		default:
			c.warnf("rule type %q is not supported by sing-box and was dropped: %q", kind, line)
			continue
		}
		rules = append(rules, rule)
	}

	route := map[string]any{
		"rules": rules,
		"final": final,
	}
	if dnsTag != "" {
		route["default_domain_resolver"] = dnsTag
	}

	if c.options.EnableModeSwitching {
		// sing-box derives mode-list from clash_mode rules alone. Without these two the app's mode
		// switch returns 204 and does nothing.
		//
		// Global means "everything through the proxy", not "whatever MATCH says": a profile whose
		// catch-all is DIRECT would otherwise make Global a no-op.
		route["rules"] = append([]any{
			map[string]any{"clash_mode": "Global", "outbound": c.globalModeTarget(final)},
			map[string]any{"clash_mode": "Direct", "outbound": "direct"},
		}, rules...)
		c.implicit["direct"] = true
	}
	return route, nil
}

// globalModeTarget picks the first selectable group (or failing that, the first real proxy), since
// that is what a user means by "global mode".
func (c *converter) globalModeTarget(fallback string) string {
	for _, outbound := range c.outbounds {
		if asString(outbound["type"]) == "selector" {
			return asString(outbound["tag"])
		}
	}
	for _, outbound := range c.outbounds {
		switch asString(outbound["type"]) {
		case "direct", "block", "selector", "urltest":
			continue
		default:
			return asString(outbound["tag"])
		}
	}
	return fallback
}

// geoIPSet and geoSiteSet point at sing-box's official rule-set mirrors: sing-box 1.12 has no
// GEOIP/GEOSITE rule, only rule_set.
func (c *converter) geoIPSet(code string) string {
	code = strings.ToLower(defaultString(code, "cn"))
	tag := "geoip-" + code
	c.addRuleSet(tag, []string{
		"https://raw.githubusercontent.com/SagerNet/sing-geoip/rule-set/geoip-" + code + ".srs",
	}, "binary")
	return tag
}

func (c *converter) geoSiteSet(code string) string {
	code = strings.ToLower(code)
	tag := "geosite-" + code
	c.addRuleSet(tag, []string{
		"https://raw.githubusercontent.com/SagerNet/sing-geosite/rule-set/geosite-" + code + ".srs",
	}, "binary")
	return tag
}

// ruleProviderSet maps a Clash rule-provider onto a route.rule_set only when the source is already
// a sing-box rule-set; Clash YAML rule providers need an offline conversion step that does not
// exist yet, and pretending otherwise would silently disable rules.
func (c *converter) ruleProviderSet(name string) (string, bool) {
	// The provider table lives on the root document, which the route converter does not see; the
	// caller fills c.providers before this point.
	provider, ok := c.providers[name]
	if !ok {
		return "", false
	}
	url := asString(provider["url"])
	path := asString(provider["path"])
	if strings.HasSuffix(url, ".srs") || strings.HasSuffix(path, ".srs") {
		tag := "rule-provider-" + name
		if url != "" {
			c.addRuleSet(tag, []string{url}, "binary")
		}
		return tag, url != ""
	}
	return "", false
}

func (c *converter) addRuleSet(tag string, urls []string, format string) {
	if _, exists := c.ruleSets[tag]; exists {
		return
	}
	c.ruleSets[tag] = map[string]any{
		"tag": tag, "type": "remote", "format": format,
		"url": urls[0],
	}
	c.orderedRuleSets = append(c.orderedRuleSets, tag)
}

func (c *converter) ruleSetList() []any {
	sets := make([]any, 0, len(c.orderedRuleSets))
	for _, tag := range c.orderedRuleSets {
		sets = append(sets, c.ruleSets[tag])
	}
	return sets
}

// convertDNS maps Clash's nameserver list onto sing-box DNS servers. fake-ip needs a matching dns
// rule and a fakeip server, which this slice does not emit yet.
func (c *converter) convertDNS(raw any) (map[string]any, bool) {
	dns := asMap(raw)
	if dns == nil {
		return nil, false
	}
	servers := make([]any, 0)
	seen := map[string]bool{}

	for _, entry := range asStringList(dns["nameserver"]) {
		server, tag, ok := c.dnsServer(entry)
		if !ok || seen[tag] {
			continue
		}
		seen[tag] = true
		servers = append(servers, server)
	}
	if len(servers) == 0 {
		return nil, false
	}
	// A server addressed by domain name needs an IP-based resolver to bootstrap itself; sing-box
	// refuses to start otherwise ("missing domain resolver for domain server address").
	bootstrap := ""
	for _, entry := range servers {
		server := entry.(map[string]any)
		if net.ParseIP(asString(server["server"])) != nil {
			bootstrap = asString(server["tag"])
			break
		}
	}
	if bootstrap == "" {
		c.warnf("dns has no plain-IP nameserver; DoH/DoT servers cannot be bootstrapped")
	} else {
		for _, entry := range servers {
			server := entry.(map[string]any)
			if net.ParseIP(asString(server["server"])) == nil {
				server["domain_resolver"] = bootstrap
			}
		}
	}
	if strings.EqualFold(asString(dns["enhanced-mode"]), "fake-ip") {
		c.warnf("dns.enhanced-mode fake-ip is not emitted yet; DNS will answer with real addresses")
	}
	if len(asStringList(dns["fallback"])) > 0 {
		c.warnf("dns.fallback is ignored: sing-box resolves through the single nameserver list")
	}
	return map[string]any{
		"servers": servers,
		"final":   asString(servers[0].(map[string]any)["tag"]),
	}, true
}

// defaultNameServers mirrors the mihomo runtime patch, minus the two resolvers that are routinely
// unreachable from mainland networks.
var defaultNameServers = []string{"223.5.5.5", "119.29.29.29"}

func (c *converter) defaultDNS() map[string]any {
	servers := make([]any, 0, len(defaultNameServers))
	firstTag := ""
	for _, entry := range defaultNameServers {
		server, tag, ok := c.dnsServer(entry)
		if !ok {
			continue
		}
		if firstTag == "" {
			firstTag = tag
		}
		servers = append(servers, server)
	}
	return map[string]any{
		"servers": servers,
		"final":   firstTag,
	}
}

func (c *converter) dnsServer(entry string) (map[string]any, string, bool) {
	entry = strings.TrimSpace(entry)
	switch {
	case entry == "" || strings.HasPrefix(entry, "dhcp://"):
		return nil, "", false
	case strings.HasPrefix(entry, "https://"):
		host, path := splitHostPath(entry)
		return map[string]any{
			"type": "https", "tag": "dns-" + sanitizeTag(host),
			"server": host, "path": path,
		}, "dns-" + sanitizeTag(host), true
	case strings.HasPrefix(entry, "http://"):
		host, path := splitHostPath(entry)
		return map[string]any{
			"type": "http", "tag": "dns-" + sanitizeTag(host),
			"server": host, "path": path,
		}, "dns-" + sanitizeTag(host), true
	case strings.HasPrefix(entry, "tls://"):
		host := strings.TrimPrefix(entry, "tls://")
		return map[string]any{
			"type": "tls", "tag": "dns-" + sanitizeTag(host), "server": host,
		}, "dns-" + sanitizeTag(host), true
	case strings.HasPrefix(entry, "quic://"):
		host := strings.TrimPrefix(entry, "quic://")
		return map[string]any{
			"type": "quic", "tag": "dns-" + sanitizeTag(host), "server": host,
		}, "dns-" + sanitizeTag(host), true
	default:
		// Plain addresses may carry a port and a #hint suffix.
		address, _, _ := strings.Cut(entry, "#")
		return map[string]any{
			"type": "udp", "tag": "dns-" + sanitizeTag(address), "server": address,
		}, "dns-" + sanitizeTag(address), true
	}
}

// convertTun builds the tun inbound. The launcher owns routing, so auto_route stays off.
func (c *converter) convertTun(raw any) (map[string]any, bool) {
	tun := asMap(raw)
	if tun == nil && c.options.Gateway == "" {
		return nil, false
	}
	address := c.options.Gateway
	if address == "" && tun != nil {
		address = "172.19.0.1/30"
	}
	inbound := map[string]any{
		"type": "tun", "tag": "tun-in",
		"address":    []string{address},
		"auto_route": false,
		"stack":      "gvisor",
	}
	if c.options.TunName != "" {
		inbound["interface_name"] = c.options.TunName
	}
	if c.options.MTU > 0 {
		inbound["mtu"] = c.options.MTU
	}
	if tun != nil {
		if stack := strings.ToLower(asString(tun["stack"])); stack == "system" {
			inbound["stack"] = "system"
		} else if stack != "" && stack != "gvisor" && stack != "mixed" {
			c.warnf("tun.stack %q is unknown; gvisor is used", stack)
		}
		if asBool(tun["strict-route"]) {
			inbound["strict_route"] = true
		}
		if mtu := asInt(tun["mtu"]); mtu > 0 && c.options.MTU == 0 {
			inbound["mtu"] = mtu
		}
	}
	return inbound, true
}

func (c *converter) convertLocalInbound(document map[string]any) (map[string]any, bool) {
	port := asInt(document["mixed-port"])
	kind := "mixed"
	if port == 0 {
		port = asInt(document["socks-port"])
		kind = "socks"
	}
	if port == 0 {
		return nil, false
	}
	inbound := map[string]any{
		"type": kind, "tag": kind + "-in",
		"listen": "127.0.0.1", "listen_port": port,
	}
	if kind == "socks" {
		inbound["version"] = "5"
	}
	return inbound, true
}

func (c *converter) convertProxyProviders(raw any) ([]any, bool) {
	providers := asMap(raw)
	if providers == nil {
		return nil, false
	}
	// Expanded at compile time by the caller in a later slice; surfacing the keys keeps the
	// provider-backed nodes from vanishing silently.
	c.warnf("proxy-providers (%d) are not expanded yet; provider nodes will be missing", len(providers))
	return nil, false
}

func clashLogLevel(raw string) string {
	switch strings.ToLower(raw) {
	case "debug", "info", "warn", "error", "silent":
		return strings.ToLower(raw)
	default:
		return "info"
	}
}

// clashDuration accepts Clash's "<n><unit>" durations and sing-box's Go-style strings.
func clashDuration(raw string, fallback string) string {
	if raw == "" {
		return fallback
	}
	raw = strings.TrimSpace(raw)
	if _, err := strconv.Atoi(raw); err == nil {
		return raw + "s"
	}
	return raw
}

// splitPorts turns Clash's "80/443" and "1000-2000" port lists into sing-box's numeric port list plus
// its range list; the two are separate fields, and a string in the numeric list is a decode error.
func splitPorts(payload string) ([]int, []string) {
	var ports []int
	var ranges []string
	for _, part := range strings.FieldsFunc(payload, func(r rune) bool { return r == '/' || r == ',' }) {
		part = strings.TrimSpace(part)
		if part == "" {
			continue
		}
		if strings.Contains(part, "-") {
			ranges = append(ranges, part)
			continue
		}
		if value, err := strconv.Atoi(part); err == nil {
			ports = append(ports, value)
		}
	}
	return ports, ranges
}

func splitHostPath(url string) (string, string) {
	rest := url
	for _, prefix := range []string{"https://", "http://"} {
		rest = strings.TrimPrefix(rest, prefix)
	}
	host, path, found := strings.Cut(rest, "/")
	if !found {
		return host, "/"
	}
	return host, "/" + path
}

func sanitizeTag(value string) string {
	replacer := strings.NewReplacer(":", "-", "/", "-", ".", "-", "[", "", "]", "", " ", "")
	return replacer.Replace(value)
}

func putIfPresent(target map[string]any, key string, value any) {
	if value == nil {
		return
	}
	if typed, ok := value.(map[string]any); ok && len(typed) == 0 {
		return
	}
	target[key] = value
}

func defaultString(value string, fallback string) string {
	if value == "" {
		return fallback
	}
	return value
}

// --- loose yaml accessors ---------------------------------------------------------------------

func asMap(value any) map[string]any {
	switch typed := value.(type) {
	case map[string]any:
		return typed
	case map[any]any:
		converted := make(map[string]any, len(typed))
		for key, entry := range typed {
			converted[fmt.Sprint(key)] = entry
		}
		return converted
	default:
		return nil
	}
}

func asList(value any) []any {
	switch typed := value.(type) {
	case []any:
		return typed
	default:
		return nil
	}
}

func asString(value any) string {
	switch typed := value.(type) {
	case string:
		return typed
	case int:
		return strconv.Itoa(typed)
	case int64:
		return strconv.FormatInt(typed, 10)
	case float64:
		if typed == float64(int64(typed)) {
			return strconv.FormatInt(int64(typed), 10)
		}
		return strconv.FormatFloat(typed, 'f', -1, 64)
	case bool:
		return strconv.FormatBool(typed)
	default:
		return ""
	}
}

func asInt(value any) int {
	switch typed := value.(type) {
	case int:
		return typed
	case int64:
		return int(typed)
	case float64:
		return int(typed)
	case string:
		parsed, err := strconv.Atoi(strings.TrimSpace(typed))
		if err != nil {
			return 0
		}
		return parsed
	default:
		return 0
	}
}

func asBool(value any) bool {
	switch typed := value.(type) {
	case bool:
		return typed
	case string:
		return strings.EqualFold(typed, "true")
	default:
		return false
	}
}

func asStringList(value any) []string {
	items := asList(value)
	output := make([]string, 0, len(items))
	for _, item := range items {
		if text := asString(item); text != "" {
			output = append(output, text)
		}
	}
	return output
}
