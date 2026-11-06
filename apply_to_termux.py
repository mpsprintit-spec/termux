#!/usr/bin/env python3
"""Patch pinned official Termux source with RA Terminal v0.2 single-screen UI."""
from pathlib import Path
import argparse
import shutil
import sys

root=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('termux_repo',type=Path)
parser.add_argument('--check-only',action='store_true')
args=parser.parse_args()
repo=args.termux_repo.expanduser().resolve()
activity=repo/'app/src/main/java/com/termux/app/TermuxActivity.java'
manifest=repo/'app/src/main/AndroidManifest.xml'
asset=repo/'app/src/main/assets/ra_background.html'
classes=repo/'app/src/main/java/com/termux/app/ra'
drawable=repo/'app/src/main/res/drawable/ra_launcher.xml'
if not activity.is_file() or not manifest.is_file() or not (repo/'app/build.gradle').is_file():
    raise SystemExit(f'ERROR: invalid Termux source tree: {repo}')
for name in ('RaTerminalChrome.java','RaSystemHud.java','RaLogoView.java','ra_background.html','ra_launcher.xml'):
    source=root/'android'/name
    if not source.is_file():
        raise SystemExit(f'ERROR: missing RA Terminal source file: {source}')
s=activity.read_text(encoding='utf-8')
marker='// RA Terminal chrome injection v1'
if marker in s:
    print('RA Terminal patch already installed, skipping duplicate change.')
    sys.exit(0)
insertions=[
    ('import com.termux.view.TerminalView;',
     'import com.termux.view.TerminalView;\nimport com.termux.app.ra.RaTerminalChrome;'),
    ('    TermuxService mTermuxService;',
     '    TermuxService mTermuxService;\n\n    private RaTerminalChrome mRaChrome;'),
    ('        setTermuxTerminalViewAndClients();',
     '        setTermuxTerminalViewAndClients();\n        // RA Terminal chrome injection v1\n        mRaChrome = new RaTerminalChrome(this, mTerminalView);'),
    ('        super.onResume();',
     '        super.onResume();\n        if (mRaChrome != null) mRaChrome.resume();'),
    ('        super.onStop();',
     '        if (mRaChrome != null) mRaChrome.pause();\n        super.onStop();'),
    ('        super.onDestroy();',
     '        if (mRaChrome != null) { mRaChrome.destroy(); mRaChrome = null; }\n        super.onDestroy();'),
]
for old,new in insertions:
    if s.count(old)!=1:
        raise SystemExit(f'ERROR: ambiguous or missing upstream source anchor {old!r}')
    s=s.replace(old,new,1)
m=manifest.read_text(encoding='utf-8')
start=m.find('    <application\n')
if start<0: raise SystemExit('ERROR: missing <application> tag')
end=m.find('>',start)
block=m[start:end+1]
replacements=[
    ('android:icon="@mipmap/ic_launcher"','android:icon="@drawable/ra_launcher"'),
    ('android:roundIcon="@mipmap/ic_launcher_round"','android:roundIcon="@drawable/ra_launcher"'),
    ('android:label="@string/application_name"','android:label="RA Terminal"'),
]
for old,new in replacements:
    if block.count(old)!=1:
        raise SystemExit(f'ERROR: missing manifest application anchor {old}')
    block=block.replace(old,new,1)
m=m[:start]+block+m[end+1:]
start=m.find('android:name=".app.TermuxActivity"')
if start<0:raise SystemExit('ERROR: missing Termux activity')
end=m.find('>',start)
block=m[start:end+1]
if block.count('android:label="@string/application_name"')!=1:
    raise SystemExit('ERROR: missing activity label')
block=block.replace('android:label="@string/application_name"','android:label="RA Terminal"',1)
m=m[:start]+block+m[end+1:]
if args.check_only:
    print('PASS: all Termux Activity and manifest source anchors are compatible.')
    sys.exit(0)
classes.mkdir(parents=True,exist_ok=True)
asset.parent.mkdir(parents=True,exist_ok=True)
drawable.parent.mkdir(parents=True,exist_ok=True)
for filename in ('RaTerminalChrome.java','RaSystemHud.java','RaLogoView.java'):
    shutil.copy2(root/'android'/filename,classes/filename)
shutil.copy2(root/'android/ra_background.html',asset)
shutil.copy2(root/'android/ra_launcher.xml',drawable)
for original,text in ((activity,s),(manifest,m)):
    backup=original.with_suffix(original.suffix+'.ra-original')
    if not backup.exists():
        backup.write_text(original.read_text(encoding='utf-8'),encoding='utf-8')
    original.write_text(text,encoding='utf-8')
print('SUCCESS: applied RA Terminal v0.2 patch to',repo)
