# Data, privacy, accessibility and practical risks

## Data inventory

| Data | Purpose | Storage | Removal |
|---|---|---|---|
| Email / display name / password hash | Account access and identifying the other participant | Supabase Auth / profiles; passwords handled by Auth, never saved by Android | Team project administrator removes account and dependent data |
| Session access/refresh tokens | Remember login | Encrypted SharedPreferences; AES key in Android Keystore | Sign out or clear app data; issued server tokens expire separately |
| Listings / likes / reservation user ID | Main reuse flow | Supabase PostgreSQL + account-specific Room cache | Delete listing in app (soft deletion); administrator can permanently purge |
| Photo | Show the offered item | Resized JPEG in private app storage and private Supabase bucket; local downloaded cache | Remove/replace in form; old files require administrator/local-cache cleanup |
| Conversation participants / messages | Arrange pickup | Supabase + account-specific Room | Administrator removes requested conversation/account data; retained after listing soft deletion |
| Outbox / last sync / demo-error flag | Offline retry and UI state | Private per-account database/preferences | Confirmation removes successful outbox rows; rejected changes can be discarded; clear app data removes local queues |

No analytics, location tracking, contacts or advertising SDK was added. Only INTERNET, ACCESS_NETWORK_STATE and optional CAMERA permissions are used. The gallery uses Android's system picker. Uploaded images are re-encoded as JPEG rather than copying EXIF metadata such as location. Android cloud backup and device transfer exclude private data.

For permanent removal: identify the account in Supabase; remove its storage objects through Storage; remove associated messages and conversations, likes, owned listings and processed actions in dependency order, release its reservations, then delete the Auth user (its profile cascades). Remove local data with Android Settings → Apps → DormSwap → Storage → Clear storage. Uninstalling does not remove hosted data. Soft-deleting an item intentionally preserves conversations. This administrative process is a demo limitation; there is no self-service account deletion UI.

## Provenance

M1/M2 screenshots, design exports and synthetic fixtures remain historical artifacts. M3 starts with an empty cloud database; it does not upload M2 synthetic rows automatically. M3 test accounts, messages and item descriptions are fictional. Use only photos the team created or has permission to share. Do not enter actual personal student data in development tests or AI prompts.

## Risks and implemented controls

- **Domain-only registration can be impersonated:** exact domain is enforced on Android and by an Auth database trigger. Email activation is disabled at the user's request. Do not claim verified student identity or mailbox ownership.
- **Cross-account access:** per-account local databases; server functions validate the authenticated user; row policies restrict conversations/messages and likes; no direct client table writes; private photo bucket. Local policy and cache-isolation tests passed, hosted security tests pending.
- **Conflicting/offline reservations:** local states are provisional; server locks the listing row and rejects later conflicting requests. UI restores server truth and shows rejected changes in Account.
- **Duplicate sends:** operation UUID deduplication and transactional Room outbox. No fake successful uploads in the active M3 path.
- **Credential leakage:** only a public project key in APK, encrypted tokens, no passwords saved, no request/token logging. Local configuration and `passwd.md` are ignored. Do not include these files in submissions.
- **Lost/stale data:** offline cache is not a guarantee of availability. Clearing app data loses unsent changes. Chat refresh runs only while visible; no background upload guarantee.
- **Cloud cost/abuse:** messages max 2000 characters, listing description max 2000, photos max 1280 px and bucket max 5 MB JPEG. No moderation/rate-limiting layer beyond the service's built-in controls. Monitor quotas for the demonstration; this is not a production readiness claim.
- **Retention:** old photos and operation deduplication records can accumulate; no automatic retention job. Administrator cleanup is required.

## Accessibility evidence

Implemented: labelled email/password/form fields; password visibility control; headings and descriptive icons; scrollable forms/chips; adaptive feed columns for enlarged text; minimum-height primary buttons; validation live region on login and listing form.

Verified: instrumented test locates the login/registration controls by accessibility text and detects the invalid-domain message. Actual screenshots are included. Large-text test status is recorded in the test summary after execution.

Still required: TalkBack traversal and complete main flow, 200% text on authenticated screens, keyboard and landscape checks, camera fallback controls. No claim of comprehensive accessibility or WCAG compliance.

## AI/cloud decision

No AI feature was added to the product. It would not help the agreed reuse task enough to justify new data exposure and complexity. Supabase is justified by the explicit need to share accounts, listings and messages across devices; a local mock cannot meet that requirement.
