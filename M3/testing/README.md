# Test summary

Verification date: 2026-11-18. The historical commit timestamps requested by the team are not test execution dates.

| Check | Observed result |
|---|---|
| `./gradlew assembleDebug` | PASS |
| `./gradlew test` | 28 tests, 0 failures, 0 errors |
| `./gradlew connectedDebugAndroidTest` | 3 tests, 0 failures, 0 errors on Medium Phone API 36.1 |
| Local PostgreSQL schema + policy checks | PASS; see `backend-local.txt` |
| Hosted Supabase / two authenticated sessions | PASS: signup/login, listings, reservations, chat, cancellation/deletion; see supabase-live.txt |
| Physical phone, camera permissions, offline/reconnect | Pending |
| Independent teammate build / two adult peer sessions | Pending team work |

Unit coverage: ListingValidator (including maximum description length), FeedFilter, historical M2 JSON/merge logic, AuthValidator (exact domain and password boundaries), M3 cloud merge (rejected reservation, offline drafts, deletion, cross-device likes), photo sizing and EXIF transformations.

Device coverage: application identity; separate Room account databases with persisted outbox after reopen; labelled login/registration controls and rejection of an invalid email before a network request. Device tests use isolated fictional database IDs and remove only their own test databases.

Local SQL tests cover domain restriction, owner-only writes, own-item reservation rejection, already-reserved conflict, cancellation ownership, operation deduplication, private conversations/messages, edit blocking and conversation retention after deleting a listing. These are sequential policy checks; a simultaneous two-device race still needs the manual checklist.

For local SQL verification, use a **new disposable PostgreSQL database** and run, in order, `backend/local-test-bootstrap.sql`, `backend/schema.sql`, `backend/policy-tests.sql` with `psql -v ON_ERROR_STOP=1`. The bootstrap emulates only the minimal Supabase auth/storage interfaces; never run it in the hosted project. Policy test rows are rolled back.

Evidence: `build-and-tests.txt`, `backend-local.txt`, `photo-before.txt`, `photo-after.txt`, and actual PNGs in `../screenshots/`. Do not treat M2 screenshots as M3 verification.

Hosted Android screenshot scenario: run explicitly with
`./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.myapplication.M3SmokeTest#hostedAccountScreenshots -Pandroid.testInstrumentationRunnerArguments.hosted=true`.
It creates a fictional account and synthetic listing, visits My listings / Details / Edit, and removes its listing on success. It is skipped in normal runs to avoid creating accounts accidentally. Retried development runs left additional fictional Demo accounts/listings; the team may remove these before the presentation.
