param(
    # Folder of a local NovelForge backend that already stores an encrypted Gemini key.
    [Parameter(Mandatory = $true)]
    [string]$NovelForgeBackend
)

$ErrorActionPreference = "Stop"
$builder = Join-Path $PSScriptRoot "backend\build_standalone_from_novelforge.py"
py -3.11 $builder --novelforge-backend $NovelForgeBackend --project-root $PSScriptRoot
if ($LASTEXITCODE -ne 0) { throw "Standalone APK build failed." }
