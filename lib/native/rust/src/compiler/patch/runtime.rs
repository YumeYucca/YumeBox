//! Non-negotiable runtime patches.
//!
//! These run after the user's overrides and encode what the app's process model requires from the
//! core, independent of what the profile asked for.

use std::path::Path;

use serde_json::{Map as JsonMap, Value as JsonValue};
use sha2::{Digest, Sha256};

use crate::compiler::fingerprint::hex_lower;
use crate::compiler::patch::paths::{
    normalize_path, normalize_provider_path, profile_provider_path, runtime_home_dir,
};
use crate::compiler::patch::values::{ensure_object_field, has_non_empty_string};
use crate::compiler::schema::{
    DEFAULT_FAKE_IP_FILTER, DEFAULT_FAKE_IP_RANGE, DEFAULT_NAME_SERVERS,
};
use crate::model::RunMode;

const PROVIDER_FIELDS: [(&str, &str); 2] =
    [("proxy-providers", "proxies"), ("rule-providers", "rules")];

fn default_name_servers() -> JsonValue {
    JsonValue::Array(
        DEFAULT_NAME_SERVERS
            .iter()
            .map(|value| JsonValue::String((*value).to_string()))
            .collect(),
    )
}

pub fn patch_static_runtime(root: &mut JsonValue, profile_dir: &Path, run_mode: RunMode) {
    let Some(object) = root.as_object_mut() else {
        return;
    };

    object.insert(
        "interface-name".to_string(),
        JsonValue::String(String::new()),
    );
    object.insert("routing-mark".to_string(), JsonValue::from(0));

    if has_non_empty_string(object.get("external-controller"))
        || has_non_empty_string(object.get("external-controller-tls"))
    {
        object.insert(
            "external-ui".to_string(),
            JsonValue::String("./ui".to_string()),
        );
    }

    let profile = ensure_object_field(object, "profile");
    profile.insert("store-selected".to_string(), JsonValue::Bool(true));
    profile.insert("store-fake-ip".to_string(), JsonValue::Bool(true));

    let dns_enabled = bool_field(object, "dns", "enable");

    if !dns_enabled {
        let dns = ensure_object_field(object, "dns");
        dns.insert("enable".to_string(), JsonValue::Bool(true));
        dns.insert("use-hosts".to_string(), JsonValue::Bool(true));
        dns.insert("default-nameserver".to_string(), default_name_servers());
        dns.insert("nameserver".to_string(), default_name_servers());
        dns.insert(
            "enhanced-mode".to_string(),
            JsonValue::String("fake-ip".to_string()),
        );
        dns.insert(
            "fake-ip-range".to_string(),
            JsonValue::String(DEFAULT_FAKE_IP_RANGE.to_string()),
        );
        dns.insert(
            "fake-ip-filter".to_string(),
            JsonValue::Array(
                DEFAULT_FAKE_IP_FILTER
                    .iter()
                    .map(|value| JsonValue::String((*value).to_string()))
                    .collect(),
            ),
        );
    }

    // `system://` is deliberately NOT appended here, even though CFA's `append-system-dns` asks for
    // it: that nameserver resolves through `dns.UpdateSystemDNS`, which only an in-process host can
    // call. This core runs out of process, so the client would be built with an empty server list
    // and fail every query it is handed. A profile that lists `system://` itself is left alone.
    backfill_enabled_dns_without_nameserver(object);

    // In the VpnService path the TUN is attached at runtime via a file descriptor; a config-provided
    // tun block must never open its own /dev/net/tun. Keep tun.stack (and any other geometry) so
    // the Go launcher can copy the compiled userspace stack (gVisor or MIPS) onto the fd TUN.
    // Native eBPF and Root Tun keep their profile authoritative because their downloaded mihomo
    // kernels own traffic attachment.
    if run_mode == RunMode::Vpn
        && let Some(tun) = object.get_mut("tun").and_then(JsonValue::as_object_mut)
    {
        tun.insert("enable".to_string(), JsonValue::Bool(false));
        tun.insert("auto-route".to_string(), JsonValue::Bool(false));
        tun.insert("auto-detect-interface".to_string(), JsonValue::Bool(false));
    }

    patch_listeners(object);
    patch_providers(object, profile_dir);
}

/// Native eBPF and mihomo Tun cannot attach at the same time. Keep this narrow mode guard
/// separate from the broader runtime patch set so eBPF profiles remain otherwise authoritative.
pub fn disable_ebpf_tun_entrypoint(root: &mut JsonValue) {
    let Some(object) = root.as_object_mut() else {
        return;
    };
    let Some(tun) = object.get_mut("tun").and_then(JsonValue::as_object_mut) else {
        return;
    };
    for key in [
        "enable",
        "auto-route",
        "auto-detect-interface",
        "auto-redirect",
    ] {
        tun.insert(key.to_string(), JsonValue::Bool(false));
    }
}

/// Strip every traffic-facing entry point from the inspect-only core. The launcher adds its own
/// Unix controller after parsing, so no profile-supplied TCP controller or listener can survive.
pub fn patch_preview_runtime(root: &mut JsonValue) {
    let Some(object) = root.as_object_mut() else {
        return;
    };

    for port in [
        "port",
        "socks-port",
        "mixed-port",
        "redir-port",
        "tproxy-port",
    ] {
        object.insert(port.to_string(), JsonValue::from(0));
    }
    object.insert(
        "external-controller".to_string(),
        JsonValue::String(String::new()),
    );
    object.insert(
        "external-controller-tls".to_string(),
        JsonValue::String(String::new()),
    );
    object.insert("external-ui".to_string(), JsonValue::String(String::new()));
    object.insert("listeners".to_string(), JsonValue::Array(Vec::new()));

    let tun = ensure_object_field(object, "tun");
    tun.insert("enable".to_string(), JsonValue::Bool(false));
    tun.insert("auto-route".to_string(), JsonValue::Bool(false));
    tun.insert("auto-detect-interface".to_string(), JsonValue::Bool(false));
    tun.insert("auto-redirect".to_string(), JsonValue::Bool(false));
    tun.insert("strict-route".to_string(), JsonValue::Bool(false));
}

/// An override may force `dns.enable: true` onto a profile that carries no nameservers (the
/// built-in Tun override does exactly this when the subscription has no `dns:` block). mihomo
/// hard-fails on "DNS enabled but NameServer empty", killing the core at parse time — backfill
/// the defaults so an enabled-but-empty DNS block always resolves.
fn backfill_enabled_dns_without_nameserver(object: &mut JsonMap<String, JsonValue>) {
    let needs_backfill = object
        .get("dns")
        .and_then(JsonValue::as_object)
        .map(|dns| {
            dns.get("enable")
                .and_then(JsonValue::as_bool)
                .unwrap_or(false)
                && dns
                    .get("nameserver")
                    .and_then(JsonValue::as_array)
                    .map(Vec::is_empty)
                    .unwrap_or(true)
        })
        .unwrap_or(false);
    if !needs_backfill {
        return;
    }

    let dns = ensure_object_field(object, "dns");
    dns.insert("nameserver".to_string(), default_name_servers());
    let default_nameserver_missing = dns
        .get("default-nameserver")
        .and_then(JsonValue::as_array)
        .map(Vec::is_empty)
        .unwrap_or(true);
    if default_nameserver_missing {
        dns.insert("default-nameserver".to_string(), default_name_servers());
    }
}

fn bool_field(object: &JsonMap<String, JsonValue>, block: &str, key: &str) -> bool {
    object
        .get(block)
        .and_then(JsonValue::as_object)
        .and_then(|value| value.get(key))
        .and_then(JsonValue::as_bool)
        .unwrap_or(false)
}

/// The app owns the Tun entry point; a profile-declared redir/tun listener would collide with it.
fn patch_listeners(object: &mut JsonMap<String, JsonValue>) {
    let Some(listeners) = object
        .get_mut("listeners")
        .and_then(JsonValue::as_array_mut)
    else {
        return;
    };
    listeners.retain(|listener| {
        listener
            .as_object()
            .and_then(|value| value.get("type"))
            .and_then(JsonValue::as_str)
            .map(|kind| !matches!(kind, "redir" | "tun"))
            .unwrap_or(true)
    });
}

pub fn patch_providers(object: &mut JsonMap<String, JsonValue>, profile_dir: &Path) {
    for (field, prefix) in PROVIDER_FIELDS {
        let Some(providers) = object.get_mut(field).and_then(JsonValue::as_object_mut) else {
            continue;
        };
        for provider in providers.values_mut() {
            let Some(provider_object) = provider.as_object_mut() else {
                continue;
            };
            let extension = provider_extension(provider_object, prefix);
            if let Some(path) = provider_object.get("path").and_then(JsonValue::as_str)
                && !path.trim().is_empty()
            {
                let normalized_path = normalize_provider_path(path, profile_dir, prefix, extension);
                provider_object.insert("path".to_string(), JsonValue::String(normalized_path));
                continue;
            }
            if is_inline_provider(provider_object) {
                continue;
            }
            let Some(url) = provider_object.get("url").and_then(JsonValue::as_str) else {
                continue;
            };
            let mut hasher = Sha256::new();
            hasher.update(url.as_bytes());
            let hash = hex_lower(&hasher.finalize());
            provider_object.insert(
                "path".to_string(),
                JsonValue::String(profile_provider_path(
                    profile_dir,
                    prefix,
                    Path::new(&format!("{hash}.{extension}")),
                )),
            );
        }
    }
}

/// Rejects any provider whose normalized path would leave `<profile_dir>/providers/<prefix>/`.
pub fn validate_provider_paths(
    object: &JsonMap<String, JsonValue>,
    profile_dir: &Path,
) -> Result<(), String> {
    let runtime_home = runtime_home_dir(profile_dir);
    for (field, prefix) in PROVIDER_FIELDS {
        let Some(providers) = object.get(field).and_then(JsonValue::as_object) else {
            continue;
        };
        let expected_base = normalize_path(profile_dir.join("providers").join(prefix));
        for (name, provider) in providers {
            let provider_object = provider
                .as_object()
                .ok_or_else(|| format!("{field}.{name} must be an object"))?;
            let path = provider_object
                .get("path")
                .and_then(JsonValue::as_str)
                .map(str::trim)
                .filter(|value| !value.is_empty());
            if path.is_none() && is_inline_provider(provider_object) {
                continue;
            }
            let path = path.ok_or_else(|| format!("{field}.{name} missing normalized path"))?;
            let candidate = Path::new(path);
            let resolved_candidate = if candidate.is_absolute() {
                candidate.to_path_buf()
            } else {
                normalize_path(runtime_home.join(candidate))
            };
            if candidate.is_absolute() || !resolved_candidate.starts_with(&expected_base) {
                return Err(format!(
                    "{field}.{name} path escaped profile scope: {path} (expected under {})",
                    expected_base.to_string_lossy()
                ));
            }
        }
    }
    Ok(())
}

fn is_inline_provider(provider: &JsonMap<String, JsonValue>) -> bool {
    provider
        .get("type")
        .and_then(JsonValue::as_str)
        .map(|value| value.eq_ignore_ascii_case("inline"))
        .unwrap_or(false)
}

fn provider_extension(provider: &JsonMap<String, JsonValue>, prefix: &str) -> &'static str {
    if prefix == "rules"
        && provider
            .get("format")
            .and_then(JsonValue::as_str)
            .map(|value| value.eq_ignore_ascii_case("mrs"))
            .unwrap_or(false)
    {
        return "mrs";
    }
    "yaml"
}

#[cfg(test)]
mod tests {
    use std::path::Path;

    use serde_json::json;

    use super::{patch_preview_runtime, patch_static_runtime};
    use crate::model::RunMode;

    #[test]
    fn preview_patch_removes_all_traffic_entry_points() {
        let mut root = json!({
            "port": 7890,
            "socks-port": 7891,
            "mixed-port": 7892,
            "redir-port": 7893,
            "tproxy-port": 7894,
            "external-controller": "127.0.0.1:9090",
            "listeners": [{ "type": "tun" }, { "type": "mixed" }],
            "tun": { "enable": true, "auto-route": true, "auto-redirect": true }
        });

        patch_preview_runtime(&mut root);

        let object = root.as_object().expect("preview config is an object");
        for port in [
            "port",
            "socks-port",
            "mixed-port",
            "redir-port",
            "tproxy-port",
        ] {
            assert_eq!(object[port], 0, "{port} must be disabled");
        }
        assert_eq!(object["external-controller"], "");
        assert_eq!(object["listeners"], json!([]));
        assert_eq!(object["tun"]["enable"], false);
        assert_eq!(object["tun"]["auto-route"], false);
        assert_eq!(object["tun"]["auto-redirect"], false);
    }

    #[test]
    fn vpn_patch_disables_tun_but_keeps_compiled_stack() {
        let mut root = json!({
            "tun": {
                "enable": true,
                "stack": "mips",
                "auto-route": true,
                "auto-detect-interface": true,
                "device": "Yume"
            }
        });

        patch_static_runtime(&mut root, Path::new("/tmp"), RunMode::Vpn);

        let tun = root["tun"].as_object().expect("tun block");
        assert_eq!(tun["enable"], false);
        assert_eq!(tun["auto-route"], false);
        assert_eq!(tun["auto-detect-interface"], false);
        assert_eq!(tun["stack"], "mips");
        assert_eq!(tun["device"], "Yume");
    }
}
