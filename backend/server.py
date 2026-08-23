"""Development AI gateway for English Bondhu.

The Gemini key stays in this server process and is never sent to the Android app.
Use a production web framework, authentication, persistent rate limiting, HTTPS,
and a managed secret store before exposing this service publicly.
"""

from __future__ import annotations

import argparse
import json
import os
import time
import urllib.error
import urllib.request
from collections import defaultdict, deque
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any


MODEL = os.getenv("GEMINI_MODEL", "gemini-3.1-flash-lite")
MAX_BODY_BYTES = 64 * 1024

TUTOR_SCHEMA = {
    "type": "object",
    "properties": {
        "correctedEnglish": {"type": "string", "description": "Corrected English, empty if no correction is needed"},
        "explanationBn": {"type": "string", "description": "Short, simple Bengali explanation"},
        "encouragementBn": {"type": "string", "description": "One natural Bengali encouragement sentence"},
        "nextPromptEnglish": {"type": "string", "description": "One level-appropriate English question for the learner"},
    },
    "required": ["correctedEnglish", "explanationBn", "encouragementBn", "nextPromptEnglish"],
}

SPEECH_SCHEMA = {
    "type": "object",
    "properties": {
        "understoodText": {"type": "string"},
        "correctedEnglish": {"type": "string"},
        "meaningBn": {"type": "string"},
        "grammarFeedbackBn": {"type": "string"},
        "practiceSentence": {"type": "string"},
    },
    "required": ["understoodText", "correctedEnglish", "meaningBn", "grammarFeedbackBn", "practiceSentence"],
}


class RateLimiter:
    def __init__(self, limit: int = 30, window_seconds: int = 60) -> None:
        self.limit = limit
        self.window_seconds = window_seconds
        self.requests: dict[str, deque[float]] = defaultdict(deque)

    def allow(self, identity: str) -> bool:
        now = time.monotonic()
        bucket = self.requests[identity]
        while bucket and bucket[0] <= now - self.window_seconds:
            bucket.popleft()
        if len(bucket) >= self.limit:
            return False
        bucket.append(now)
        return True


class GeminiClient:
    def __init__(self, api_key: str, model: str = MODEL) -> None:
        if not api_key.strip():
            raise ValueError("GEMINI_API_KEY is required")
        self.api_key = api_key.strip()
        self.model = model

    def structured(self, system_instruction: str, prompt: str, schema: dict[str, Any]) -> dict[str, Any]:
        endpoint = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model}:generateContent"
        payload = {
            "systemInstruction": {"parts": [{"text": system_instruction}]},
            "contents": [{"role": "user", "parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": 0.25,
                "maxOutputTokens": 900,
                "responseMimeType": "application/json",
                "responseJsonSchema": schema,
            },
        }
        request = urllib.request.Request(
            endpoint,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "x-goog-api-key": self.api_key,
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                document = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")[:800]
            raise RuntimeError(f"Gemini API returned {exc.code}: {detail}") from exc
        parts = document["candidates"][0]["content"]["parts"]
        text = "".join(str(part.get("text", "")) for part in parts)
        return json.loads(text)


def tutor_prompt(payload: dict[str, Any]) -> tuple[str, str]:
    message = str(payload.get("message", "")).strip()
    if not message or len(message) > 4_000:
        raise ValueError("message must contain 1 to 4000 characters")
    level = str(payload.get("level", "A1"))[:12]
    system = (
        "You are English Bondhu, a warm and precise English tutor for Bengali speakers. "
        f"Teach at CEFR {level}. Understand Bengali and English. If the learner writes English, "
        "correct grammar and natural wording. Explain the important point in simple Bengali. "
        "Never claim to evaluate pronunciation when you only receive a transcript. End with one "
        "short English question that continues realistic practice. Keep the whole response concise."
    )
    return system, f"Learner message:\n{message}"


def format_tutor_reply(result: dict[str, Any]) -> str:
    sections = []
    correction = str(result.get("correctedEnglish", "")).strip()
    if correction:
        sections.append(f"Correct English: {correction}")
    explanation = str(result.get("explanationBn", "")).strip()
    if explanation:
        sections.append(explanation)
    encouragement = str(result.get("encouragementBn", "")).strip()
    if encouragement:
        sections.append(encouragement)
    next_prompt = str(result.get("nextPromptEnglish", "")).strip()
    if next_prompt:
        sections.append(f"Your turn: {next_prompt}")
    return "\n\n".join(sections)


class TutorHandler(BaseHTTPRequestHandler):
    client: GeminiClient | None = None
    limiter = RateLimiter()

    def log_message(self, fmt: str, *args: Any) -> None:
        # Deliberately never log request bodies or credentials.
        print(f"{self.address_string()} - {fmt % args}")

    def _json(self, status: int, payload: dict[str, Any]) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:  # noqa: N802
        if self.path == "/health":
            self._json(HTTPStatus.OK, {"status": "ok", "model": MODEL, "configured": self.client is not None})
        else:
            self._json(HTTPStatus.NOT_FOUND, {"error": "not_found"})

    def do_POST(self) -> None:  # noqa: N802
        if self.path not in {"/v1/tutor", "/v1/speech-feedback"}:
            self._json(HTTPStatus.NOT_FOUND, {"error": "not_found"})
            return
        if not self.limiter.allow(self.client_address[0]):
            self._json(HTTPStatus.TOO_MANY_REQUESTS, {"error": "rate_limited"})
            return
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0 or length > MAX_BODY_BYTES:
            self._json(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, {"error": "invalid_body_size"})
            return
        try:
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            if not isinstance(payload, dict):
                raise ValueError("JSON body must be an object")
            if self.client is None:
                self._json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": "gemini_not_configured"})
                return
            if self.path == "/v1/tutor":
                system, prompt = tutor_prompt(payload)
                result = self.client.structured(system, prompt, TUTOR_SCHEMA)
                self._json(HTTPStatus.OK, {"reply": format_tutor_reply(result), "model": self.client.model})
            else:
                transcript = str(payload.get("transcript", "")).strip()
                expected = str(payload.get("expected", "")).strip()
                if not transcript or len(transcript) > 4_000:
                    raise ValueError("transcript must contain 1 to 4000 characters")
                prompt = f"Expected lesson sentence: {expected}\nSpeech recognizer transcript: {transcript}"
                system = (
                    "You correct spoken-English transcripts for Bengali learners. Correct grammar, wording, "
                    "and meaning in Bengali. You did not receive audio, so do not score pronunciation or accent."
                )
                result = self.client.structured(system, prompt, SPEECH_SCHEMA)
                self._json(HTTPStatus.OK, {"feedback": result, "model": self.client.model})
        except (ValueError, json.JSONDecodeError) as exc:
            self._json(HTTPStatus.BAD_REQUEST, {"error": "invalid_request", "detail": str(exc)})
        except Exception as exc:  # Keep provider details server-side.
            print(f"provider_error: {type(exc).__name__}: {exc}")
            self._json(HTTPStatus.BAD_GATEWAY, {"error": "provider_error"})


def main() -> None:
    parser = argparse.ArgumentParser(description="English Bondhu local Gemini gateway")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8787)
    args = parser.parse_args()
    key = os.getenv("GEMINI_API_KEY", "")
    TutorHandler.client = GeminiClient(key) if key.strip() else None
    server = ThreadingHTTPServer((args.host, args.port), TutorHandler)
    print(f"English Bondhu AI gateway: http://{args.host}:{args.port}")
    print(f"Model: {MODEL}; Gemini configured: {TutorHandler.client is not None}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("Stopping English Bondhu AI gateway.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
