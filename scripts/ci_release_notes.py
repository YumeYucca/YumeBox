#!/usr/bin/env python3
"""Compose release notes for GitHub releases and the in-app update manifest.

``commit`` mode prints the subject of the built commit as a single Markdown bullet, so a
nightly build never repeats the history of earlier ones. ``changelog`` mode prints the
whole ``CHANGELOG.md`` body and falls back to the commit subject when it is empty.

``CHANGELOG.md`` holds the notes of one release at a time: it is replaced with the next
release's entries instead of accumulating history, so no version lookup is involved.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

TITLE = re.compile(r"^#{1,2}\s")
COMMENT = re.compile(r"<!--.*?-->", re.DOTALL)


def commit_notes(sha: str) -> str:
    result = subprocess.run(
        ["git", "log", "-1", "--pretty=%s", sha],
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    subject = result.stdout.strip()
    return f"- {subject}" if subject else ""


def changelog_notes(path: Path) -> str:
    if not path.is_file():
        return ""
    text = COMMENT.sub("", path.read_text(encoding="utf-8"))
    lines = [line for line in text.splitlines() if not TITLE.match(line)]
    return "\n".join(lines).strip()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("commit", "changelog"), default="commit")
    parser.add_argument("--tag", default="", help="Release tag, e.g. v0.6.5")
    parser.add_argument("--sha", default="HEAD", help="Commit the build was made from")
    parser.add_argument("--changelog", default="CHANGELOG.md")
    args = parser.parse_args()

    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    if args.mode == "changelog":
        notes = changelog_notes(Path(args.changelog))
        if not notes:
            print(
                "warning: CHANGELOG.md is empty; falling back to the commit message",
                file=sys.stderr,
            )
            notes = commit_notes(args.sha)
    else:
        notes = commit_notes(args.sha)

    print(notes)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
