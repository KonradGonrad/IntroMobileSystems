# Supabase setup

Use a separate, empty project. Never put a service-role key, database password or access token in the Android app or repository.

1. Run `schema.sql` once in the project's SQL Editor, then run [council-import.sql](council-import.sql). For an existing M3 database, run only the council migration. It preserves existing listings and gives existing accounts the standard student role.
2. In Authentication → Sign In / Providers → Email, disable **Confirm email**. Set minimum password length to **8**. Keep email/password sign-in enabled. Registration is immediate; it checks the address domain, not mailbox ownership or student status.
3. In the project Connect/API settings copy the project URL and publishable (or legacy anon) key.
4. Add these values to the ignored root `local.properties`, alongside `sdk.dir`:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLIC_PUBLISHABLE_OR_ANON_KEY
```

The build also accepts environment variables with these names. Public keys identify the project; database policies enforce access. No configuration means the login screen explains how to configure it, rather than using fake authentication.

5. Build and install the same APK on the emulator and a phone. Create two **fictional demonstration accounts** ending exactly in `@edu.p.lodz.pl`; do not impersonate real students. Email delivery is disabled for signup.

Registration creates a profile through a database trigger. An invalid domain is rejected even when bypassing Android validation. Profiles expose display names, not email addresses. Writes use authenticated RPCs, not unrestricted table inserts. The first successfully committed reservation wins. Operation UUIDs prevent retries from creating duplicate messages or applying old reservations twice.

Official references: [email configuration](https://supabase.com/docs/guides/auth/general-configuration), [sessions](https://supabase.com/docs/guides/auth/sessions), [user data and triggers](https://supabase.com/docs/guides/auth/managing-user-data), [row level security](https://supabase.com/docs/guides/database/postgres/row-level-security).

Setup and hosted integration status: **pending execution in the team's project**. Local source files alone do not provision Supabase.

Live API verification subsequently passed; see `../testing/supabase-live.txt`. Re-run from the repository root with `python3 M3/testing/live-backend.py`. This creates two fictional accounts and an archived conversation; it soft-deletes its test listing. Run only against the demo project.

## Student council fair

Register the council's university account with its public display name. After confirming the organizer's identity, a project administrator copies that account's UUID from Supabase Authentication → Users and runs the following in the SQL Editor. The returned row identifies the profile that received access.

```sql
update public.profiles set is_council = true
where id = '<council account UUID>'::uuid
returning id, display_name, is_council;
```

Set the same flag to `false` to revoke access. The server checks the role on every import. Other students see council listings in the **Student council** feed tab and can open, reserve or message about them as usual.

For a local Supabase project, the debug app also accepts a loopback URL such as `http://127.0.0.1:55421`. Put the local API URL and its publishable key in `local.properties`, run `adb reverse tcp:55421 tcp:55421` for the connected emulator or USB device, and build the debug APK. Account and listing data are specific to that local database. Release builds require an HTTPS backend.

1. Sign in as the council and open **My account → Manage fair listings → Download CSV template**. The same [CSV template](../../app/src/main/assets/student-council-template.csv) is included in the repository.
2. Open it in Excel and fill in one row per item. For example, enter `FAIR-2026-001`, `Desk lamp`, `Working reading lamp from Dorm 5.`, `ELECTRONICS`, `GOOD`. Save as **CSV UTF-8**. Keep the five headers in order. Both comma and semicolon separators work, including quoted descriptions with line breaks. Each file can contain up to 500 items and 2 MB.
3. Use a new reference for each new physical item. Reuse its reference for edits. Matching is case-insensitive and scoped to the council account. Manually created and older listings have no fair reference and are outside the import.
4. Select **Choose filled CSV**. Review additions, edits and skipped rows. Existing matches are skipped by default. To apply the displayed edits, select **Overwrite the changed listings shown below**. **Confirm import** applies the reviewed changes together. Reserved, deleted and identical entries are skipped. Removing a row from the file leaves its listing in place.
5. If a listing changes during review or the connection drops, use **Refresh preview** to check the current server state before confirming again.
