"""Catch accidental changes to the requested Myket key and release identifiers."""
import base64
import hashlib
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class ReleaseConfigTest(unittest.TestCase):
    def test_public_key_is_the_one_provided_for_this_app(self):
        key = (ROOT / 'config/myket-public-key.txt').read_text().strip()
        decoded = base64.b64decode(key, validate=True)
        # SHA-256 of the X.509 SubjectPublicKeyInfo, not a signing certificate.
        self.assertEqual(hashlib.sha256(decoded).hexdigest(), 'f0794f3f489406021f1d82dc774080907a64abc2e7e2cc9362756baefba4a3c8')

    def test_release_identifiers(self):
        gradle = (ROOT / 'app/build.gradle.kts').read_text()
        self.assertRegex(gradle, r'applicationId\s*=\s*"com\.parsaapps\.nichmarket"')
        self.assertRegex(gradle, r'versionCode\s*=\s*3\b')
        self.assertRegex(gradle, r'versionName\s*=\s*"1\.1\.1"')
