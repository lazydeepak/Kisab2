# Kisab M16 — Cross-Platform Upgrade Plan (Android, iOS, Windows, macOS)

## Status

**PROPOSED & ROUTED**

- **Maintainer Decision (2026-09-24):** M14 physical field pilot is deferred due to absence of local farmer participants in the immediate environment. Work shifts to cross-platform upgrade as authorized by ADR-0002 reactivation conditions.
- **Target Platforms:** Android, iOS, Windows, macOS.
- **Technology Strategy:** Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP).

---

## 1. Context & Objectives

Kisab is currently an offline-first Android farm ledger (`com.susankhya.kisab`) built with Kotlin, XML UI layouts, and an offline key-value storage engine (`FarmPersistenceCodec` / `MultiFarmStore`). 

The objective of Milestone 16 is to elevate Kisab into a modern cross-platform application sharing **>90% of its codebase** across:
1. **Android** (phones & tablets)
2. **iOS** (iPhones & iPads)
3. **Windows** (desktop PCs & laptops)
4. **macOS** (MacBooks & desktops)

The migration preserves all existing business logic, calculations, data models, schema-14 persistence structures, and backup/restore formats without data loss or semantic divergence.

---

## 2. Architectural Blueprint

```text
                                 kisab-root
                                     │
         ┌───────────────────────────┴───────────────────────────┐
         ▼                                                       ▼
   :shared (KMP)                                           :composeApp (CMP)
   ├── commonMain                                          ├── commonMain
   │   ├── domain (Entities, Calculators, Totals)          │   ├── navigation (Adaptive Scaffold)
   │   ├── persistence (MultiFarmStore, Codecs)            │   ├── screens (Overview, Khata, Records,
   │   ├── localization (BS Calendar, Nepali formats)      │   │            Tools, Farm Planning)
   │   └── backup (Export/Import engine)                   │   ├── components (Inputs, Pickers, Tiles)
   │                                                       │   └── theme (Material 3 tokens, Typography)
   ├── androidMain (SharedPreferences backend)             ├── androidMain (Activity entry point)
   ├── desktopMain (File/Preferences backend)              ├── desktopMain (Window, Menu bar, Shortcuts)
   └── iosMain (NSUserDefaults/File backend)               └── iosMain (ComposeUIViewController)
```

### Module Responsibilities

1. **`:shared` (Pure Kotlin Multiplatform)**
   - **`domain/`**: `FarmState`, `Party`, `Trade`, `Settlement`, `FarmTransaction`, `FarmActivities`, `Production`, `KisanCalculators`, `FarmTotals`, `PartyLedger`.
   - **`persistence/`**: `MultiFarmStoreBackend` interface with platform implementations. Retains `FarmPersistenceCodec` (Schema 14) and `FarmBackupCodec` byte-for-byte.
   - **`i18n/`**: Nepali Bikram Sambat calendar calculation, Devanagari numerals, currency formatters (`Rs.`, `रू`), and unit conversions.
   - **`test/`**: 600+ domain unit tests running uniformly against JVM and Native targets.

2. **`:composeApp` (Compose Multiplatform UI)**
   - 100% shared declarative UI written in Compose Multiplatform.
   - **Adaptive Layout Engine**:
     - *Compact window size (Mobile: Android & iOS)*: Bottom navigation bar, modal sheets for Record actions, vertical Khata list.
     - *Expanded window size (Desktop: Windows & macOS)*: Persistent left sidebar navigation, split-view Master-Detail (Party list on left, Khata ledger on right), fast keyboard data entry.
   - Accessible Devanagari font rendering across Skia (Desktop/Android/iOS).

3. **Platform Entry Points**:
   - **`:androidApp` / `:composeApp:androidMain`**: Extends `ComponentActivity`, binds Android lifecycle and permissions.
   - **`:desktopApp` / `:composeApp:desktopMain`**: `main()` function running Compose for Desktop with native window chrome, menu items, and file dialogs.
   - **`:iosApp`**: Minimal SwiftUI application hosting `ComposeUIViewController`.

---

## 3. Ground Truth & Compatibility Invariants

1. **Zero Domain Rewrites:** All existing domain equations (e.g. `PartyHisabCalculator`, `ProductionAllocation`, `TradeType`, `Settlement`) migrate directly to `commonMain`.
2. **Schema 14 Stability:** The persistence codec retains Schema 14 layout. Any backup archive (`.backup` / `.json`) created on an Android phone can be imported on Windows or macOS, and vice versa.
3. **Offline Autonomy:** Every platform maintains full functionality without requiring an internet connection or cloud account.
4. **Platform-Neutral Storage:**
   - Android: `SharedPreferencesFarmStore`
   - Desktop (Windows/macOS): `PreferencesFarmStore` (using Java Preferences or standard `AppData`/`Application Support` file storage).
   - iOS: `NSUserDefaultsFarmStore` or local documents storage.

---

## 4. Execution Phases

### Phase 1: Shared Core Extraction (`:shared`)
- Configure root Gradle for KMP: `kotlin("multiplatform")`.
- Move `com.susankhya.kisab.domain` into `shared/src/commonMain/kotlin`.
- Move `FarmPersistenceCodec`, `FarmBackupCodec`, and `MultiFarmStore` into `shared/src/commonMain/kotlin`.
- Verify domain test suite passes under Gradle multiplatform test runners.

### Phase 2: Platform Backends & Desktop Engine
- Implement `DesktopFileStoreBackend` for Windows/macOS file system persistence.
- Implement file export/import dialog adapters for Desktop (using system file choosers).
- Validate round-trip backup exports across desktop and mobile.

### Phase 3: Compose Multiplatform UI Foundation
- Port theme tokens (colors, shapes, typography) from XML `values/` and `values-night/` into Compose `MaterialTheme`.
- Build the adaptive navigation shell:
  - Farm Switcher & Overview Dashboard
  - Record Action Sheet (Sale, Purchase, Cash In, Cash Out, Production)
  - Khata (Customer & Supplier Ledger)
  - Farm Activities & Production Allocation
  - Kisan Calculator Toolbox (feed, seed, fertilizer, unit converters)
- Add Devanagari typography support with font fallback.

### Phase 4: Platform Builds & Distribution
- **Android:** Package existing `com.susankhya.kisab` APK/AAB from Compose UI.
- **Desktop (Windows):** Package `.msi` / `.exe` installer.
- **Desktop (macOS):** Package `.dmg` / `.app` bundle with macOS menu bar integration.
- **iOS:** Configure Xcode project and test on iOS Simulator.

---

## 5. Verification & Quality Gates

| Target | Validation Gate | Criteria |
| :--- | :--- | :--- |
| **Domain Core** | `./gradlew :shared:allTests` | 100% PASS across JVM and Native test runners |
| **Android** | `./gradlew :app:assembleDebug` | Valid debug APK, zero regressions in local persistence |
| **Desktop** | `./gradlew :desktopApp:run` / `:desktopApp:package` | Desktop app launches on port/window, persists data across restarts |
| **Data Integrity** | Round-trip backup test | Export on Android -> Import on Desktop -> Identical balances |
