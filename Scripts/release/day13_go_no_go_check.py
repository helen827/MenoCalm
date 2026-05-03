#!/usr/bin/env python3
from pathlib import Path


REQUIRED_FILES = [
    "Legal/Privacy-Policy.md",
    "Legal/User-Agreement.md",
    "Legal/Medical-Disclaimer.md",
    "ops/drills/day10-gray-release.md",
    "ops/drills/day11-rollback-drill.md",
]


def main() -> int:
    missing = [f for f in REQUIRED_FILES if not Path(f).exists()]
    report = Path("ops/reports/day13-go-no-go.md")
    report.parent.mkdir(parents=True, exist_ok=True)

    if missing:
        report.write_text(
            "# Day13 Go/No-Go 评审结果\n\n- 结论: No-Go\n- 缺失资产:\n"
            + "\n".join([f"  - {m}" for m in missing]),
            encoding="utf-8",
        )
        print("No-Go, missing files:")
        for m in missing:
            print(f"- {m}")
        return 1

    report.write_text(
        "# Day13 Go/No-Go 评审结果\n\n- 结论: Go（资产齐备）\n- 已检查项:\n"
        + "\n".join([f"  - {f}" for f in REQUIRED_FILES]),
        encoding="utf-8",
    )
    print("Go, all required assets present.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
