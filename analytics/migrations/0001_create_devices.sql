CREATE TABLE devices (
    device_hash TEXT PRIMARY KEY CHECK (length(device_hash) = 64),
    first_seen_at INTEGER NOT NULL,
    last_seen_at INTEGER NOT NULL,
    app_version TEXT NOT NULL,
    version_code INTEGER NOT NULL,
    android_version TEXT NOT NULL,
    launch_count INTEGER NOT NULL DEFAULT 1 CHECK (launch_count > 0)
);

CREATE INDEX idx_devices_first_seen_at ON devices (first_seen_at);
CREATE INDEX idx_devices_last_seen_at ON devices (last_seen_at);

