<#
.SYNOPSIS
  Applies the compiled ReplayMod GTNH compatibility layer to the official ReplayMod jar.

.DESCRIPTION
  1. copies the official jar to -OutJar
  2. overwrites the files listed in overlay/ (manifest, one upstream mixin config, zh_CN.lang,
     and our own mixins.replaycompat.json)
  3. injects every compiled com/gtnh/replaycompat/** class
  4. verifies the result and prints an inventory

  The upstream jar is not modified in place unless you point -OutJar at it.

.EXAMPLE
  .\apply.ps1 -BaseJar '.\libs\replaymod-1.7.10-2.5.2.jar' -MixinOut '.\mixin-out' `
              -OutJar '.\dist\replaymod-1.7.10-2.5.2-gtnh.jar'
#>
param(
    [Parameter(Mandatory = $true)][string]$BaseJar,
    [Parameter(Mandatory = $true)][string]$MixinOut,
    [Parameter(Mandatory = $true)][string]$OutJar
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root    = Resolve-Path (Join-Path $PSScriptRoot '..')
$overlay = Join-Path $root 'overlay'

if (-not (Test-Path $BaseJar))  { throw "base jar not found: $BaseJar" }
if (-not (Test-Path $MixinOut)) { throw "compile output not found: $MixinOut" }
if (-not (Test-Path $overlay))  { throw "overlay directory not found: $overlay" }

$classRoot = Join-Path $MixinOut 'com\gtnh\replaycompat'
if (-not (Test-Path $classRoot)) { throw "no compiled classes under $classRoot - run build.ps1 first" }

# --- 1) copy -----------------------------------------------------------------
$outFull = [IO.Path]::GetFullPath($OutJar)
New-Item -ItemType Directory -Path (Split-Path $outFull -Parent) -Force | Out-Null
if ([IO.Path]::GetFullPath($BaseJar) -ne $outFull) {
    Copy-Item $BaseJar $outFull -Force
    Write-Host "copied  $BaseJar`n     -> $outFull"
} else {
    Write-Host "patching $outFull in place"
}

# --- 2) overlay files --------------------------------------------------------
$zip = [System.IO.Compression.ZipFile]::Open($outFull, 'Update')
try {
    Write-Host "`noverlaying files we replace inside the upstream jar:"
    Get-ChildItem $overlay -Recurse -File | ForEach-Object {
        $rel = $_.FullName.Substring((Resolve-Path $overlay).Path.Length + 1).Replace('\', '/')
        $entry = $zip.GetEntry($rel)
        if ($entry) { $entry.Delete() }
        $new = $zip.CreateEntry($rel, [System.IO.Compression.CompressionLevel]::Optimal)
        $stream = $new.Open()
        try {
            $bytes = [IO.File]::ReadAllBytes($_.FullName)
            $stream.Write($bytes, 0, $bytes.Length)
        } finally { $stream.Dispose() }
        Write-Host ("  {0,-52} {1,8} B" -f $rel, $_.Length)
    }

    # --- 3) inject our classes ----------------------------------------------
    $classes = Get-ChildItem $classRoot -Recurse -Filter *.class
    Write-Host "`ninjecting $($classes.Count) compiled class(es):"
    $prefix = (Resolve-Path $MixinOut).Path.Length + 1
    foreach ($c in $classes) {
        $rel = $c.FullName.Substring($prefix).Replace('\', '/')
        $entry = $zip.GetEntry($rel)
        if ($entry) { $entry.Delete() }
        $new = $zip.CreateEntry($rel, [System.IO.Compression.CompressionLevel]::Optimal)
        $stream = $new.Open()
        try {
            $bytes = [IO.File]::ReadAllBytes($c.FullName)
            $stream.Write($bytes, 0, $bytes.Length)
        } finally { $stream.Dispose() }
        Write-Host "  + $rel"
    }
} finally { $zip.Dispose() }

# --- 4) verify --------------------------------------------------------------
$base = [System.IO.Compression.ZipFile]::OpenRead($BaseJar)
$names = @{}
foreach ($e in $base.Entries) { $names[$e.FullName] = $e.Length }
$base.Dispose()

$res = [System.IO.Compression.ZipFile]::OpenRead($outFull)
$added = @(); $changed = 0
foreach ($e in $res.Entries) {
    if (-not $names.ContainsKey($e.FullName)) { $added += $e.FullName }
    elseif ($names[$e.FullName] -ne $e.Length) { $changed++ }
}
$mine = @($added | Where-Object { $_ -like 'com/gtnh/replaycompat/*' })
$other = @($added | Where-Object { $_ -notlike 'com/gtnh/replaycompat/*' })
$res.Dispose()

$hash = (Get-FileHash $outFull -Algorithm SHA256).Hash
$len  = (Get-Item $outFull).Length

Write-Host "`n=== result ==="
Write-Host "out        : $outFull"
Write-Host "size       : $len bytes"
Write-Host "sha256     : $hash"
Write-Host "added      : $($added.Count)  ($($mine.Count) under com/gtnh/replaycompat/, $($other.Count) other)"
$other | ForEach-Object { Write-Host "             + $_" }
Write-Host "changed    : $changed file(s) replaced by overlay/"
if ($mine.Count -ne (Get-ChildItem $classRoot -Recurse -Filter *.class).Count) {
    throw "verification failed: injected class count does not match the compile output"
}
Write-Host "verified   : every compiled class is in the jar" -ForegroundColor Green
