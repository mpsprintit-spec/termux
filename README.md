# ValTherOn Terminal — Native Android / Termux

A minimal, hacker-style Android terminal built by **patching the official Termux
application**, not by replacing it with a browser or a fake terminal emulator.

## Design direction (v0.6.1, native Android)

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

## Features in this branch

1. **Horizontal session tabs**: compact half-rounded tabs, no numbers, native scroll,
   tap-to-switch, long press to rename and **+** to create a real Termux session.
2. **Live split terminal**: tap the small **▤** header control or select
   **Split terminal + live htop** in the menu. A new Termux PTY opens two
   interactive `tmux` panes: the **upper pane is a shell**, and the **lower pane
   starts real `htop` automatically**, reading the accessible CPU, RAM and
   process statistics from the Android device. The lower pane initially receives
   58% of the terminal rows; tmux responds to keyboard/terminal resizing.
   If `tmux` or `htop` is absent, the app asks before installing the missing
   packages via `pkg` in a fresh session. No commands are injected into other
   sessions. If `htop` quits, the lower pane becomes a shell instead of
   disappearing. This is a real **tmux-based** split; it is not two separate
   native Android `TerminalView` widgets or a fake dashboard.
3. **Command autocomplete**: on-demand completion palette against the real
   executable names in the current Termux prefix, plus shell commands. Filtering
   is live; inserting a selected suggestion sends **no Enter key**. Native Bash,
   Fish, and Zsh completion remain available via the Termux extra-key Tab.
4. **Command search**: case-insensitive search of available Bash, Zsh, or Fish
   history files. The result is inserted without running it; history with
   common sensitive-key patterns is excluded. Existing command buffers are not
   silently reset or rewritten.
5. **Performance monitor**: CPU/app CPU and RAM in the header; tap for more
   real device metrics, showing unavailable readings as unavailable instead of
   invented values.

All optional tools are accessible from the small ⋮ menu; existing
Termux extra keys, Android keyboard, drawer, shell packages, and terminal
emulation continue to use the original upstream implementation.

## Native components

- `android/RaTerminalChrome.java`: header, menu, native tabs, and original terminal.
- `android/RaSessionTabsView.java`: real horizontally scrolling Termux sessions.
- `android/RaSplitLauncher.java`: isolated fresh PTY + tmux split.
- `android/RaCommandPalette.java`: command lookup and autocomplete.
- `android/RaSystemHud.java`: genuine Android metrics.
- `apply_to_termux.py`: stable upstream source patch and session-change notifications.
- `android/ra_launcher.xml`: minimal terminal-prompt launcher mark.

Pinned upstream revision: `8629e632fcb95da272221be327db653fb24befe9`.

## Build and test

The GitHub Actions workflow `Build ValTherOn Terminal APK` validates and
applies the patch, compiles the native Java sources, then builds a **real
Termux debug APK with verified official bootstrap archives**. It publishes
`ValTherOn-Terminal-v0.6.1-APK` with SHA-256 checksums if and only if all steps
succeed. CI does not prove device-specific Android runtime compatibility.

A valid APK requires a successful `assembleDebug`, complete bootstrap
downloads, correct signature, verified SHA256, and manual device testing of
keyboard/sessions/tmux/monitoring before any production roll-out.

## Important install/safety notes

- The app retains the original Termux package ID and the pinned Termux
  debug key. It may **conflict with existing Termux installations** signed
  with another key. Do not uninstall your working Termux or delete HOME to
  test a build.
- The project does not touch Horizon's repository.
- The changes remain on a feature branch until installation and runtime testing.
- Command palette is intentionally *on-demand* rather than intercepting
  and rewriting arbitrary shell input or full-screen app keystrokes.

## Visual and runtime limits

The previous example screenshot was a design mockup, not a real application
capture. The actual `htop` appearance varies with phone screen width,
keyboard visibility, `htop` version and Android restrictions on process/CPU
metrics. A successful APK build does not establish that everything works on the
target phone; verify it on a compatible installation before replacing data.
