param(
    [string]$NovelForgeBackend = "D:\Nilanjan\Projects\Novel-Hub\AI Writers\test2\test2\backend"
)

$ErrorActionPreference = "Stop"
$builder = Join-Path $PSScriptRoot "backend\build_standalone_from_novelforge.py"
py -3.11 $builder --novelforge-backend $NovelForgeBackend --project-root $PSScriptRoot
if ($LASTEXITCODE -ne 0) { throw "Standalone APK build failed." }
