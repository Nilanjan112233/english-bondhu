"""Build a personal standalone APK with NovelForge's Gemini key injected in-memory."""

from __future__ import annotations

import argparse
import asyncio
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path


async def load_key(backend_path: Path) -> str:
    os.chdir(backend_path)
    sys.path.insert(0, str(backend_path))
    from motor.motor_asyncio import AsyncIOMotorClient
    from app.core.config import settings
    from app.services.secret_encryption_service import secret_encryption_service

    mongo = AsyncIOMotorClient(settings.mongodb_url, serverSelectionTimeoutMS=3_000)
    try:
        document = await mongo[settings.database_name]["app_settings"].find_one(
            {"_id": "global_settings"}
        )
        providers = secret_encryption_service.runtime_providers(
            (document or {}).get("providers", {})
        )
        return str(providers.get("gemini", {}).get("api_key", ""))
    finally:
        mongo.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--novelforge-backend", type=Path, required=True)
    parser.add_argument("--project-root", type=Path, required=True)
    args = parser.parse_args()
    key = asyncio.run(load_key(args.novelforge_backend.resolve()))
    if not key:
        raise SystemExit("NovelForge does not have a configured Gemini key.")
    project = args.project_root.resolve()
    environment = os.environ.copy()
    environment["GEMINI_API_KEY"] = key
    subprocess.run(
        ["pwsh", "-NoProfile", "-File", str(project / "build-apk.ps1")],
        cwd=project,
        env=environment,
        check=True,
    )
    generated = next((project / "app" / "build" / "generated" / "source" / "buildConfig" / "debug").rglob("BuildConfig.java"))
    build_config = generated.read_text(encoding="utf-8")
    match = re.search(r'GEMINI_API_KEY\s*=\s*"([^"]*)"', build_config)
    if not match or match.group(1) != key:
        raise SystemExit("Build verification failed: Gemini key was not injected into BuildConfig.")
    source_apk = project / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    handoff_apk = source_apk.with_name("EnglishBondhu-standalone-debug.apk")
    shutil.copy2(source_apk, handoff_apk)
    print("Standalone personal APK built with Gemini key injected into BuildConfig.")
    print(f"Injection verified: true; embedded key length: {len(key)}")
    print(f"Handoff APK: {handoff_apk}")


if __name__ == "__main__":
    main()
