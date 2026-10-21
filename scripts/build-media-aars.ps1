# Build FongMi/media AARs and copy to app/libs as lib-*.aar
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Media = Join-Path $Root "media"
$Libs = Join-Path $Root "app\libs"

$moduleFolders = @{
    "lib-common" = "common"
    "lib-container" = "container"
    "lib-database" = "database"
    "lib-datasource" = "datasource"
    "lib-datasource-okhttp" = "datasource_okhttp"
    "lib-datasource-rtmp" = "datasource_rtmp"
    "lib-decoder" = "decoder"
    "lib-decoder-av1" = "decoder_av1"
    "lib-decoder-ffmpeg" = "decoder_ffmpeg"
    "lib-decoder-flac" = "decoder_flac"
    "lib-decoder-opus" = "decoder_opus"
    "lib-decoder-vp9" = "decoder_vp9"
    "lib-exoplayer" = "exoplayer"
    "lib-exoplayer-dash" = "exoplayer_dash"
    "lib-exoplayer-hls" = "exoplayer_hls"
    "lib-exoplayer-rtsp" = "exoplayer_rtsp"
    "lib-exoplayer-smoothstreaming" = "exoplayer_smoothstreaming"
    "lib-extractor" = "extractor"
    "lib-session" = "session"
    "lib-ui-danmaku" = "ui_danmaku"
    "lib-ui" = "ui"
    "lib-mpvplayer" = "mpvplayer"
}

Push-Location $Media
try {
    $tasks = $moduleFolders.Keys | ForEach-Object { ":${_}:assembleRelease" }
    & .\gradlew.bat @tasks --no-daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    foreach ($entry in $moduleFolders.GetEnumerator()) {
        $name = $entry.Key
        $folder = $entry.Value
        $aarDir = Join-Path $Media "libraries\$folder\buildout\outputs\aar"
        $aar = Get-ChildItem -Path $aarDir -Filter "*-release.aar" -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $aar) {
            Write-Error "Missing AAR for $name in $aarDir"
        }
        $dest = Join-Path $Libs "$name.aar"
        Copy-Item $aar.FullName $dest -Force
        Write-Host "Copied $($aar.Name) -> $dest"
    }
}
finally {
    Pop-Location
}

Write-Host "Done. $($moduleFolders.Count) media AARs in app/libs"
