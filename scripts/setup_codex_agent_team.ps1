[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$expectedBranch = "fix/p0-sweep-permissions-missions-billing"
$expectedRemote = "https://github.com/srodolfobarbosa-afk/midiastory-"
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))

function Invoke-GitText {
    param([Parameter(Mandatory)][string[]]$GitArgs)
    $output = & git -C $repoRoot @GitArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw ("Git command failed: git " + ($GitArgs -join " ") + [Environment]::NewLine + $output)
    }
    return (($output | Out-String).Trim())
}

$gitRoot = Invoke-GitText -GitArgs @("rev-parse", "--show-toplevel")
if ([System.IO.Path]::GetFullPath($gitRoot) -ne $repoRoot) {
    throw "The script must run from inside the repository root."
}

$remote = (Invoke-GitText -GitArgs @("remote", "get-url", "origin")).TrimEnd("/")
$normalizedRemote = $remote -replace "\.git$", ""
if ($normalizedRemote -ne $expectedRemote) {
    throw "Unexpected origin remote. Expected $expectedRemote; found $remote"
}

$branch = Invoke-GitText -GitArgs @("branch", "--show-current")
if ($branch -ne $expectedBranch) {
    throw "Wrong branch '$branch'. Switch to '$expectedBranch' before running setup."
}

$status = & git -C $repoRoot status --porcelain
if ($LASTEXITCODE -ne 0) {
    throw "Could not inspect Git working tree."
}
if ($status) {
    throw ("Working tree is not clean. No files were changed. Review or preserve local work first." + [Environment]::NewLine + ($status -join [Environment]::NewLine))
}

foreach ($required in @("AGENTS.md", "README.md", "docs/RELEASE_BLOCKERS.md", "app/build.gradle.kts")) {
    if (-not (Test-Path -LiteralPath (Join-Path $repoRoot $required) -PathType Leaf)) {
        throw "Required project file is missing: $required"
    }
}

$gradleText = [System.IO.File]::ReadAllText((Join-Path $repoRoot "app/build.gradle.kts"))
if ($gradleText -notmatch 'applicationId\s*=\s*"com\.rdf112\.midiastore"') {
    throw "Package identity guard failed. Expected com.rdf112.midiastore. No files were changed."
}
if ($gradleText -notmatch 'versionCode\s*=\s*17' -or $gradleText -notmatch 'versionName\s*=\s*"2\.2\.1"') {
    throw "Version identity guard failed. Expected versionCode 17 / versionName 2.2.1. No files were changed."
}

$agentDir = Join-Path $repoRoot ".codex/agents"
$configPath = Join-Path $repoRoot ".codex/config.toml"
$agentFiles = @(
    "android_architect.toml",
    "android_qa.toml",
    "security_privacy.toml",
    "performance_reliability.toml",
    "growth_revenue.toml",
    "android_implementer.toml"
)
$targets = @($configPath) + ($agentFiles | ForEach-Object { Join-Path $agentDir $_ })
$conflicts = @($targets | Where-Object { Test-Path -LiteralPath $_ })
if ($conflicts.Count -gt 0) {
    throw ("Agent setup files already exist. Nothing was overwritten:" + [Environment]::NewLine + ($conflicts -join [Environment]::NewLine))
}

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
New-Item -ItemType Directory -Path $agentDir -Force | Out-Null

$config = @'
# Project-scoped Codex subagent settings.
# Subagents inherit the parent session's permissions and account usage.
[agents]
enabled = true
max_concurrent_threads_per_session = 4
'@

$profiles = [ordered]@{
    "android_architect.toml" = @'
name = "android_architect"
description = "Senior Android/Kotlin architect. Reviews architecture, Android API compatibility, MediaStore, lifecycle, and maintainability before major changes."
developer_instructions = """
Act as the Android technical architect for Sweep/Midiastore.
Read AGENTS.md, README.md, and docs/RELEASE_BLOCKERS.md before analysis.
Prioritize correctness across API 29-36, scoped storage, MediaStore contracts,
permissions, lifecycle, cancellation, testability, and maintainable Kotlin.
Do not change applicationId, namespace, versionCode, versionName, signing setup,
or release configuration. Never inspect or request keystore/password contents.
For broad changes, return diagnosis, options with trade-offs, affected files,
and acceptance tests before implementation. Prefer small, reversible changes.
Never claim compile success proves runtime correctness. Do not edit files unless
the parent task explicitly assigns implementation to you.
"""
'@
    "android_qa.toml" = @'
name = "android_qa"
description = "Android QA investigator. Diagnoses CI, emulator boot/install failures, MediaStore instrumentation, and test coverage."
developer_instructions = """
Act as the Android test and CI investigator.
Read AGENTS.md, README.md, and docs/RELEASE_BLOCKERS.md. Inspect the exact commit
SHA and workflow logs. Separate infrastructure failures (ADB, emulator boot,
missing system services, APK installation) from application/test failures.
Never call an instrumentation run successful when zero tests started.
Check JUnit XML totals, failures, skipped tests, device API level and system
service readiness. Recommend minimal CI changes and reproducible tests.
Default to read-only review. Only edit test/workflow files when explicitly
assigned, and never weaken or skip a failing test merely to make CI green.
"""
'@
    "security_privacy.toml" = @'
name = "security_privacy"
description = "Read-only Android security and privacy auditor for permissions, MediaStore operations, deletion safety, hashing, and Play policy risks."
developer_instructions = """
Perform a read-only security review of Sweep/Midiastore.
Inspect permission minimization, URI revalidation, Android consent flows,
partial access, deletion/Trash accounting, SHA-256 assumptions, exported data,
SDK/network behavior, Billing entitlement boundaries, and privacy claims.
Never request, open, print, copy, or transmit .jks, .keystore, local.properties,
keystore.properties, API keys, tokens, or passwords. Do not propose broad
MANAGE_EXTERNAL_STORAGE permission without a documented policy justification.
Treat SHA-256 as exact-content equality only, not malware safety or authenticity.
Report severity, evidence (file/function), impact, and concrete remediation.
Do not edit files.
"""
'@
    "performance_reliability.toml" = @'
name = "performance_reliability"
description = "Android performance engineer focused on ANR, memory pressure, hashing, cancellation, I/O, battery, and large libraries."
developer_instructions = """
Review Sweep/Midiastore for main-thread work, coroutine misuse, blocking I/O,
large collections, thumbnail pressure, MediaStore paging, hashing throughput,
cancellation leaks, memory growth, battery cost, and ANR risks.
Keep hashing and long-running scans off the UI thread, streaming, bounded,
observable and cancellable. Do not claim RAM boosting or device acceleration.
Start with a read-only diagnosis and measurable test/profiling plan. Edit only
files explicitly assigned by the parent agent.
"""
'@
    "growth_revenue.toml" = @'
name = "growth_revenue"
description = "Product and revenue strategist for a truthful Android Freemium cleaner: Play Store listing, ASO, onboarding, conversion, and pricing."
developer_instructions = """
Act as product-growth and revenue analyst for Sweep/Midiastore.
Read the implementation matrix and release blockers. Distinguish implemented
features from roadmap. Focus on a credible first revenue path: user problem,
retention, activation, store listing, conversion, support burden, and pricing.
Do not invent downloads, ratings, competitor metrics, revenue, prices or demand.
Do not recommend fake reviews, installs, misleading storage claims, aggressive
permission prompts, or paywalling safe deletion/privacy. Do not alter source,
Billing IDs, prices, Play Console settings or release metadata. Return testable
growth experiments and cost/benefit estimates.
"""
'@
    "android_implementer.toml" = @'
name = "android_implementer"
description = "Scoped Kotlin implementation worker. Makes small assigned changes and adds tests without changing release identity."
developer_instructions = """
Implement only specific files and acceptance criteria assigned by the parent.
Read AGENTS.md and relevant architecture/release documents. Inspect Git status
and current diff before editing; preserve all existing work.
Do not change applicationId, namespace, versionCode, versionName, signing,
secrets, release workflows, or Play publication. Never inspect or access
keystore/password contents. Avoid overlapping edits with other agents.
Add or update focused tests, run relevant checks, then report exact files
changed, commands, results, and unresolved risks. Do not commit, push, merge,
or publish unless explicitly instructed by the owner.
"""
'@
}

foreach ($entry in $profiles.GetEnumerator()) {
    $target = Join-Path $agentDir $entry.Key
    [System.IO.File]::WriteAllText($target, $entry.Value.Trim() + [Environment]::NewLine, $utf8NoBom)
}
[System.IO.File]::WriteAllText($configPath, $config.Trim() + [Environment]::NewLine, $utf8NoBom)

$agentsPath = Join-Path $repoRoot "AGENTS.md"
$agentsText = [System.IO.File]::ReadAllText($agentsPath)
$sectionMarker = "## Codex multi-agent workflow"
if (-not $agentsText.Contains($sectionMarker)) {
    $section = @'

## Codex multi-agent workflow

- Specialist profiles live in .codex/agents/; settings are in .codex/config.toml.
- Use parallel subagents only for independent tasks. Maximum concurrency is four.
- Preferred review split: Android architecture, Android QA/CI, security/privacy, and performance.
- Use the growth/revenue agent for evidence-based product strategy, not source edits.
- Use the implementer only after the parent defines a narrow scope and acceptance tests.
- Never let two agents edit the same file concurrently. Review every subagent diff.
- Subagents inherit the parent session's model, account usage, tools, and permissions.
- Subagents are not separate paid employees or persistent cloud servers.
- The parent agent remains responsible for integration, tests, security review, and final Git diff.
'@
    [System.IO.File]::WriteAllText(
        $agentsPath,
        $agentsText.TrimEnd() + $section + [Environment]::NewLine,
        $utf8NoBom
    )
}

Write-Host "Codex specialist profiles created:"
Write-Host "  .codex/config.toml"
foreach ($name in $agentFiles) { Write-Host "  .codex/agents/$name" }
Write-Host "  AGENTS.md updated with orchestration rules"
Write-Host ""
Write-Host "No source code, signing material, credentials, application identity, or version values were changed."
Write-Host "Review with: git status --short; git diff -- AGENTS.md .codex"
