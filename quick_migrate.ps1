#!/usr/bin/env pwsh
# Direct migration execution via Supabase CLI
# This is a simplified version that doesn't require psql

param(
    [Parameter(Mandatory=$true)]
    [string]$DBPassword,
    [Parameter(Mandatory=$false)]
    [string]$ProjectId = "tqvemfaxqiisxvjhrxta"
)

$ErrorActionPreference = "Stop"
Write-Host "=== Applying wallet_transactions Migration ===" -ForegroundColor Cyan

# Link project
Write-Host "`n[1/3] Linking Supabase project..." -ForegroundColor Yellow
supabase link --project-ref $ProjectId 2>&1
Write-Host "✓ Project linked" -ForegroundColor Green

# Push migration via Supabase CLI
Write-Host "`n[2/3] Creating migration in Supabase..." -ForegroundColor Yellow

# Read the migration SQL
$migrationSql = Get-Content "migration_wallet_transactions_additive.sql" -Raw

# Create a temporary migration file in supabase/migrations
$migrationDir = "supabase/migrations"
if (-not (Test-Path $migrationDir)) {
    New-Item -ItemType Directory -Path $migrationDir -Force | Out-Null
}

# Generate timestamp for migration name
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$migrationFile = Join-Path $migrationDir "$timestamp`_wallet_transactions_schema_alignment.sql"

# Copy the migration SQL
Copy-Item "migration_wallet_transactions_additive.sql" -Destination $migrationFile
Write-Host "✓ Migration prepared: $migrationFile" -ForegroundColor Green

# Push migration
Write-Host "`n[3/3] Executing migration on remote database..." -ForegroundColor Yellow
try {
    # Use supabase db push to apply the migration
    $env:SUPABASE_DB_PASSWORD = $DBPassword
    supabase db push --dry-run 2>&1 | Out-String | Write-Host
    
    $response = Read-Host "Apply migration? (yes/no)"
    if ($response -eq "yes") {
        supabase db push 2>&1 | Out-String | Write-Host
        Write-Host "`n✅ Migration applied successfully!" -ForegroundColor Green
    } else {
        Write-Host "Migration cancelled." -ForegroundColor Yellow
    }
    
    Remove-Item env:SUPABASE_DB_PASSWORD -ErrorAction SilentlyContinue
} catch {
    Write-Host "⚠ CLI migration failed. Trying alternative method..." -ForegroundColor Yellow
    Remove-Item env:SUPABASE_DB_PASSWORD -ErrorAction SilentlyContinue
    
    # Alternative: Use http client to Supabase SQL editor via API
    Write-Host "Please apply this migration manually via Supabase Dashboard:" -ForegroundColor Yellow
    Write-Host $migrationSql -ForegroundColor Gray
}

Write-Host "`n=== Migration Complete ===" -ForegroundColor Cyan
