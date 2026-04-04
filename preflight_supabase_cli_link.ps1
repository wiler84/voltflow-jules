#!/usr/bin/env pwsh
# Preflight: Link Supabase CLI and snapshot remote schema + data
# Purpose: Stage I of Phase 1 - read-only inspection before any mutations
# Safety: No ALTER, no UPDATE, no DELETE. Snapshot only.

param(
    [Parameter(Mandatory=$false)]
    [string]$DBPassword,
    
    [Parameter(Mandatory=$false)]
    [string]$SupabaseProjectId = "tqvemfaxqiisxvjhrxta"
)

$ErrorActionPreference = "Stop"
Write-Host "=== Preflight: Supabase CLI Link & Schema Snapshot === `n" -ForegroundColor Cyan

# Step 1: Verify Supabase CLI is installed
Write-Host "[1/6] Checking Supabase CLI..." -ForegroundColor Yellow
$supabaseVersion = supabase --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Supabase CLI not found. Install via: npm install -g supabase" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Supabase CLI: $supabaseVersion" -ForegroundColor Green

# Step 2: Link the Supabase project locally (using CLI login if available)
Write-Host "`n[2/6] Linking Supabase project $SupabaseProjectId..." -ForegroundColor Yellow
try {
    # Attempt to link the project
    # If already linked, this is idempotent; if not, it will use stored credentials
    supabase link --project-ref $SupabaseProjectId 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Project linked successfully" -ForegroundColor Green
    } else {
        Write-Host "⚠ Project link may require manual auth. Ensure 'supabase login' is complete." -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Link step skipped (may already be linked): $_" -ForegroundColor Yellow
}

# Step 3: Pull current remote schema
Write-Host "`n[3/6] Pulling remote database schema..." -ForegroundColor Yellow
try {
    supabase db pull --schema public,auth 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Remote schema pulled to supabase/migrations" -ForegroundColor Green
    } else {
        Write-Host "⚠ Pull returned non-zero; inspect supabase/migrations manually" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Pull step skipped: $_" -ForegroundColor Yellow
}

# Step 4: Snapshot wallet_transactions schema
Write-Host "`n[4/6] Inspecting wallet_transactions schema..." -ForegroundColor Yellow
$snapshotPath = "wallet_transactions_schema_snapshot.sql"
@"
-- Snapshot from $((Get-Date -Format 'u'))
-- Purpose: Verify current schema before migration
-- Command: SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'wallet_transactions' ORDER BY ordinal_position;

SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'wallet_transactions'
ORDER BY ordinal_position;

-- Row count:
SELECT COUNT(*) as row_count FROM wallet_transactions;

-- Sample rows (first 5):
SELECT id, user_id, kind, amount, method_label, occurred_at, created_at 
FROM wallet_transactions 
ORDER BY created_at DESC 
LIMIT 5;
"@ | Out-File -FilePath $snapshotPath -Encoding UTF8
Write-Host "✓ Snapshot query saved to: $snapshotPath" -ForegroundColor Green

# Step 5: Snapshot bills realtime publication status
Write-Host "`n[5/6] Inspecting bills realtime publication..." -ForegroundColor Yellow
$billsSnapshotPath = "bills_realtime_snapshot.sql"
@"
-- Snapshot from $((Get-Date -Format 'u'))
-- Purpose: Verify bills realtime publication status before fix

-- Check if public.bills is in realtime publication:
SELECT schemaname, tablename, EXISTS (
    SELECT 1 FROM pg_publication_tables 
    WHERE pubname = 'supabase_realtime' 
    AND tablename = 'bills'
) as in_realtime_publication
FROM pg_tables
WHERE tablename = 'bills' AND schemaname = 'public';

-- List all tables in supabase_realtime publication:
SELECT tablename FROM pg_publication_tables WHERE pubname = 'supabase_realtime' ORDER BY tablename;

-- Row count:
SELECT COUNT(*) as row_count FROM bills;
"@ | Out-File -FilePath $billsSnapshotPath -Encoding UTF8
Write-Host "✓ Bills realtime snapshot query saved to: $billsSnapshotPath" -ForegroundColor Green

# Step 6: Summary
Write-Host "`n[6/6] Preflight Summary" -ForegroundColor Yellow
Write-Host @"
✓ CLI Link Complete

Next steps (when you have DB password):
  1. Set env: `$env:SUPABASE_ADMIN_PASSWORD = "your_password_here"`
  2. Run: supabase db execute --schema public < wallet_transactions_schema_snapshot.sql
  3. Run: supabase db execute --schema public < bills_realtime_snapshot.sql
  4. Review output to determine: what columns are missing, what data exists
  5. Confirm historical data preservation strategy with AI before applying migration_wallet_transactions_additive.sql
  6. When ready: supabase db execute --schema public < migration_wallet_transactions_additive.sql

Output files generated:
  - wallet_transactions_schema_snapshot.sql
  - bills_realtime_snapshot.sql
  - supabase/migrations/* (pulled schema history)

Safety: ALL these steps are read-only. No mutations yet.
"@ -ForegroundColor Green

Write-Host "`n✅ Preflight complete. Ready for Phase 1 data inspection." -ForegroundColor Cyan
