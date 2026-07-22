$ErrorActionPreference = 'Stop'

# Path configuration
$propertiesPath = Join-Path $PSScriptRoot "../src/main/resources/application.properties"
if (-not (Test-Path -LiteralPath $propertiesPath)) {
    $propertiesPath = "d:/Kì_5/SWP391/SEAL/shm-backend/src/main/resources/application.properties"
}

# Read properties
$dbPassword = "12345"
$postgresBin = "C:\Program Files\PostgreSQL\18\bin"
if (Test-Path -LiteralPath $propertiesPath) {
    $properties = Get-Content -LiteralPath $propertiesPath
    $passwordLine = $properties | Where-Object { $_ -like 'spring.datasource.password=*' } | Select-Object -First 1
    if ($passwordLine) {
        $dbPassword = $passwordLine.Substring($passwordLine.IndexOf('=') + 1).Trim()
    }
    $binLine = $properties | Where-Object { $_ -like 'seal.postgres.bin=*' } | Select-Object -First 1
    if ($binLine) {
        $postgresBin = $binLine.Substring($binLine.IndexOf('=') + 1).Trim()
    }
}

# Resolve psql path
$psql = Join-Path $postgresBin "psql.exe"
if (-not (Test-Path -LiteralPath $psql)) {
    # Try searching common paths
    $found = Get-ChildItem "C:\Program Files\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $psql = $found.FullName
    } else {
        throw "Could not find psql.exe. Please install PostgreSQL or configure its bin path in application.properties."
    }
}

Write-Host "Using psql: $psql" -ForegroundColor Green

# Prepare SQL statements
$sqlBuilder = [System.Text.StringBuilder]::new()
$null = $sqlBuilder.AppendLine("BEGIN;")
$null = $sqlBuilder.AppendLine("DELETE FROM team_members WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'test.user.%@seal.dev');")
$null = $sqlBuilder.AppendLine("DELETE FROM team_join_requests WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'test.user.%@seal.dev');")
$null = $sqlBuilder.AppendLine("DELETE FROM users WHERE email LIKE 'test.user.%@seal.dev';")

$null = $sqlBuilder.AppendLine("INSERT INTO users (full_name, email, password, student_id, is_fpt_student, university_name, role, status, created_at, updated_at) VALUES")

# Bcrypt hash for "123456"
$passwordHash = '$2a$10$dpnye/kA4TseeECTdSRL9uAU57uNSgNNWi6z1FqnljJ/yV9djXtHa'

for ($i = 1; $i -le 20; $i++) {
    $fullName = "Nguyen Van Test $i"
    $email = "test.user.$i`@seal.dev"
    $studentId = "SE18" + $i.ToString("D4") # SE180001, SE180002, ...
    
    $valueRow = "    (N'$fullName', '$email', '$passwordHash', '$studentId', true, 'FPT University', 'USER', 'APPROVED', NOW(), NOW())"
    if ($i -lt 20) {
        $valueRow += ","
    } else {
        $valueRow += ";"
    }
    $null = $sqlBuilder.AppendLine($valueRow)
}

$null = $sqlBuilder.AppendLine("COMMIT;")

# Set PGPASSWORD environment variable
$env:PGPASSWORD = $dbPassword
try {
    $tempSqlFile = [System.IO.Path]::GetTempFileName()
    # Write SQL as UTF-8 without BOM so psql handles N'...' or accents correctly
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($tempSqlFile, $sqlBuilder.ToString(), $utf8NoBom)

    Write-Host "Inserting 20 test users..." -ForegroundColor Cyan
    & $psql -h localhost -U postgres -d seal_hackathon -v ON_ERROR_STOP=1 -f $tempSqlFile
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Successfully generated and inserted 20 unassigned test users!" -ForegroundColor Green
        Write-Host "Default password: 123456" -ForegroundColor Green
    } else {
        throw "Failed to insert test users via psql"
    }
}
finally {
    if (Test-Path $tempSqlFile) { Remove-Item $tempSqlFile }
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
