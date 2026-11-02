# AgroLink BD — Web MVP

A modern, mobile-friendly agricultural marketplace for Bangladesh: farmers list harvests, buyers place orders and reserve shared truck space, and transporters publish routes. Buyers and farmers can coordinate through private order messages. This is a **functional student-built MVP**, not a production logistics business.

## Tech stack

- **Next.js 16** App Router + **React 19** + TypeScript
- **Supabase**: PostgreSQL, Auth, Row-Level Security, SQL functions
- **CSS**: lightweight responsive custom design; Lucide icons
- **Deploy**: Vercel (web root directory is **web/**)

The older JavaFX/MySQL desktop project remains at the repository root and is independent of this web app.

## What works

| User | Capabilities |
| --- | --- |
| Visitor | View active product listings and filter by crop / district |
| Farmer | Sign up, sign in, publish harvests, view received orders, reply to buyer messages, confirm/dispatch orders, reserve truck capacity |
| Buyer | Sign up, place orders (with stock locking), cancel pending orders (restocks automatically), message sellers privately, reserve truck space |
| Transporter | Sign up, publish dated truck routes, see bookings on their routes |
| All members | Responsive dashboard with role-relevant views |

Order discussions have participant-only read/write database policies so unrelated accounts cannot read them. Order creation, cancellation and truck reservations use atomic PostgreSQL functions to prevent overselling/overbooking. Row-level policies prevent other users from changing data they do not own.

**Current limitations:** email login only, manually coordinated payment/collection, no GPS tracking, mobile money, SMS, escrow, delivery guarantees, ratings, order chat or image uploads. Do not use real NIDs or sensitive personal data.

## Run locally

1. Install Node.js 22+ and npm.
2. Create a new [Supabase](https://supabase.com) project in a suitable region.
3. Open Supabase **SQL Editor**, copy **supabase/schema.sql**, and run it on this **new** project.
4. In Supabase **Authentication** settings, set the Site URL to http://localhost:3000 and configure email confirmation / redirect URLs as needed.
5. Copy the example environment file to web/.env.local and replace placeholders. The **publishable key** and public project URL are safe to expose to the browser **with RLS enabled**; do not add your service-role/secret key to NEXT_PUBLIC variables.

~~~bash
cd web
cp .env.example .env.local
npm install
npm run dev
~~~

Visit **http://localhost:3000**. Open a second terminal for the build check:

~~~bash
npm run typecheck
npm run build
~~~

PowerShell: replace cp with Copy-Item.

If Supabase env variables are missing, the marketing homepage still renders but live features cannot connect.

## Deploy to Vercel

1. Push the project to your GitHub repository.
2. In Vercel, **Add New Project → Import Git Repository**.
3. Choose your AgroLink repository. Set **Root Directory** to **web** (very important because JavaFX lives at repo root).
4. Framework preset: Next.js. Build command: npm run build. You may leave the defaults.
5. Set the public environment variables in **Project Settings → Environment Variables**:
   - NEXT_PUBLIC_SUPABASE_URL
   - NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY
6. In the Supabase dashboard, add your Vercel production URL (and any preview URLs you explicitly trust) to **Authentication → URL Configuration**. Use the correct Site URL and allowed redirect URLs.
7. Deploy. Confirm login, listings, orders, and truck booking with test accounts before inviting real users.

No live deployment is created until you connect a Vercel project and provide **your own** Supabase project URL and publishable key. There is no hardcoded server secret.

## Suggested smoke tests (manual)

- New farmer signs up; confirm email if enabled; logs in; publishes potatoes, quantity 100 kg.
- New buyer logs in, buys 10 kg; inventory now shows 90 kg.
- Buyer cancels pending order; inventory returns to 100 kg.
- Farmer sees a different buyer's order on their own crops and can confirm/dispatch it.
- Transporter creates Dhaka → Gazipur route, capacity 500 kg. Farmer reserves 60 kg; remaining 440 kg.
- Attempt to book 500 more kg must fail without changing the record.
- Buyer cannot insert another farmer's listing; farmer cannot place buyer orders. RLS policies protect these cases.
- Buyer and farmer can exchange private order messages; unrelated users cannot access the thread.
- Check screens at 375px mobile and on desktop; verify empty-state handling.

All figures above are **manual test examples**, not assertions that production data exists.

If your development database was initialized using an older copy of schema.sql, apply the new `order_messages` table, function and policies from the end of the updated schema separately. Do not rerun all policy CREATE statements on an existing database.

## Production roadmap

Before real trading: verify farmer/transporter identities, add phone OTP with a legitimate SMS provider, privacy/data retention policy, disputes/refunds, reliable fulfillment and order notifications, fraud protection, rate limiting, monitoring, backups, and accessibility/security review. Prefer gathering the minimum user details necessary.

## Deployment/security design notes

- Supabase RLS is the authorization boundary, not the UI tabs.
- Account role is from the stored profile row; the auth trigger accepts only predefined roles.
- Never introduce an admin account through untrusted user metadata.
- SQL functions run as SECURITY DEFINER, check auth.uid(), validate roles/state and are executable by authenticated users only.
- Monetary amounts are stored as PostgreSQL NUMERIC and displayed as BDT.
- Avoid running the Supabase schema against your legacy JavaFX/MySQL database; they are different database engines and apps.

**Project owner:** [Toufiqul Islam](https://github.com/Toufiqul-27)
