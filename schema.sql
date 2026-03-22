create extension if not exists pgcrypto;

create table if not exists profiles (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique,
    first_name text not null default '',
    last_name text not null default '',
    email text not null default '',
    phone text,
    account_status text not null default 'Pending',
    created_at timestamptz not null default timezone('utc', now()),
    updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists wallets (
    user_id uuid primary key,
    balance numeric not null default 0,
    updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists usage (
    user_id uuid primary key,
    total_spent numeric not null default 0,
    electricity_spent numeric not null default 0,
    water_spent numeric not null default 0,
    gas_spent numeric not null default 0,
    monthly_usage numeric not null default 0,
    updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists payment_methods (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    card_last4 text not null,
    card_brand text not null,
    expiry_month int not null,
    expiry_year int not null,
    is_default boolean not null default false,
    created_at timestamptz not null default timezone('utc', now())
);

create table if not exists payments (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    amount numeric not null,
    currency text not null default 'USD',
    status text not null,
    processor text not null,
    processor_reference text not null,
    payment_method_id uuid,
    source text not null default 'wallet',
    idempotency_key text not null unique,
    client_reference text not null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default timezone('utc', now())
);

create table if not exists transactions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    kind text not null,
    utility_type text not null,
    amount numeric not null,
    currency text not null default 'USD',
    status text not null,
    payment_method text not null,
    payment_method_id uuid,
    processor_reference text not null default '',
    description text not null,
    client_reference text not null unique,
    metadata jsonb not null default '{}'::jsonb,
    occurred_at timestamptz not null default timezone('utc', now()),
    created_at timestamptz not null default timezone('utc', now())
);

create table if not exists notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    title text not null,
    body text not null,
    type text not null,
    is_read boolean not null default false,
    read_at timestamptz,
    action_url text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default timezone('utc', now())
);

create table if not exists autopay_settings (
    user_id uuid primary key,
    enabled boolean not null default false,
    payment_method_id uuid,
    amount_limit numeric not null default 0,
    billing_cycle text not null default 'monthly',
    billing_day int,
    timezone text default 'UTC',
    next_run_at timestamptz,
    last_run_at timestamptz,
    utility_types text[] not null default '{}'::text[],
    status text not null default 'active',
    updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists security_settings (
    user_id uuid primary key,
    biometric_enabled boolean not null default false,
    mfa_enabled boolean not null default false,
    pin_enabled boolean not null default false,
    auto_lock_minutes int not null default 1,
    updated_at timestamptz not null default timezone('utc', now())
);

create table if not exists connected_devices (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    device_id text not null,
    device_name text not null,
    platform text not null,
    last_active timestamptz not null default timezone('utc', now()),
    location text,
    session_token text not null,
    unique (user_id, device_id)
);

create table if not exists analytics_events (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    event_name text not null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default timezone('utc', now())
);

create table if not exists email_outbox (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    to_email text not null,
    subject text not null,
    body text not null,
    status text not null default 'queued',
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default timezone('utc', now())
);

create index if not exists idx_transactions_user_created_at on transactions (user_id, created_at desc);
create index if not exists idx_notifications_user_created_at on notifications (user_id, created_at desc);
create index if not exists idx_payment_methods_user_default on payment_methods (user_id, is_default);
create index if not exists idx_connected_devices_user_last_active on connected_devices (user_id, last_active desc);
create index if not exists idx_payments_user_created_at on payments (user_id, created_at desc);

alter table profiles enable row level security;
alter table wallets enable row level security;
alter table usage enable row level security;
alter table payment_methods enable row level security;
alter table payments enable row level security;
alter table transactions enable row level security;
alter table notifications enable row level security;
alter table autopay_settings enable row level security;
alter table security_settings enable row level security;
alter table connected_devices enable row level security;
alter table analytics_events enable row level security;
alter table email_outbox enable row level security;

drop policy if exists "profiles owner" on profiles;
create policy "profiles owner" on profiles for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "wallets owner" on wallets;
create policy "wallets owner" on wallets for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "usage owner" on usage;
create policy "usage owner" on usage for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "payment methods owner" on payment_methods;
create policy "payment methods owner" on payment_methods for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "payments owner" on payments;
create policy "payments owner" on payments for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "transactions owner" on transactions;
create policy "transactions owner" on transactions for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "notifications owner" on notifications;
create policy "notifications owner" on notifications for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "autopay owner" on autopay_settings;
create policy "autopay owner" on autopay_settings for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "security owner" on security_settings;
create policy "security owner" on security_settings for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "devices owner" on connected_devices;
create policy "devices owner" on connected_devices for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "analytics owner" on analytics_events;
create policy "analytics owner" on analytics_events for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "email outbox owner" on email_outbox;
create policy "email outbox owner" on email_outbox for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

alter table transactions add column if not exists currency text not null default 'USD';
alter table transactions add column if not exists payment_method_id uuid;
alter table transactions add column if not exists processor_reference text not null default '';
alter table transactions add column if not exists metadata jsonb not null default '{}'::jsonb;
alter table transactions add column if not exists occurred_at timestamptz not null default timezone('utc', now());
alter table notifications add column if not exists read_at timestamptz;
alter table notifications add column if not exists action_url text;
alter table notifications add column if not exists metadata jsonb not null default '{}'::jsonb;
alter table autopay_settings add column if not exists billing_day int;
alter table autopay_settings add column if not exists timezone text default 'UTC';
alter table autopay_settings add column if not exists next_run_at timestamptz;
alter table autopay_settings add column if not exists last_run_at timestamptz;
alter table autopay_settings add column if not exists utility_types text[] not null default '{}'::text[];
alter table autopay_settings add column if not exists status text not null default 'active';
alter table profiles add column if not exists account_status text not null default 'Pending';
alter table security_settings add column if not exists biometric_enabled boolean not null default false;
alter table security_settings add column if not exists mfa_enabled boolean not null default false;
alter table security_settings add column if not exists pin_enabled boolean not null default false;
alter table security_settings add column if not exists auto_lock_minutes int not null default 1;
