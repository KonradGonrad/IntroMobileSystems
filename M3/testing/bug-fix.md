# Verified bug: zero-width image after scaling

Requirement: every accepted photo must remain a valid bitmap and fit within 1280 × 1280 pixels.

Reproduction: `PhotoUtils.scaledSize(1, 10000, 1280)` returned `(0, 1280)`. Android cannot create a zero-width bitmap. The regression test expected `(1, 1280)` from the requirement, not from the implementation.

Cause: converting the scaled floating-point width to an integer rounded it down to zero.

Fix: clamp each resulting dimension to at least one pixel. Reject non-positive inputs so a zero target cannot cause an endless sampling loop. Handle all eight EXIF orientations, including mirrored photos, and show the existing gallery fallback when an image cannot be read.

Evidence: `photo-before.txt` records the failing regression (4 tests, 1 failure). `photo-after.txt` records the passing photo suite after the change. Run `./gradlew testDebugUnitTest --tests '*PhotoUtilsTest'` to repeat.
