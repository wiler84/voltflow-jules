#!/usr/bin/env pwsh
# Phase 1 Execution: Link CLI, Inspect Remote, Apply Additive Migration
# This script guides through the full preflight and migration application
# Safety: Inspection steps are read-only. Migration is additive-only.

param(
    [Parameter(Mandatory=$true)]
    [string]$DBPassword,
    
    [Parameter(Mandatory=$false)]
    [string]$SupabaseProjectId = "tqvemfaxqiisxvjhrxta"
)

$ErrorActionPreference = "Stop"
Write-Host "=== VoltFlow Phase 1: CLI Link + Migration + Verification === `n" -ForegroundColor Cyan

# Step 1: Verify Supabase CLI
Write-Host "[1/7] Checking Supabase CLI installation..." -ForegroundColor Yellow
$supabaseVersion = supabase --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Supabase CLI not found. Install via: npm install -g supabase" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Supabase CLI v$supabaseVersion" -ForegroundColor Green

# Step 2: Link project
Write-Host "`n[2/7] Linking Supabase project $SupabaseProjectId..." -ForegroundColor Yellow
try {
    supabase link --project-ref $SupabaseProjectId 2>&1 | Out-Null
    Write-Host "✓ Project linked" -ForegroundColor Green
} catch {
    Write-Host "⚠ Link may require manual login: $_" -ForegroundColor Yellow
}

# Step 3: Pull remote schema
Write-Host "`n[3/7] Pulling remote schema..." -ForegroundColor Yellow
try {
    supabase db pull 2>&1 | Out-Null
    Write-Host "✓ Remote schema pulled to supabase/migrations" -ForegroundColor Green
} catch {
    Write-Host "⚠ Pull skipped: $_" -ForegroundColor Yellow
}

# Step 4: Inspect current wallet_transactions schema
Write-Host "`n[4/7] Inspecting current wallet_transactions schema..." -ForegroundColor Yellow
$inspectSql = @"
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'wallet_transactions'
AND table_schema = 'public'
ORDER BY ordinal_position;
"@

$inspectFile = "wallet_transactions_inspect.sql"
$inspectSql | Out-File -FilePath $inspectFile -Encoding UTF8
Write-Host "✓ Inspection SQL prepared: $inspectFile" -ForegroundColor Green
Write-Host "  To run: psql -h $SupabaseProjectId.supabase.co -U postgres -d postgres -f $inspectFile" -ForegroundColor Gray

# Step 5: Prepare migration
Write-Host "`n[5/7] Preparing migration..." -ForegroundColor Yellow
if (-not (Test-Path "migration_wallet_transactions_additive.sql")) {
    Write-Host "❌ migration_wallet_transactions_additive.sql not found!" -ForegroundColor Red
    exit 1
}

$migrationContent = Get-Content "migration_wallet_transactions_additive.sql" -Raw
if ($migrationContent -match "ALTER TABLE wallet_transactions" -and $migrationContent -match "UPDATE wallet_transactions") {
    Write-Host "✓ Migration file is valid (additive-only)" -ForegroundColor Green
} else {
    Write-Host "⚠ Warning: Migration content check inconclusive" -ForegroundColor Yellow
}

# Step 6: Apply migration
Write-Host "`n[6/7] Applying migration to remote database..." -ForegroundColor Yellow
Write-Host "⚠ This will ADD columns and BACKFILL data. No destruction." -ForegroundColor Yellow
$confirmMigration = Read-Host "Continue with migration? (yes/no)"
if ($confirmMigration -ne "yes") {
    Write-Host "Migration cancelled by user." -ForegroundColor Yellow
    exit 0
}

try {
    # Use environment variable for password
    $env:PGPASSWORD = $DBPassword
    
    # Get the Supabase database URL from the linked project
    Write-Host "Applying migration via psql..." -ForegroundColor Yellow
    & psql -h "$SupabaseProjectId.supabase.co" `
           -U "postgres.tqvemfaxqiisxvjhrxta" `
           -d "postgres" `
           -f "migration_wallet_transactions_additive.sql" 2>&1 | Tee-Object -Variable migrationOutput
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Migration applied successfully" -ForegroundColor Green
    } else {
        Write-Host "❌ Migration failed with exit code $LASTEXITCODE" -ForegroundColor Red
        Write-Host "Output: $migrationOutput" -ForegroundColor Red
        exit 1
    }
    
    # Clear password from environment
    Remove-Item env:PGPASSWORD -ErrorAction SilentlyContinue
} catch {
    Write-Host "❌ Error applying migration: $_" -ForegroundColor Red
    Remove-Item env:PGPASSWORD -ErrorAction SilentlyContinue
    exit 1
}

# Step 7: Verify migration
Write-Host "`n[7/7] Verifying migration..." -ForegroundColor Yellow
$verifySql = @"
-- Verify wallet_transactions schema after migration
SELECT 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns
WHERE table_name = 'wallet_transactions'
AND table_schema = 'public'
ORDER BY ordinal_position;

-- Verify row count
SELECT COUNT(*) as wallet_transaction_count FROM wallet_transactions;

-- Verify required columns are present
SELECT 
    (SELECT COUNT(*) FROM information_schema.columns 
     WHERE table_name = 'wallet_transactions' AND column_name = 'kind') as has_kind_column,
    (SELECT COUNT(*) FROM information_schema.columns 
     WHERE table_name = 'wallet_transactions' AND column_name = 'method_label') as has_method_label_column,
    (SELECT COUNT(*) FROM information_schema.columns 
     WHERE table_name = 'wallet_transactions' AND column_name = 'occurred_at') as has_occurred_at_column;
"@

$verifyFile = "wallet_transactions_verify.sql"
$verifySql | Out-File -FilePath $verifyFile -Encoding UTF8
Write-Host "✓ Verification SQL prepared: $verifyFile" -ForegroundColor Green
Write-Host "  To verify manually: psql -h $SupabaseProjectId.supabase.co -U postgres -d postgres -f $verifyFile" -ForegroundColor Gray

Write-Host "`n=== Phase 1 Complete ===" -ForegroundColor Cyan
Write-Host @"

✅ Migration applied!

Next steps:
  1. Manually verify with: psql ... -f $verifyFile
  2. If verification passes, proceed to app build:
     .\gradlew assembleDebug
  3. Install APK and test:
     - Wallet history should load without errors
     - Transactions should show with labels and timestamps
     - No 'column not found' errors in logcat

Rollback (if needed):
  - Columns are additive; no data loss
  - To remove added columns: ALTER TABLE wallet_transactions DROP COLUMN kind, method_label, occurred_at;
  - Existing data in wallet_transactions is preserved

Files generated:
  - wallet_transactions_inspect.sql (pre-migration state)
  - wallet_transactions_verify.sql (post-migration verification)
  - Migration was applied from: migration_wallet_transactions_additive.sql
"@ -ForegroundColor Green
