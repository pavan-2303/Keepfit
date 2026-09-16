param(
    [Parameter(Mandatory = $true)]
    [string]$SourceDirectory,

    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$expectedRevision = '7455efae41b330c265e7cd4b78dfa848e7ce5ebd'
$sourceName = 'hasaneyldrm/exercises-dataset'
$sourceJsonPath = Join-Path $SourceDirectory 'data\exercises.json'
$sourceSchemaPath = Join-Path $SourceDirectory 'data\exercises.schema.json'
$sourceLicensePath = Join-Path $SourceDirectory 'LICENSE'
$sourceNoticePath = Join-Path $SourceDirectory 'NOTICE.md'

function Assert-SourceFile([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Pinned source file missing: $Path"
    }
}

function Normalize-Whitespace([string]$Value) {
    return ($Value -replace '\s+', ' ').Trim()
}

function Normalize-NameKey([string]$Value) {
    $normalized = (Normalize-Whitespace $Value).ToLowerInvariant()
    return (($normalized -replace '[^a-z0-9]+', ' ') -replace '\s+', ' ').Trim()
}

function Format-DisplayName([string]$Value) {
    $normalized = Normalize-Whitespace $Value
    if ($normalized.Length -eq 0 -or -not [char]::IsLetter($normalized[0])) {
        return $normalized
    }
    return $normalized.Substring(0, 1).ToUpperInvariant() + $normalized.Substring(1)
}

function ConvertTo-StableUuid([string]$SourceId) {
    $seed = "$sourceName`:$SourceId"
    $bytes = [Text.Encoding]::UTF8.GetBytes($seed)
    $hash = [Security.Cryptography.SHA256]::HashData($bytes)
    $hex = [Convert]::ToHexString($hash).ToLowerInvariant().Substring(0, 32).ToCharArray()
    $hex[12] = '5'
    $variant = [Convert]::ToInt32($hex[16].ToString(), 16) -band 3
    $hex[16] = '89ab'[$variant]
    $compact = -join $hex
    return @(
        $compact.Substring(0, 8)
        $compact.Substring(8, 4)
        $compact.Substring(12, 4)
        $compact.Substring(16, 4)
        $compact.Substring(20, 12)
    ) -join '-'
}

function Write-DeterministicText([string]$Path, [string]$Content) {
    $normalized = $Content -replace "`r`n", "`n"
    if (-not $normalized.EndsWith("`n")) {
        $normalized += "`n"
    }
    [IO.File]::WriteAllText($Path, $normalized, [Text.UTF8Encoding]::new($false))
}

foreach ($requiredPath in @($sourceJsonPath, $sourceSchemaPath, $sourceLicensePath, $sourceNoticePath)) {
    Assert-SourceFile $requiredPath
}

$actualRevision = (git -C $SourceDirectory rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $actualRevision -ne $expectedRevision) {
    throw "Expected source revision $expectedRevision, found $actualRevision."
}

$sourceJson = Get-Content -Raw -LiteralPath $sourceJsonPath
$sourceSchema = Get-Content -Raw -LiteralPath $sourceSchemaPath
$schemaValid = $sourceJson | Test-Json -Schema $sourceSchema
if (-not $schemaValid) {
    throw 'Pinned exercises.json does not validate against exercises.schema.json.'
}

$sourceRecords = $sourceJson | ConvertFrom-Json
$sourceIds = $sourceRecords.id | Sort-Object -Unique
if ($sourceIds.Count -ne $sourceRecords.Count) {
    throw 'Pinned source identifiers are not unique.'
}

$duplicateGroups = $sourceRecords |
    Group-Object { Normalize-NameKey $_.name } |
    Where-Object Count -gt 1 |
    Sort-Object Name
$duplicateDecisions = @()
$acceptedSourceRecords = @()
foreach ($group in ($sourceRecords | Group-Object { Normalize-NameKey $_.name } | Sort-Object Name)) {
    $ordered = @($group.Group | Sort-Object { [int]$_.id })
    $acceptedSourceRecords += $ordered[0]
    if ($ordered.Count -gt 1) {
        $duplicateDecisions += [ordered]@{
            normalizedName = $group.Name
            keptSourceId = $ordered[0].id
            removedSourceIds = [string[]]@($ordered | Select-Object -Skip 1 | ForEach-Object id)
        }
    }
}

$catalogue = @($acceptedSourceRecords | ForEach-Object {
    $record = $_
    $secondaryMuscles = @($record.secondary_muscles |
        ForEach-Object { (Normalize-Whitespace $_).ToLowerInvariant() } |
        Where-Object { $_.Length -gt 0 } |
        Sort-Object -Unique)
    $instructions = @($record.instruction_steps.en |
        ForEach-Object { Normalize-Whitespace $_ } |
        Where-Object { $_.Length -gt 0 })
    [ordered]@{
        id = ConvertTo-StableUuid $record.id
        source = $sourceName
        sourceRevision = $expectedRevision
        sourceId = $record.id
        name = Format-DisplayName $record.name
        bodyPart = (Normalize-Whitespace $record.body_part).ToLowerInvariant()
        targetMuscle = (Normalize-Whitespace $record.target).ToLowerInvariant()
        muscleGroup = (Normalize-Whitespace $record.muscle_group).ToLowerInvariant()
        secondaryMuscles = [string[]]$secondaryMuscles
        equipment = (Normalize-Whitespace $record.equipment).ToLowerInvariant()
        instructions = [string[]]$instructions
        isBodyweight = (Normalize-Whitespace $record.equipment).Equals('body weight', [StringComparison]::OrdinalIgnoreCase)
    }
} | Sort-Object name, sourceId)

if ($catalogue.Count -ne 1316) {
    throw "Expected 1316 accepted records after duplicate resolution, found $($catalogue.Count) from $($acceptedSourceRecords.Count) accepted source rows."
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$cataloguePath = Join-Path $OutputDirectory 'exercises-v1.json'
$catalogueJson = $catalogue | ConvertTo-Json -Depth 6 -Compress
Write-DeterministicText $cataloguePath $catalogueJson

$audit = [ordered]@{
    source = $sourceName
    sourceRevision = $expectedRevision
    sourceRecordCount = $sourceRecords.Count
    acceptedRecordCount = $catalogue.Count
    rejectedRecordCount = 0
    duplicateGroupCount = $duplicateGroups.Count
    duplicateDecisions = $duplicateDecisions
    schemaValid = $schemaValid
    sourceHashes = [ordered]@{
        exercisesJsonSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceJsonPath).Hash.ToLowerInvariant()
        exercisesSchemaSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceSchemaPath).Hash.ToLowerInvariant()
        licenseSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceLicensePath).Hash.ToLowerInvariant()
        noticeSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceNoticePath).Hash.ToLowerInvariant()
    }
    outputSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $cataloguePath).Hash.ToLowerInvariant()
    excludedFields = @('media_id', 'image', 'gif_url', 'attribution', 'created_at', 'non-English instructions')
}
Write-DeterministicText (Join-Path $OutputDirectory 'catalogue-audit.json') ($audit | ConvertTo-Json -Depth 8)
Write-DeterministicText (Join-Path $OutputDirectory 'UPSTREAM_LICENSE.txt') (Get-Content -Raw -LiteralPath $sourceLicensePath)
Write-DeterministicText (Join-Path $OutputDirectory 'UPSTREAM_NOTICE.md') (Get-Content -Raw -LiteralPath $sourceNoticePath)
Write-DeterministicText (Join-Path $OutputDirectory 'upstream-exercises.schema.json') $sourceSchema

Write-Output "Imported $($catalogue.Count) exercises from $expectedRevision."
