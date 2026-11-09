# ValTherOn Terminal — Native Android / Termux

A minimal, hacker-style Android terminal built by **patching the official Termux
application**, not by replacing it with a browser or a fake terminal emulator.

## Design direction (v0.6, experimental)

The native UI follows the second approved reference design:

- Black, flat workspace; no solar video, plasma animation, or decorative backdrop.
- Small **ValTherOn** header with real CPU/app CPU and device RAM percentages.
- Compact, half-rounded, horizontally scrollable session tabs.
- Real session names (no session indices) and blue underline for the active tab.
- Native `+` button to open a **real Termux session**.
- Tap to switch sessions and long-press to rename through the existing Termux client.
- Original `TerminalView`, `TermuxService`, shell/PTY, bootstrap, Android keyboard,
  Termux extra-keys, Git, Go, and terminal applications remain in place.
- The optional session drawer remains reachable using the header menu.

The split-pane output in the *reference image* is a visual target and is **not
yet implemented as a new native split feature** in this branch. Use `tmux`
inside Termux for real split panes in the meantime. Smart autocomplete and
command search also remain future work. Nothing in the UI fabricates terminal
output or starts `htop` automatically.

## Native components

- `android/RaTerminalChrome.java`: simple header / session strip / original terminal.
- `android/RaSessionTabsView.java`: native Android scrollable session tabs.
- `android/RaSystemHud.java`: live device stats (when available).
- `apply_to_termux.py`: applies the patch to the pinned upstream source,
  including session-change notifications, without rebuilding the terminal engine.

The pinned upstream `termux-app` revision is
`8629e632fcb95da272221be327db653fb24befe9`.

## Build

Run GitHub Actions workflow `Build ValTherOn Terminal APK` on the feature
branch (or its pull request). It checks out the pinned Termux source, validates
the source anchors, applies the custom UI patch, compiles a debug Android APK,
and publishes an artifact named `ValTherOn-Terminal-v0.6-APK`.

The build artifact and SHA256 checksums should only be treated as usable once
the GitHub Actions build has succeeded. Even a successful build is not a
substitute for testing on the target Android device.

## Safety

**Do not uninstall or overwrite an existing Termux installation to test this.**

The APK currently uses the original Termux package identity and a
non-production debug signing key. Installing it over another Termux-signed
variant can fail, or require destructive actions. Keep your current HOME,
bootstrap, and repositories backed up. Do not alter Android package identity
without a dedicated bootstrap/package-compatibility migration.

The repository's `main` branch remains at RA Terminal v0.5 until this change
is reviewed and validated. This feature branch is deliberately isolated.
