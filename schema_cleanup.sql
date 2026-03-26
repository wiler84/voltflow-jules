-- Optional cleanup: drop public tables not required by Voltflow.
-- Review carefully before running. This does NOT touch non-public schemas.
-- It also skips extension-owned tables.

do $$
declare
    r record;
    keep_tables text[] := array[
        'profiles',
        'wallets',
        'usage',
        'payment_methods',
        'payments',
        'transactions',
        'notifications',
        'autopay_settings',
        'billing_accounts',
        'bills',
        'wallet_transactions',
        'usage_metrics',
        'security_settings',
        'connected_devices',
        'analytics_events',
        'email_outbox'
    ];
begin
    for r in
        select n.nspname as schema_name, c.relname as table_name
        from pg_class c
        join pg_namespace n on n.oid = c.relnamespace
        left join pg_depend d on d.objid = c.oid and d.deptype = 'e'
        where c.relkind = 'r'
          and n.nspname = 'public'
          and d.objid is null
          and not (c.relname = any(keep_tables))
    loop
        execute format('drop table if exists %I.%I cascade', r.schema_name, r.table_name);
    end loop;
end $$;
