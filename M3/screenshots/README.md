# Actual M3 emulator screenshots

Captured from the running debug APK on Medium Phone API 36.1, 2026-11-18, through `M3SmokeTest`. These are not mockups or M2 screenshots.

- `01_login.png`: mandatory login entry screen.
- `02_registration.png`: registration form with exact university-domain rule.
- `03_invalid_email.png`: rejected non-university email, before contacting the backend.

Authenticated Feed, Details, Add/Edit, Chat, synced photo and offline/error screenshots are **pending Supabase configuration and end-to-end testing**. Add them after testing the hosted project. Do not present these login-only screenshots as evidence that cross-device synchronization was verified.

## Authenticated screenshots (verified hosted run)

- `06_authenticated_feed.png`: My listings after signing up and publishing through Supabase.
- `07_listing_details.png`: own listing, with editing/deletion controls.
- `08_edit_listing.png`: edit form loaded from the saved listing.

These were captured from the running emulator with a fictional Demo Student account. Hosted camera upload, physical-phone and chat-screen screenshots remain manual follow-up work.
