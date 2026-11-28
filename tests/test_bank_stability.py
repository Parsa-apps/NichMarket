"""Protect the existing question IDs, wording, options and answers during future expansions."""
import hashlib
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BANK = ROOT / 'app/src/main/assets/questions'
FIXTURES = ROOT / 'tests/fixtures/bank_published_prefix.json'


class BankStabilityTest(unittest.TestCase):
    def test_existing_questions_are_unchanged(self):
        expected = json.loads(FIXTURES.read_text(encoding='utf-8'))
        found = set()
        for name in json.loads((BANK / 'index.json').read_text(encoding='utf-8'))['files']:
            category = json.loads((BANK / name).read_text(encoding='utf-8'))
            for pack in category['packages']:
                spec = expected[pack['id']]
                payload = json.dumps(pack['questions'][:spec['count']], ensure_ascii=False,
                                     sort_keys=True, separators=(',', ':')).encode()
                self.assertEqual(spec['sha256'], hashlib.sha256(payload).hexdigest(), pack['id'])
                found.add(pack['id'])
        self.assertEqual(set(expected), found)

    def test_expanded_bank_and_product_ids(self):
        expected = {'omoomi_full_1': ('pkg_omoomi_full_1', 144),
                    'amoozesh_full_1': ('pkg_amoozesh_full_1', 110),
                    'bank_full_1': ('pkg_bank_full_1', 106),
                    'dadgostari_full_1': ('pkg_dadgostari_full_1', 103)}
        total = 0
        for name in json.loads((BANK / 'index.json').read_text(encoding='utf-8'))['files']:
            category = json.loads((BANK / name).read_text(encoding='utf-8'))
            for pack in category['packages']:
                total += len(pack['questions'])
                if pack['id'] in expected:
                    self.assertEqual(expected[pack['id']], (pack['sku'], len(pack['questions'])))
        self.assertEqual(743, total)


if __name__ == '__main__':
    unittest.main()
