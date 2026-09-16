param(
    [Parameter(Mandatory = $true)]
    [string]$SourceDirectory
)

$ErrorActionPreference = 'Stop'
$importer = Join-Path $PSScriptRoot 'Import-ExercisesDataset.ps1'
if (-not (Test-Path -LiteralPath $importer -PathType Leaf)) {
    throw "Importer missing: $importer"
}

$firstOutput = Join-Path ([IO.Path]::GetTempPath()) ("keepfit-catalogue-first-" + [guid]::NewGuid().ToString('N'))
$secondOutput = Join-Path ([IO.Path]::GetTempPath()) ("keepfit-catalogue-second-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $firstOutput, $secondOutput | Out-Null

try {
    & $importer -SourceDirectory $SourceDirectory -OutputDirectory $firstOutput
    & $importer -SourceDirectory $SourceDirectory -OutputDirectory $secondOutput

    $expectedFiles = @(
        'exercises-v1.json',
        'catalogue-audit.json',
        'UPSTREAM_LICENSE.txt',
        'UPSTREAM_NOTICE.md',
        'upstream-exercises.schema.json'
    )
    foreach ($file in $expectedFiles) {
        $firstPath = Join-Path $firstOutput $file
        $secondPath = Join-Path $secondOutput $file
        if (-not (Test-Path -LiteralPath $firstPath -PathType Leaf)) {
            throw "Expected output missing: $file"
        }
        $firstHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $firstPath).Hash
        $secondHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $secondPath).Hash
        if ($firstHash -ne $secondHash) {
            throw "Importer output is not deterministic: $file"
        }
    }

    $catalogue = Get-Content -Raw -LiteralPath (Join-Path $firstOutput 'exercises-v1.json') | ConvertFrom-Json
    if ($catalogue.Count -ne 1316) {
        throw "Expected 1316 accepted exercises, found $($catalogue.Count)."
    }
    if (($catalogue.id | Sort-Object -Unique).Count -ne $catalogue.Count) {
        throw 'Stable Keepfit UUIDs are not unique.'
    }
    if (($catalogue.sourceId | Sort-Object -Unique).Count -ne $catalogue.Count) {
        throw 'Accepted source identifiers are not unique.'
    }
    foreach ($exercise in $catalogue) {
        if ($exercise.id -notmatch '^[0-9a-f]{8}-[0-9a-f]{4}-5[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$') {
            throw "Invalid stable UUID for source $($exercise.sourceId): $($exercise.id)"
        }
        if ($exercise.source -ne 'hasaneyldrm/exercises-dataset') {
            throw "Missing provenance for source $($exercise.sourceId)."
        }
        if ([string]::IsNullOrWhiteSpace($exercise.name) -or
            [string]::IsNullOrWhiteSpace($exercise.bodyPart) -or
            [string]::IsNullOrWhiteSpace($exercise.targetMuscle) -or
            [string]::IsNullOrWhiteSpace($exercise.equipment) -or
            $exercise.instructions.Count -eq 0) {
            throw "Incomplete normalized record for source $($exercise.sourceId)."
        }
    }

    $rawCatalogue = Get-Content -Raw -LiteralPath (Join-Path $firstOutput 'exercises-v1.json')
    foreach ($forbiddenField in @('"gif_url"', '"image"', '"media_id"', 'gymvisual')) {
        if ($rawCatalogue.Contains($forbiddenField, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Distributable catalogue contains forbidden media data: $forbiddenField"
        }
    }

    $audit = Get-Content -Raw -LiteralPath (Join-Path $firstOutput 'catalogue-audit.json') | ConvertFrom-Json
    if ($audit.sourceRecordCount -ne 1324 -or
        $audit.acceptedRecordCount -ne 1316 -or
        $audit.duplicateGroupCount -ne 8 -or
        $audit.rejectedRecordCount -ne 0 -or
        -not $audit.schemaValid) {
        throw 'Audit totals do not match the approved pinned-source review.'
    }

    Write-Output 'Catalogue importer verification passed.'
} finally {
    if (Test-Path -LiteralPath $firstOutput) {
        Remove-Item -LiteralPath $firstOutput -Recurse -Force
    }
    if (Test-Path -LiteralPath $secondOutput) {
        Remove-Item -LiteralPath $secondOutput -Recurse -Force
    }
}
