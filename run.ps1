$ErrorActionPreference = "Stop"

Set-Location -Path $PSScriptRoot

function Load-DotEnv {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Get-Content -Path $Path | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            return
        }

        $eqIndex = $line.IndexOf("=")
        if ($eqIndex -lt 1) {
            return
        }

        $key = $line.Substring(0, $eqIndex).Trim()
        $value = $line.Substring($eqIndex + 1).Trim()
        if ($value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        if ($value.StartsWith("'") -and $value.EndsWith("'")) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}

function Require-Command {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Write-Error "$Name command not found."
    }
}

function Require-Env {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Names
    )

    foreach ($name in $Names) {
        $value = [Environment]::GetEnvironmentVariable($name, "Process")
        if ([string]::IsNullOrWhiteSpace($value)) {
            Write-Error "Missing required environment variable: $name. Check .env."
        }
    }
}

function Test-WeakPlaceholder {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    $trimmed = $Value.Trim().ToLowerInvariant()
    if ($trimmed.StartsWith("change_me") -or $trimmed -eq "changeme" -or $trimmed -eq "password") {
        Write-Error "$Name is still using a placeholder value. Update .env before starting."
    }
}

function Wait-TcpPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$HostName,
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $async = $client.BeginConnect($HostName, $Port, $null, $null)
            if ($async.AsyncWaitHandle.WaitOne(1000) -and $client.Connected) {
                $client.EndConnect($async)
                $client.Close()
                return $true
            }
        } catch {
        } finally {
            $client.Close()
        }
        Start-Sleep -Milliseconds 500
    }

    return $false
}

function Wait-HttpReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return $true
            }
        } catch {
        }
        Start-Sleep -Milliseconds 800
    }

    return $false
}

function Test-PortInUse {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop
        return ($connections | Measure-Object).Count -gt 0
    } catch {
        $match = netstat -ano | Select-String -Pattern "[:\.]$Port\s+.*LISTENING"
        return $null -ne $match
    }
}

$envFile = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path -Path $envFile)) {
    Write-Error "Missing .env. Copy .env.example to .env first."
}

Require-Command -Name "docker"
Require-Command -Name "mvn"

Load-DotEnv -Path $envFile

if ([string]::IsNullOrWhiteSpace($env:APP_PORT)) {
    $env:APP_PORT = "8080"
}
if ([string]::IsNullOrWhiteSpace($env:MINIO_ENABLED)) {
    $env:MINIO_ENABLED = "true"
}

Require-Env -Names @(
    "APP_PORT",
    "DB_HOST",
    "DB_PORT",
    "DB_NAME",
    "DB_USER",
    "DB_PASSWORD",
    "MINIO_ENDPOINT",
    "MINIO_ACCESS_KEY",
    "MINIO_SECRET_KEY",
    "MINIO_BUCKET"
)

Test-WeakPlaceholder -Name "DB_PASSWORD" -Value $env:DB_PASSWORD
Test-WeakPlaceholder -Name "MINIO_ACCESS_KEY" -Value $env:MINIO_ACCESS_KEY
Test-WeakPlaceholder -Name "MINIO_SECRET_KEY" -Value $env:MINIO_SECRET_KEY

$appPort = 0
if (-not [int]::TryParse($env:APP_PORT, [ref]$appPort)) {
    Write-Error "APP_PORT must be a valid number."
}
$dbPort = 0
if (-not [int]::TryParse($env:DB_PORT, [ref]$dbPort)) {
    Write-Error "DB_PORT must be a valid number."
}

if (Test-PortInUse -Port $appPort) {
    Write-Error "Port $appPort is already in use. Set APP_PORT in .env to another port, for example 8081."
}

$minioEnabled = @("1", "true", "yes", "on") -contains $env:MINIO_ENABLED.Trim().ToLowerInvariant()

Write-Host "[job-application-tracker] Starting Docker dependencies..."
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "[job-application-tracker] Waiting for PostgreSQL on $($env:DB_HOST):$dbPort ..."
if (-not (Wait-TcpPort -HostName $env:DB_HOST -Port $dbPort -TimeoutSeconds 120)) {
    Write-Error "PostgreSQL is not ready on $($env:DB_HOST):$dbPort."
}

if ($minioEnabled) {
    $minioHealth = ($env:MINIO_ENDPOINT.TrimEnd("/") + "/minio/health/live")
    Write-Host "[job-application-tracker] Waiting for MinIO at $minioHealth ..."
    if (-not (Wait-HttpReady -Url $minioHealth -TimeoutSeconds 120)) {
        Write-Error "MinIO is not ready at $minioHealth."
    }
}

Write-Host "[job-application-tracker] App port: $appPort"
Write-Host "[job-application-tracker] DB: jdbc:postgresql://$($env:DB_HOST):$dbPort/$($env:DB_NAME)"
Write-Host "[job-application-tracker] MinIO enabled: $minioEnabled"
Write-Host "[job-application-tracker] Main URLs:"
Write-Host "  - Login: http://localhost:$appPort/login"
Write-Host "  - Dashboard: http://localhost:$appPort/dashboard"
Write-Host "  - Applications: http://localhost:$appPort/applications"
Write-Host "  - Attachments: http://localhost:$appPort/documents"
if ($minioEnabled) {
    Write-Host "  - MinIO Console: http://localhost:9001"
}

& mvn "-DskipTests" "spring-boot:run" "-Dspring-boot.run.arguments=--server.port=$appPort"
exit $LASTEXITCODE
