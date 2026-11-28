import importlib.util
import unittest

ROOT = __import__("pathlib").Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location("report_question_bank", ROOT / "tools/report_question_bank.py")
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class QuestionBankTargetsTest(unittest.TestCase):
    def test_targets_sum_to_one_thousand(self):
        self.assertEqual(sum(target for _, target in MODULE.TARGETS.values()), 1000)

    def test_existing_categories_have_targets(self):
        self.assertTrue({"omoomi", "amoozesh", "bank", "dadgostari"}.issubset(MODULE.TARGETS))

    def test_new_categories_are_included(self):
        self.assertTrue({"pezeshki", "tamin_ejtemaei", "shahrdari", "dastgah_ejraei", "fani_herfei"}.issubset(MODULE.TARGETS))


if __name__ == "__main__":
    unittest.main()
