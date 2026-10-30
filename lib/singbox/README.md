# lib/singbox — YumeBox 的 sing-box 核心（P0 spike）

把 YumeBox 的内核从 mihomo 换成 sing-box 的第一块地基。**它保留与 Kotlin 侧完全一致的启动契约**，只替换引擎：

- 启动参数与 `lib/native/go/native/entry.go` 对齐（`--home --controller --config --mode --stack --channel --sdk --test`），新增 `--secret`、`--tun-name`、`--mtu`。
- 通过 `SOCK_SEQPACKET` socketpair 接收**编译后的配置 + VpnService 的 TUN fd**（`SCM_RIGHTS`），并复用同一条通道做 `protect` 与 socket-owner RPC。
- 仍然导出 `SingboxMain(argc, argv, channel)`，由现有 PIE loader `dlopen` 后调用。
- sing-box 的 clash API 只监听 TCP，而 App 连的是 unix socket：本进程内起一个 unix→TCP 转发器（`controller.go`），**Kotlin 侧无需改动**。

## 状态

| 项 | 状态 |
|---|---|
| Go 模块可编译（android/arm64，CGO 关闭，全量 tag） | ✅ 本地已验证 |
| 桌面端配置校验（`option` 解码 + `box.New`） | ✅ 本地已验证，见下方 |
| cgo `c-shared` 构建 | ✅ 本机 NDK r27c 构建通过，`SingboxMain` 已导出，剥符号后 27.9MB，只依赖 `liblog/libdl/libc` |
| 真机加载 `.so` 并跑通 | ✅ dlopen + `dlsym` 调用 `SingboxMain`，`sing-box started (0.66s)`，SOCKS 出网 204 |
| 真机 TUN fd 透传 | ✅ **已验证**：spike APK 的 VpnService 把 fd 交给核心，`platform.OpenTun` 建栈，`adb shell curl` 经 tun 出网 200/204 |
| 默认网卡监控 | ⚠️ 空实现（P1 接 ConnectivityManager RPC） |

已上机验证（SM-G9810 / Android 13）：`/configs` `/proxies` `/group` `/connections` `/traffic` `/rules` 读、`PUT /proxies/{name}` 切换、`PATCH /configs` 切模式、SOCKS5 出网。
未通：`PATCH /rules/disable`（404）、`/providers/proxies`（空实现）、`/traffic` 无累计值。


## 本地校验（Windows/macOS/Linux 都能跑）

```bash
cd lib/singbox
go run -tags "with_clash_api with_gvisor" ./cmd/check testdata-spike.json
# testdata-spike.json is a valid sing-box config

# 完整测试（带 utls/quic，否则真实配置里的 REALITY 与 Hysteria2 出站无法构建）
go test -tags "with_clash_api with_gvisor with_utls with_quic" ./...
```

`cmd/check` 走的是 Android 核心里同一条代码路径（`include.Context` → `option.Options` 解码 → `box.New`），而且**接受 Clash YAML**（自动转换后再校验），所以它就是转换器的合并门禁。

## Clash 转换器（`internal/clash`）

Rust 编译器不动：它继续输出 Clash YAML，只有最后一公里在这里翻译成 sing-box options。`core.Prepare` 按首字符判断 JSON（直通）还是 YAML（转换）。

已覆盖：

- 协议：ss（含 obfs/v2ray-plugin）、vmess（ws/grpc/tls）、vless（含 reality）、trojan、hysteria2、tuic、socks5、http
- 组：`select` → selector，`url-test` → urltest；`fallback` / `load-balance` 降级为 urltest 并**明确告警**
- 规则：DOMAIN / DOMAIN-SUFFIX / DOMAIN-KEYWORD / IP-CIDR / SRC-IP-CIDR / DST-PORT / SRC-PORT / PROCESS-NAME / PROCESS-PATH / NETWORK / GEOIP / GEOSITE / RULE-SET / MATCH；端口支持 `80/443` 与 `1000-2000`
- 逻辑规则：`AND`/`OR` → sing-box logical rule；`NOT` → `invert: true`（sing-box 没有 not 模式）
- GEOIP/GEOSITE 自动生成指向 `sing-geoip`/`sing-geosite` 的 `route.rule_set`
- DNS：udp/tls/https/quic，DoH/DoT 自动挂 `domain_resolver` 引导；**没有 dns 块时注入 223.5.5.5 / 119.29.29.29**（否则 Android 上退回 `[::1]:53`，全部域名出站失败）
- 模式切换：自动生成 `clash_mode: Global/Direct` 两条规则（Global 指向第一个 selector）
- 非 VPN 运行（无 gateway）时，把 `mixed-port` / `socks-port` 变成本地入站

已知不支持（会告警或报错，不会静默丢）：

- `snell`、`wireguard` 等未列出的代理类型 → **直接报错并点名节点**
- `proxy-providers` 暂不展开（节点会缺，明确告警）
- Clash YAML 格式的 `rule-providers` 需要离线转成 `.srs`，否则 RULE-SET 规则被丢弃并告警
- `dns.enhanced-mode: fake-ip`、`dns.fallback`、`no-resolve` 暂不映射

> **build tag 是硬要求**：不带 `with_clash_api`，`box.New` 直接报 `clash api is not included in this build`；不带 `with_gvisor`，`stack: gvisor` 的 tun inbound 无法启动。

## 构建 `.so`

```bash
ANDROID_NDK_HOME=/path/to/ndk scripts/singbox/build-core.sh arm64
# -> build/native/singbox/arm64-v8a/libsingboxcore.so
```

CI 上手动跑：Actions → **Build sing-box core (spike)** → Run workflow。产物是未压缩的 `.so`，可直接 `llvm-readelf -d` 看依赖、`nm -D` 看 `SingboxMain` 是否导出。

## 真实配置回归夹具

`testdata/real-world-singbox.json` 是一份生产环境的 sing-box 配置（脱敏后：89 个出站、17 个 remote binary rule_set、fakeip DNS、嵌套逻辑规则）。`internal/core/payload_test.go` 用它做 JSON 直通回归——引擎版本一升级，先拿它撞一遍 `box.New`。

## 真机验证 TUN fd —— 已用 C 方案跑通

`spike/vpnhost/` 是一个 30 行的宿（不参与正式构建，用 `aapt2/javac/d8` 手搓 APK）：

1. `VpnService.Builder` 建 tun0（172.19.0.1/30 + 默认路由 + DNS 172.19.0.2），`addDisallowedApplication` 把自己排除出隧道；
2. 起一个线程调用 `CoreBridge.run(...)`（30 行 JNI 胶水，`dlopen` + `dlsym("SingboxMain")`）；
3. 核心以 `--tun-fd <fd>` 继承 fd，`platform.OpenTun` 建 gvisor 栈。

实测结果（SM-G9810 / Android 13）：

```
Vpn  : Established by com.yumebox.spike on tun0
YumeBoxSpike: tun established, fd=104
→ adb shell curl http://www.baidu.com            http=200
→ adb shell curl https://www.gstatic.com/...     http=204
/connections: type=tun/tun-in, host=alt3-mtalk.google.com, downloadTotal=18849
```

**为什么是"同进程"而不是子进程**：Android 的 Java `ProcessBuilder` 用 `POSIX_SPAWN_CLOEXEC_DEFAULT` 派生，任意 fd 都过不去——所以 YumeBox 才自带 JNI fork/exec（`NativeProcess.kt`）。正式迁移直接复用它即可，spike 里没必要重写一遍。

**踩到的坑（写进转换器的第 4 条）**：规则顺序必须是先 `{"action":"sniff"}` 再 `{"protocol":"dns","action":"hijack-dns"}`。反过来，协议尚未识别，DNS 包会落到 `final` 出站，指向 tun 自己的 172.19.0.2:53，形成回环、所有域名解析失败。

## P1 必须补齐的洞（按风险排序）

1. **默认网卡监控**：现在是空实现，靠 `VpnService.protect` 走通。真机上要接 Kotlin `ConnectivityManager` 的默认网络/计费状态，否则网络切换（Wi-Fi↔蜂窝）后的行为不可控。
2. **`process.Searcher` 协议**：sing-box 传的是 `netip.AddrPort` 四元组，现有 RPC 是 `"<proto>\t<src>\t<dst>"`（字符串形式一致，但 uid→包名反查缺失）。`package_name`/`process_name` 规则是否等价要在真机确认。
3. **tun 名字**：现在靠 `--tun-name` 传入，缺省时由 config 决定。VpnService 建立后接口名建议由 Kotlin 直接给。
4. **证书**：`SystemCertificates` 直接读 `/system/etc/security/cacerts`，Android 14 之后用户 CA 的存放位置有差异，要复核。
5. **`--test` 契约**：`ProfileConfigTester.kt` 现在匹配 mihomo 的输出文案，换成 sing-box 后要同步改。
6. **`--mode ebpf`**：sing-box 没有对应 inbound，这个模式应当直接拒绝启动，而不是静默降级。

## 文件地图

| 文件 | 作用 |
|---|---|
| `main.go` | c-shared 的空 `main()`（`-buildmode=c-shared` 下由 loader 进入） |
| `main_android.go` | `//export SingboxMain`，参数从 C 转 Go |
| `internal/core/options.go` | 启动参数，与 mihomo 版对齐 |
| `internal/core/run.go` | 读配置/fd → `box.New` → `Start` → 等信号 → `Close`；`Check()` 供桌面校验 |
| `internal/core/channel_unix.go` | socketpair：配置 + TUN fd + `protect`/socket-owner RPC |
| `internal/core/apply.go` | 运行时注入：clash_api 端口/secret、日志、tun stack（替代 mihomo 的 `patch/runtime.rs`） |
| `internal/core/controller.go` | unix socket → 127.0.0.1 clash_api 的转发 |
| `internal/core/platform.go` | 无 JVM 的 `platform.Interface`：OpenTun 用继承的 fd |
| `internal/core/monitor.go` | `DefaultInterfaceMonitor` 占位实现（P1 换真实现） |
| `cmd/check` | 桌面配置校验器 |
