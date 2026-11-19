# M3 AI use and verified suggestions

An AI assistant helped implement and document this release. Team members must understand and independently verify their contributions. Requested author/date metadata is not evidence of test execution dates.

1. The suggested broad `edu` email check was replaced by the exact `edu.p.lodz.pl` rule. AuthValidator tests cover suffix attacks and malformed addresses; live signup was checked with fictional accounts.
2. Keeping M2 reservation flags would preserve rejected reservations. CloudSyncTest checks server truth after rejection, offline drafts, deletion and cross-device likes. Live Supabase reservation/cancellation and private chat were exercised.
3. Photo scaling could return a zero-width bitmap. A failing regression test preceded the positive-dimension fix; before/after logs are in M3/testing.

Peer sessions and independent human review are recorded only after the team actually performs them.
