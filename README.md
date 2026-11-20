# DormSwap

Android app for students in Dormitory 5, Łódź to share unused items, reserve them and arrange pickup.

- [M3 setup, functionality, architecture and five-minute demo](M3/README.md)
- [Supabase configuration](M3/backend/README.md)
- [Student council account setup and CSV imports](M3/backend/README.md#student-council-fair)
- [Tests and outstanding verification](M3/testing/README.md)
- [Privacy, accessibility and limitations](M3/privacy-and-accessibility.md)
- [AI use and three checked suggestions](AI_USE.md)
- [M2 working MVP](M2/README.md) / [M1 problem and prototype](M1/README.md)

```bash
./gradlew assembleDebug
./gradlew test
```

Configure the public Supabase URL/key locally before testing sign-in. Live Supabase API checks passed. Physical two-device verification, peer sessions and an independent teammate build are pending. See M3 documentation for exact status rather than assuming the application is ready to submit.
