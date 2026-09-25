"""Geo asset downloader and XZ compressor."""

from __future__ import annotations

from dataclasses import dataclass
import lzma
from pathlib import Path
import shutil
import tempfile
from urllib.request import urlopen

from .config import ProjectConfig


@dataclass(frozen=True)
class Asset:
    name: str
    url: str
    compress: bool


class ResourceDownloader:
    def __init__(self, config: ProjectConfig):
        self.config = config
        self.output_dir = config.project_root / "build/generated/assets/geo"

    def download_geo_files(self) -> None:
        self.output_dir.mkdir(parents=True, exist_ok=True)
        (self.output_dir / "BundleMRS.7z").unlink(missing_ok=True)
        assets = [
            Asset("geoip.metadb", self.config.get_string("asset.geoip.url", "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geoip.metadb"), True),
            Asset("geosite.dat", self.config.get_string("asset.geosite.url", "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/geosite.dat"), True),
            Asset("ASN.mmdb", self.config.get_string("asset.asn.url", "https://github.com/MetaCubeX/meta-rules-dat/releases/download/latest/GeoLite2-ASN.mmdb"), True),
        ]
        for asset in assets:
            if asset.url.startswith("https://"):
                self.download_file(asset)

    def download_file(self, asset: Asset) -> None:
        try:
            print(f"[Geo] Downloading {asset.name} from {asset.url}...")
            with tempfile.NamedTemporaryFile(prefix="geo-", suffix=f"-{asset.name}", delete=False) as temporary:
                temporary_path = Path(temporary.name)
            try:
                with urlopen(asset.url) as response, temporary_path.open("wb") as output:
                    shutil.copyfileobj(response, output)
                if asset.compress:
                    output_file = self.output_dir / f"{asset.name}.xz"
                    with temporary_path.open("rb") as source, lzma.open(output_file, "wb", format=lzma.FORMAT_XZ) as compressed:
                        shutil.copyfileobj(source, compressed)
                    print(f"[Geo] Downloaded and compressed {asset.name} -> {output_file}")
                else:
                    output_file = self.output_dir / asset.name
                    shutil.copyfile(temporary_path, output_file)
                    print(f"[Geo] Downloaded {asset.name} to {output_file}")
            finally:
                temporary_path.unlink(missing_ok=True)
        except OSError as error:
            print(f"[Geo] Failed to download {asset.name}: {error}")
        except Exception as error:
            print(f"[Geo] Failed to download {asset.name}: {error}")

