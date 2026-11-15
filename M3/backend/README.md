# Supabase setup

Use a separate, empty project. Never put a service-role key, database password or access token in the Android app or repository.

1. Run `schema.sql` once in the project's SQL Editor. It creates the tables, checked RPCs, row access policies and private `listing-photos` bucket.
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
