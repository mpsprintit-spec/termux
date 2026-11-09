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

## Features in this branch

1. **Horizontal session tabs**: compact half-rounded tabs, no numbers, native scroll,
   tap-to-switch, long press to rename and **+** to create a real Termux session.
2. **Split terminal**: top/bottom real interactive panes provided by the supported
   `tmux` multiplexer in a freshly created Termux session. Open the header menu
   and choose **Split terminal (tmux)**. If `tmux` is absent, the app offers to run
   `pkg install tmux` in a fresh session after user confirmation. This avoids
   injecting commands into a running editor or other process. Termux's PTY
   and existing shell remain unchanged. This is **tmux-based, not two native Android
   TerminalView panes**.
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
`ValTherOn-Terminal-v0.6-APK` with SHA-256 checksums if and only if all steps
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
- The v0.6 changes are confined to a draft feature branch until verified.
- Command palette is intentionally *on-demand* rather than intercepting
  and rewriting arbitrary shell input or full-screen app keystrokes.
