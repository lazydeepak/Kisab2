# Google Play Store Publishing Preparation Guide

This guide outlines the steps and preparations required to publish Kisab (`com.susankhya.kisab`) to the Google Play Store.

## 1. Google Play Developer Account
- **Cost**: A one-time registration fee of **$25 USD**.
- **Process**: Register at the [Google Play Console](https://play.google.com/console/), provide developer details, and complete identity verification using a valid government ID.

## 2. Release Configuration & App Bundles (`.aab`)
Google Play requires apps to be submitted as **Android App Bundles (`.aab`)** rather than standalone APKs.

### Build a Signed App Bundle Locally:
```bash
# Ensure release signing environment variables are set:
export KISAB_KEYSTORE_PATH=/absolute/path/to/release.jks
export KISAB_KEYSTORE_PASSWORD=...
export KISAB_KEY_ALIAS=...
export KISAB_KEY_PASSWORD=...

# Build the release App Bundle with private build expiry disabled for public store release:
./gradlew :app:bundleRelease -Pkisab.privateBuildExpiryEnabled=false
```

*Note on Private Build Expiry*: Private pilot APK drops in Kisab include a 90-day build lifetime check (`app/build.gradle.kts`). For public Google Play Store releases, pass `-Pkisab.privateBuildExpiryEnabled=false` to ensure store builds never expire.

## 3. Store Listing Assets Checklist
Before uploading to the Play Console, prepare the following metadata and graphics:
- **App Name**: Kisab
- **Short Description** (up to 80 chars): Offline-first farm ledger and financial manager for smallholders.
- **Full Description**: Detailed explanation of Kisab's features (Khata, Hisab, sales, purchases, payments, agricultural calculators, offline storage).
- **App Icon**: 512 × 512 px PNG (32-bit color, opaque).
- **Feature Graphic**: 1024 × 500 px banner image.
- **Phone Screenshots**: At least 2–4 high-resolution screenshots of key screens (Today Dashboard, Party Khata, Record sheets, Kisan Calculators).

## 4. Compliance, Privacy Policy & Disclosures
- **Privacy Policy URL**: Required by Google Play because Kisab handles user financial records and local/cloud sync features. Publish a privacy policy page and provide its public HTTPS URL in the Play Console.
- **Content Rating Questionnaire**: Fill out the IARC content rating questionnaire in the console (typically rated for Productivity / Finance, Everyone).
- **Data Safety Form**: Accurately declare data practices (local device persistence, optional email OTP authentication / cloud sync).

## 5. Publishing Workflow
1. **Internal Testing Track**: Upload your signed `.aab` to the Internal Testing track first. Invite initial testers to verify installation, runtime permissions, and stability.
2. **Closed/Open Testing** (Optional): Expand testing to a wider pilot group.
3. **Production Track**: Submit for Google Play review (typically takes 24–48 hours). Once approved, release publicly to users worldwide.
