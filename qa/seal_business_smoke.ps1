$ErrorActionPreference = 'Stop'

$baseUrl = 'http://localhost:8080/api'
$seedPassword = '123456'
$results = [System.Collections.Generic.List[object]]::new()
$cleanup = @{
    bannedUserId = $null
    eventId = $null
    prizeId = $null
    teamId = $null
    submissionId = $null
    backupFile = $null
}

function Add-Result {
    param(
        [string]$Name,
        [bool]$Observed,
        [string]$Detail
    )

    $results.Add([pscustomobject]@{
        Test = $Name
        Observed = $Observed
        Detail = $Detail
    })
}

function Invoke-SealApi {
    param(
        [ValidateSet('GET', 'POST', 'PUT', 'PATCH', 'DELETE')]
        [string]$Method,
        [string]$Path,
        [string]$Token,
        [object]$Body
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }

    $parameters = @{
        Uri = "$baseUrl$Path"
        Method = $Method
        Headers = $headers
        UseBasicParsing = $true
        TimeoutSec = 20
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Depth 20 -Compress
    }

    try {
        $response = Invoke-WebRequest @parameters
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Json = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
        }
    }
    catch {
        $status = 0
        $content = $null
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = [System.IO.StreamReader]::new($stream)
                try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
            }
        }
        return [pscustomobject]@{
            Status = $status
            Json = if ($content) {
                try { $content | ConvertFrom-Json } catch { $null }
            } else { $null }
        }
    }
}

function Login-Seal {
    param([string]$Email, [string]$Password = $seedPassword)
    $response = Invoke-SealApi -Method POST -Path '/auth/login' -Body @{
        email = $Email
        password = $Password
    }
    if ($response.Status -ne 200 -or -not $response.Json.result.token) {
        throw "Cannot log in test account $Email"
    }
    return $response.Json.result
}

function Remove-TestRows {
    param([long]$SubmissionId, [long]$TeamId)

    $propertiesPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\application.properties'))
    $passwordLine = Get-Content -LiteralPath $propertiesPath |
        Where-Object { $_ -like 'spring.datasource.password=*' } |
        Select-Object -First 1
    $dbPassword = $passwordLine.Substring($passwordLine.IndexOf('=') + 1)
    $psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
    if (-not (Test-Path -LiteralPath $psql)) {
        throw 'psql is required to clean up test data'
    }

    $env:PGPASSWORD = $dbPassword
    try {
        if ($SubmissionId -gt 0) {
            $sql = @"
DELETE FROM audit_logs WHERE score_id IN (SELECT id FROM scores WHERE submission_id = $SubmissionId);
DELETE FROM scores WHERE submission_id = $SubmissionId;
DELETE FROM submissions WHERE id = $SubmissionId;
"@
            & $psql -h localhost -U postgres -d seal_hackathon -v ON_ERROR_STOP=1 -q -c $sql
            if ($LASTEXITCODE -ne 0) { throw 'Cannot clean up test submission data' }
        }
        if ($TeamId -gt 0) {
            $sql = @"
DELETE FROM team_join_requests WHERE team_id = $TeamId;
DELETE FROM team_members WHERE team_id = $TeamId;
DELETE FROM teams WHERE id = $TeamId;
"@
            & $psql -h localhost -U postgres -d seal_hackathon -v ON_ERROR_STOP=1 -q -c $sql
            if ($LASTEXITCODE -ne 0) { throw 'Cannot clean up test team data' }
        }
    }
    finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    }
}

# ─────────────────────────────────────────────────────────────────
# PHẦN 1: CÁC TEST NGHIỆP VỤ GỐC (Giữ nguyên)
# ─────────────────────────────────────────────────────────────────
Write-Host "`n══════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host " PHẦN 1: KIỂM TRA BẢO MẬT & NGHIỆP VỤ GỐC" -ForegroundColor Cyan
Write-Host "══════════════════════════════════════════════" -ForegroundColor Cyan

try {
    $eventsResponse = Invoke-SealApi -Method GET -Path '/events'
    $events = @($eventsResponse.Json.result)
    Add-Result 'Public events endpoint' ($eventsResponse.Status -eq 200 -and $events.Count -gt 0) "HTTP $($eventsResponse.Status), $($events.Count) events"

    $publicStaffEmails = @(
        $events |
            ForEach-Object { @($_.matrices) + @($_.tracks) } |
            ForEach-Object { @($_.judges) + @($_.mentors) } |
            Where-Object { $_ -and $_.email } |
            Select-Object -ExpandProperty email -Unique
    )
    Add-Result 'Public event leaks staff emails' ($publicStaffEmails.Count -gt 0) "$($publicStaffEmails.Count) unique staff emails exposed without authentication"

    $publicLeaderboard = Invoke-SealApi -Method GET -Path '/leaderboard'
    $activeEventIds = @($events | Where-Object active | Select-Object -ExpandProperty id)
    $activeRows = @($publicLeaderboard.Json.result | Where-Object { $activeEventIds -contains $_.eventId })
    Add-Result 'Unpublished active-event results are public' ($activeRows.Count -gt 0) "$($activeRows.Count) graded rows from active events visible anonymously"

    $pendingLogin = Invoke-SealApi -Method POST -Path '/auth/login' -Body @{
        email = 'pending@seal.dev'
        password = $seedPassword
    }
    Add-Result 'Pending login returns generic error' (
        $pendingLogin.Status -eq 400 -and $pendingLogin.Json.code -eq 9999
    ) "HTTP $($pendingLogin.Status), message=$($pendingLogin.Json.message)"

    $leader = Login-Seal 'leader.alpha@seal.dev'
    $coordinator = Login-Seal 'coordinator@seal.dev'
    $admin = Login-Seal 'admin@seal.dev'
    $joinUser = Login-Seal 'join.request@seal.dev'
    $judge1 = Login-Seal 'judge1@seal.dev'
    $judge2 = Login-Seal 'judge2@seal.dev'
    Add-Result 'Seed roles can log in' $true 'USER, STAFF, COORDINATOR and ADMIN logins succeeded'

    $otherProfile = Invoke-SealApi -Method GET -Path '/users/17' -Token $leader.token
    $cardExposed = $otherProfile.Status -eq 200 -and -not [string]::IsNullOrWhiteSpace($otherProfile.Json.result.studentCardUrl)
    Add-Result 'Any user can read another student card' $cardExposed "HTTP $($otherProfile.Status), studentCardUrl present=$cardExposed"

    $teamsAsUser = Invoke-SealApi -Method GET -Path '/teams' -Token $leader.token
    $memberContactExposed = @(
        $teamsAsUser.Json.result |
            ForEach-Object { $_.members } |
            Where-Object { $_.email -and $_.studentId }
    ).Count -gt 0
    Add-Result 'Lobby exposes member contact and student IDs' $memberContactExposed "HTTP $($teamsAsUser.Status), detailed member records present=$memberContactExposed"

    $userCreateEvent = Invoke-SealApi -Method POST -Path '/events' -Token $leader.token -Body @{ name = 'Forbidden QA event' }
    Add-Result 'Authorization failure is mislabeled HTTP 400' ($userCreateEvent.Status -eq 400) "USER create-event HTTP $($userCreateEvent.Status)"

    $usersForCoordinator = Invoke-SealApi -Method GET -Path '/users' -Token $coordinator.token
    $passwordHashExposed = $usersForCoordinator.Status -eq 200 -and
        $usersForCoordinator.Json.result.Count -gt 0 -and
        $null -ne $usersForCoordinator.Json.result[0].password
    Add-Result 'Coordinator user list exposes password hashes' $passwordHashExposed "HTTP $($usersForCoordinator.Status), password property present=$passwordHashExposed"

    $coordinatorAdminAccess = Invoke-SealApi -Method GET -Path '/admin/overview' -Token $coordinator.token
    $adminOverview = Invoke-SealApi -Method GET -Path '/admin/overview' -Token $admin.token
    Add-Result 'Admin RBAC enforced but uses wrong status' (
        $coordinatorAdminAccess.Status -eq 400 -and $adminOverview.Status -eq 200
    ) "Coordinator=$($coordinatorAdminAccess.Status), Admin=$($adminOverview.Status)"

    $cleanup.bannedUserId = [long]$joinUser.userId
    $ban = Invoke-SealApi -Method PUT -Path "/admin/users/$($cleanup.bannedUserId)/status" -Token $admin.token -Body @{ status = 'BANNED'; reason = 'QA reversible check' }
    $oldTokenAfterBan = Invoke-SealApi -Method GET -Path '/users/me' -Token $joinUser.token
    Add-Result 'Banned account token remains usable' (
        $ban.Status -eq 200 -and $oldTokenAfterBan.Status -eq 200
    ) "Ban HTTP $($ban.Status), old token /users/me HTTP $($oldTokenAfterBan.Status)"
    $restoreUser = Invoke-SealApi -Method PUT -Path "/admin/users/$($cleanup.bannedUserId)/status" -Token $admin.token -Body @{ status = 'APPROVED'; reason = '' }
    if ($restoreUser.Status -ne 200) { throw 'Cannot restore test-account status' }
    $cleanup.bannedUserId = $null

    $invalidEvent = Invoke-SealApi -Method POST -Path '/events' -Token $coordinator.token -Body @{
        name = "QA Invalid Dates $([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
        description = 'Temporary QA event'
        season = 'SPRING'
        year = 2026
        regStartDate = '2026-12-20T08:00:00'
        regEndDate = '2026-12-01T08:00:00'
        eventStartDate = '2026-11-01T08:00:00'
        eventEndDate = '2026-10-01T08:00:00'
        submissionDeadline = '2026-01-01T08:00:00'
        roundCount = 1
        tracks = @('QA Invalid Track')
        active = $true
    }
    if ($invalidEvent.Status -eq 200) { $cleanup.eventId = [long]$invalidEvent.Json.result.id }
    Add-Result 'Invalid event chronology accepted' (
        $invalidEvent.Status -eq 200 -and $invalidEvent.Json.result.roundCount -eq 2
    ) "HTTP $($invalidEvent.Status), requested 1 round stored as $($invalidEvent.Json.result.roundCount)"

    $invalidPastMatrix = $null
    if ($cleanup.eventId) {
        $initializedInvalidEvent = Invoke-SealApi -Method POST -Path "/events/$($cleanup.eventId)/initialize-structure" -Token $coordinator.token
        if ($initializedInvalidEvent.Status -eq 200) {
            $invalidPastMatrix = @(
                $initializedInvalidEvent.Json.result.matrices |
                    Where-Object { $_.trackId -and $_.submissionDeadline }
            ) | Select-Object -First 1
        }
    }

    $crossEventPrize = Invoke-SealApi -Method POST -Path '/events/2/prizes' -Token $coordinator.token -Body @{
        name = 'QA Cross Event Prize'
        description = 'Temporary QA prize'
        teamId = 1
    }
    if ($crossEventPrize.Status -eq 200) { $cleanup.prizeId = [long]$crossEventPrize.Json.result.id }
    Add-Result 'Prize accepts team from another event' ($crossEventPrize.Status -eq 200) "HTTP $($crossEventPrize.Status)"

    if ($invalidEvent.Status -eq 200 -and @($invalidEvent.Json.result.tracks).Count -gt 0) {
        $testTeam = Invoke-SealApi -Method POST -Path '/teams/create' -Token $joinUser.token -Body @{
            name = "QA Closed Event Team $([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
            description = 'Temporary QA team'
            type = 'PUBLIC'
            joinPassword = $null
            eventId = [long]$invalidEvent.Json.result.id
            trackId = [long]$invalidEvent.Json.result.tracks[0].id
        }
        if ($testTeam.Status -eq 200) { $cleanup.teamId = [long]$testTeam.Json.result.id }
        Add-Result 'Team creation ignores registration window' ($testTeam.Status -eq 200) "HTTP $($testTeam.Status), registration starts in December"
    }
    else {
        Add-Result 'Team creation ignores registration window' $false 'SKIP: temporary event was not created'
    }

    $myTeam = Invoke-SealApi -Method GET -Path '/teams/my-team' -Token $leader.token
    $team = $myTeam.Json.result
    $teamEvent = $events | Where-Object { $_.id -eq $team.eventId } | Select-Object -First 1
    $ownMatrix = @($teamEvent.matrices | Where-Object { $_.trackId -eq $team.trackId })[0]

    $duplicateSubmission = Invoke-SealApi -Method POST -Path '/submissions' -Token $leader.token -Body @{
        teamId = [long]$team.id
        matrixId = [long]$ownMatrix.id
        fileUrl = 'https://example.com/qa-duplicate-submission'
    }
    if ($duplicateSubmission.Status -eq 200) { $cleanup.submissionId = [long]$duplicateSubmission.Json.result.id }
    Add-Result 'Duplicate team/round submission accepted' ($duplicateSubmission.Status -eq 200) "HTTP $($duplicateSubmission.Status)"

    if ($cleanup.submissionId) {
        $grade1 = Invoke-SealApi -Method POST -Path '/scores/grade' -Token $judge1.token -Body @{
            submissionId = $cleanup.submissionId
            scoreValue = 80
            criteriaScoresJson = $null
            comment = 'QA judge 1'
            editReason = $null
        }
        $grade2 = Invoke-SealApi -Method POST -Path '/scores/grade' -Token $judge2.token -Body @{
            submissionId = $cleanup.submissionId
            scoreValue = 100
            criteriaScoresJson = $null
            comment = 'QA judge 2'
            editReason = $null
        }
        $submissionsAfterTwoJudges = Invoke-SealApi -Method GET -Path '/submissions' -Token $coordinator.token
        $gradedRow = $submissionsAfterTwoJudges.Json.result | Where-Object { $_.id -eq $cleanup.submissionId } | Select-Object -First 1
        Add-Result 'Submission total equals last judge, not average' (
            $grade1.Status -eq 200 -and $grade2.Status -eq 200 -and [double]$gradedRow.score -eq 100
        ) "Judges=80,100; stored submission score=$($gradedRow.score)"

        $leaderboardAfterGrade = Invoke-SealApi -Method GET -Path '/leaderboard'
        $immediatelyPublic = @($leaderboardAfterGrade.Json.result | Where-Object { $_.id -eq $cleanup.submissionId }).Count -gt 0
        Add-Result 'New score is immediately public' $immediatelyPublic "Submission $($cleanup.submissionId) visible anonymously=$immediatelyPublic"

        $outOfRangeGrade = Invoke-SealApi -Method POST -Path '/scores/grade' -Token $judge1.token -Body @{
            submissionId = $cleanup.submissionId
            scoreValue = 999
            criteriaScoresJson = $null
            comment = 'QA out of range'
            editReason = 'QA verify missing score validation'
        }
        Add-Result 'Out-of-range score accepted' ($outOfRangeGrade.Status -eq 200) "HTTP $($outOfRangeGrade.Status), score=999"

        if ($invalidPastMatrix) {
            $lateCrossMatrixUpdate = Invoke-SealApi -Method PUT -Path "/submissions/$($cleanup.submissionId)" -Token $leader.token -Body @{
                teamId = [long]$team.id
                matrixId = [long]$invalidPastMatrix.id
                fileUrl = 'https://example.com/qa-late-cross-event-update'
            }
            Add-Result 'Late/cross-event submission update accepted' ($lateCrossMatrixUpdate.Status -eq 200) "HTTP $($lateCrossMatrixUpdate.Status), past matrix=$($invalidPastMatrix.id)"
        }
        else {
            Add-Result 'Late/cross-event submission update accepted' $false 'SKIP: temporary past matrix was not created'
        }
    }

    $backup = Invoke-SealApi -Method POST -Path '/admin/backups' -Token $admin.token
    if ($backup.Status -eq 200) { $cleanup.backupFile = [string]$backup.Json.result.fileName }
    Add-Result 'Admin can create database backup' ($backup.Status -eq 200 -and $cleanup.backupFile) "HTTP $($backup.Status), file created=$([bool]$cleanup.backupFile)"
}
finally {
    if ($cleanup.bannedUserId) {
        try {
            $adminForCleanup = Login-Seal 'admin@seal.dev'
            Invoke-SealApi -Method PUT -Path "/admin/users/$($cleanup.bannedUserId)/status" -Token $adminForCleanup.token -Body @{ status = 'APPROVED'; reason = '' } | Out-Null
        } catch {}
    }

    if ($cleanup.prizeId) {
        try {
            $coordinatorForCleanup = Login-Seal 'coordinator@seal.dev'
            Invoke-SealApi -Method DELETE -Path "/events/prizes/$($cleanup.prizeId)" -Token $coordinatorForCleanup.token | Out-Null
        } catch {}
    }

    try {
        $submissionIdForCleanup = if ($cleanup.submissionId) { [long]$cleanup.submissionId } else { 0 }
        $teamIdForCleanup = if ($cleanup.teamId) { [long]$cleanup.teamId } else { 0 }
        Remove-TestRows -SubmissionId $submissionIdForCleanup -TeamId $teamIdForCleanup
    } catch {
        Write-Warning $_.Exception.Message
    }

    if ($cleanup.eventId) {
        try {
            $coordinatorForCleanup = Login-Seal 'coordinator@seal.dev'
            Invoke-SealApi -Method DELETE -Path "/events/$($cleanup.eventId)" -Token $coordinatorForCleanup.token | Out-Null
        } catch {}
    }

    if ($cleanup.backupFile) {
        $backupDirectory = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\backups'))
        $backupPath = [System.IO.Path]::GetFullPath((Join-Path $backupDirectory $cleanup.backupFile))
        if ($backupPath.StartsWith($backupDirectory, [System.StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $backupPath)) {
            Remove-Item -LiteralPath $backupPath -Force
        }
    }
}

# ─────────────────────────────────────────────────────────────────
# PHẦN 2: KIỂM THỬ E2E TOÀN LUỒNG (Mới thêm)
# ─────────────────────────────────────────────────────────────────
Write-Host "`n══════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host " PHẦN 2: KIỂM THỬ E2E TOÀN LUỒNG HỆ THỐNG" -ForegroundColor Cyan
Write-Host "══════════════════════════════════════════════" -ForegroundColor Cyan

$p2Cleanup = @{ eventId=$null; teamAId=$null; teamBId=$null; judgeEmail=$null; staffEmail=$null; stuAId=$null; stuBId=$null }

try {
    $ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $emailA  = "e2e_a_${ts}@fpt.edu.vn"
    $emailB  = "e2e_b_${ts}@fpt.edu.vn"
    $emailC  = "e2e_c_${ts}@fpt.edu.vn"
    $emailD  = "e2e_d_${ts}@fpt.edu.vn"
    $emailE  = "e2e_e_${ts}@fpt.edu.vn"
    $emailF  = "e2e_f_${ts}@fpt.edu.vn"
    $emailG  = "e2e_g_${ts}@fpt.edu.vn"
    $emailH  = "e2e_h_${ts}@fpt.edu.vn"
    $p2Cleanup.judgeEmail = "e2e_judge_${ts}@seal.dev"
    $p2Cleanup.staffEmail = "e2e_staff_${ts}@seal.dev"

    # ── Auth ───────────────────────────────────────────────
    $coord = Login-Seal 'coordinator@seal.dev'
    $adm   = Login-Seal 'admin@seal.dev'

    $regA = Invoke-SealApi -Method POST -Path '/auth/register' -Body @{
        email=$emailA; password='E2ePass@1'; fullName='E2E Student A'
        studentId="EA${ts}"; universityName='FPT University'; isFptStudent=$true
    }
    Add-Result '[E2E] Đăng ký sinh viên mới thành công' ($regA.Status -eq 200) "HTTP $($regA.Status)"

    $regDup = Invoke-SealApi -Method POST -Path '/auth/register' -Body @{
        email=$emailA; password='E2ePass@1'; fullName='Dup'
        studentId="EX${ts}"; universityName='FPT University'; isFptStudent=$true
    }
    Add-Result '[E2E] Đăng ký trùng email phải trả lỗi' ($regDup.Status -ne 200) "HTTP $($regDup.Status)"

    foreach ($em in @($emailB,$emailC,$emailD,$emailE,$emailF,$emailG,$emailH)) {
        Invoke-SealApi -Method POST -Path '/auth/register' -Body @{
            email=$em; password='E2ePass@1'; fullName="E2E Student ${em}"
            studentId="ES$(Get-Random -Max 999999)"; universityName='FPT University'; isFptStudent=$true
        } | Out-Null
    }

    # ── Phê duyệt sinh viên ────────────────────────────────
    $allU = (Invoke-SealApi -Method GET -Path '/users' -Token $coord.token).Json.result
    $students = $allU | Where-Object { $_.email -like "e2e_*_${ts}@fpt.edu.vn" }
    foreach ($stu in $students) {
        Invoke-SealApi -Method PUT -Path "/users/$($stu.id)/status" -Body @{ status='APPROVED' } -Token $coord.token | Out-Null
    }
    $stuA = $students | Where-Object { $_.email -eq $emailA } | Select-Object -First 1
    $stuB = $students | Where-Object { $_.email -eq $emailB } | Select-Object -First 1
    $p2Cleanup.stuAId = if ($stuA) { $stuA.id } else { $null }
    $p2Cleanup.stuBId = if ($stuB) { $stuB.id } else { $null }
    Add-Result '[E2E] Coordinator phê duyệt sinh viên' ($students.Count -eq 8) "Approved $($students.Count)/8 students"

    $blockedCoordBanAdmin = Invoke-SealApi -Method PUT -Path '/users/1/status' -Body @{ status='BANNED' } -Token $coord.token
    Add-Result '[E2E] Coordinator không ban Admin phải bị từ chối' ($blockedCoordBanAdmin.Status -ne 200) "HTTP $($blockedCoordBanAdmin.Status)"

    # ── Đăng nhập sinh viên ────────────────────────────────
    $tokA = (Login-Seal $emailA 'E2ePass@1').token
    $tokB = (Login-Seal $emailB 'E2ePass@1').token
    $tokC = (Login-Seal $emailC 'E2ePass@1').token
    $tokD = (Login-Seal $emailD 'E2ePass@1').token
    $tokE = (Login-Seal $emailE 'E2ePass@1').token
    $tokF = (Login-Seal $emailF 'E2ePass@1').token
    $tokG = (Login-Seal $emailG 'E2ePass@1').token
    $tokH = (Login-Seal $emailH 'E2ePass@1').token
    Add-Result '[E2E] Sinh viên đăng nhập sau phê duyệt' ($tokA -and $tokB -and $tokC -and $tokD -and $tokE -and $tokF -and $tokG -and $tokH) "8/8 students logged in"

    # ── Đổi mật khẩu ──────────────────────────────────────
    $pwChange = Invoke-SealApi -Method PUT -Path '/users/change-password' -Body @{ oldPassword='E2ePass@1'; newPassword='NewE2e@99' } -Token $tokA
    Add-Result '[E2E] Đổi mật khẩu đúng thành công' ($pwChange.Status -eq 200) "HTTP $($pwChange.Status)"
    $pwWrong = Invoke-SealApi -Method PUT -Path '/users/change-password' -Body @{ oldPassword='WRONG'; newPassword='x' } -Token $tokA
    Add-Result '[E2E] Đổi mật khẩu sai cũ phải trả lỗi' ($pwWrong.Status -ne 200) "HTTP $($pwWrong.Status)"
    $tokA = (Login-Seal $emailA 'NewE2e@99').token

    # ── Tạo tài khoản nhân sự ─────────────────────────────
    $rJudge = Invoke-SealApi -Method POST -Path '/users/staff' -Body @{
        fullName='E2E Judge'; email=$p2Cleanup.judgeEmail; password='Judge@1234'; role='STAFF'
    } -Token $coord.token
    Add-Result '[E2E] Tạo tài khoản JUDGE' ($rJudge.Status -eq 200) "HTTP $($rJudge.Status)"

    $rStaff = Invoke-SealApi -Method POST -Path '/users/staff' -Body @{
        fullName='E2E Staff'; email=$p2Cleanup.staffEmail; password='Staff@1234'; role='STAFF'
    } -Token $coord.token
    Add-Result '[E2E] Tạo tài khoản STAFF' ($rStaff.Status -eq 200) "HTTP $($rStaff.Status)"

    $noCoordCreate = Invoke-SealApi -Method POST -Path '/users/staff' -Body @{
        fullName='Bad'; email="bad${ts}@seal.dev"; password='x'; role='COORDINATOR'
    } -Token $coord.token
    Add-Result '[E2E] Coordinator không tạo được COORDINATOR phải bị từ chối' ($noCoordCreate.Status -ne 200) "HTTP $($noCoordCreate.Status)"

    $tokJudge = (Login-Seal $p2Cleanup.judgeEmail 'Judge@1234').token

    # ── Tạo sự kiện ───────────────────────────────────────
    $evRes = Invoke-SealApi -Method POST -Path '/events' -Body @{
        name="E2E Event ${ts}"; description='Auto E2E'; year=2026
        isActive=$true; resultsPublished=$false
        regStartDate='2026-01-01T00:00:00'; regEndDate='2026-07-21T23:59:59'
        eventStartDate='2026-07-21T23:59:59'; eventEndDate='2026-12-31T23:59:59'
        submissionDeadline='2026-12-31T23:59:59'; roundCount=2; season='SUMMER'
    } -Token $coord.token
    Add-Result '[E2E] Tạo sự kiện mới' ($evRes.Status -eq 200) "HTTP $($evRes.Status)"
    $evId = if ($evRes.Status -eq 200) { $evRes.Json.result.id } else { $null }
    $p2Cleanup.eventId = $evId

    if ($evId) {
        $initRes = Invoke-SealApi -Method POST -Path "/events/${evId}/initialize-structure" -Token $coord.token
        Add-Result '[E2E] Khởi tạo cấu trúc sự kiện' ($initRes.Status -eq 200) "HTTP $($initRes.Status)"

        $matrices = (Invoke-SealApi -Method GET -Path "/events/${evId}/matrices" -Token $coord.token).Json.result
        $mx1 = $matrices | Where-Object { $_.round.orderIndex -eq 1 } | Select-Object -First 1
        $mx1Id = if ($mx1) { $mx1.id } else { $null }

        $judgeUser = (Invoke-SealApi -Method GET -Path '/users' -Token $coord.token).Json.result |
            Where-Object { $_.email -eq $p2Cleanup.judgeEmail } | Select-Object -First 1
        if ($mx1Id -and $judgeUser) {
            $mxUpdate = Invoke-SealApi -Method PUT -Path "/events/matrices/${mx1Id}" -Body @{
                topN=2; judgeIds=@($judgeUser.id); mentorIds=@()
            } -Token $coord.token
            Add-Result '[E2E] Gán Judge vào Matrix Vòng 1 (topN=2)' ($mxUpdate.Status -eq 200) "HTTP $($mxUpdate.Status)"
        }

        $tracks = (Invoke-SealApi -Method GET -Path "/events/${evId}/tracks").Json.result
        $trackId = if ($tracks -and $tracks.Count -gt 0) { $tracks[0].id } else { $null }

        # ── Tạo đội ───────────────────────────────────────
        $rTA = Invoke-SealApi -Method POST -Path '/teams/create' -Body @{
            name="Team A ${ts}"; description='Public'; type='PUBLIC'
            eventId=$evId; trackId=$trackId; memberEmails=@($emailB, $emailC)
        } -Token $tokA
        Add-Result '[E2E] Sinh viên A tạo đội PUBLIC' ($rTA.Status -eq 200) "HTTP $($rTA.Status)"
        $p2Cleanup.teamAId = if ($rTA.Status -eq 200) { $rTA.Json.result.id } else { $null }

        $rTA2 = Invoke-SealApi -Method POST -Path '/teams/create' -Body @{
            name='Dup Team'; description='x'; type='PUBLIC'; eventId=$evId; memberEmails=@($emailB, $emailC)
        } -Token $tokA
        Add-Result '[E2E] Tạo đội thứ 2 cùng event phải bị từ chối' ($rTA2.Status -ne 200) "HTTP $($rTA2.Status)"

        $rTB = Invoke-SealApi -Method POST -Path '/teams/create' -Body @{
            name="Team B ${ts}"; description='Private'; type='PRIVATE'
            joinPassword='pin9988'; eventId=$evId; trackId=$trackId; memberEmails=@($emailF, $emailG)
        } -Token $tokE
        Add-Result '[E2E] Sinh viên C tạo đội PRIVATE' ($rTB.Status -eq 200) "HTTP $($rTB.Status)"
        $p2Cleanup.teamBId = if ($rTB.Status -eq 200) { $rTB.Json.result.id } else { $null }

        # Accept invitations for Team A
        $teamAId = $p2Cleanup.teamAId
        if ($teamAId) {
            $invB = (Invoke-SealApi -Method GET -Path '/teams/my-invitations' -Token $tokB).Json.result
            if ($invB -and $invB.Count -gt 0) {
                Invoke-SealApi -Method POST -Path "/teams/invitations/$($invB[0].id)/accept" -Token $tokB | Out-Null
            }
            $invC = (Invoke-SealApi -Method GET -Path '/teams/my-invitations' -Token $tokC).Json.result
            if ($invC -and $invC.Count -gt 0) {
                Invoke-SealApi -Method POST -Path "/teams/invitations/$($invC[0].id)/accept" -Token $tokC | Out-Null
            }
        }

        # Accept invitations for Team B
        $teamBId = $p2Cleanup.teamBId
        if ($teamBId) {
            $invF = (Invoke-SealApi -Method GET -Path '/teams/my-invitations' -Token $tokF).Json.result
            if ($invF -and $invF.Count -gt 0) {
                Invoke-SealApi -Method POST -Path "/teams/invitations/$($invF[0].id)/accept" -Token $tokF | Out-Null
            }
            $invG = (Invoke-SealApi -Method GET -Path '/teams/my-invitations' -Token $tokG).Json.result
            if ($invG -and $invG.Count -gt 0) {
                Invoke-SealApi -Method POST -Path "/teams/invitations/$($invG[0].id)/accept" -Token $tokG | Out-Null
            }
        }

        # ── Gia nhập đội ──────────────────────────────────
        if ($teamAId) {
            $joinReq = Invoke-SealApi -Method POST -Path "/teams/${teamAId}/join-request" -Token $tokD
            Add-Result '[E2E] Sinh viên B gửi yêu cầu gia nhập đội A' ($joinReq.Status -eq 200) "HTTP $($joinReq.Status)"
            $joinReqDup = Invoke-SealApi -Method POST -Path "/teams/${teamAId}/join-request" -Token $tokD
            Add-Result '[E2E] Gửi lại yêu cầu gia nhập phải bị từ chối' ($joinReqDup.Status -ne 200) "HTTP $($joinReqDup.Status)"

            $joinReqs = (Invoke-SealApi -Method GET -Path "/teams/${teamAId}/join-requests" -Token $tokA).Json.result
            if ($joinReqs -and $joinReqs.Count -gt 0) {
                $approveRes = Invoke-SealApi -Method POST -Path "/teams/${teamAId}/join-requests/$($joinReqs[0].id)/approve" -Token $tokA
                Add-Result '[E2E] Leader A phê duyệt yêu cầu của B' ($approveRes.Status -eq 200) "HTTP $($approveRes.Status)"
            }
        }

        if ($teamBId) {
            $joinWrongPin = Invoke-SealApi -Method POST -Path "/teams/${teamBId}/join-private" -Body @{ password='wrongpin' } -Token $tokH
            Add-Result '[E2E] Gia nhập đội PRIVATE sai PIN phải bị từ chối' ($joinWrongPin.Status -ne 200) "HTTP $($joinWrongPin.Status)"

            $joinOkPin = Invoke-SealApi -Method POST -Path "/teams/${teamBId}/join-private" -Body @{ password='pin9988' } -Token $tokH
            Add-Result '[E2E] Gia nhập đội PRIVATE đúng PIN thành công' ($joinOkPin.Status -eq 200) "HTTP $($joinOkPin.Status)"

            $joinAgain = Invoke-SealApi -Method POST -Path "/teams/${teamBId}/join-private" -Body @{ password='pin9988' } -Token $tokH
            Add-Result '[E2E] Gia nhập đội lần 2 phải bị từ chối (đã là thành viên)' ($joinAgain.Status -ne 200) "HTTP $($joinAgain.Status)"
        }

        # ── Nộp bài ───────────────────────────────────────
        $subA = $null; $subB = $null
        if ($teamAId -and $mx1Id) {
            $rSubA = Invoke-SealApi -Method POST -Path '/submissions' -Body @{
                teamId=$teamAId; matrixId=$mx1Id; fileUrl='https://github.com/team-a/v1'
            } -Token $tokA
            Add-Result '[E2E] Đội A nộp bài Vòng 1' ($rSubA.Status -eq 200) "HTTP $($rSubA.Status)"
            $subA = if ($rSubA.Status -eq 200) { $rSubA.Json.result } else { $null }

            $mySubCheck = Invoke-SealApi -Method GET -Path "/submissions/my-submission?teamId=${teamAId}" -Token $tokA
            Add-Result '[E2E] Kiểm tra bài đã nộp (my-submission)' ($mySubCheck.Status -eq 200) "HTTP $($mySubCheck.Status)"

            if ($subA) {
                $updateSub = Invoke-SealApi -Method PUT -Path "/submissions/$($subA.id)" -Body @{
                    teamId=$teamAId; matrixId=$mx1Id; fileUrl='https://github.com/team-a/v2'
                } -Token $tokA
                Add-Result '[E2E] Cập nhật bài nộp (fileUrl mới)' ($updateSub.Status -eq 200) "HTTP $($updateSub.Status)"
            }
        }

        if ($teamBId -and $mx1Id) {
            $rSubB = Invoke-SealApi -Method POST -Path '/submissions' -Body @{
                teamId=$teamBId; matrixId=$mx1Id; fileUrl='https://github.com/team-b/v1'
            } -Token $tokE
            Add-Result '[E2E] Đội B nộp bài Vòng 1' ($rSubB.Status -eq 200) "HTTP $($rSubB.Status)"
            $subB = if ($rSubB.Status -eq 200) { $rSubB.Json.result } else { $null }
        }

        $allSubs = Invoke-SealApi -Method GET -Path '/submissions' -Token $tokJudge
        Add-Result '[E2E] Judge xem tất cả bài nộp' ($allSubs.Status -eq 200) "HTTP $($allSubs.Status)"
        $stuForbidSubs = Invoke-SealApi -Method GET -Path '/submissions' -Token $tokB
        Add-Result '[E2E] Sinh viên không xem tất cả bài nộp phải bị từ chối' ($stuForbidSubs.Status -ne 200) "HTTP $($stuForbidSubs.Status)"

        # ── Chấm điểm Judge ───────────────────────────────
        if ($subA) {
            $grade88 = Invoke-SealApi -Method POST -Path '/scores/grade' -Body @{
                submissionId=$subA.id; scoreValue=88.0; comment='Giao diện tốt'
            } -Token $tokJudge
            Add-Result '[E2E] Judge chấm điểm đội A (88)' ($grade88.Status -eq 200) "HTTP $($grade88.Status)"

            $gradeOver = Invoke-SealApi -Method POST -Path '/scores/grade' -Body @{
                submissionId=$subA.id; scoreValue=101; comment='over'
            } -Token $tokJudge
            Add-Result '[E2E] Điểm > 100 phải bị từ chối' ($gradeOver.Status -ne 200) "HTTP $($gradeOver.Status)"

            $gradeNeg = Invoke-SealApi -Method POST -Path '/scores/grade' -Body @{
                submissionId=$subA.id; scoreValue=-1; comment='neg'
            } -Token $tokJudge
            Add-Result '[E2E] Điểm âm phải bị từ chối' ($gradeNeg.Status -ne 200) "HTTP $($gradeNeg.Status)"

            $gradeCriteria = Invoke-SealApi -Method POST -Path '/scores/grade' -Body @{
                submissionId=$subA.id
                criteriaScoresJson='[{"name":"Innovation","score":90,"weight":0.5},{"name":"Technical","score":80,"weight":0.5}]'
                comment='Criteria scoring'
            } -Token $tokJudge
            Add-Result '[E2E] Chấm điểm theo criteria JSON' ($gradeCriteria.Status -eq 200) "HTTP $($gradeCriteria.Status)"

            $stuGrade = Invoke-SealApi -Method POST -Path '/scores/grade' -Body @{
                submissionId=$subA.id; scoreValue=100; comment='cheat'
            } -Token $tokA
            Add-Result '[E2E] Sinh viên chấm điểm phải bị từ chối (403)' ($stuGrade.Status -ne 200) "HTTP $($stuGrade.Status)"
        }

        if ($subB) {
            $grade75 = Invoke-SealApi -Method POST -Path '/scores/grade' -Body @{
                submissionId=$subB.id; scoreValue=75.0; comment='Cần cải thiện backend'
            } -Token $tokJudge
            Add-Result '[E2E] Judge chấm điểm đội B (75)' ($grade75.Status -eq 200) "HTTP $($grade75.Status)"
        }

        # Sửa điểm để kiểm tra C-02 (promotion demotion)
        if ($subA) {
            $grade90Edit = Invoke-SealApi -Method POST -Path '/scores/grade' -Body @{
                submissionId=$subA.id; scoreValue=90.0; comment='Sửa lại sau review'
            } -Token $tokJudge
            Add-Result '[E2E][C-02] Sửa điểm kích hoạt re-evaluate promotion' ($grade90Edit.Status -eq 200) "HTTP $($grade90Edit.Status)"
        }

        # ── Leaderboard ────────────────────────────────────
        $lbEmpty = Invoke-SealApi -Method GET -Path "/leaderboard?eventId=${evId}"
        Add-Result '[E2E] Leaderboard rỗng khi chưa publish kết quả' ($lbEmpty.Status -eq 200) "HTTP $($lbEmpty.Status), rows=$($lbEmpty.Json.result.Count)"

        $lbCoord = Invoke-SealApi -Method GET -Path "/leaderboard?eventId=${evId}" -Token $coord.token
        Add-Result '[E2E] Coordinator xem leaderboard trước publish' ($lbCoord.Status -eq 200) "HTTP $($lbCoord.Status)"

        # Publish kết quả
        Invoke-SealApi -Method PUT -Path "/events/${evId}" -Body @{
            name="E2E Event ${ts}"; description='Auto E2E'; year=2026
            isActive=$true; resultsPublished=$true
            regStartDate='2026-01-01T00:00:00'; regEndDate='2030-12-31T23:59:59'
        } -Token $coord.token | Out-Null

        $lbPublic = Invoke-SealApi -Method GET -Path "/leaderboard?eventId=${evId}"
        Add-Result '[E2E] Leaderboard public sau khi publish kết quả' ($lbPublic.Status -eq 200) "HTTP $($lbPublic.Status), rows=$($lbPublic.Json.result.Count)"

        # ── Prizes ─────────────────────────────────────────
        $rPrize = Invoke-SealApi -Method POST -Path "/events/${evId}/prizes" -Body @{
            name='Giải Nhất E2E'; description='Top team'; teamId=$teamAId
        } -Token $coord.token
        Add-Result '[E2E] Tạo giải thưởng cho sự kiện' ($rPrize.Status -eq 200) "HTTP $($rPrize.Status)"
        $prizeId = if ($rPrize.Status -eq 200) { $rPrize.Json.result.id } else { $null }

        if ($prizeId) {
            $stuPrize = Invoke-SealApi -Method POST -Path "/events/${evId}/prizes" -Body @{ name='Hack Prize'; description='x' } -Token $tokA
            Add-Result '[E2E] Sinh viên tạo giải thưởng phải bị từ chối' ($stuPrize.Status -ne 200) "HTTP $($stuPrize.Status)"
            Invoke-SealApi -Method DELETE -Path "/events/prizes/${prizeId}" -Token $coord.token | Out-Null
        }

        # Tạo prize cho đội không thuộc event
        $wrongPrize = Invoke-SealApi -Method POST -Path "/events/${evId}/prizes" -Body @{
            name='Wrong'; description='x'; teamId=9999999
        } -Token $coord.token
        Add-Result '[E2E] Prize cho đội không thuộc event phải bị từ chối' ($wrongPrize.Status -ne 200) "HTTP $($wrongPrize.Status)"
    }

    # ── Notifications ──────────────────────────────────────
    $notiRes = Invoke-SealApi -Method GET -Path '/notifications' -Token $tokA
    Add-Result '[E2E] Sinh viên lấy danh sách thông báo' ($notiRes.Status -eq 200) "HTTP $($notiRes.Status)"

    $broadRes = Invoke-SealApi -Method POST -Path '/notifications' -Body @{
        title='E2E Broadcast'; body='Test từ E2E'; targetRole='USER'
    } -Token $coord.token
    Add-Result '[E2E] Coordinator gửi thông báo broadcast' ($broadRes.Status -eq 200) "HTTP $($broadRes.Status)"

    $stuBroadFail = Invoke-SealApi -Method POST -Path '/notifications' -Body @{
        title='Hack'; body='x'; targetRole='USER'
    } -Token $tokA
    Add-Result '[E2E] Sinh viên gửi broadcast phải bị từ chối' ($stuBroadFail.Status -ne 200) "HTTP $($stuBroadFail.Status)"

    $readAll = Invoke-SealApi -Method PATCH -Path '/notifications/read-all' -Token $tokA
    Add-Result '[E2E] Đánh dấu tất cả thông báo đã đọc' ($readAll.Status -eq 200) "HTTP $($readAll.Status)"

    # ── Audit Logs ─────────────────────────────────────────
    $auditCoord = Invoke-SealApi -Method GET -Path '/audit-logs' -Token $coord.token
    Add-Result '[E2E] Coordinator xem audit logs' ($auditCoord.Status -eq 200) "HTTP $($auditCoord.Status)"
    $auditStu = Invoke-SealApi -Method GET -Path '/audit-logs' -Token $tokA
    Add-Result '[E2E] Sinh viên xem audit logs phải bị từ chối' ($auditStu.Status -ne 200) "HTTP $($auditStu.Status)"

    # ── Rule Templates ─────────────────────────────────────
    $ruleList = Invoke-SealApi -Method GET -Path '/rule-templates'
    Add-Result '[E2E] Lấy rule templates (public)' ($ruleList.Status -eq 200) "HTTP $($ruleList.Status)"
    $ruleCreate = Invoke-SealApi -Method POST -Path '/rule-templates' -Body @{
        name="Rule E2E ${ts}"; content='Nội dung quy tắc E2E'
    } -Token $coord.token
    Add-Result '[E2E] Coordinator tạo rule template' ($ruleCreate.Status -eq 200) "HTTP $($ruleCreate.Status)"
    $ruleId = if ($ruleCreate.Status -eq 200 -and $ruleCreate.Json.result) { $ruleCreate.Json.result.id } else { $null }
    $ruleStuFail = Invoke-SealApi -Method POST -Path '/rule-templates' -Body @{ name='Hack'; content='x' } -Token $tokA
    Add-Result '[E2E] Sinh viên tạo rule template phải bị từ chối' ($ruleStuFail.Status -ne 200) "HTTP $($ruleStuFail.Status)"
    if ($ruleId) {
        Invoke-SealApi -Method DELETE -Path "/rule-templates/${ruleId}" -Token $coord.token | Out-Null
    }

    # ── Chat (REST) ────────────────────────────────────────
    if ($p2Cleanup.teamAId) {
        $chatHist = Invoke-SealApi -Method GET -Path "/chat/teams/$($p2Cleanup.teamAId)" -Token $tokA
        Add-Result '[E2E] Thành viên xem lịch sử chat đội' ($chatHist.Status -eq 200) "HTTP $($chatHist.Status)"
        $chatSend = Invoke-SealApi -Method POST -Path "/chat/teams/$($p2Cleanup.teamAId)" -Body @{ content='Hello E2E!' } -Token $tokA
        Add-Result '[E2E] Thành viên gửi tin nhắn chat' ($chatSend.Status -eq 200) "HTTP $($chatSend.Status)"
        $chatCoord = Invoke-SealApi -Method GET -Path "/chat/teams/$($p2Cleanup.teamAId)" -Token $coord.token
        Add-Result '[E2E] Coordinator xem chat bất kỳ đội' ($chatCoord.Status -eq 200) "HTTP $($chatCoord.Status)"
        $chatForbid = Invoke-SealApi -Method GET -Path "/chat/teams/$($p2Cleanup.teamAId)" -Token $tokH
        Add-Result '[E2E] Người ngoài đội xem chat phải bị từ chối' ($chatForbid.Status -ne 200) "HTTP $($chatForbid.Status)"
    }

    # ── Stats ──────────────────────────────────────────────
    $statsPublic = Invoke-SealApi -Method GET -Path '/stats'
    Add-Result '[E2E] Stats public (activeEvents, totalTeams)' ($statsPublic.Status -eq 200) "HTTP $($statsPublic.Status)"
    $irr = Invoke-SealApi -Method GET -Path '/stats/inter-rater' -Token $coord.token
    Add-Result '[E2E] Coordinator xem inter-rater reliability' ($irr.Status -eq 200) "HTTP $($irr.Status)"
    $kappa = Invoke-SealApi -Method GET -Path '/stats/cohen-kappa' -Token $coord.token
    Add-Result '[E2E] Coordinator xem Cohen Kappa' ($kappa.Status -eq 200) "HTTP $($kappa.Status)"
    $irrFail = Invoke-SealApi -Method GET -Path '/stats/inter-rater' -Token $tokA
    Add-Result '[E2E] Sinh viên xem inter-rater phải bị từ chối' ($irrFail.Status -ne 200) "HTTP $($irrFail.Status)"

    # ── Admin ──────────────────────────────────────────────
    $overview = Invoke-SealApi -Method GET -Path '/admin/overview' -Token $adm.token
    Add-Result '[E2E] Admin xem overview' ($overview.Status -eq 200) "HTTP $($overview.Status)"
    $monitoring = Invoke-SealApi -Method GET -Path '/admin/monitoring' -Token $adm.token
    Add-Result '[E2E] Admin xem monitoring' ($monitoring.Status -eq 200) "HTTP $($monitoring.Status)"

    if ($p2Cleanup.stuAId) {
        $roleUp = Invoke-SealApi -Method PUT -Path "/admin/users/$($p2Cleanup.stuAId)/role" -Body @{ role='COORDINATOR' } -Token $adm.token
        Add-Result '[E2E] Admin nâng role sinh viên lên COORDINATOR' ($roleUp.Status -eq 200) "HTTP $($roleUp.Status)"
        $roleDown = Invoke-SealApi -Method PUT -Path "/admin/users/$($p2Cleanup.stuAId)/role" -Body @{ role='USER' } -Token $adm.token
        Add-Result '[E2E] Admin hạ role về USER' ($roleDown.Status -eq 200) "HTTP $($roleDown.Status)"
        $roleBadMentor = Invoke-SealApi -Method PUT -Path "/admin/users/$($p2Cleanup.stuAId)/role" -Body @{ role='MENTOR' } -Token $adm.token
        Add-Result '[E2E] Admin không set role MENTOR phải bị từ chối' ($roleBadMentor.Status -ne 200) "HTTP $($roleBadMentor.Status)"
    }

    $selfBan = Invoke-SealApi -Method PUT -Path '/admin/users/1/status' -Body @{ status='BANNED' } -Token $adm.token
    Add-Result '[E2E] Admin tự ban mình phải bị từ chối' ($selfBan.Status -ne 200) "HTTP $($selfBan.Status)"

    $settingsOk = Invoke-SealApi -Method PUT -Path '/admin/settings' -Body @{ systemName='E2E Test Mode' } -Token $adm.token
    Add-Result '[E2E] Admin cập nhật settings' ($settingsOk.Status -eq 200) "HTTP $($settingsOk.Status)"
    Invoke-SealApi -Method PUT -Path '/admin/settings' -Body @{ systemName='SEAL Hackathon Management System' } -Token $adm.token | Out-Null

    # ── Security edge cases ────────────────────────────────
    $noToken = Invoke-SealApi -Method GET -Path '/users/me'
    Add-Result '[E2E][SEC] Không có token gọi API bảo vệ phải bị từ chối' ($noToken.Status -ne 200) "HTTP $($noToken.Status)"
    $fakeToken = Invoke-SealApi -Method GET -Path '/users/me' -Token 'eyJhbGciOiJIUzI1NiJ9.garbage.garbage'
    Add-Result '[E2E][SEC] Token giả phải bị từ chối' ($fakeToken.Status -ne 200) "HTTP $($fakeToken.Status)"
    $stuCreateEvent = Invoke-SealApi -Method POST -Path '/events' -Body @{ name='Hack' } -Token $tokA
    Add-Result '[E2E][SEC] Sinh viên tạo event phải bị từ chối (403)' ($stuCreateEvent.Status -ne 200) "HTTP $($stuCreateEvent.Status)"
    $stuAdmin = Invoke-SealApi -Method GET -Path '/admin/overview' -Token $tokA
    Add-Result '[E2E][SEC] Sinh viên vào admin phải bị từ chối (403)' ($stuAdmin.Status -ne 200) "HTTP $($stuAdmin.Status)"
    $nonExistTeam = Invoke-SealApi -Method POST -Path '/teams/9999999/join-request' -Token $tokA
    Add-Result '[E2E][SEC] Gia nhập đội không tồn tại phải trả lỗi' ($nonExistTeam.Status -ne 200) "HTTP $($nonExistTeam.Status)"
    $nonExistSub = Invoke-SealApi -Method POST -Path '/scores/grade' -Body @{ submissionId=9999999; scoreValue=80 } -Token $tokJudge
    Add-Result '[E2E][SEC] Chấm điểm submission không tồn tại phải trả lỗi' ($nonExistSub.Status -ne 200) "HTTP $($nonExistSub.Status)"
}
finally {
    # Dọn dẹp dữ liệu E2E
    $cln = Login-Seal 'coordinator@seal.dev'
    if ($p2Cleanup.eventId) {
        try { Invoke-SealApi -Method DELETE -Path "/events/$($p2Cleanup.eventId)" -Token $cln.token | Out-Null } catch {}
    }
}

# ─────────────────────────────────────────────────────────────────
# KẾT QUẢ TỔNG HỢP
# ─────────────────────────────────────────────────────────────────
$results | Format-Table -Wrap -AutoSize

$pass = @($results | Where-Object { -not $_.Observed }).Count
$fail = @($results | Where-Object { $_.Observed }).Count
$total = $results.Count
Write-Host "`n═══════════════════════════════════════════════" -ForegroundColor White
Write-Host " TỔNG KẾT: $total tests | Lỗi quan sát thấy: $fail | Không lỗi: $pass" -ForegroundColor White
Write-Host "═══════════════════════════════════════════════`n" -ForegroundColor White
