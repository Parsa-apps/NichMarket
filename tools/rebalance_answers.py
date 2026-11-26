#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
متوازن‌سازی جایگاه پاسخ صحیح در بانک سوالات نیچ مارکت.

با جابه‌جایی گزینه‌ها (بدون تغییر درستی هیچ سوال)، توزیع answerIndex در
هر بسته را تا حد امکان یکنواخت می‌کند (هر گزینه ≈ ۲۵٪) تا داوطلب نتواند
از روی الگوی جایگاه پاسخ حدس بزند.

اجرا:
    python3 tools/rebalance_answers.py          # پیش‌نمایش، بدون تغییر فایل
    python3 tools/rebalance_answers.py --apply  # فقط پس از بررسی سازگاری کارنامه‌ها
سپس برای اطمینان:
    python3 tools/validate_questions.py
"""

import argparse
import json
from collections import Counter
from pathlib import Path

QDIR = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "questions"


def targets_for(n):
    base, rem = divmod(n, 4)
    return [base + 1 if i < rem else base for i in range(4)]


def rebalance_pack(pack):
    questions = pack.get("questions", [])
    n = len(questions)
    if n == 0:
        return
    targets = targets_for(n)
    counts = Counter(q["answerIndex"] for q in questions)

    for q in questions:
        ai = q["answerIndex"]
        if counts[ai] <= targets[ai]:
            continue
        # یافتن کم‌تکرارترین گزینه نسبت به هدف
        target = min(range(4), key=lambda i: counts[i] - targets[i])
        if counts[target] >= targets[target]:
            continue
        # جابه‌جایی گزینه صحیح با گزینه هدف
        opts = q["options"]
        opts[ai], opts[target] = opts[target], opts[ai]
        q["answerIndex"] = target
        counts[ai] -= 1
        counts[target] += 1


def main():
    parser = argparse.ArgumentParser(description="Preview or rebalance answer positions")
    parser.add_argument("--apply", action="store_true", help="write changes after reviewing migration impact")
    args = parser.parse_args()
    index = json.loads((QDIR / "index.json").read_text(encoding="utf-8"))
    for fname in index.get("files", []):
        path = QDIR / fname
        data = json.loads(path.read_text(encoding="utf-8"))
        for pack in data.get("packages", []):
            before = dict(sorted(Counter(q["answerIndex"] for q in pack["questions"]).items()))
            rebalance_pack(pack)
            after = dict(sorted(Counter(q["answerIndex"] for q in pack["questions"]).items()))
            print(f"{fname} / {pack['id']}: {before}  →  {after}")
        if args.apply:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("\n" + ("تغییرات ذخیره شد." if args.apply else "فقط پیش‌نمایش؛ فایلی تغییر نکرد."))
    print("اعتبارسنجی را اجرا کنید:")
    print("  python3 tools/validate_questions.py")


if __name__ == "__main__":
    main()
