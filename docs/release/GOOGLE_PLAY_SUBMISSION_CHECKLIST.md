# Google Play Store Submission Checklist

Use this checklist to track your progress when submitting Kisab (`com.susankhya.kisab`) to the Google Play Console.

## 1. Developer Account & Setup
- [ ] Google Play Developer Account created and verified ($25 USD one-time fee paid).
- [ ] Merchant account connected (if selling paid features/subscriptions; N/A if free).

## 2. App Signing
- [ ] Production keystore generated and securely backed up (`kisab-release.jks`).
- [ ] Play App Signing opted in (recommended for Google Play).

## 3. Build & Artifacts
- [ ] App permissions verified (no restricted `REQUEST_INSTALL_PACKAGES` permission in `AndroidManifest.xml`).
- [ ] Release App Bundle (`.aab`) generated:
  ```bash
  ./gradlew :app:bundleRelease -Pkisab.privateBuildExpiryEnabled=false
  ```
- [ ] Bundle verified on a physical test device.

## 4. Play Console Store Listing
- [ ] **App Name**: Kisab
- [ ] **Short Description** (up to 80 chars): Offline-first farm ledger and financial manager for smallholders.
- [ ] **Full Description**: Detailed overview of features (Khata, Hisab, sales, purchases, calculators).
- [ ] **App Icon**: 512 × 512 px PNG.
- [ ] **Feature Graphic**: 1024 × 500 px.
- [ ] **Screenshots**: Phone and tablet screenshots uploaded.

## 5. Store Policies & Compliance
- [ ] **Privacy Policy URL**: Hosted online (via the included site templates under `/site/`) and linked in the Play Console.
- [ ] **Content Rating**: Questionnaire completed.
- [ ] **Data Safety Form**: Completed (declaring local SQLite/SharedPreferences storage and optional authentication/sync).
- [ ] **Target Audience**: Selected appropriate age group.

## 6. Testing & Rollout
- [ ] Uploaded `.aab` to **Internal Testing** track.
- [ ] Tested installation and core workflows on test devices.
- [ ] Promoted to **Production** track and submitted for Google Play review.
