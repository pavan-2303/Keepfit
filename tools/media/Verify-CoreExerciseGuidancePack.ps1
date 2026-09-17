param(
    [string]$RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")),
    [int]$ExpectedCount = 25
)

$ErrorActionPreference = "Stop"

$registryPath = Join-Path $RepositoryRoot "core\media\src\main\kotlin\com\keepfit\core\media\CoreExerciseGuidanceCatalog.kt"
$cataloguePath = Join-Path $RepositoryRoot "core\database\src\main\assets\catalogue\exercises-v1.json"

if (-not (Test-Path -LiteralPath $registryPath)) {
    throw "Guidance registry not found: $registryPath"
}
if (-not (Test-Path -LiteralPath $cataloguePath)) {
    throw "Bundled exercise catalogue not found: $cataloguePath"
}

$registry = Get-Content -Raw -LiteralPath $registryPath
$pattern = 'guide\(\s*"(?<exerciseId>[0-9a-f-]{36})",\s*"(?<sourceId>[^"]+)",\s*"(?<name>[^"]+)"'
$entries = [regex]::Matches($registry, $pattern) | ForEach-Object {
    [pscustomobject]@{
        ExerciseId = $_.Groups['exerciseId'].Value
        SourceId = $_.Groups['sourceId'].Value
        Name = $_.Groups['name'].Value
    }
}

if ($entries.Count -ne $ExpectedCount) {
    throw "Expected $ExpectedCount guidance entries but found $($entries.Count)."
}
if (($entries.ExerciseId | Sort-Object -Unique).Count -ne $ExpectedCount) {
    throw "Guidance exercise UUIDs must be unique."
}
if (($entries.SourceId | Sort-Object -Unique).Count -ne $ExpectedCount) {
    throw "Guidance source IDs must be unique."
}

foreach ($requiredText in @(
    'artworkFamily = "Keepfit movement figures v1"',
    'creator = "Keepfit"',
    'rightsBasis = "Original code-native artwork"',
    'reviewedOn = "2026-09-15"'
)) {
    if (-not $registry.Contains($requiredText)) {
        throw "Missing guidance rights declaration: $requiredText"
    }
}

$catalogue = Get-Content -Raw -LiteralPath $cataloguePath | ConvertFrom-Json
$catalogueById = @{}
foreach ($exercise in $catalogue) {
    $catalogueById[$exercise.id] = $exercise
}

foreach ($entry in $entries) {
    $exercise = $catalogueById[$entry.ExerciseId]
    if ($null -eq $exercise) {
        throw "Guidance entry $($entry.ExerciseId) is not in the bundled catalogue."
    }
    if ($exercise.sourceId -ne $entry.SourceId) {
        throw "Source ID mismatch for $($entry.ExerciseId): expected $($exercise.sourceId), found $($entry.SourceId)."
    }
    if ($exercise.name -ne $entry.Name) {
        throw "Exercise name mismatch for $($entry.ExerciseId): expected '$($exercise.name)', found '$($entry.Name)'."
    }
}

$visualExtensions = @('.gif', '.mp4', '.webm', '.png', '.jpg', '.jpeg', '.webp', '.svg')
$bundledVisuals = Get-ChildItem -Path (Join-Path $RepositoryRoot 'core\media\src\main') -Recurse -File |
    Where-Object { $_.Extension.ToLowerInvariant() -in $visualExtensions }
if ($bundledVisuals.Count -gt 0) {
    throw "The code-native guidance pack unexpectedly contains visual files: $($bundledVisuals.FullName -join ', ')"
}

Write-Output "Verified $ExpectedCount original guidance entries against the bundled catalogue."
Write-Output "No bitmap, SVG, GIF, or video asset is present in the code-native pack."
