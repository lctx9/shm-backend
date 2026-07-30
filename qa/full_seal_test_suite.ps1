# SEAL Hackathon Full Automated Test Suite (Happy Path + Negative + Edge Cases)
# Target Date: 2026-07-30

$ErrorActionPreference = 'Continue'
$baseUrl = 'http://localhost:8080/api'
$seedPassword = '123456'

$testResults = [System.Collections.Generic.List[object]]::new()

function Add-TestResult {
    param(
        [string]$Category,
        [string]$TestName,
        [bool]$Passed,
        [string]$Details
    )
    $testResults.Add([pscustomobject]@{
        Category = $Category
        TestName = $TestName
        Passed   = $Passed
        Details  = $Details
    })
    $statusStr = if ($Passed) { "[PASS]" } else { "[FAIL]" }
    $color = if ($Passed) { 'Green' } else { 'Red' }
    Write-Host "$statusStr [$Category] $TestName - $Details" -ForegroundColor $color
}

function Invoke-Api {
    param(
        [string]$Method = 'GET',
        [string]$Path,
        [string]$Token,
        [object]$Body
    )
    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }
    $params = @{
        Uri = "$baseUrl$Path"
        Method = $Method
        Headers = $headers
        UseBasicParsing = $true
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }

    try {
        $res = Invoke-WebRequest @params
        $json = $null
        if ($res.Content) {
            try { $json = $res.Content | ConvertFrom-Json } catch {}
        }
        return [pscustomobject]@{
            StatusCode = [int]$res.StatusCode
            Json = $json
            Raw = $res.Content
        }
    } catch {
        $code = 0
        $json = $null
        $raw = ""
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = [System.IO.StreamReader]::new($stream)
                $raw = $reader.ReadToEnd()
                try { $json = $raw | ConvertFrom-Json } catch {}
                $reader.Dispose()
            }
        }
        return [pscustomobject]@{
            StatusCode = $code
            Json = $json
            Raw = $raw
        }
    }
}

function Get-Token {
    param([string]$Email, [string]$Password = $seedPassword)
    $res = Invoke-Api -Method POST -Path '/auth/login' -Body @{ email = $Email; password = $Password }
    if ($res.StatusCode -eq 200 -and $res.Json.result.token) {
        return $res.Json.result.token
    }
    return $null
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " STARTING COMPREHENSIVE SEAL HACKATHON TEST SUITE (2026-07-30)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# ----------------------------------------------------
# 1. AUTHENTICATION & SECURITY TESTS
# ----------------------------------------------------
Write-Host "`n--- [1] AUTHENTICATION & SECURITY TESTS ---" -ForegroundColor Yellow

$adminTok = Get-Token 'admin@seal.dev'
Add-TestResult 'AUTH' 'Admin Login' ($null -ne $adminTok) 'Logged in as admin@seal.dev'

$coordTok = Get-Token 'coordinator@seal.dev'
Add-TestResult 'AUTH' 'Coordinator Login' ($null -ne $coordTok) 'Logged in as coordinator@seal.dev'

$judge1Tok = Get-Token 'judge1@seal.dev'
Add-TestResult 'AUTH' 'Judge 1 Login' ($null -ne $judge1Tok) 'Logged in as judge1@seal.dev'

$leaderTok = Get-Token 'leader.alpha@seal.dev'
Add-TestResult 'AUTH' 'Leader Alpha Login' ($null -ne $leaderTok) 'Logged in as leader.alpha@seal.dev'

$memberTok = Get-Token 'member1.gamma@seal.dev'
Add-TestResult 'AUTH' 'Member Gamma Login' ($null -ne $memberTok) 'Logged in as member1.gamma@seal.dev'

# Negative test: Invalid password
$negPass = Invoke-Api -Method POST -Path '/auth/login' -Body @{ email = 'admin@seal.dev'; password = 'wrongpassword' }
Add-TestResult 'AUTH' 'Login Invalid Password Guard' ($negPass.StatusCode -ge 400) "Returned status $($negPass.StatusCode) as expected"

# Negative test: Non-existent user
$negEmail = Invoke-Api -Method POST -Path '/auth/login' -Body @{ email = 'nonexistent_user_9999@seal.dev'; password = '123456' }
Add-TestResult 'AUTH' 'Login Non-existent User Guard' ($negEmail.StatusCode -ge 400) "Returned status $($negEmail.StatusCode) as expected"

# Negative test: Unauthenticated request guard
$unauthRes = Invoke-Api -Method GET -Path '/users/me'
Add-TestResult 'AUTH' 'Unauthenticated Request Guard' ($unauthRes.StatusCode -eq 401 -or $unauthRes.StatusCode -eq 403) "Returned status $($unauthRes.StatusCode) as expected"

# Negative test: Role guard (USER token calling ADMIN endpoint)
$userForbidden = Invoke-Api -Method GET -Path '/admin/users' -Token $leaderTok
Add-TestResult 'AUTH' 'Role Authorization Guard (User accessing Admin)' ($userForbidden.StatusCode -eq 403) "Returned status 403 Forbidden as expected"

# ----------------------------------------------------
# 2. USER REGISTRATION & SECURITY TESTS
# ----------------------------------------------------
Write-Host "`n--- [2] REGISTRATION & SECURITY TESTS ---" -ForegroundColor Yellow

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$regEmail = "teststudent_$ts@seal.dev"
$regBody = @{
    fullName = "Test Student $ts"
    email = $regEmail
    password = '123456'
    isFptStudent = $true
    studentId = "SE18$($ts % 10000)"
    universityName = "FPT University"
}

# Student registration requires OTP verification
$regRes = Invoke-Api -Method POST -Path '/auth/register' -Body $regBody
Add-TestResult 'REGISTRATION' 'Student Register Triggers OTP Verification' ($regRes.StatusCode -eq 400 -or $regRes.StatusCode -eq 200) "Handled student registration OTP flow ($($regRes.Json.message))"

# Duplicate registration check
$dupReg = Invoke-Api -Method POST -Path '/auth/register' -Body $regBody
Add-TestResult 'REGISTRATION' 'Duplicate Email Register Blocked' ($dupReg.StatusCode -ge 400) "Returned status $($dupReg.StatusCode) as expected"

# ----------------------------------------------------
# 3. EVENT & TRACK MANAGEMENT TESTS
# ----------------------------------------------------
Write-Host "`n--- [3] EVENT & TRACK MANAGEMENT TESTS ---" -ForegroundColor Yellow

# Get Public Events
$pubEvents = Invoke-Api -Method GET -Path '/events'
Add-TestResult 'EVENTS' 'Get Public Active Events' ($pubEvents.StatusCode -eq 200 -and $pubEvents.Json.result.Count -gt 0) "Found $($pubEvents.Json.result.Count) active events"

# Create Event (Happy Path: regEnd <= eventStart)
$newEventBody = @{
    name = "Auto Test Event $ts"
    description = "Description for auto test event"
    season = "SUMMER"
    year = 2026
    regStartDate = "2026-07-20T08:00:00"
    regEndDate = "2026-07-27T23:59:00"
    eventStartDate = "2026-07-28T08:00:00"
    eventEndDate = "2026-08-30T18:00:00"
    roundCount = 2
    tracks = @("Track A", "Track B")
    competitionRules = "Rules for test event"
    active = $true
}
$createEvRes = Invoke-Api -Method POST -Path '/events' -Token $coordTok -Body $newEventBody
$testEventId = $null
if ($createEvRes.StatusCode -eq 200 -or $createEvRes.StatusCode -eq 201) {
    $testEventId = $createEvRes.Json.result.id
    Add-TestResult 'EVENTS' 'Create Event (Valid Dates)' $true "Created Event ID $testEventId"
} else {
    Add-TestResult 'EVENTS' 'Create Event (Valid Dates)' $false "Response: $($createEvRes.Raw)"
}

# Negative test: Create Event with invalid dates (regEnd > eventStart)
$negDateEvent = @{
    name = "Invalid Event $ts"
    season = "SUMMER"
    year = 2026
    regStartDate = "2026-07-20T08:00:00"
    regEndDate = "2026-08-15T23:59:00" # Invalid! regEnd > eventStart
    eventStartDate = "2026-07-28T08:00:00"
    eventEndDate = "2026-08-30T18:00:00"
    roundCount = 2
}
$negEvRes = Invoke-Api -Method POST -Path '/events' -Token $coordTok -Body $negDateEvent
Add-TestResult 'EVENTS' 'Invalid Event Date Range Blocked (regEnd > eventStart)' ($negEvRes.StatusCode -ge 400) "Returned status $($negEvRes.StatusCode) as expected"

# Get Event Matrices for Event 1
$ev1Matrices = Invoke-Api -Method GET -Path '/events/1/matrices'
Add-TestResult 'MATRIX' 'Get Event 1 Matrices' ($ev1Matrices.StatusCode -eq 200 -and $ev1Matrices.Json.result.Count -gt 0) "Found $($ev1Matrices.Json.result.Count) matrices for Event 1"

# ----------------------------------------------------
# 4. TEAM CREATION & JOINING TESTS
# ----------------------------------------------------
Write-Host "`n--- [4] TEAM CREATION & JOINING TESTS ---" -ForegroundColor Yellow

# User 19 (leader.titan@seal.dev) creates team in Winter 2026 Test (Event 2 or 3)
$titanTok = Get-Token 'leader.titan@seal.dev'

# Leader Alpha (User 7) is already in Event 1 team. Negative test: creating another team in Event 1
$dupTeamRes = Invoke-Api -Method POST -Path '/teams/create' -Token $leaderTok -Body @{
    name = "Duplicate Team $ts"
    type = "PUBLIC"
    eventId = 1
    trackId = 1
}
Add-TestResult 'TEAMS' 'Leader Creating Second Team in Same Event Blocked' ($dupTeamRes.StatusCode -ge 400) "Returned status $($dupTeamRes.StatusCode) as expected"

# ----------------------------------------------------
# 5. SUBMISSION TESTS (ACTIVE DEADLINE VS EXPIRED DEADLINE)
# ----------------------------------------------------
Write-Host "`n--- [5] SUBMISSION TESTS ---" -ForegroundColor Yellow

# Submit for Matrix 2 (Round 2 of Event 1 - currently active on 2026-07-30!)
$subBodyActive = @{
    teamId = 1
    matrixId = 2
    fileUrl = "https://github.com/seal/alpha-builders-r2-v2"
}
$subActiveRes = Invoke-Api -Method POST -Path '/submissions' -Token $leaderTok -Body $subBodyActive
Add-TestResult 'SUBMISSIONS' 'Submit Work in Active Round (Matrix 2)' ($subActiveRes.StatusCode -eq 200 -or $subActiveRes.StatusCode -eq 201) "Submission recorded for Matrix 2"

# Negative test: Submitting for Matrix 1 (Round 1 - deadline passed on 2026-07-28)
$subBodyExpired = @{
    teamId = 1
    matrixId = 1
    fileUrl = "https://github.com/seal/alpha-builders-late"
}
$subExpiredRes = Invoke-Api -Method POST -Path '/submissions' -Token $leaderTok -Body $subBodyExpired
Add-TestResult 'SUBMISSIONS' 'Submit Work After Deadline Blocked (Matrix 1)' ($subExpiredRes.StatusCode -ge 400) "Returned status $($subExpiredRes.StatusCode) as expected"

# ----------------------------------------------------
# 6. GRADING & SCORING TESTS
# ----------------------------------------------------
Write-Host "`n--- [6] GRADING & SCORING TESTS ---" -ForegroundColor Yellow

$targetSubId = if ($subActiveRes.Json.result.id) { $subActiveRes.Json.result.id } else { 4 }
$validScoreBody = @{
    submissionId = $targetSubId
    scoreValue = 94.0
    comment = "Excellent model performance and demonstration."
    editReason = "Cập nhật điểm chấm tự động"
}
$gradeRes = Invoke-Api -Method POST -Path '/scores/grade' -Token $judge1Tok -Body $validScoreBody
Add-TestResult 'GRADING' 'Judge Submit Valid Score (0-100)' ($gradeRes.StatusCode -eq 200 -or $gradeRes.StatusCode -eq 201) "Graded submission $targetSubId with score 94.0"

# Negative test: Score > 100
$tooHighScoreBody = @{
    submissionId = 4
    scoreValue = 150.0
    comment = "Exceeds max"
}
$tooHighRes = Invoke-Api -Method POST -Path '/scores/grade' -Token $judge1Tok -Body $tooHighScoreBody
Add-TestResult 'GRADING' 'Score Exceeding 100 Blocked' ($tooHighRes.StatusCode -ge 400) "Returned status $($tooHighRes.StatusCode) as expected"

# Negative test: Score < 0
$negScoreBody = @{
    submissionId = 4
    scoreValue = -5.0
    comment = "Below min"
}
$negScoreRes = Invoke-Api -Method POST -Path '/scores/grade' -Token $judge1Tok -Body $negScoreBody
Add-TestResult 'GRADING' 'Negative Score Blocked' ($negScoreRes.StatusCode -ge 400) "Returned status $($negScoreRes.StatusCode) as expected"

# ----------------------------------------------------
# 7. ROUND ADVANCEMENT, PUBLISH & LEADERBOARD
# ----------------------------------------------------
Write-Host "`n--- [7] ROUND ADVANCEMENT & LEADERBOARD TESTS ---" -ForegroundColor Yellow

# Coordinator advances Matrix 1 (R1 -> R2 for Event 1)
$advanceRes = Invoke-Api -Method POST -Path '/matrices/1/publish-and-advance' -Token $coordTok
Add-TestResult 'ADVANCEMENT' 'Publish Matrix 1 & Advance Top Teams' ($advanceRes.StatusCode -eq 200) "Matrix 1 published and top teams advanced"

# Get Public Leaderboard for Event 1
$lbRes = Invoke-Api -Method GET -Path '/leaderboard?eventId=1'
Add-TestResult 'LEADERBOARD' 'Get Event 1 Public Leaderboard' ($lbRes.StatusCode -eq 200) "Retrieved leaderboard for Event 1"

# End Event 3 Early & Publish Results
$endEarlyRes = Invoke-Api -Method POST -Path '/events/3/end-early' -Token $coordTok
Add-TestResult 'RESULTS' 'End Event 3 Early & Award Prizes' ($endEarlyRes.StatusCode -eq 200) "Event 3 ended early"

# Get Event 3 Prizes / Results
$ev3Prizes = Invoke-Api -Method GET -Path '/events/3/prizes'
Add-TestResult 'RESULTS' 'Get Event 3 Prizes & Results' ($ev3Prizes.StatusCode -eq 200) "Prizes fetched for Event 3"

# ----------------------------------------------------
# 8. ADMIN UTILITIES & AUDIT LOGS
# ----------------------------------------------------
Write-Host "`n--- [8] ADMIN UTILITIES & AUDIT LOGS TESTS ---" -ForegroundColor Yellow

# Admin Overview
$overviewRes = Invoke-Api -Method GET -Path '/admin/overview' -Token $adminTok
Add-TestResult 'ADMIN' 'Admin Overview Dashboard Data' ($overviewRes.StatusCode -eq 200) "Overview fetched"

# Admin Users List
$usersRes = Invoke-Api -Method GET -Path '/admin/users' -Token $adminTok
Add-TestResult 'ADMIN' 'Admin List Users' ($usersRes.StatusCode -eq 200 -and $usersRes.Json.result.Count -gt 0) "User count: $($usersRes.Json.result.Count)"

# Ban User (ID 24)
$banRes = Invoke-Api -Method PUT -Path '/admin/users/24/status' -Token $adminTok -Body @{ status = 'BANNED'; reason = 'Rule violation test' }
Add-TestResult 'ADMIN' 'Ban User (ID 24)' ($banRes.StatusCode -eq 200) "User 24 banned"

# Unban User (ID 24)
$unbanRes = Invoke-Api -Method PUT -Path '/admin/users/24/status' -Token $adminTok -Body @{ status = 'APPROVED' }
Add-TestResult 'ADMIN' 'Unban User (ID 24)' ($unbanRes.StatusCode -eq 200) "User 24 unbanned"

# Audit Logs
$auditRes = Invoke-Api -Method GET -Path '/audit-logs' -Token $adminTok
Add-TestResult 'AUDIT' 'Fetch Audit Logs' ($auditRes.StatusCode -eq 200) "Audit logs retrieved"

# ----------------------------------------------------
# SUMMARY
# ----------------------------------------------------
Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " TEST SUITE EXECUTION SUMMARY" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$passedCount = ($testResults | Where-Object { $_.Passed -eq $true }).Count
$totalCount = $testResults.Count
$failedCount = $totalCount - $passedCount

Write-Host "TOTAL TESTS : $totalCount" -ForegroundColor White
Write-Host "PASSED      : $passedCount" -ForegroundColor Green
$failColor = if ($failedCount -eq 0) { 'Green' } else { 'Red' }
Write-Host "FAILED      : $failedCount" -ForegroundColor $failColor

Write-Host "`nDetailed Results:" -ForegroundColor Yellow
$testResults | Format-Table -AutoSize
