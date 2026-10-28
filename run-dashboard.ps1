param(
    [switch]$Preview,
    [ValidateRange(0, 100)][int]$Stocks = 60,
    [ValidateRange(1, 30)][int]$Years = 5,
    [ValidateRange(1000, 100000000)][int]$Amount = 10000,
    [string]$PreviewPath
)

$ErrorActionPreference = 'Stop'

# Prefer a real JDK over Windows Java shortcut shims.
$jdkCandidates = @()
if ($env:JAVA_HOME) {
    $jdkCandidates += $env:JAVA_HOME
}
foreach ($installRoot in @("$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium")) {
    if (Test-Path -LiteralPath $installRoot) {
        $jdkCandidates += Get-ChildItem -LiteralPath $installRoot -Directory |
            Sort-Object LastWriteTime -Descending | Select-Object -ExpandProperty FullName
    }
}

$jdkDirectory = $jdkCandidates | Where-Object {
    (Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe')) -and
    (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe'))
} | Select-Object -First 1

if (-not $jdkDirectory) {
    throw 'Set JAVA_HOME to an installed JDK (Java 8 or newer), then run this script again.'
}

$outputDirectory = Join-Path $PSScriptRoot 'out'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\portfolio') -Filter '*.java' |
    Select-Object -ExpandProperty FullName
& (Join-Path $jdkDirectory 'bin\javac.exe') -encoding UTF-8 -d $outputDirectory @sourceFiles
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. See the compiler output above.' }
if ($Preview) {
    if (-not $PreviewPath) { $PreviewPath = Join-Path $PSScriptRoot 'dashboard-preview.png' }
    & (Join-Path $jdkDirectory 'bin\java.exe') '-Djava.awt.headless=true' -cp $outputDirectory portfolio.PortfolioGUI --preview $PreviewPath $Stocks $Years $Amount
} else {
    & (Join-Path $jdkDirectory 'bin\java.exe') -cp $outputDirectory portfolio.PortfolioGUI
}
if ($LASTEXITCODE -ne 0) { throw 'The dashboard could not start. See the Java output above.' }
