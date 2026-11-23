Task T-12 — Professional UI/UX + accessibility pass on the Wearless Android app (you are jcode, the implementation seat).

Use these skills (load and apply them, they are named with a leading slash):
- /mobile-ui-standards
- /mobile-ux-improver
- /anti-slop-design
- /responsive-design
- /ui-designer
- /mobile-app-debugging

Read first: SKILLS-MAP.md, TODO.md, WEARLESS-BOOK.md (V1 feature list), BUILD-ROADMAP.md.

Exclusive editable paths: app/app/src/main/res/** and app/app/src/main/java/com/wearless/app/MainActivity.kt,
NodeConfigActivity.kt, ResourceMonitorActivity.kt. Do NOT touch app/bonding/**, node/**, scripts/**, qa/**, root docs.

What exists today (verify yourself): a single ScrollView + LinearLayout on MainActivity with plain TextViews
("Stopped", "NOT MEASURED" speed, Start button, links container, two buttons). No custom colors, no cards,
no dark-mode design, no empty states, no content descriptions. The app theme is Theme.Material3.DayNight.NoActionBar.

Deliver a professional, honest UI/UX:
1. Replace the flat layout with a proper Material 3 design: a top app bar with the Wearless name, a large
   connection-status card (Stopped / Connecting / Connected / Bonding N links) with a clear visual state,
   the combined-speed as the hero number, a primary Start/Stop FAB or filled button, and per-link cards
   (name, live speed, data used, on/off) using MaterialCardView.
2. Honesty rule (binding): when a value is not measured, show "NOT MEASURED" — never a made-up number.
   Keep that exact string for unmeasured values.
3. Add proper dark-mode support via Material 3 color roles (values/colors.xml + themes), so DayNight looks right.
4. Accessibility: every interactive element gets a contentDescription; touch targets >= 48dp; text contrast
   meets WCAG AA; the hero speed number is announced once, not spammed.
5. Empty states: when there are no links yet, show a short honest hint ("No links detected yet."), not a blank area.
6. Professional polish: consistent 8dp spacing grid, one accent color, no purple-gradient AI-slop look, no emoji.
7. Keep all existing functionality and all honest-data behaviour intact. Do not invent features.

Constraints:
- Keep minSdk/compileSdk as-is. Do not add new dependencies unless required for Material components; if you do,
  add them to app/app/build.gradle.kts and say so.
- Do not rename package or activities. Do not change the manifest permissions.
- Finish with a worker_done report containing: files changed, a summary of the new design, how you verified it
  builds (goose will also run assembleDebug), and anything you could not do. If you hit an error you cannot fix,
  escalate through orchestration with the EXACT error before stopping. Stopping silently is forbidden.