import requests
import os
import glob
import html
import json
import hashlib
from contextlib import ExitStack
from pathlib import Path

BOT_TOKEN = os.environ.get("BOT_TOKEN")
CHAT_ID = os.environ.get("CHAT_ID")
TITLE = os.environ.get("TITLE")
BRANCH = os.environ.get("BRANCH")
WORKFLOW_NAME = os.environ.get("WORKFLOW_NAME", "")
EVENT_NAME = os.environ.get("EVENT_NAME", "")
RUN_NUMBER = os.environ.get("RUN_NUMBER", "")
COMMIT_SHA = os.environ.get("COMMIT_SHA", "")
REPOSITORY = os.environ.get("REPOSITORY", "")
RUN_ID = os.environ.get("RUN_ID", "")
SERVER_URL = os.environ.get("SERVER_URL", "https://github.com")
PUBLISH_DIR = os.environ.get("PUBLISH_DIR", "")
LOGO_PATH = os.environ.get("LOGO_PATH", "website/images/brand/project-preview.webp")
LOGO_URL = os.environ.get("LOGO_URL", "https://yumebox.yumeyuka.moe/images/brand/project-preview.webp")
BOT_API_BASE_URL = os.environ.get("BOT_API_BASE_URL", "https://api.telegram.org").rstrip("/")

MAX_LOGO_BYTES = 10 * 1024 * 1024


def html_escape(text):
    return html.escape(text or "")


def _upload_name(path):
    """Telegram reuses a previously sent document when the file name matches.

    Publish names are date-based for channel builds, so every build on the same
    day would collide. Tag the uploaded name with the content hash instead.
    """
    name = os.path.basename(path)
    stem, dot, ext = name.rpartition(".")
    digest = _sha256(path)[:8]
    if not dot:
        return f"{name}-{digest}"
    return f"{stem}-{digest}.{ext}"


def _action_url():
    if not REPOSITORY or not RUN_ID:
        return ""
    return f"{SERVER_URL}/{REPOSITORY}/actions/runs/{RUN_ID}"


def _sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as apk:
        for chunk in iter(lambda: apk.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _apk_variant(path):
    name = os.path.basename(path).lower()
    if "builtin" in name:
        return "builtin"
    if "external" in name:
        return "external"
    return ""


def get_caption(files):
    lines = [
        f"<b>{html_escape(TITLE)}</b>",
        "",
        "⚠️ Nightly builds may be unstable. Use with caution.",
    ]

    action_url = _action_url()
    if action_url:
        lines.extend(
            [
                "",
                "<b>GitHub Actions</b>",
                f'<a href="{action_url}">{html_escape(action_url)}</a>',
            ]
        )

    hashes = {}
    for path in files:
        variant = _apk_variant(path)
        if variant:
            hashes[variant] = _sha256(path)

    if hashes:
        digests = [hashes[v] for v in ("builtin", "external") if hashes.get(v)]
        if digests:
            # Full hashes stay in <code> entities: that is what Telegram copies on tap. Listing
            # them as bullets matches the APK-variant block below.
            lines.extend(["", "<b>SHA-256</b>"])
            lines.extend(
                f"• <code>{digest}</code>" for digest in digests
            )
            lines.append("Tap a hash to copy")

    lines.extend(
        [
            "",
            "<b>Choose an APK variant</b>",
            "• <code>builtin</code>: includes Geo data, recommended for first install",
            "• <code>external</code>: smaller, recommended for updates",
            "• Both variants are functionally identical",
        ]
    )
    return "\n".join(lines)


def check_environ():
    if BOT_TOKEN is None:
        print("[-] Invalid BOT_TOKEN")
        exit(1)
    if CHAT_ID is None:
        print("[-] Invalid CHAT_ID")
        exit(1)
    if TITLE is None:
        print("[-] Invalid TITLE")
        exit(1)
    if BRANCH is None:
        print("[-] Invalid BRANCH")
        exit(1)


def find_apk_files():
    if PUBLISH_DIR:
        pattern = os.path.join(PUBLISH_DIR, "*.apk")
        found = sorted(glob.glob(pattern))
        if found:
            print(f"[+] Found {len(found)} files in {pattern}")
            return found

    patterns = [
        "./app/build/outputs/apk/release/*arm64-v8a*.apk",
        "app/build/outputs/apk/release/*arm64-v8a*.apk",
        "/github/workspace/app/build/outputs/apk/release/*arm64-v8a*.apk"
    ]

    files = []
    for pattern in patterns:
        found = glob.glob(pattern)
        if found:
            files.extend(found)
            print(f"[+] Found {len(found)} files in {pattern}")

    files = sorted(set(files))

    if not files:
        print("[-] No APK files found!")
        exit(1)

    print(f"[+] Total files to upload: {len(files)}")
    for f in files:
        print(f"    - {f}")

    return files


def _validate_logo(data, source):
    if not data:
        raise ValueError(f"Logo is empty: {source}")
    if len(data) > MAX_LOGO_BYTES:
        raise ValueError(f"Logo exceeds {MAX_LOGO_BYTES} bytes: {source}")
    return data


def load_logo():
    if LOGO_PATH:
        path = Path(LOGO_PATH)
        if path.is_file():
            data = _validate_logo(path.read_bytes(), path)
            content_type = "image/webp" if path.suffix.lower() == ".webp" else "application/octet-stream"
            print(f"[+] Loaded current checkout logo: {path} ({len(data)} bytes)")
            return path.name, data, content_type

    if not LOGO_URL:
        raise FileNotFoundError(f"Logo not found: {LOGO_PATH}")

    cache_key = RUN_ID or COMMIT_SHA or RUN_NUMBER
    params = {"ci_run": cache_key} if cache_key else None
    with requests.get(
        LOGO_URL,
        params=params,
        headers={"Cache-Control": "no-cache"},
        stream=True,
        timeout=(15, 60),
    ) as response:
        response.raise_for_status()
        content_type = response.headers.get("Content-Type", "image/webp").split(";", 1)[0]
        if not content_type.startswith("image/"):
            raise ValueError(f"Logo URL returned unexpected content type: {content_type}")

        data = bytearray()
        for chunk in response.iter_content(chunk_size=64 * 1024):
            data.extend(chunk)
            if len(data) > MAX_LOGO_BYTES:
                raise ValueError(f"Logo exceeds {MAX_LOGO_BYTES} bytes: {LOGO_URL}")

    logo = _validate_logo(bytes(data), LOGO_URL)
    print(f"[+] Downloaded fresh logo: {LOGO_URL} ({len(logo)} bytes)")
    return "og.webp", logo, content_type


def send_files_via_bot_api():
    print("[+] Starting Telegram upload")
    check_environ()

    files = find_apk_files()

    # Bot API URL
    bot_url = f"{BOT_API_BASE_URL}/bot{BOT_TOKEN}"

    caption = get_caption(files)
    print("[+] Caption:", caption)

    caption_sent = False
    try:
        photo_name, photo, photo_content_type = load_logo()
        photo_data = {
            'chat_id': CHAT_ID,
            'caption': caption,
            'parse_mode': 'HTML',
        }

        photo_resp = requests.post(
            f"{bot_url}/sendPhoto",
            data=photo_data,
            files={'photo': (photo_name, photo, photo_content_type)},
            timeout=60,
        )

        print(f"[+] Photo+caption: {photo_resp.status_code}")

        if photo_resp.status_code != 200:
            print(f"[-] Photo send failed: {photo_resp.text}")
        else:
            caption_sent = True

    except Exception as e:
        print(f"[-] Photo send failed: {e}")

    if not caption_sent:
        try:
            text_resp = requests.post(
                f"{bot_url}/sendMessage",
                data={
                    "chat_id": CHAT_ID,
                    "text": caption,
                    "parse_mode": "HTML",
                },
                timeout=60,
            )
            if text_resp.status_code == 200:
                caption_sent = True
                print("[+] Text caption sent without a logo")
            else:
                print(f"[-] Text caption send failed: {text_resp.text}")
        except Exception as e:
            print(f"[-] Text caption send failed: {e}")


    # Telegram renders a document media group as one album-style chat item.
    all_uploaded = caption_sent
    try:
        media = []
        upload_files = {}
        with ExitStack() as stack:
            for index, file_path in enumerate(files):
                field_name = f"document{index}"
                media.append({"type": "document", "media": f"attach://{field_name}"})
                upload_files[field_name] = (
                    _upload_name(file_path),
                    stack.enter_context(open(file_path, "rb")),
                    "application/vnd.android.package-archive",
                )
                print(f"[+] Queued {file_path} for media group")

            response = requests.post(
                f"{bot_url}/sendMediaGroup",
                data={"chat_id": CHAT_ID, "media": json.dumps(media)},
                files=upload_files,
                timeout=(30, 600),
            )

        if response.status_code == 200:
            print(f"[+] Uploaded {len(files)} APKs in one media group")
        else:
            print(f"[-] Failed to upload APK media group: {response.text}")
            all_uploaded = False
    except Exception as e:
        print(f"[-] Failed to upload APK media group: {e}")
        all_uploaded = False

    return all_uploaded


if __name__ == "__main__":
    try:
        if not send_files_via_bot_api():
            raise SystemExit(1)
    except Exception as e:
        print(f"[-] Error: {e}")
        raise SystemExit(1)
