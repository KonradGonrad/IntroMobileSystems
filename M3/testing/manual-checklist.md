# Manual acceptance checklist

Use fictional accounts A and B ending in `@edu.p.lodz.pl`, no real names/photos. Record date, device/OS, result and any issue beside each case. Unchecked means **not performed**, not passed.

- [ ] Configure hosted schema, Confirm email off, minimum password 8. Record configuration without secrets.
- [ ] Register A in emulator; no activation email. Duplicate registration is rejected. Wrong domain, short/mismatching passwords and empty name show actionable errors.
- [ ] Log in as A on physical phone; verify same profile and shared listings. Restart app; session remains. Wrong password fails.
- [ ] Publish as A; B sees title, description and uploaded photo after refresh. B cannot edit/delete it. A can edit an available listing.
- [ ] Search/category/My listings/Liked and empty states work; likes follow the account across devices.
- [ ] A cannot reserve own listing. B reserves; a third fictional account cannot reserve it. Repeat with simultaneous requests and verify one winner.
- [ ] Owner cannot edit while reserved. Only the reserving account can cancel. After cancellation owner can edit.
- [ ] Start a chat from Details. Both participants see messages and history after restart; third account sees neither conversation nor messages.
- [ ] Delete listing; it disappears from Feed on both devices, existing conversation remains marked unavailable.
- [ ] Turn connection off after login. Cached items and chat remain readable. Queue listing, message, like, reservation/cancellation; restart offline and verify queue persists.
- [ ] Reconnect; each message/listing is uploaded once. Conflicting reservation shows server truth and a rejected-change explanation in Account.
- [ ] Sign out with pending changes, sign in as another account: no previous account cache or messages. Sign back in as original account: pending queue is preserved.
- [ ] Enable simulated HTTP 503; verify error, cached data and Retry. Disable switch and retry successfully.
- [ ] Camera allowed / denied / permanently denied; gallery fallback; no camera app; cancel picker; optional photo; large and rotated JPEG; unreadable image.
- [ ] Font scale 200%, portrait/landscape, keyboard open: all actions reachable, no essential clipped text. Inspect Feed, Details, form, login and chat.
- [ ] TalkBack: controls have meaningful names, traversal order makes sense, validation is announced, main task can be completed.
- [ ] Inspect logs/repository/ZIP: no passwords, access tokens, private keys or real user data.

Finish with tester initials (team only), date, device and a link to issue/fix/retest evidence. Avoid storing peer participant identities.
