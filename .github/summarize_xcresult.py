#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
import sys
from typing import Any, Dict, List


def unwrap(node: Any) -> Any:
    if isinstance(node, dict):
        if set(node.keys()) == {"_value"}:
            return unwrap(node["_value"])
        if set(node.keys()) == {"_values"}:
            return [unwrap(item) for item in node["_values"]]
        return {k: unwrap(v) for k, v in node.items()}
    if isinstance(node, list):
        return [unwrap(item) for item in node]
    return node


def collect_failures(node: Any, out: List[Dict[str, Any]]) -> None:
    if isinstance(node, dict):
        if "testCaseName" in node and ("message" in node or "failureText" in node):
            out.append(node)
        for value in node.values():
            collect_failures(value, out)
    elif isinstance(node, list):
        for value in node:
            collect_failures(value, out)


def append(lines: List[str], summary_path: str) -> None:
    with open(summary_path, "a", encoding="utf-8") as f:
        for line in lines:
            f.write(line + "\n")


def main() -> int:
    parser = argparse.ArgumentParser(description="Append xcresult failure summary to GitHub summary.")
    parser.add_argument("--bundle", required=True, help="xcresult bundle path")
    parser.add_argument("--title", default="", help="markdown section title")
    parser.add_argument(
        "--prefix",
        default="- ",
        help="bullet prefix for each line (default: '- ')",
    )
    args = parser.parse_args()

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_path:
        print("GITHUB_STEP_SUMMARY is not set.", file=sys.stderr)
        return 1

    lines: List[str] = []
    if args.title.strip():
        lines.append(f"### {args.title}")

    if not os.path.exists(args.bundle):
        lines.append(f"{args.prefix}xcresult not found: `{args.bundle}`")
        append(lines, summary_path)
        return 0

    cmd = ["xcrun", "xcresulttool", "get", "--path", args.bundle, "--format", "json"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        lines.append(f"{args.prefix}Unable to parse xcresult: `{args.bundle}`")
        stderr_text = result.stderr.strip()
        if stderr_text:
            lines.extend(["", "```text", stderr_text, "```"])
        append(lines, summary_path)
        return 0

    try:
        payload = json.loads(result.stdout)
    except json.JSONDecodeError as error:
        lines.append(f"{args.prefix}Invalid xcresult json: `{error}`")
        append(lines, summary_path)
        return 0

    normalized = unwrap(payload)
    failures: List[Dict[str, Any]] = []
    collect_failures(normalized, failures)

    if not failures:
        lines.append(f"{args.prefix}No test failures found in xcresult.")
    else:
        for failure in failures[:20]:
            case_name = failure.get("testCaseName", "<unknown>")
            message = failure.get("message") or failure.get("failureText") or "<no message>"
            file_name = failure.get("fileName")
            line_number = failure.get("lineNumber")
            location = f" ({file_name}:{line_number})" if file_name and line_number else ""
            lines.append(f"{args.prefix}`{case_name}`: {message}{location}")

    append(lines, summary_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
