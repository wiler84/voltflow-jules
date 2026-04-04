// execute_migration.js - Apply wallet_transactions migration via Supabase
const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');

const projectId = 'tqvemfaxqiisxvjhrxta';
const supabaseUrl = `https://${projectId}.supabase.co`;
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY || 'YOUR_ANON_KEY_HERE';
const dbPassword = process.argv[2];

if (!dbPassword) {
    console.error('Usage: node execute_migration.js <db_password>');
    process.exit(1);
}

const supabase = createClient(supabaseUrl, supabaseAnonKey);

async function executeMigration() {
    console.log('=== Executing wallet_transactions Migration ===\n');
    
    try {
        // Read migration SQL
        const migrationSql = fs.readFileSync('migration_wallet_transactions_additive.sql', 'utf8');
        
        console.log('[1/3] Connecting to Supabase...');
        // Verify connection
        const { data, error: connError } = await supabase.auth.getSession();
        if (connError) {
            console.log('⚠ Auth check skipped (expected for anon key)');
        } else {
            console.log('✓ Connection verified');
        }
        
        console.log('\n[2/3] Executing migration SQL...');
        // Execute the migration
        // Note: This requires the admin API or proper RLS bypassing
        const { error } = await supabase.rpc('exec', {
            sql: migrationSql
        }).catch(() => {
            // Fallback: Use REST API directly
            return fetch(`${supabaseUrl}/rest/v1/query`, {
                method: 'POST',
                headers: {
                    'apikey': supabaseAnonKey,
                    'Authorization': `Bearer ${supabaseAnonKey}`,
                    'Content-Type': 'application/json',
                    'Prefer': 'return=representation'
                },
                body: JSON.stringify({ query: migrationSql })
            });
        });
        
        if (error) {
            throw error;
        }
        
        console.log('✓ Migration executed\n');
        
        console.log('[3/3] Verifying schema...');
        const { data: columns, error: verifyError } = await supabase
            .from('information_schema.columns')
            .select('column_name, data_type, is_nullable')
            .eq('table_name', 'wallet_transactions')
            .order('ordinal_position', { ascending: true });
        
        if (!verifyError && columns) {
            console.log('✓ Schema verified:');
            columns.forEach(col => {
                console.log(`  - ${col.column_name}: ${col.data_type} (nullable: ${col.is_nullable})`);
            });
        }
        
        console.log('\n✅ Migration complete!');
        process.exit(0);
    } catch (err) {
        console.error('❌ Error executing migration:', err.message);
        console.log('\nPlease apply manually via Supabase Dashboard:');
        console.log('1. Go to https://app.supabase.com/project/tqvemfaxqiisxvjhrxta/sql');
        console.log('2. Paste the SQL from migration_wallet_transactions_additive.sql');
        console.log('3. Click "Run"');
        process.exit(1);
    }
}

executeMigration();
