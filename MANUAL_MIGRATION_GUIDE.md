# Manual Migration Application Guide

## Quick Summary
The wallet_transactions table needs 3 new columns. You'll apply this via the Supabase dashboard SQL editor (1-2 minutes).

## Step 1: Open Supabase Dashboard SQL Editor

1. Go to: https://app.supabase.com
2. Log in (if not already)
3. Select project: **Voltflow** (tqvemfaxqiisxvjhrxta)
4. Click "SQL Editor" in the left sidebar
5. Click "New Query"

## Step 2: Copy and Paste the Migration SQL

Replace any existing text in the editor with this exact SQL:

```sql
-- Migration: wallet_transactions alignment (additive + backfill)
BEGIN;

-- Step 1: Add missing columns if they don't exist
ALTER TABLE wallet_transactions
ADD COLUMN IF NOT EXISTS kind TEXT,
ADD COLUMN IF NOT EXISTS method_label TEXT,
ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMPTZ;

-- Step 2: Backfill new columns with safe defaults
UPDATE wallet_transactions
SET kind = COALESCE(kind, 'payment')
WHERE kind IS NULL;

UPDATE wallet_transactions
SET method_label = COALESCE(method_label, 'Legacy wallet entry')
WHERE method_label IS NULL;

UPDATE wallet_transactions
SET occurred_at = COALESCE(occurred_at, created_at, timezone('utc', now()))
WHERE occurred_at IS NULL;

-- Step 3: Create index for app reads
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_occurred_at
ON wallet_transactions (user_id, occurred_at DESC NULLS LAST);

-- Step 4: Verify columns exist
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'wallet_transactions'
ORDER BY ordinal_position;

COMMIT;
```

## Step 3: Execute the SQL

1. Click the **"Run"** button (or press Ctrl+Enter)
2. Watch for success message: `COMMIT` at the bottom
3. You should see the schema verification output showing all columns

## Step 4: Verify Success

Expected output (last query):
| column_name | data_type | is_nullable |
|---|---|---|
| id | uuid | NO |
| user_id | uuid | NO |
| kind | text | YES |
| amount | numeric | NO |
| method_label | text | YES |
| occurred_at | timestamp with time zone | YES |
| created_at | timestamp with time zone | NO |

If you see all 7 columns including `kind`, `method_label`, and `occurred_at`, **migration is complete!** ✅

---

## Troubleshooting

### Error: "table wallet_transactions does not exist"
- Verify you're in the correct Supabase project (Voltflow)
- Check that the project has the wallet features enabled

### Error: "column already exists"
- This is OK! The SQL uses `IF NOT EXISTS` to handle this safely
- The migration is idempotent, so running it twice is harmless

### Error: "permission denied"
- Your database user may lack alter table permissions
- Contact Supabase support or use a higher-privilege user

### Query runs but no result shown
- Sometimes the verification query may not return results in the UI
- This is OK; check the "Messages" tab for any errors
- If no errors, the migration succeeded

---

## After Migration: Build & Test the App

Once verified in Supabase, build the updated Android app:

```powershell
cd c:\Users\SCOFIELD\Desktop\code 2\voltflow check point one
.\gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then test:
- Launch the app
- Navigate to Wallet screen
- Verify no "column not found" errors
- Transactions should display with labels and timestamps

---

## Alternative: Command Line (if you have psql installed)

If psql is available on your machine:

```bash
# 1. Copy migration to temp file
cp migration_wallet_transactions_additive.sql /tmp/migrate.sql

# 2. Execute via psql
export PGPASSWORD="williyamino123"
psql -h tqvemfaxqiisxvjhrxta.supabase.co \
     -U postgres.tqvemfaxqiisxvjhrxta \
     -d postgres \
     -f /tmp/migrate.sql

# 3. Verify
psql -h tqvemfaxqiisxvjhrxta.supabase.co \
     -U postgres.tqvemfaxqiisxvjhrxta \
     -d postgres \
     -c "SELECT column_name FROM information_schema.columns WHERE table_name='wallet_transactions';"
```

---

**Status: Ready for manual application** ✅

The migration is safe (additive, reversible, no data loss). Apply via Supabase dashboard and we'll proceed to build verification.
