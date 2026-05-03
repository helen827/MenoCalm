#!/usr/bin/env python3
import argparse
import sys
from urllib.parse import urlparse


def _is_https_non_local(url: str) -> bool:
    parsed = urlparse(url)
    return parsed.scheme == "https" and parsed.hostname not in {"localhost", "127.0.0.1"}


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate runtime environment configuration.")
    parser.add_argument("--env", default="dev", help="Runtime environment: dev/staging/prod")
    parser.add_argument("--backend-url", default="", help="Backend base URL")
    args = parser.parse_args()

    env = args.env.strip().lower()
    if env not in {"dev", "staging", "prod"}:
        print(f"[config] invalid env: {env}")
        return 1

    backend = args.backend_url.strip()
    if env == "prod":
        if not backend:
            print("[config] production requires backend URL")
            return 1
        if not _is_https_non_local(backend):
            print("[config] production backend URL must be https and non-localhost")
            return 1

    print(f"[config] ok env={env} backend={'set' if backend else 'empty'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
