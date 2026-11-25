"""Offline checks for the bundled design assets and critical theme contrast."""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"


def luminance(rgb):
    values = [int(rgb[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    values = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in values]
    return sum(a * v for a, v in zip((.2126, .7152, .0722), values))


def contrast(a, b):
    low, high = sorted((luminance(a), luminance(b)))
    return (high + .05) / (low + .05)


class VisualAssetsTest(unittest.TestCase):
    def test_offline_fonts_and_license_are_packaged(self):
        for weight in ("regular", "medium", "semibold", "bold"):
            data = (RES / f"font/vazirmatn_{weight}.ttf").read_bytes()
            self.assertEqual(data[:4], b"\x00\x01\x00\x00")
            self.assertGreater(len(data), 50_000)
        license_text = (ROOT / "app/src/main/assets/licenses/Vazirmatn-OFL.txt").read_text()
        self.assertIn("SIL OPEN FONT LICENSE", license_text.upper())
        self.assertIn("Vazirmatn Project Authors", license_text)

    def test_hero_is_a_local_jpeg_referenced_by_home(self):
        data = (RES / "drawable-nodpi/study_hero.jpg").read_bytes()
        self.assertTrue(data.startswith(b"\xff\xd8\xff") and data.endswith(b"\xff\xd9"))
        self.assertLess(len(data), 500_000)
        home = (ROOT / "app/src/main/java/com/parsaapps/nichmarket/ui/screens/home/HomeScreen.kt").read_text()
        self.assertIn("R.drawable.study_hero", home)

    def test_main_text_color_pairs_have_aa_contrast(self):
        source = (ROOT / "app/src/main/java/com/parsaapps/nichmarket/ui/theme/Color.kt").read_text()
        colors = dict(re.findall(r"val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", source))
        pairs = [
            ("OnPrimary", "Primary"), ("OnPrimaryContainer", "PrimaryContainer"),
            ("OnSecondary", "Secondary"), ("OnSecondaryContainer", "SecondaryContainer"),
            ("OnPrimaryDark", "PrimaryDark"), ("OnPrimaryContainerDark", "PrimaryContainerDark"),
            ("OnSecondaryDark", "SecondaryDark"), ("OnSecondaryContainerDark", "SecondaryContainerDark"),
            ("Primary", "SurfaceLight"), ("PrimaryDark", "SurfaceDark"),
            ("ErrorLight", "SurfaceLight"), ("ErrorDark", "SurfaceDark"),
        ]
        for foreground, background in pairs:
            with self.subTest(foreground=foreground, background=background):
                self.assertGreaterEqual(contrast(colors[foreground], colors[background]), 4.5)


if __name__ == "__main__":
    unittest.main()
