"""Regression tests for the bank validator; no Android SDK required."""
import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/assets/questions"


class ValidationTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.bank = Path(self.tmp.name) / "bank"
        shutil.copytree(SOURCE, self.bank)

    def tearDown(self):
        self.tmp.cleanup()

    def run_validator(self):
        code = "import sys, tools.validate_questions as v; from pathlib import Path; v.QDIR=Path(sys.argv[1]); sys.exit(v.main())"
        return subprocess.run([sys.executable, "-c", code, str(self.bank)], cwd=ROOT, capture_output=True, text=True)

    def change(self, fn):
        path = self.bank / "bank.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        fn(data)
        path.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")

    def test_current_bank_passes(self):
        self.assertEqual(self.run_validator().returncode, 0)

    def test_rejects_duplicate_pack_id(self):
        self.change(lambda d: d["packages"][1].__setitem__("id", d["packages"][0]["id"]))
        result = self.run_validator()
        self.assertEqual(result.returncode, 1)
        self.assertIn("شناسه بسته تکراری", result.stdout)

    def test_rejects_malformed_question(self):
        self.change(lambda d: d["packages"][0]["questions"][0].__setitem__("options", None))
        result = self.run_validator()
        self.assertEqual(result.returncode, 1)
        self.assertIn("options", result.stdout)
        self.assertNotIn("Traceback", result.stderr)

    def test_rejects_invalid_answer(self):
        self.change(lambda d: d["packages"][0]["questions"][0].__setitem__("answerIndex", 4))
        result = self.run_validator()
        self.assertEqual(result.returncode, 1)
        self.assertIn("answerIndex", result.stdout)


if __name__ == "__main__":
    unittest.main()
