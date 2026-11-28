"""Keep the user-facing catalog and Myket SKU instructions in sync with assets."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BANK = ROOT / "app/src/main/assets/questions"
README = (ROOT / "README.md").read_text(encoding="utf-8")
MYKET = (ROOT / "docs/MYKET_RELEASE.md").read_text(encoding="utf-8")
QA = (ROOT / "docs/RELEASE_QA.md").read_text(encoding="utf-8")


class CatalogDocumentationTest(unittest.TestCase):
    def test_readme_question_totals_match_catalog(self):
        index = json.loads((BANK / "index.json").read_text(encoding="utf-8"))
        total = 0
        free = 0
        paid = 0
        for name in index["files"]:
            category = json.loads((BANK / name).read_text(encoding="utf-8"))
            for pack in category["packages"]:
                count = len(pack["questions"])
                total += count
                if pack.get("sku"):
                    paid += count
                else:
                    free += count
        self.assertEqual((743, 90, 653), (total, free, paid))
        self.assertIn("**۷۴۳ سؤال چهارگزینه‌ای** در **۹ دسته** و **۲۲ بسته**", README)
        self.assertIn("**۹۰** | **۶۵۳** | **۷۴۳**", README)

    def test_every_paid_sku_is_documented_for_myket(self):
        index = json.loads((BANK / "index.json").read_text(encoding="utf-8"))
        skus = set()
        for name in index["files"]:
            category = json.loads((BANK / name).read_text(encoding="utf-8"))
            for pack in category["packages"]:
                if pack.get("sku"):
                    skus.add(pack["sku"])
        documented = set(re.findall(r"`(pkg_[a-z0-9_]+)`", MYKET))
        self.assertEqual(13, len(skus))
        self.assertEqual(skus, documented)
        self.assertIn("تمام ۱۳ محصول", MYKET)
        self.assertIn("۱۳ محصول پولی", QA)


if __name__ == "__main__":
    unittest.main()
