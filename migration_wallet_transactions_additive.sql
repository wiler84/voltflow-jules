-- Migration: wallet_transactions alignment (additive + backfill)
-- Purpose: Add missing columns (kind, method_label, occurred_at) to wallet_transactions
-- Safety: No DROP, no destructive changes. Full row preservation with smart backfill.
-- Status: Additive-only pass. Constraint tightening deferred to phase 2 if all reads verify.

BEGIN;

-- Step 1: Add missing columns if they don't exist
-- These will be NULL initially and backfilled in step 2.
ALTER TABLE wallet_transactions
ADD COLUMN IF NOT EXISTS kind TEXT,
ADD COLUMN IF NOT EXISTS method_label TEXT,
ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMPTZ;

-- Step 2: Backfill new columns with safe defaults
-- kind: Use "payment" for all existing rows (generic safe default)
UPDATE wallet_transactions
SET kind = COALESCE(kind, 'payment')
WHERE kind IS NULL;

-- method_label: Use "Legacy wallet entry" for all existing rows
UPDATE wallet_transactions
SET method_label = COALESCE(method_label, 'Legacy wallet entry')
WHERE method_label IS NULL;

-- occurred_at: Backfill from created_at or use now()
UPDATE wallet_transactions
SET occurred_at = COALESCE(occurred_at, created_at, timezone('utc', now()))
WHERE occurred_at IS NULL;

-- Step 3: Add NOT NULL constraints ONLY after backfill
-- (These are deferred to phase 2 after verification)
-- ALTER TABLE wallet_transactions
-- ALTER COLUMN kind SET NOT NULL,
-- ALTER COLUMN method_label SET NOT NULL,
-- ALTER COLUMN occurred_at SET NOT NULL;

-- Step 4: Ensure wallet transaction index exists for app reads
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_occurred_at
ON wallet_transactions (user_id, occurred_at DESC NULLS LAST);

-- Step 5: Verify schema alignment with app contract
-- Expected columns: id, user_id, kind, amount, method_label, occurred_at, created_at
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'wallet_transactions'
-- ORDER BY ordinal_position;

COMMIT;
