# -----------------------------------------------------------------------------
# sonar-scan.ps1  -  Run SonarQube analysis for all OmniCharge microservices
#                    using local Maven (no Docker needed)
# Usage:  powershell -ExecutionPolicy Bypass .\sonar-scan.ps1
#         powershell -ExecutionPolicy Bypass .\sonar-scan.ps1 -Service user-service
# -----------------------------------------------------------------------------
param(
    [string]$Service = "all",
    [string]$SonarUrl = "http://localhost:9000",
    [string]$SonarToken = "squ_83126dd27aaf74d8fce5ee68849786786d9744f1"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot

$Services = @(
    "user-service",
    "payment-service",
    "operator-service",
    "recharge-service",
    "notification-service",
    "api-gateway",
    "eureka-server",
    "config-server"
)

# Set JAVA_HOME if not already set
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
$MvnCmd = "C:\Users\naray\maven\bin\mvn.cmd"

function Scan-Service {
    param([string]$svc)

    $svcPath = Join-Path $ProjectRoot $svc
    if (-not (Test-Path $svcPath)) {
        Write-Warning "Service folder not found: $svcPath - skipping"
        return
    }

    $projectKey = "Omnicharge-$svc"

    Write-Host ""
    Write-Host "===========================" -ForegroundColor Cyan
    Write-Host "  Scanning: $svc" -ForegroundColor Yellow
    Write-Host "  Project Key: $projectKey" -ForegroundColor Yellow
    Write-Host "===========================" -ForegroundColor Cyan

    Push-Location $svcPath

    Write-Host "Running: mvn clean verify sonar:sonar for $svc ..." -ForegroundColor DarkGray

    & $MvnCmd clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar `
        "-Dsonar.projectKey=$projectKey" `
        "-Dsonar.projectName=$projectKey" `
        "-Dsonar.host.url=$SonarUrl" `
        "-Dsonar.token=$SonarToken" `
        "-Dmaven.test.failure.ignore=true"

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  OK   $svc - scan complete!" -ForegroundColor Green
    } else {
        Write-Host "  WARN $svc - scan finished with warnings (exit $LASTEXITCODE)" -ForegroundColor Red
    }

    Pop-Location
}

Write-Host ""
Write-Host "OmniCharge - SonarQube Analysis Runner (Local Maven)" -ForegroundColor Magenta
Write-Host "SonarQube URL : $SonarUrl"
Write-Host "Token         : $SonarToken"
Write-Host "JAVA_HOME     : $env:JAVA_HOME"
Write-Host "Maven         : $MvnCmd"
Write-Host ""

try {
    $status = Invoke-RestMethod -Uri "$SonarUrl/api/system/status" -TimeoutSec 5
    Write-Host "OK SonarQube is UP (v$($status.version))" -ForegroundColor Green
} catch {
    Write-Error "SonarQube is not reachable at $SonarUrl. Make sure SonarQube is running."
    exit 1
}

if ($Service -ne "all") {
    Scan-Service -svc $Service
} else {
    foreach ($svc in $Services) {
        Scan-Service -svc $svc
    }
}

Write-Host ""
Write-Host "All scans complete! Open $SonarUrl to view results" -ForegroundColor Green
