"""Protect the official publisher details embedded in the offline About page."""
import struct
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ABOUT = ROOT / 'app/src/main/java/com/parsaapps/nichmarket/ui/screens/about/AboutScreen.kt'
LOGO = ROOT / 'app/src/main/res/drawable-nodpi/parsa_apps_logo.png'


class PublisherIdentityTest(unittest.TestCase):
    def test_about_contains_verified_contact_information(self):
        text = ABOUT.read_text(encoding='utf-8')
        for value in ('https://parsa-apps.github.io/', 'farshadparsa2019@gmail.com',
                      'https://t.me/Parsaappsadmin', 'https://instagram.com/parsa_apps',
                      'https://github.com/Parsa-apps', 'فرشاد پارسا'):
            self.assertIn(value, text)

    def test_brand_logo_is_a_valid_png_header(self):
        data = LOGO.read_bytes()
        self.assertEqual(b'\x89PNG\r\n\x1a\n', data[:8])
        self.assertEqual(b'IHDR', data[12:16])
        self.assertEqual((320, 320), struct.unpack('>II', data[16:24]))


if __name__ == '__main__':
    unittest.main()
