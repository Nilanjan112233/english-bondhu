"""Development-only bridge that reuses NovelForge's encrypted Gemini credential in memory."""

from __future__ import annotations

import argparse
import asyncio
import os
import sys
from pathlib import Path

from server import GeminiClient, ThreadingHTTPServer, TutorHandler


async def load_novelforge_key(backend_path: Path) -> str:
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
    parser = argparse.ArgumentParser(description="Start English Bondhu using NovelForge's local encrypted Gemini key")
    parser.add_argument("--novelforge-backend", type=Path, required=True)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8787)
    args = parser.parse_args()

    backend_path = args.novelforge_backend.resolve()
    if not (backend_path / "app" / "services" / "secret_encryption_service.py").is_file():
        raise SystemExit("The supplied folder is not a compatible NovelForge backend.")
    key = asyncio.run(load_novelforge_key(backend_path))
    if not key:
        raise SystemExit("NovelForge does not have a configured Gemini credential.")

    TutorHandler.client = GeminiClient(key)
    server = ThreadingHTTPServer((args.host, args.port), TutorHandler)
    print(f"English Bondhu AI gateway: http://{args.host}:{args.port}")
    print(f"Credential source: NovelForge encrypted settings; model: {TutorHandler.client.model}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("Stopping English Bondhu AI gateway.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
