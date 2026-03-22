# VoltFlow UI + Backend Alignment Plan (Snapshot)

Last updated: 2026-03-20
Scope: Planning only. No code changes in this pass.

## 1) UI Parity Targets (from new screenshots)
- Home: hero card with utility provider, masked account, current balance, due date, usage progress bar, Pay Now pill, quick action tiles, floating blurred nav pill.
- Pay: amount card with preset chips, selected payment method list with outline highlight, wallet option, floating blurred nav pill.
- History: filter chip dropdown, grouped payment/bill entries, status color, floating blurred nav pill.
- Wallet: gradient balance card, Add Funds + Withdraw pills, wallet transaction list, floating blurred nav pill.
- Auto‑Pay: toggle header card, Payment Day dropdown, Payment Method row, “How it works” steps, floating blurred nav pill.
- Analytics: time range segmented chips, metric cards, spending trend chart, monthly breakdown list, floating blurred nav pill.
- Notifications: iconed cards, unread dot, relative timestamps, floating blurred nav pill.

UI behavior requirements
- Frosted floating bottom bar: blurred background layer, but icons and text remain crisp.
- Smooth transitions between tabs and stack screens.
- Haptic feedback on primary actions and toggles.
- Consistent glassmorphism cards, soft gradients, rounded corners, subtle borders.

## 2) Backend Data Alignment (Supabase)

### 2.1 Core tables already present
- profiles, wallets, usage, payment_methods, payments, transactions, notifications, autopay_settings, connected_devices, analytics_events, email_outbox, security_settings

### 2.2 New/extended data needed to match UI
Add or extend tables/fields:
- bills (new):
  - id, user_id, provider_name, account_masked, amount_due, due_date, status, utility_type, created_at
  - Purpose: Home hero card + Pay screen “current balance / due date”.
- payment_accounts (new):
  - id, user_id, provider_name, account_masked, utility_type, is_default
  - Purpose: Home “City Power & Light”, Pay screen subtitle.
- wallet_transactions (new):
  - id, user_id, kind (deposit/withdraw/utility_payment), amount, method_label, occurred_at, created_at
  - Purpose: Wallet history list.
- notification_assets (optional):
  - type, icon_name, accent_color
  - Purpose: map notification type → icon color.
- analytics_rollups (new or derived view):
  - user_id, period_start, period_end, total_spent, units_used, avg_monthly, breakdown jsonb
  - Purpose: Analytics screen without heavy client-side aggregation.

### 2.3 Realtime subscriptions
- notifications: new rows, read status changes.
- transactions: new payments and bill events.
- wallets: balance updates.
- bills: due date / status updates.

## 3) Screen‑by‑Screen Data Mapping

### Home
- Greeting: `profiles.first_name` + time buckets.
- Provider + account: `payment_accounts.default` (or `bills.latest` fallback).
- Current balance & due date: `bills.latest`.
- Usage progress: derived from `usage.monthly_usage` and bill amount (or `usage` + `bills`).
- Quick actions: link to Pay, Wallet, History, Autopay.

### Pay
- “Amount to pay”: `bills.latest.amount_due` default, override via chips.
- Preset chips: static amounts (50/100/150/200).
- Payment methods: `payment_methods` + `wallets`.
- On Pay: write `payments` + `transactions`, update `wallets` (if wallet), update `usage`, create `notifications`.

### History
- Data: `transactions` ordered by `occurred_at`.
- Filter chip: by `kind` or `utility_type`.
- Status text and color from `transactions.status`.

### Wallet
- Balance: `wallets.balance`.
- Add Funds: create `payments` + `transactions` (kind = wallet_funding) + update `wallets`.
- Withdraw: create `transactions` (kind = wallet_withdrawal) + update `wallets`.
- List: `wallet_transactions` (or derived `transactions`).

### Auto‑Pay
- Toggle + settings: `autopay_settings`.
- Payment day: `autopay_settings.billing_day`.
- Method: `autopay_settings.payment_method_id`.
- On save: update `autopay_settings`, create notification.
- Autopay execution (future): scheduled job updates `payments`, `transactions`, `wallets`, `notifications`.

### Analytics
- Time range: query `analytics_rollups` or aggregate `transactions` by month.
- Cards: total spent + units used.
- Chart: last 4–6 months totals.
- Monthly breakdown: list from rollups.

### Notifications
- List: `notifications` ordered by `created_at`.
- Unread dot: `is_read`.
- Logo: use app icon as leading glyph, or type‑based icon mapping.

## 4) Supabase Policies & Functions
- RLS for new tables mirroring existing pattern: `auth.uid() = user_id`.
- Functions (optional but recommended):
  - `process_payment(p_user_id, p_amount, p_method_id, p_utility_type)` to atomically update payments, transactions, wallets, usage, notifications.
  - `create_bill(p_user_id, p_amount, p_due_date, p_provider, p_account)` for bill generation.

## 5) Navigation & Motion
- Floating blurred nav pill (Compose):
  - base surface (rounded, semi‑transparent), blur layer behind, icons/text on top.
  - ensure blur does not apply to icon layer.
- Transition set:
  - tab switches: slide + fade.
  - stack screens: subtle scale + fade with spring.
- Haptics:
  - primary actions, toggles, chip selection, nav taps.

## 6) Notifications: App Logo
- Use provided app logo asset as:
  - notification icon in list rows (left circle),
  - system notification small icon (Android notification channel).
- Add asset pipeline:
  - `mipmap` for system notification,
  - vector/PNG for in‑app list.

## 7) Splash Screen Plan
- Use Android 12+ SplashScreen API.
- Sequence:
  1) Show logo centered on dark gradient background.
  2) While showing: hydrate session, fetch profile + dashboard.
  3) On success: transition to Home with subtle fade.
  4) On no session: transition to Auth screen.
- Branding assets:
  - app logo as center glyph,
  - brand color background from theme.

## 8) Test & Validation Plan
- UI: snapshot/preview checks for all screens.
- Data: create seed rows in Supabase for each screen.
- Realtime: verify notifications + wallet updates.
- Edge cases:
  - no payment methods
  - no bills
  - insufficient wallet balance
  - session expired

## 9) Execution Order (when coding resumes)
1. Data layer: add missing tables + RLS + functions.
2. Repository: add bill/payment account/wallet transaction fetches.
3. UI: Home, Pay, History, Wallet, Auto‑Pay, Analytics, Notifications
4. Motion + haptics polish.
5. Splash screen integration.
6. End‑to‑end tests + fixes.

## 10) Open Inputs Needed Later
- App logo asset (PNG/SVG) and desired sizes.
- Final payment provider name + account formatting rules.
- Real analytics definition (units used formula).
