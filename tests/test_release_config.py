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
        self.assertRegex(gradle, r'versionCode\s*=\s*4\b')
        self.assertRegex(gradle, r'versionName\s*=\s*"1\.1\.2"')

class StoreIsolationTest(unittest.TestCase):
    def test_store_flavors_and_keys_are_separate(self):
        gradle = (ROOT / 'app/build.gradle.kts').read_text()
        self.assertIn('applicationIdSuffix = ".bazaar"', gradle)
        bazaar = gradle.split('create("bazaar")', 1)[1].split('\n        }', 1)[0]
        self.assertIn('versionCode = 4', bazaar)
        self.assertIn('versionName = "1.1.2"', bazaar)
        self.assertIn('providers.environmentVariable("BAZAAR_PUBLIC_KEY")', gradle)
        self.assertIn('require(bazaarPublicKey != myketPublicKey)', gradle)
        self.assertIn('config/bazaar-public-key.txt', gradle)
        self.assertIn('BAZAAR_PUBLIC_KEY does not match', gradle)
        self.assertIn('"myketImplementation"(libs.myket.billing)', gradle)
        self.assertIn('"bazaarImplementation"("com.github.cafebazaar.Poolakey:poolakey:2.3.0")', gradle)

    def test_bazaar_public_key_is_the_one_provided(self):
        key = (ROOT / 'config/bazaar-public-key.txt').read_text().strip()
        decoded = base64.b64decode(key, validate=True)
        self.assertEqual(hashlib.sha256(decoded).hexdigest(), 'ac2adecb71760e4aa608e6fa32bdb042b4f25d094ed649c27f4437bfdff2b432')

    def test_receipts_must_match_store_and_app(self):
        manager = (ROOT / 'app/src/bazaar/java/com/parsaapps/nichmarket/billing/BazaarBillingManager.kt').read_text()
        self.assertIn('SecurityCheck.Enable(BuildConfig.IAB_PUBLIC_KEY)', manager)
        self.assertIn('purchase.packageName != context.packageName', manager)
        self.assertIn('purchase.payload != payload', manager)
        self.assertIn('it.purchaseState == BazaarPurchaseState.PURCHASED', manager)
        self.assertNotIn('ir.myket.billingclient', manager)
