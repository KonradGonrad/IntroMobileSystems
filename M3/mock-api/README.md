# Historical fixtures and synthetic data

M3 no longer reads the M2 GitHub JSON endpoint or uses FakeUploadService in the active application. Real authenticated requests go to Supabase. The historical parser and merge tests remain as M2 regression coverage.

Use fictional accounts and items entered through the application for the M3 demonstration. Backend policy tests in `../backend/policy-tests.sql` create synthetic rows inside a transaction and roll them back. No fabricated production API responses or fake signed-in screenshots are included.
