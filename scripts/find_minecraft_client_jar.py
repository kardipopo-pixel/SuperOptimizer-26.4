import os
import sys
import zipfile

root = os.path.expanduser("~/.gradle/caches")
needle = "net/minecraft/client/renderer/entity/EntityRenderDispatcher.class"

for base, _, files in os.walk(root):
    for name in files:
        if not name.endswith(".jar"):
            continue
        path = os.path.join(base, name)
        try:
            with zipfile.ZipFile(path) as archive:
                if needle in archive.namelist():
                    print(path)
                    raise SystemExit(0)
        except (OSError, zipfile.BadZipFile):
            continue

print("Minecraft client jar not found", file=sys.stderr)
raise SystemExit(1)
