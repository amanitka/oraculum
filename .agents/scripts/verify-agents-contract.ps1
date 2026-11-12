$ErrorActionPreference = 'Stop'

$maxRetries = 2
$conversationId = "default"

try {
    $inputJson = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($inputJson)) {
        Write-Output '{"decision":"allow"}'
        exit 0
    }
    $payload = $inputJson | ConvertFrom-Json
    if ($payload.terminationReason -ne 'NO_TOOL_CALL') {
        Write-Output '{"decision":"allow"}'
        exit 0
    }
    if ($payload.conversationId) {
        $conversationId = $payload.conversationId
    }
} catch {
    Write-Output '{"decision":"allow"}'
    exit 0
}

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$stateFile = Join-Path $env:TEMP "oraculum_stop_hook_${conversationId}.txt"
Push-Location $repoRoot

try {
    $changedFiles = @()
    $gitStatus = git status --porcelain -- "*.java" 2>$null
    if ($LASTEXITCODE -eq 0 -and $gitStatus) {
        foreach ($line in ($gitStatus -split "`r?`n")) {
            if ($line.Length -gt 3) {
                $status = $line.Substring(0, 2)
                if ($status -notmatch 'D') {
                    $relPath = $line.Substring(3).Trim().Trim('"')
                    if ($relPath -match ' -> ') {
                        $relPath = ($relPath -split ' -> ')[-1].Trim('"')
                    }
                    $fullPath = Join-Path $repoRoot $relPath
                    if (Test-Path $fullPath) {
                        $changedFiles += @{ RelPath = $relPath; FullPath = $fullPath }
                    }
                }
            }
        }
    }

    if ($changedFiles.Count -eq 0) {
        if (Test-Path $stateFile) { Remove-Item $stateFile -Force -ErrorAction SilentlyContinue }
        Write-Output '{"decision":"allow"}'
        exit 0
    }

    $violations = New-Object System.Collections.Generic.List[string]

    foreach ($file in $changedFiles) {
        $lines = @(Get-Content -Path $file.FullPath)
        $rel = $file.RelPath
        $isTest = $rel -match '^src[\\/]test[\\/]'

        # 0. Check production class size cap (350 lines)
        if (-not $isTest -and $lines.Count -gt 350) {
            $violations.Add("${rel} -> Class is $($lines.Count) lines long (hard cap is 350 lines). Split responsibilities into smaller classes.")
        }

        $inMethod = $false
        $braceDepth = 0
        $methodStartLine = 0
        $methodName = ""

        for ($i = 0; $i -lt $lines.Count; $i++) {
            $lineNum = $i + 1
            $line = $lines[$i]
            $trimmed = $line.Trim()

            if ($trimmed.StartsWith('//') -or $trimmed.StartsWith('*') -or $trimmed.StartsWith('/*')) {
                continue
            }

            # 1. Check wildcard imports in main sources
            if (-not $isTest -and $trimmed -match '^import\s+(?!static\s+)[\w\.]+\.\*;') {
                $violations.Add("${rel}:${lineNum} -> Wildcard import prohibited (`$trimmed`). Use explicit single-class imports.")
            }

            # 2. Check inline FQNs in code bodies (skip package & import lines)
            if ($trimmed -notmatch '^(package|import)\s+') {
                if ($trimmed -match '\b(java\.(util|time|lang|io|math|net|nio|sql)|org\.springframework|com\.oraculum|tools\.jackson|com\.fasterxml|jakarta\.)\.[A-Z]\w*') {
                    $matchedFqn = $Matches[0]
                    $violations.Add("${rel}:${lineNum} -> Inline Fully Qualified Name detected (${matchedFqn}). Add a top-level import instead.")
                }
            }

            # 3. Check @Autowired field injection in main sources
            if (-not $isTest -and $trimmed -match '^@Autowired\b') {
                $violations.Add("${rel}:${lineNum} -> Avoid @Autowired; use @RequiredArgsConstructor with private final fields.")
            }

            # 4. Track method length in main sources (cap at 30 non-empty code lines)
            if (-not $isTest) {
                if (-not $inMethod -and $braceDepth -eq 1 -and $trimmed -match '(public|protected|private|\w+)\s+[\w\<\>\[\]\,\s\?]+\s+(\w+)\s*\([^\)]*\)\s*(throws\s+[\w\,\s]+)?\{$') {
                    $methodName = $Matches[2]
                    if ($methodName -notin @('if', 'for', 'while', 'switch', 'catch')) {
                        $inMethod = $true
                        $methodStartLine = $lineNum
                    }
                }

                $openBraces = ([regex]::Matches($line, '\{')).Count
                $closeBraces = ([regex]::Matches($line, '\}')).Count
                $braceDepth += ($openBraces - $closeBraces)

                if ($inMethod -and $braceDepth -le 1) {
                    $inMethod = $false
                    $methodLines = 0
                    for ($m = $methodStartLine; $m -lt $i; $m++) {
                        $mTrim = $lines[$m].Trim()
                        if ($mTrim.Length -gt 0 -and -not $mTrim.StartsWith('//')) {
                            $methodLines++
                        }
                    }
                    if ($methodLines -gt 30) {
                        $violations.Add("${rel}:${methodStartLine} -> Method '${methodName}' is ${methodLines} non-empty lines long (cap is 25-30 lines). Extract helper methods or use early-return guard clauses.")
                    }
                }
            }
        }
    }

    if ($violations.Count -gt 0) {
        $retryCount = 0
        if (Test-Path $stateFile) {
            [int]::TryParse((Get-Content $stateFile -Raw).Trim(), [ref]$retryCount) | Out-Null
        }

        if ($retryCount -ge $maxRetries) {
            Remove-Item $stateFile -Force -ErrorAction SilentlyContinue
            Write-Output '{"decision":"allow"}'
            exit 0
        }

        Set-Content -Path $stateFile -Value ($retryCount + 1)
        $joined = ($violations | Select-Object -First 8) -join "; "
        $attemptInfo = "Attempt $($retryCount + 1) of $maxRetries"
        $reason = "AGENTS.md Verification Loop Blocked Stop ($attemptInfo): Found $($violations.Count) violation(s) in modified Java files: $joined. Please fix these violations before completing your turn."
        $out = @{
            decision = "continue"
            reason   = $reason
        } | ConvertTo-Json -Compress
        Write-Output $out
        exit 0
    }

    if (Test-Path $stateFile) { Remove-Item $stateFile -Force -ErrorAction SilentlyContinue }
    Write-Output '{"decision":"allow"}'
} finally {
    Pop-Location
}
