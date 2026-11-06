# RA Terminal — Build APK

Customized single-Activity Termux Android application: real Termux shell/PTY, personal vector RA logo, cyan/purple Canvas background animation behind stable controls, and live Android CPU/RAM/storage/network metrics.

**Status:** Source/build pipeline. No APK has been compiled or verified yet. Do not uninstall your current Termux.

## Source files

Upload the complete contents of **RA_Terminal_Build_Kit_v0_2.zip** provided in the ChatGPT conversation to the root of this repository. Keep the paths exactly:
- `android/RaTerminalChrome.java`
- `android/RaSystemHud.java`
- `android/RaLogoView.java`
- `android/ra_background.html`
- `android/icons/mipmap*/ra_launcher*.png`
- `apply_to_termux.py`

The workflow file is already included in this repository. Do not upload nested ZIP as a single file; extract its **contents**. Keep any existing files when copying.

## Build

Open the [Actions tab](https://github.com/mpsprintit-spec/termux/actions/workflows/build-ra-terminal.yml), choose **Build RA Terminal APK**, then **Run workflow**.

The build clones official Termux at commit `8629e632fcb95da272221be327db653fb24befe9`, applies the RA modifications, builds `assembleDebug`, and checks that the output APK exists. The result, if successful, is available as `RA-Terminal-v0.2-APK` under the workflow run's Artifacts section.

Warning: this APK uses an upstream untrusted debug/test signing key. Keep the current Termux installation until you have a verified working APK and a full data backup. Do not distribute the debug APK as an official Termux release.
