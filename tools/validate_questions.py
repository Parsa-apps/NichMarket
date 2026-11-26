#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
اعتبارسنجی بانک سوالات نیچ مارکت (assets/questions/*.json)

این اسکریپت قبل از انتشار، همه فایل‌های بانک سوالات را بررسی می‌کند و در
صورت وجود هر ایراد، با کد خروجی ۱ متوقف می‌شود تا خطا وارد نسخه انتشار نشود.

بررسی‌ها:
  - JSON معتبر و ساختار کلی (دسته/بسته/سوال)
  - شناسه یکتا در هر بسته
  - دقیقاً ۴ گزینه، بدون گزینه خالی یا تکراری
  - شاخص پاسخ در بازه معتبر
  - توضیح تشریحی کافی (حداقل ۳۰ نویسه)
  - متن سوال تکراری در کل بانک نباشد
  - توازن جایگاه پاسخ صحیح در هر بسته (جلوگیری از الگوی قابل حدس)
  - وجود درس (subject) و سطح دشواری معتبر (difficulty)
  - سازگاری sku و قیمت (رایگان ⇔ sku=null و price=0)
  - زمان آزمون متناسب با تعداد سوال‌ها
  - هم‌خوانی index.json با فایل‌های موجود

اجرا:
    python3 tools/validate_questions.py
"""

import json
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
QDIR = ROOT / "app" / "src" / "main" / "assets" / "questions"

ALLOWED_DIFFICULTY = {"آسان", "متوسط", "دشوار"}
MIN_EXPLANATION_LEN = 30

errors = []
warnings = []
all_question_texts = Counter()
stats = []  # (category, pack, count, dist, subjects, difficulties)


def err(msg):
    errors.append(msg)


def warn(msg):
    warnings.append(msg)


def norm(text):
    return " ".join((text or "").split())


def check_pack(cat_id, pack):
    pid = pack.get("id", "?")
    where = f"{cat_id}/{pid}"
    questions = pack.get("questions", [])
    n = len(questions)

    if n == 0:
        err(f"{where}: بسته بدون سوال است")
        return

    # سازگاری sku و قیمت
    sku = pack.get("sku")
    price = pack.get("priceToman", 0)
    if sku is None and price != 0:
        err(f"{where}: بسته رایگان (sku=null) نباید قیمت داشته باشد (priceToman={price})")
    if sku is not None and (not sku or price <= 0):
        err(f"{where}: بسته پولی باید sku و priceToman مثبت داشته باشد (sku={sku!r}, price={price})")

    # زمان آزمون: حداقل ۳۰ ثانیه و حداکثر ۳ دقیقه به ازای هر سوال
    dur = pack.get("durationMinutes", 0)
    if not (0.5 * n <= dur <= 3 * n):
        warn(f"{where}: durationMinutes={dur} برای {n} سوال غیرمعمول است (پیشنهاد: حدود {n} تا {int(1.5 * n)})")

    ids = [q.get("id") for q in questions]
    if len(ids) != len(set(ids)):
        dup = [i for i, c in Counter(ids).items() if c > 1]
        err(f"{where}: شناسه سوال تکراری: {dup}")
    if any(not isinstance(i, int) or i <= 0 for i in ids):
        err(f"{where}: شناسه سوال باید عدد مثبت باشد")

    dist = Counter()
    subjects = Counter()
    difficulties = Counter()

    for q in questions:
        qid = q.get("id", "?")
        qw = f"{where} Q{qid}"
        text = norm(q.get("text", ""))
        options = q.get("options", [])
        ai = q.get("answerIndex")
        expl = norm(q.get("explanation", ""))

        if not text:
            err(f"{qw}: متن سوال خالی است")
        all_question_texts[text] += 1

        if len(options) != 4:
            err(f"{qw}: باید دقیقاً ۴ گزینه داشته باشد (دارد {len(options)})")
        else:
            if any(not norm(o) for o in options):
                err(f"{qw}: گزینه خالی وجود دارد")
            if len({norm(o) for o in options}) != len(options):
                err(f"{qw}: گزینه تکراری وجود دارد")

        if not isinstance(ai, int) or not (0 <= ai < len(options)):
            err(f"{qw}: answerIndex نامعتبر است ({ai!r})")
        else:
            dist[ai] += 1

        if len(expl) < MIN_EXPLANATION_LEN:
            err(f"{qw}: توضیح تشریحی خیلی کوتاه است ({len(expl)} نویسه)")

        subject = norm(q.get("subject", ""))
        if not subject:
            err(f"{qw}: فیلد subject (درس) خالی است")
        else:
            subjects[subject] += 1

        diff = norm(q.get("difficulty", ""))
        if diff not in ALLOWED_DIFFICULTY:
            err(f"{qw}: difficulty نامعتبر است ({diff!r}) — مجاز: {sorted(ALLOWED_DIFFICULTY)}")
        else:
            difficulties[diff] += 1

        for bad in ("TODO", "لورم", "Lorem", "xxx"):
            if bad in text or bad in expl:
                err(f"{qw}: متن ناقص/placeholder یافت شد ({bad!r})")

    # توازن جایگاه پاسخ صحیح
    if n >= 20:
        lo, hi = 0.15 * n, 0.40 * n
        for idx in range(4):
            c = dist.get(idx, 0)
            if not (lo <= c <= hi):
                err(f"{where}: توزیع پاسخ نامتوازن است — گزینه {idx} تعداد {c} از {n} "
                    f"(مجاز: {lo:.0f} تا {hi:.0f})")
    else:
        for idx in range(4):
            c = dist.get(idx, 0)
            if c == 0:
                err(f"{where}: هیچ سوالی پاسخش گزینه {idx} نیست (الگوی قابل حدس)")
            if c > 0.4 * n:
                err(f"{where}: گزینه {idx} بیش از حد تکرار شده ({c} از {n})")

    stats.append((cat_id, pid, n, dict(sorted(dist.items())), dict(subjects), dict(difficulties)))


def valid_pack_schema(pack, where):
    """Reject malformed types before accessing values, including JSON booleans as integers."""
    if not isinstance(pack, dict):
        err(f"{where}: بسته باید شیء JSON باشد")
        return False
    for key in ("id", "title"):
        if not isinstance(pack.get(key), str) or not pack[key].strip():
            err(f"{where}: {key} باید متن غیرخالی باشد")
            return False
    if pack.get("sku") is not None and not isinstance(pack.get("sku"), str):
        err(f"{where}: sku باید متن یا null باشد")
        return False
    for key in ("priceToman", "durationMinutes"):
        if type(pack.get(key)) is not int or pack[key] < (1 if key == "durationMinutes" else 0):
            err(f"{where}: {key} نامعتبر است")
            return False
    questions = pack.get("questions")
    if not isinstance(questions, list) or not questions:
        err(f"{where}: questions باید فهرست غیرخالی باشد")
        return False
    for q in questions:
        if not isinstance(q, dict) or type(q.get("id")) is not int or type(q.get("answerIndex")) is not int:
            err(f"{where}: ساختار یا شناسه/پاسخ سؤال نامعتبر است")
            return False
        if any(not isinstance(q.get(k), str) for k in ("text", "explanation", "subject", "difficulty")):
            err(f"{where}: متن، توضیح، درس و دشواری باید متن باشند")
            return False
        if not isinstance(q.get("options"), list) or not all(isinstance(o, str) for o in q["options"]):
            err(f"{where}: options باید فهرست متن باشد")
            return False
    return True


def main():
    if not QDIR.is_dir():
        print(f"پوشه بانک سوالات یافت نشد: {QDIR}")
        return 1

    index_path = QDIR / "index.json"
    try:
        index = json.loads(index_path.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"خطا در خواندن index.json: {e}")
        return 1

    listed = index.get("files") if isinstance(index, dict) else None
    if not isinstance(listed, list) or not all(isinstance(x, str) and Path(x).name == x for x in listed):
        print("index.json باید فهرست نام فایل‌های معتبر داشته باشد")
        return 1
    actual = sorted(p.name for p in QDIR.glob("*.json") if p.name != "index.json")
    if sorted(listed) != actual:
        err(f"index.json با فایل‌های موجود هم‌خوان نیست — ثبت‌شده: {sorted(listed)} — موجود: {actual}")

    total_q = 0
    category_ids, pack_ids, skus = set(), set(), set()
    for fname in listed:
        fpath = QDIR / fname
        if not fpath.exists():
            err(f"فایل ثبت‌شده در index.json وجود ندارد: {fname}")
            continue
        try:
            data = json.loads(fpath.read_text(encoding="utf-8"))
        except Exception as e:
            err(f"{fname}: JSON نامعتبر است ({e})")
            continue

        if not isinstance(data, dict) or not isinstance(data.get("id"), str) or not data["id"].strip():
            err(f"{fname}: شناسه دسته نامعتبر است")
            continue
        cat_id = data["id"]
        if cat_id in category_ids:
            err(f"{fname}: شناسه دسته تکراری: {cat_id}")
        category_ids.add(cat_id)
        if not isinstance(data.get("title"), str) or not data["title"].strip():
            err(f"{fname}: عنوان دسته خالی است")
        packages = data.get("packages")
        if not isinstance(packages, list) or not packages:
            err(f"{fname}: فهرست بسته‌ها خالی یا نامعتبر است")
            continue
        for pack in packages:
            if not valid_pack_schema(pack, fname):
                continue
            pid = pack["id"]
            if pid in pack_ids:
                err(f"{fname}: شناسه بسته تکراری: {pid}")
            pack_ids.add(pid)
            sku = pack.get("sku")
            if sku is not None:
                if sku in skus:
                    err(f"{fname}: شناسه محصول تکراری: {sku}")
                skus.add(sku)
            check_pack(cat_id, pack)
            total_q += len(pack["questions"])

    for text, c in all_question_texts.items():
        if c > 1:
            err(f"متن سوال تکراری در بانک ({c} بار): «{text[:80]}…»")

    print("=" * 78)
    print("گزارش بانک سوالات نیچ مارکت")
    print("=" * 78)
    for cat_id, pid, n, dist, subjects, difficulties in stats:
        print(f"\n[{cat_id}] {pid} — {n} سوال")
        print(f"  توزیع پاسخ صحیح: " + "، ".join(f"گزینه {k}: {v}" for k, v in dist.items()))
        print(f"  دروس: " + "، ".join(f"{k} ({v})" for k, v in subjects.items()))
        print(f"  دشواری: " + "، ".join(f"{k}: {v}" for k, v in difficulties.items()))
    print(f"\nمجموع کل: {total_q} سوال در {len(stats)} بسته")

    if warnings:
        print("\nهشدارها:")
        for w in warnings:
            print(f"  ⚠ {w}")

    if errors:
        print(f"\n❌ {len(errors)} خطا یافت شد:")
        for e in errors:
            print(f"  ✗ {e}")
        return 1

    print("\n✅ بررسی‌های ساختاری پاس شد؛ بازبینی تخصصی محتوای سوالات همچنان لازم است.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
