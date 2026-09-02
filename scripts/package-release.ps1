param(
    [Parameter(Mandatory = $true)]
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)
if ($OutputPath.StartsWith($RepoRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Release archive must be written outside the repository.'
}
if (Test-Path -LiteralPath $OutputPath) {
    throw "Refusing to overwrite existing archive: $OutputPath"
}

[System.IO.Directory]::CreateDirectory((Split-Path -Parent $OutputPath)) | Out-Null
$Stage = Join-Path ([System.IO.Path]::GetTempPath()) ("jerseysee-release-" + [System.Guid]::NewGuid())
$PackageRoot = Join-Path $Stage 'JerseySee-Complete'
[System.IO.Directory]::CreateDirectory($PackageRoot) | Out-Null

try {
    $ExcludedRoots = @('.git', '.idea', '.superpowers', 'target', 'uploads', 'demo-uploads', 'demo-data', 'logs', 'secrets')
    $ExcludedExact = @('.env', 'application-local.properties', 'application-local.yml', 'application-local.yaml')
    Get-ChildItem -LiteralPath $RepoRoot -Recurse -Force -File | ForEach-Object {
        $Relative = $_.FullName.Substring($RepoRoot.Length).TrimStart('\', '/')
        $FirstSegment = ($Relative -split '[\\/]')[0]
        if ($ExcludedRoots -contains $FirstSegment -or $ExcludedExact -contains $Relative) { return }
        if ($Relative -ne '.env.example' -and $Relative -like '.env.*') { return }
        if ($Relative -match '\.(zip|h2\.db|trace\.db|log|pem|key)$') { return }

        $Destination = Join-Path $PackageRoot $Relative
        [System.IO.Directory]::CreateDirectory((Split-Path -Parent $Destination)) | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $Destination
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::CreateFromDirectory($PackageRoot, $OutputPath,
        [System.IO.Compression.CompressionLevel]::Optimal, $true)
    Write-Output "Created $OutputPath"
}
finally {
    if (Test-Path -LiteralPath $Stage) {
        Remove-Item -LiteralPath $Stage -Recurse -Force
    }
}
