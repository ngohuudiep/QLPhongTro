$ErrorActionPreference = 'Stop'

$version = '12.4.2'
$expectedSha256 = 'E9ABDB21E3E205450CA95E61413AC514C36FEFF380A53BAAB38D6D0B2785147E'
$installDirectory = Join-Path $env:USERPROFILE "qlphongtro-data\mssql-jdbc-auth\$version"
$targetDirectory = Join-Path $installDirectory 'x64'
$targetDll = Join-Path $targetDirectory "mssql-jdbc_auth-$version.x64.dll"

if (Test-Path $targetDll) {
    $existingSignature = Get-AuthenticodeSignature -FilePath $targetDll
    if ($existingSignature.Status -eq 'Valid' -and
        $existingSignature.SignerCertificate.Subject -match 'Microsoft Corporation') {
        Write-Host "SQL Server Windows authentication library is ready."
        exit 0
    }
    throw "Existing authentication library has an invalid signature: $targetDll"
}

$archivePath = Join-Path $env:TEMP "mssql-jdbc_auth-$version.zip"
$extractDirectory = Join-Path $env:TEMP "mssql-jdbc_auth-$version-extract"
$downloadUrl = "https://github.com/microsoft/mssql-jdbc/releases/download/v$version/mssql-jdbc_auth.zip"

try {
    Invoke-WebRequest -Uri $downloadUrl -OutFile $archivePath -UseBasicParsing
    $actualSha256 = (Get-FileHash -Path $archivePath -Algorithm SHA256).Hash
    if ($actualSha256 -ne $expectedSha256) {
        throw 'Downloaded SQL Server authentication archive failed SHA-256 verification.'
    }

    Expand-Archive -LiteralPath $archivePath -DestinationPath $extractDirectory -Force
    $sourceDll = Join-Path $extractDirectory "x64\mssql-jdbc_auth-$version.x64.dll"
    if (-not (Test-Path $sourceDll)) {
        throw "The expected 64-bit authentication library was not found in the Microsoft archive."
    }

    $signature = Get-AuthenticodeSignature -FilePath $sourceDll
    if ($signature.Status -ne 'Valid' -or
        $signature.SignerCertificate.Subject -notmatch 'Microsoft Corporation') {
        throw 'Downloaded SQL Server authentication library does not have a valid Microsoft signature.'
    }

    New-Item -ItemType Directory -Path $targetDirectory -Force | Out-Null
    Copy-Item -LiteralPath $sourceDll -Destination $targetDll
    Write-Host "Installed verified Microsoft SQL Server authentication library:"
    Write-Host $targetDll
}
finally {
    Remove-Item -LiteralPath $archivePath -Force -ErrorAction SilentlyContinue
    if (Test-Path $extractDirectory) {
        Remove-Item -LiteralPath $extractDirectory -Recurse -Force
    }
}
