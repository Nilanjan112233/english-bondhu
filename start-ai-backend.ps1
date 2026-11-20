param(
    [string]$BindAddress = "127.0.0.1",
    [int]$Port = 8787
)

$ErrorActionPreference = "Stop"
if (-not $env:GEMINI_API_KEY) {
    $secureKey = Read-Host "Enter Gemini API key (input is hidden)" -AsSecureString
    $keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
    try {
        $env:GEMINI_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
    }
}

$server = Join-Path $PSScriptRoot "backend\server.py"
python $server --host $BindAddress --port $Port
