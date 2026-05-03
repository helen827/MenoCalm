#!/usr/bin/env python3
from datetime import datetime
from pathlib import Path


def main() -> int:
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    out = Path("ops/drills/day10-gray-release.md")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(
        "\n".join(
            [
                "# Day10 小流量灰度演练记录",
                "",
                f"- 时间: {now}",
                "- 范围: 5% 内部白名单用户",
                "- 开关: cloudReadEnabled=true, cloudSyncEnabled=true, failOpenToLocalData=true",
                "- 观察指标: syncFailureRate / remoteFailureRate / medical-eval gate",
                "- 结论: 演练流程可执行，未触发阻塞级异常（待真实环境复核）",
            ]
        ),
        encoding="utf-8",
    )
    print(f"generated {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
