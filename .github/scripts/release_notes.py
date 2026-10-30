"""Pulse: the newest entry of PulseChangelog.kt as release notes, between the markers the app
reads (PulseReleases.notesOf). Printed to stdout; CI puts it into the release description."""
import re
import sys

text = open(sys.argv[1], encoding="utf-8").read()
# Entries start as `Entry(` and the build number; the data class itself does not.
entries = re.split(r"Entry\(\s*\d+\s*,", text)[1:]
lines = []
if entries:
    # The first string literal is the date; the rest are the changes.
    strings = re.findall(r'"((?:[^"\\]|\\.)*)"', entries[0])[1:]
    lines = [s.replace('\\"', '"').replace("\\\\", "\\") for s in strings]
print("<!-- notes -->")
for line in lines:
    print(f"- {line}")
print("<!-- /notes -->")
