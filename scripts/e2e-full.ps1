# I flussi di gara vengono verificati su database temporanei: le date reali non vengono aggirate.
$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot
try {
    $testOptions = @("-q", "test")
    if ($env:OS -eq "Windows_NT") {
        & ".\mvnw.cmd" @testOptions
    } else {
        & sh "./mvnw" @testOptions
    }
    if ($LASTEXITCODE -ne 0) { throw "Verifica non riuscita (Maven exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}
