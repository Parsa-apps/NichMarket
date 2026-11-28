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

    def test_split_categories_are_merged_and_question_texts_are_unique(self):
        import re
        categories = []
        texts = []
        index = json.loads((BANK / 'index.json').read_text(encoding='utf-8'))
        for name in index['files']:
            category = json.loads((BANK / name).read_text(encoding='utf-8'))
            categories.append(category['id'])
            for pack in category['packages']:
                for question in pack['questions']:
                    value = question['text'].replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک')
                    value = value.replace('ۀ', 'ه').replace('ة', 'ه').replace('‌', '')
                    value = re.sub(r'''[؟?!.,،؛:«»"'ـ]''', "", value)
                    texts.append(' '.join(value.split()).strip())
        self.assertEqual(len(categories), len(set(categories)))
        self.assertEqual(9, len(categories))
        self.assertEqual(len(texts), len(set(texts)))
        package_ids = {p['id'] for name in index['files']
                       for p in json.loads((BANK / name).read_text(encoding='utf-8'))['packages']}
        self.assertTrue({'bank_full_2', 'dadgostari_full_2', 'omoomi_full_2', 'amoozesh_full_2'} <= package_ids)
        self.assertFalse({'bank2.json', 'dadgostari2.json', 'omoomi2.json', 'amoozesh2.json'} & set(index['files']))


if __name__ == '__main__':
    unittest.main()
