# Milestone: v0.4.0 - Gmail Login & Google Drive Cloud Sync

This milestone outlines the upcoming features for Kisab v0.4.0, focusing on cloud integration and account synchronization.

## Objectives

1. **Gmail Login / Google Account Authentication**
   - Integrate Google Sign-In using OAuth 2.0.
   - Display authenticated user profile (Gmail address and avatar) in Settings.
   - Maintain offline-first local state while linking cloud identity.

2. **Google Drive Online Backup & Sync**
   - Connect to Google Drive API using authorized OAuth credentials.
   - Implement automatic/manual cloud backup upload of farm ledger JSON exports (`.json`).
   - Support restoring and syncing farm data across devices via Google Drive app-data folder.

## Implementation Tasks

- [ ] Provision OAuth scopes for Google Drive and Google Sign-In (`set_up_oauth`).
- [ ] Implement Google Auth manager in Kotlin/Android client.
- [ ] Implement Google Drive sync client (upload/download backup files).
- [ ] Add Cloud Sync section in App Settings UI.
- [ ] End-to-end testing of sync flows on real devices.
