param(
    [string]$JavaHome = "$env:ProgramFiles\Java\jdk-17",
    [ValidateRange(1, 65535)][int]$Port = 8080,
    [switch]$Test
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\javac.exe'))) {
    throw 'JDK 17 was not found. Pass -JavaHome with the JDK 17 installation directory.'
}

# Environment changes apply only to this PowerShell process.
$env:JAVA_HOME = $JavaHome
$env:MAVEN_USER_HOME = Join-Path $PSScriptRoot '.local\m2'
$env:MAVEN_OPTS = '-Dfile.encoding=UTF-8'
$taskArguments = @('-B', '-ntp', '-f', (Join-Path $PSScriptRoot 'backend\pom.xml'),
    "-Dmaven.repo.local=$(Join-Path $PSScriptRoot '.local\m2\repository')")

# Optional machine-local proxy configuration; never committed to Git.
$taskSettingsPath = Join-Path $PSScriptRoot '.local\maven-settings.xml'
if (Test-Path -LiteralPath $taskSettingsPath) {
    $taskArguments += @('-s', $taskSettingsPath)
}

if ($Test) {
    $taskArguments += 'test'
} else {
    Write-Host "Demo: http://localhost:$Port/demo.html"
    Write-Host "API docs: http://localhost:$Port/doc.html"
    Write-Host 'Stop with Ctrl+C. Demo data resets on restart.'
    $taskArguments += @('-Dspring-boot.run.profiles=demo',
        "-Dspring-boot.run.arguments=--server.port=$Port", 'spring-boot:run')
}

& (Join-Path $PSScriptRoot 'backend\mvnw.cmd') @taskArguments
exit $LASTEXITCODE
