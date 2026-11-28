#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""گزارش پیشرفت بانک سؤال نیچ‌مارکت نسبت به هدف نسخه بعد."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
QDIR = ROOT / "app" / "src" / "main" / "assets" / "questions"
TARGETS = {
    "omoomi": ("عمومی استخدامی", 200),
    "amoozesh": ("آموزش و پرورش", 160),
    "bank": ("استخدامی بانک‌ها", 150),
    "dadgostari": ("دادگستری", 140),
    "it": ("فناوری اطلاعات و مخابرات", 60),
    "modiriat": ("مدیریت و پشتیبانی", 60),
    "riazi": ("ریاضی، آمار و هوش عددی", 40),
    "maaref": ("معارف اسلامی و انقلاب", 40),
    "adabiat": ("هنر و ادبیات فارسی", 35),
    "pezeshki": ("بهداشت و درمان", 45),
    "tamin_ejtemaei": ("تأمین اجتماعی", 25),
    "shahrdari": ("شهرداری‌ها", 20),
    "dastgah_ejraei": ("دستگاه‌های اجرایی", 15),
    "fani_herfei": ("فنی و حرفه‌ای", 10),
}


def main():
    index = json.loads((QDIR / "index.json").read_text(encoding="utf-8"))
    counts = {}
    for filename in index["files"]:
        data = json.loads((QDIR / filename).read_text(encoding="utf-8"))
        counts[data["id"]] = sum(len(p.get("questions", [])) for p in data.get("packages", []))

    print("گزارش پیشرفت بانک سؤال نیچ‌مارکت")
    print("-" * 72)
    total = 0
    target_total = sum(target for _, target in TARGETS.values())
    for cat_id, (title, target) in TARGETS.items():
        current = counts.get(cat_id, 0)
        gap = max(0, target - current)
        total += current
        status = "هدف محقق شد" if current >= target else f"{gap} سؤال باقی‌مانده"
        print(f"{title}: {current}/{target} — {status}")
    extra = sorted(set(counts) - set(TARGETS))
    for cat_id in extra:
        print(f"دسته خارج از سهمیه: {cat_id} — {counts[cat_id]} سؤال")
        total += counts[cat_id]
    print("-" * 72)
    print(f"کل سؤال‌ها: {total}")
    print(f"هدف: {target_total}")
    print(f"فاصله تا هدف: {max(0, target_total - total)}")
    print(f"پیشرفت: {min(100, total * 100 / target_total):.1f}%")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
