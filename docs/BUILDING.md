# Building & applying

Two scripts. `build.ps1` compiles the patch layer, `apply.ps1` turns the official jar into the
patched jar. Both are plain PowerShell + `javac` + the .NET zip API — no Gradle, no reobf.

## 1. Gather the inputs

| What | Where to get it |
|---|---|
| JDK 8 | any 1.8 JDK |
| ReplayMod **1.7.10-2.5.2** | <https://replaymod.com> (or the GitHub releases) |
| 1.7.10 Forge **dev** jar, MCP-named | produced by RetroFuturaGradle / RFG (`recompiled_minecraft-1.7.10.jar`) |
| Forge universal | `net.minecraftforge:forge:1.7.10-10.13.4.1614-1.7.10:universal` |
| launchwrapper 1.12 | `net.minecraft:launchwrapper:1.12` |
| Netty / Gson / Guava | `io.netty:netty-all:4.0.10.Final`, `com.google.code.gson:gson`, `com.google.guava:guava:17.0` |
| ASM 9.x | `org.ow2.asm:asm:9.x`, `asm-tree`, `asm-commons` |
| Applied Energistics 2 | any GTNH-era AE2 jar (`appliedenergistics2-rv3-beta-*-GTNH.jar`) |

Put them in a folder (say `libs/`) — `.gitignore` keeps jars out of the repository.

## 2. Compile

```powershell
.\tools\build.ps1 `
    -JavaHome        "C:\Program Files\Java\jdk-1.8" `
    -MinecraftDevJar ".\libs\recompiled_minecraft-1.7.10.jar" `
    -ReplayModJar    ".\libs\replaymod-1.7.10-2.5.2.jar" `
    -ExtraJars       ".\libs\launchwrapper-1.12.jar",".\libs\forge-universal.jar",".\libs\netty-all-4.0.10.Final.jar",".\libs\gson.jar",".\libs\guava-17.0.jar",".\libs\asm-9.9.jar",".\libs\asm-tree-9.9.jar",".\libs\asm-commons-9.9.jar",".\libs\ae2.jar" `
    -OutDir          ".\mixin-out"
```

Expected tail:

```
compiling util/ (7 file(s), one javac pass)
  ok
compiling coremod + asm/ (4 file(s), one javac pass)
  ok
compiling 29 mixin(s) listed in overlay/mixins.replaycompat.json
  ok   MixinGuiCustomReplayEntry
  ...
OK - 51 class(es): 29 mixin + 9 util + 13 coremod/asm
```

**51 classes** is the number to expect: 29 mixins + 9 utility classes (7 + 2 inner) +
12 ASM transformer classes + 1 coremod entry point.

## 3. Apply

```powershell
.\tools\apply.ps1 `
    -BaseJar  ".\libs\replaymod-1.7.10-2.5.2.jar" `
    -MixinOut ".\mixin-out" `
    -OutJar   ".\dist\replaymod-1.7.10-2.5.2-gtnh.jar"
```

It copies the official jar, overwrites the four files in `overlay/`, injects all 51 classes,
then **verifies** and prints:

```
added      : 52  (51 under com/gtnh/replaycompat/, 1 other)
             + mixins.replaycompat.json
changed    : 3 file(s) replaced by overlay/
verified   : every compiled class is in the jar
```

A correct build is **6153 entries** (upstream has 6101) and exactly **10880577 bytes**.

## Doing it without PowerShell (Linux / macOS)

The scripts only wrap `javac` and zip rewriting. The equivalent by hand:

```bash
# compile helpers (one pass - they reference each other)
javac -proc:none -encoding UTF-8 -nowarn -cp "$CP" -d mixin-out src/com/gtnh/replaycompat/util/*.java
# coremod + asm
javac -proc:none -encoding UTF-8 -nowarn -cp "$CP" -d mixin-out \
      src/com/gtnh/replaycompat/*.java src/com/gtnh/replaycompat/asm/*.java
# mixins, one by one (the list comes from overlay/mixins.replaycompat.json)
for n in $(grep -o '"Mixin[A-Za-z0-9]*"' overlay/mixins.replaycompat.json | tr -d '"'); do
  javac -proc:none -encoding UTF-8 -nowarn -cp "$CP" -d mixin-out "src/com/gtnh/replaycompat/mixin/$n.java" || exit 1
done

# apply: copy the official jar, then replace overlay files and add the classes
cp libs/replaymod-1.7.10-2.5.2.jar dist/replaymod-1.7.10-2.5.2-gtnh.jar
cd overlay && zip -r ../dist/replaymod-1.7.10-2.5.2-gtnh.jar . && cd ..
cd mixin-out && zip -r ../dist/replaymod-1.7.10-2.5.2-gtnh.jar com && cd ..
```

`zip -r` appends/overwrites entries, which is exactly what `apply.ps1` does through the .NET API.

## Troubleshooting

| Symptom | Cause |
|---|---|
| `NoClassDefFoundError: com/google/gson/...` or **exit 3** | `-proc:none` missing — javac ran mixin's annotation processor |
| `package net.minecraft.launchwrapper does not exist` | launchwrapper 1.12 not on the classpath (needed by the coremod/ASM classes) |
| `cannot find symbol` on `S0CPacketSpawnPlayer` etc. | classpath missing the MCP-named Minecraft dev jar, or it was put **after** a conflicting jar |
| `MixinGuiScreen` still applied | you patched the wrong jar, or `overlay/` was not copied |
| game starts but no Replay button | `META-INF/MANIFEST.MF` was not replaced (check `MixinConfigs` lists `mixins.replaycompat.json`) |

## Verifying the result

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Ent($p){ $z=[System.IO.Compression.ZipFile]::OpenRead($p); $h=@{}
  foreach($e in $z.Entries){ $h[$e.FullName]=$e.Length }; $z.Dispose(); return $h }
$official = Ent '.\libs\replaymod-1.7.10-2.5.2.jar'
$patched  = Ent '.\dist\replaymod-1.7.10-2.5.2-gtnh.jar'
"added   : $((($patched.Keys | Where-Object { -not $official.ContainsKey($_) })).Count)"   # 52
"removed : $((($official.Keys | Where-Object { -not $patched.ContainsKey($_) })).Count)"  # 0
"changed : $((($patched.Keys | Where-Object { $official.ContainsKey($_) -and $official[$_] -ne $patched[$_] })).Count)"  # 3
```

## Reproducibility

Rebuilding from the official upstream jar reproduces the released jar **exactly**:

* same **6153 entries** (upstream has 6101) and the same file size (**10880577 bytes**);
* all **51** `com/gtnh/replaycompat/**` classes are **byte-for-byte identical** to the released build
  (verified by SHA-256 per class).

The zip container's own SHA-256 may differ between builds, because zip writers make different
metadata/compression choices — the *contents* do not.