<#
.SYNOPSIS
  Compiles the ReplayMod GTNH compatibility layer (com.gtnh.replaycompat).

.DESCRIPTION
  Compiles in three passes:

    1.  src/com/gtnh/replaycompat/util/*.java      - all at once, they reference each other
    1b. the coremod entry point + asm/*.java       - all at once, they reference each other
    2.  every mixin named in overlay/mixins.replaycompat.json - one by one, so a failure names the file

  Two hard rules inherited from the field (see docs/IMPLEMENTATION_NOTES.md):

    * `-proc:none` is mandatory. With mixin's annotation processor on the classpath, javac would run
      it, and it needs gson -> the compile dies with exit 3.
    * The exit code of EVERY javac invocation is checked and failures are collected; the script
      throws outside the loop. (`throw` inside a PowerShell pipeline is swallowed, which once led to
      "compile failed but the old classes got injected anyway".)

.EXAMPLE
  .\build.ps1 -JavaHome 'C:\Program Files\Java\jdk-1.8' `
              -MinecraftDevJar 'D:\rfg\recompiled_minecraft-1.7.10.jar' `
              -ReplayModJar '.\libs\replaymod-1.7.10-2.5.2.jar' `
              -ExtraJars '.\libs\forge-universal.jar','.\libs\netty-all.jar','.\libs\gson.jar','.\libs\guava.jar'
#>
param(
    [Parameter(Mandatory = $true)][string]$JavaHome,
    [Parameter(Mandatory = $true)][string]$MinecraftDevJar,
    [Parameter(Mandatory = $true)][string]$ReplayModJar,
    [string[]]$ExtraJars = @(),
    [string]$OutDir = (Join-Path $PSScriptRoot '..\mixin-out')
)

$ErrorActionPreference = 'Stop'

$root      = Resolve-Path (Join-Path $PSScriptRoot '..')
$srcRoot   = Join-Path $root 'src'
$pkgRoot   = Join-Path $srcRoot 'com\gtnh\replaycompat'
$utilDir   = Join-Path $pkgRoot 'util'
$asmDir    = Join-Path $pkgRoot 'asm'
$mixinDir  = Join-Path $pkgRoot 'mixin'
$mixinJson = Join-Path $root 'overlay\mixins.replaycompat.json'

$javac = Join-Path $JavaHome 'bin\javac.exe'
if (-not (Test-Path $javac)) { $javac = Join-Path $JavaHome 'bin\javac' }   # non-Windows
if (-not (Test-Path $javac)) { throw "javac not found under JavaHome '$JavaHome'" }
foreach ($p in @($MinecraftDevJar, $ReplayModJar, $mixinJson)) {
    if (-not (Test-Path $p)) { throw "required input not found: $p" }
}

New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

$sep = [IO.Path]::PathSeparator
$cpParts = @($OutDir, $MinecraftDevJar, $ReplayModJar) + $ExtraJars
$cp = ($cpParts | Where-Object { $_ }) -join $sep

Write-Host "javac     : $javac"
Write-Host "output    : $OutDir"
Write-Host "classpath : $($cpParts.Count) entries"

$failures = New-Object System.Collections.ArrayList

function Invoke-JavacPass {
    param([string]$Label, [string[]]$Files)
    if ($Files.Count -eq 0) { Write-Host "`n$Label - nothing to do"; return }
    Write-Host "`n$Label ($($Files.Count) file(s), one javac pass)"
    $out = & $javac -proc:none -encoding UTF-8 -nowarn -cp $cp -d $OutDir @Files 2>&1
    if ($LASTEXITCODE -ne 0) {
        [void]$failures.Add("$Label  (exit $LASTEXITCODE)")
        Write-Host "  FAIL" -ForegroundColor Red
        $out | Select-Object -First 30 | ForEach-Object { "       $_" }
    } else {
        Write-Host "  ok" -ForegroundColor Green
    }
}

# --- 1) helpers (they reference each other, so one pass) ---------------------
$utilFiles = @(Get-ChildItem $utilDir -Filter '*.java' -File | Sort-Object Name | ForEach-Object { $_.FullName })
Invoke-JavacPass -Label 'compiling util/' -Files $utilFiles

# --- 1b) coremod entry point + ASM transformers ------------------------------
$coreFiles = @()
$coreFiles += @(Get-ChildItem $pkgRoot -Filter '*.java' -File | ForEach-Object { $_.FullName })
$coreFiles += @(Get-ChildItem $asmDir  -Filter '*.java' -File | ForEach-Object { $_.FullName })
Invoke-JavacPass -Label 'compiling coremod + asm/' -Files $coreFiles

# --- 2) the mixins named in the manifest, one by one -------------------------
$json  = Get-Content $mixinJson -Raw
$names = [regex]::Matches($json, '"Mixin([A-Za-z0-9]+)"') | ForEach-Object { $_.Groups[1].Value }
Write-Host "`ncompiling $($names.Count) mixin(s) listed in overlay/mixins.replaycompat.json"
foreach ($n in $names) {
    $file = Join-Path $mixinDir "Mixin$n.java"
    if (-not (Test-Path $file)) {
        [void]$failures.Add("MIXIN Mixin$n  (source file missing)")
        Write-Host "  MISSING Mixin$n.java" -ForegroundColor Red
        continue
    }
    $out = & $javac -proc:none -encoding UTF-8 -nowarn -cp $cp -d $OutDir $file 2>&1
    if ($LASTEXITCODE -ne 0) {
        [void]$failures.Add("MIXIN Mixin$n  (exit $LASTEXITCODE)")
        Write-Host "  FAIL Mixin$n" -ForegroundColor Red
        $out | Select-Object -First 25 | ForEach-Object { "       $_" }
    } else {
        Write-Host "  ok   Mixin$n"
    }
}

# --- 3) abort loudly, then report -------------------------------------------
if ($failures.Count -gt 0) {
    Write-Host "`n$($failures.Count) step(s) failed:" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    throw 'COMPILE FAILED - nothing was injected'
}

$classes = Get-ChildItem (Join-Path $OutDir 'com\gtnh\replaycompat') -Recurse -Filter *.class
$nMixin  = @($classes | Where-Object { $_.FullName -match '\\mixin\\' }).Count
$nUtil   = @($classes | Where-Object { $_.FullName -match '\\util\\'  }).Count
$nOther  = $classes.Count - $nMixin - $nUtil
Write-Host "`nOK - $($classes.Count) class(es): $nMixin mixin + $nUtil util + $nOther coremod/asm" -ForegroundColor Green
Write-Host "next: .\tools\apply.ps1 -BaseJar <official jar> -MixinOut '$OutDir' -OutJar .\dist\replaymod-1.7.10-2.5.2-gtnh.jar"
