$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
$exitCode = 1

try {
    $candidates = @()
    if ($env:JDK17_HOME) {
        $candidates += $env:JDK17_HOME
    }
    else {
        $roots = @(
            (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
            (Join-Path $env:ProgramFiles 'Java'),
            (Join-Path $env:ProgramFiles 'Microsoft')
        )
        if (${env:ProgramFiles(x86)}) {
            $roots += Join-Path ${env:ProgramFiles(x86)} 'Eclipse Adoptium'
        }

        foreach ($root in $roots) {
            if (Test-Path -LiteralPath $root) {
                $candidates += Get-ChildItem -LiteralPath $root -Directory -Filter 'jdk-17*' |
                    Select-Object -ExpandProperty FullName
            }
        }
    }

    $jdkHome = $null
    $javaExecutable = $null
    foreach ($candidate in $candidates) {
        $candidateJava = Join-Path $candidate 'bin\java.exe'
        if (-not (Test-Path -LiteralPath $candidateJava)) {
            continue
        }

        $versionOutput = (& $env:ComSpec /d /c ('"{0}" -version 2>&1' -f $candidateJava) | Out-String)
        if ($versionOutput -match '(?:openjdk|java) version "17(?:\.|")') {
            $jdkHome = $candidate
            $javaExecutable = $candidateJava
            break
        }
    }

    if (-not $jdkHome) {
        Write-Error 'JDK 17 was not found. Install Eclipse Temurin JDK 17 or set JDK17_HOME to a valid JDK 17 installation.'
        $exitCode = 1
    }
    else {
        $env:JAVA_HOME = $jdkHome
        $env:Path = "$jdkHome\bin;$previousPath"

        & $env:ComSpec /d /c ('"{0}" -version 2>&1' -f $javaExecutable)
        if ($LASTEXITCODE -ne 0) {
            $exitCode = $LASTEXITCODE
        }
        else {
            & mvn @args
            $exitCode = $LASTEXITCODE
        }
    }
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}

exit $exitCode
