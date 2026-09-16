//! End-to-end `compile_request` behavior: override loading warnings and the runtime patches
//! applied after the override chain.

mod common;

use serde_json::Value as JsonValue;

use r#override::model::RunMode;

use common::{override_spec, temp_dir, test_request};

#[test]
fn compile_request_emits_warning_for_empty_override_file() {
    let temp_dir = temp_dir("empty-override-test");

    let profile_path = temp_dir.join("profile.yaml");
    std::fs::write(&profile_path, "mode: rule\n").expect("write profile yaml");
    let empty_override_path = temp_dir.join("empty.js");
    std::fs::write(&empty_override_path, "  \n").expect("write empty override");

    let mut request = test_request(&temp_dir, &profile_path);
    request.overrides = vec![override_spec(&empty_override_path, "js")];

    let result =
        r#override::compile_request(request, false).expect("compile request should succeed");
    assert_eq!(result.warnings.len(), 1);
    assert!(result.warnings[0].contains("skip empty override file"));

    let _ = std::fs::remove_dir_all(&temp_dir);
}

#[test]
fn tun_override_on_profile_without_dns_backfills_nameserver() {
    let temp_dir = temp_dir("compiler-test-tun-dns");

    // Subscription-style profile with NO dns block.
    let profile_path = temp_dir.join("config.yaml");
    std::fs::write(&profile_path, "mode: rule\nproxies: []\n").expect("write profile yaml");

    // The built-in Tun override: enables dns (mode choice) and the tun block.
    let override_path = temp_dir.join("__tun_override__.yaml");
    std::fs::write(
        &override_path,
        "tun:\n  enable: true\n  device: Yume\n  auto-route: true\ndns:\n  enable: true\n  enhanced-mode: redir-host\n",
    )
    .expect("write tun override");

    let mut request = test_request(&temp_dir, &profile_path);
    request.run_mode = RunMode::Tun;
    request.overrides = vec![override_spec(&override_path, "yaml")];

    let result = r#override::compile_request(request, false).expect("compile should succeed");
    assert!(result.success, "compile failed: {:?}", result.error);
    let root: JsonValue = serde_yaml::from_str(&result.final_yaml).expect("parse final yaml");

    // The tun block must survive in Tun run mode (no force-off).
    let tun = root
        .get("tun")
        .and_then(JsonValue::as_object)
        .expect("tun block");
    assert_eq!(tun.get("enable"), Some(&JsonValue::Bool(true)));

    // dns.enable=true with an empty nameserver would make mihomo refuse the whole config
    // ("NameServer cannot be empty"); the patch must backfill the defaults.
    let dns = root
        .get("dns")
        .and_then(JsonValue::as_object)
        .expect("dns block");
    assert_eq!(dns.get("enable"), Some(&JsonValue::Bool(true)));
    let nameserver = dns
        .get("nameserver")
        .and_then(JsonValue::as_array)
        .expect("nameserver array");
    assert!(!nameserver.is_empty(), "nameserver must be backfilled");
    let default_nameserver = dns
        .get("default-nameserver")
        .and_then(JsonValue::as_array)
        .expect("default-nameserver array");
    assert!(!default_nameserver.is_empty());

    let _ = std::fs::remove_dir_all(&temp_dir);
}

#[test]
fn tun_override_keeps_profile_nameservers_intact() {
    let temp_dir = temp_dir("compiler-test-tun-dns-keep");

    let profile_path = temp_dir.join("config.yaml");
    std::fs::write(
        &profile_path,
        "mode: rule\ndns:\n  enable: false\n  nameserver:\n    - 1.1.1.1\n",
    )
    .expect("write profile yaml");

    let override_path = temp_dir.join("__tun_override__.yaml");
    std::fs::write(
        &override_path,
        "dns:\n  enable: true\n  enhanced-mode: redir-host\n",
    )
    .expect("write tun override");

    let mut request = test_request(&temp_dir, &profile_path);
    request.run_mode = RunMode::Tun;
    request.overrides = vec![override_spec(&override_path, "yaml")];

    let result = r#override::compile_request(request, false).expect("compile should succeed");
    assert!(result.success, "compile failed: {:?}", result.error);
    let root: JsonValue = serde_yaml::from_str(&result.final_yaml).expect("parse final yaml");

    // The profile's own nameservers must win over the backfill defaults.
    let dns = root
        .get("dns")
        .and_then(JsonValue::as_object)
        .expect("dns block");
    let nameserver = dns
        .get("nameserver")
        .and_then(JsonValue::as_array)
        .expect("nameserver array");
    assert_eq!(
        nameserver,
        &vec![JsonValue::String("1.1.1.1".to_string())],
        "profile nameservers must be kept, not replaced"
    );

    let _ = std::fs::remove_dir_all(&temp_dir);
}

#[test]
fn vpn_runtime_patch_strips_tun_stack_before_parse() {
    let temp_dir = temp_dir("compiler-test-vpn-strip-stack");
    let profile_path = temp_dir.join("config.yaml");
    std::fs::write(
        &profile_path,
        "mode: rule\ntun:\n  enable: true\n  stack: mips\n  auto-route: true\n  device: profile\n",
    )
    .expect("write profile yaml");

    let mut request = test_request(&temp_dir, &profile_path);
    request.run_mode = RunMode::Vpn;

    let result = r#override::compile_request(request, false).expect("compile should succeed");
    assert!(result.success, "compile failed: {:?}", result.error);
    let root: JsonValue = serde_yaml::from_str(&result.final_yaml).expect("parse final yaml");
    let tun = root
        .get("tun")
        .and_then(JsonValue::as_object)
        .expect("tun block");
    assert_eq!(tun.get("enable"), Some(&JsonValue::Bool(false)));
    assert_eq!(tun.get("auto-route"), Some(&JsonValue::Bool(false)));
    assert!(
        tun.get("stack").is_none(),
        "vpn compiled yaml must not carry tun.stack; the launcher applies it after parse"
    );
    assert_eq!(
        tun.get("device"),
        Some(&JsonValue::String("profile".to_string()))
    );

    let _ = std::fs::remove_dir_all(&temp_dir);
}

#[test]
fn ebpf_mode_keeps_profile_config_authoritative() {
    let temp_dir = temp_dir("compiler-test-ebpf-profile");
    let profile_path = temp_dir.join("config.yaml");
    std::fs::write(
        &profile_path,
        "mode: rule\ntun:\n  enable: true\n  auto-route: true\n",
    )
    .expect("write profile yaml");

    let mut request = test_request(&temp_dir, &profile_path);
    request.run_mode = RunMode::Ebpf;
    let result = r#override::compile_request(request, false).expect("compile should succeed");
    assert!(result.success, "compile failed: {:?}", result.error);
    let root: JsonValue = serde_yaml::from_str(&result.final_yaml).expect("parse final yaml");
    let tun = root
        .get("tun")
        .and_then(JsonValue::as_object)
        .expect("tun block");
    assert_eq!(tun.get("enable"), Some(&JsonValue::Bool(false)));
    assert_eq!(tun.get("auto-route"), Some(&JsonValue::Bool(false)));
    assert_eq!(
        tun.get("auto-detect-interface"),
        Some(&JsonValue::Bool(false))
    );
    assert_eq!(tun.get("auto-redirect"), Some(&JsonValue::Bool(false)));
    assert!(root.get("interface-name").is_none());
    assert!(root.get("routing-mark").is_none());

    let _ = std::fs::remove_dir_all(&temp_dir);
}

#[test]
fn ebpf_cn_override_preserves_profile_providers_and_listeners() {
    let temp_dir = temp_dir("compiler-test-ebpf-cn-merge");
    let profile_path = temp_dir.join("config.yaml");
    std::fs::write(
        &profile_path,
        r#"
mode: rule
listeners:
  - name: existing-in
    type: mixed
    port: 7890
rules:
  - RULE-SET,hijacking,REJECT,no-resolve
rule-providers:
  hijacking:
    type: inline
    behavior: domain
    payload:
      - +.example.com
"#,
    )
    .expect("write profile yaml");

    let override_path = temp_dir.join("__ebpf_cn_override__.yaml");
    std::fs::write(
        &override_path,
        r#"
listeners+:
  - name: ebpf-in
    type: ebpf
    mode: hybrid
    dns-mode: hijack
    bypass-rule-set: [CN-IP]
    shared:
      interface: [br0]
rule-providers-merge:
  CN-IP:
    type: http
    behavior: ipcidr
    format: mrs
    interval: 86400
    path: ./rule_provider/cn-ip.mrs
    url: https://example.com/cn.mrs
"#,
    )
    .expect("write eBPF override");

    let mut request = test_request(&temp_dir, &profile_path);
    request.run_mode = RunMode::Ebpf;
    request.overrides = vec![override_spec(&override_path, "yaml")];
    let result = r#override::compile_request(request, false).expect("compile should succeed");
    assert!(result.success, "compile failed: {:?}", result.error);
    let root: JsonValue = serde_yaml::from_str(&result.final_yaml).expect("parse final yaml");

    let providers = root["rule-providers"]
        .as_object()
        .expect("rule providers object");
    assert!(providers.contains_key("hijacking"));
    assert!(providers.contains_key("CN-IP"));

    let listeners = root["listeners"].as_array().expect("listeners array");
    assert!(
        listeners
            .iter()
            .any(|listener| listener["name"] == "existing-in")
    );
    assert!(
        listeners
            .iter()
            .any(|listener| listener["name"] == "ebpf-in")
    );
    assert_eq!(
        root["rules"][0],
        JsonValue::String("RULE-SET,hijacking,REJECT,no-resolve".to_string())
    );

    let _ = std::fs::remove_dir_all(&temp_dir);
}
