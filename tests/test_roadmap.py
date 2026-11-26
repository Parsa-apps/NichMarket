"""Keep the user-requested 100-phase roadmap complete and navigable."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class RoadmapTest(unittest.TestCase):
    def test_one_hundred_phases_with_deliverables_and_done_criteria(self):
        text = (ROOT / 'docs/ROADMAP_100.md').read_text(encoding='utf-8')
        phases = re.findall(r'^### فاز (\d+):.*\n([^\n]+)', text, re.MULTILINE)
        self.assertEqual(list(range(1, 101)), [int(number) for number, _ in phases])
        for number, body in phases:
            with self.subTest(phase=number):
                for field in ('**هدف:**', '**اقدامات:**', '**خروجی:**', '**معیار اتمام:**'):
                    self.assertIn(field, body)
