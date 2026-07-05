# Oraculum SSO Architecture Plan

This document outlines the planned Single Sign-On (SSO) architecture for Oraculum, designed specifically for a Homelab environment where Keycloak serves as the master gatekeeper, while Google SSO is provided for restricted public access.

## Architecture Overview

We will use the **Spring Boot Native Approach** (`spring-boot-starter-oauth2-client`). 
Spring Boot will handle two Identity Providers (IdPs) directly:
1. **Keycloak**: Trusted, private SSO for the Homelab Admin.
2. **Google**: Public-facing SSO for friends and family.

Because Keycloak grants master access to the Homelab (e.g., Proxmox, Portainer), we **cannot** use Keycloak as an Identity Broker for Google users. Friends and family must authenticate directly with Google and never interact with the Keycloak server.

## Authorization Model

Two roles are sufficient for a homelab use case:

- **`ADMIN`** — Full access: view data, trigger refreshes, manage users, configure harvesters, view logs.
- **`USER`** — Read-only access: view company analyses, screener, economy data. Subject to monthly analysis limits.

Roles are stored as a single `role` column on `t_user` (no separate `t_user_role` join table — KISS principle). Using `VARCHAR(20)` keeps the door open for a future third role without a schema migration.

## Database Schema

### `t_user` Table (Migration: `V01__t_user.sql`)

```sql
CREATE TABLE public.t_user (
    id                     BIGSERIAL    PRIMARY KEY,
    email                  VARCHAR(255) NOT NULL UNIQUE,
    first_name             VARCHAR(255),
    last_name              VARCHAR(255),
    provider               VARCHAR(50)  NOT NULL,  -- 'keycloak' or 'google'
    role                   VARCHAR(20)  NOT NULL DEFAULT 'USER',
    analysis_limit         VARCHAR(10),  -- e.g. '5D', '10W', '20M'; NULL = unlimited
    enabled                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login_at          TIMESTAMPTZ
);
```

Design notes:
- **`first_name` / `last_name`** — Populated from OIDC claims (`given_name`, `family_name`) on each login. Both nullable because an admin may pre-register a friend by email before their first login. Display name is derived in the Java entity (`firstName` or `firstName + " " + lastName`), not stored.
- **`provider`** — Tracks how the user was registered, useful for enforcing auto-register logic and audit.
- **`analysis_limit`** — Rate limiting for user-triggered company analyses, using a compact format: number + period code (`D` = daily, `W` = weekly, `M` = monthly). Examples: `5D` = 5 per day, `10W` = 10 per week, `20M` = 20 per month. `NULL` = unlimited (used for admins). Counted against `t_company_analysis` rows for the given period.
- **`enabled`** — Soft-disable a user without deleting them.
- **`last_login_at`** — Updated on each login; useful for seeing who's actively using the system.

### `t_company_analysis` Change (Migration: `V05__t_company_analysis.sql`)

Add a `requested_by` foreign key to track who triggered each analysis:

```sql
requested_by BIGINT REFERENCES t_user(id)
```

This column is added directly into the existing `CREATE TABLE` statement (not as a separate ALTER migration). It serves dual purpose:
1. **Rate limiting** — Count analyses per user per month.
2. **Audit** — Know who triggered what.

No `user_id` is needed on `t_llm_execution_log` or `t_api_usage` — LLM calls trace back to the user through `t_company_analysis.requested_by` via `correlation_id`, and `t_api_usage` is internal free-API protection unrelated to users.

### Migration Renumbering

Inserting `t_user` as `V01` requires shifting all existing migrations by +1. The Flyway history table (`flyway_schema_history`) will be deleted and the database re-synced manually.

| Old     | New     | Table                        |
|---------|---------|------------------------------|
| —       | **V01** | **`t_user`** (NEW)           |
| V01     | V02     | `t_industry`                 |
| V02     | V03     | `t_market`                   |
| V03     | V04     | `t_company`                  |
| V04     | V05     | `t_company_analysis` (+FK)   |
| V05     | V06     | `t_load_log`                 |
| V06     | V07     | `t_balance_sheet`            |
| V07     | V08     | `t_cash_flow_statement`      |
| V08     | V09     | `t_income_statement`         |
| V09     | V10     | `t_share_price`              |
| V10     | V11     | `t_news`                     |
| V11     | V12     | `t_news_ticker`              |
| V12     | V13     | `event_publication`          |
| V13     | V14     | `t_api_usage`                |
| V14     | V15     | `t_llm_execution_log`        |
| V15     | V16     | `t_insider_transaction_ticker`|
| V16     | V17     | `t_macro_observation`        |

## Application Bootstrap & User Whitelisting

To prevent unauthorized users from accessing Oraculum, we will implement a **"First-User Bootstrap"** pattern combined with a strict database whitelist for all subsequent logins. This elegantly eliminates the need for complex, provider-specific configuration properties.

### Custom `OAuth2UserService` Interceptor
Spring Security will intercept all successful OIDC logins (from both Keycloak and Google) and execute the following universal logic:

#### Scenario A: The First Login (System Bootstrap)
1. The `t_user` table is completely empty.
2. The Admin logs in for the very first time (via Keycloak or Google).
3. The Interceptor detects that `userRepository.count() == 0`.
4. The Interceptor auto-registers the user, sets their role to `ADMIN`, their `analysis_limit` to `NULL` (unlimited), and grants access.
5. **Result:** Frictionless first-time setup for the Homelab owner.

#### Scenario B: Unauthorized User Login
1. A random person authenticates via Google (or Keycloak).
2. The Interceptor checks `t_user`. The table is not empty (`count > 0`), so bootstrap mode is disabled.
3. The Interceptor checks if the user's email exists in the database. It is not found.
4. Interceptor throws an `AccessDeniedException`. The denied attempt is logged at `WARN` level in the application log.
5. User is redirected to a branded "Access Denied" page (see UI Considerations).
6. **Result:** The user is blocked.

#### Scenario C: Authorized Friend Login
1. The Admin previously added the friend's email to `t_user` via the Admin Panel (assigning `ROLE_USER` and a limit like `10M`).
2. The friend authenticates via Google.
3. The Interceptor checks `t_user` and finds the friend's email.
4. Interceptor updates `first_name`, `last_name`, and `last_login_at` from OIDC claims, then grants access with their assigned role.
5. **Result:** Secure, restricted access for friends and family.

## Rate Limiting

User-triggered company analyses are rate-limited per user via `t_user.analysis_limit`, using a compact format:

| Value  | Meaning              |
|--------|----------------------|
| `5D`   | 5 per day            |
| `10W`  | 10 per week          |
| `20M`  | 20 per month         |
| `NULL` | Unlimited (for admins)|

- The limit is counted against `t_company_analysis` rows for the relevant period per user.
- Users with `NULL` limit (typically admins) bypass the check entirely — no counter shown in the UI.
- The UI displays a counter for rate-limited users: **"Analyses: 7 / 10"**.
- When the limit is reached, the user receives a clear message and cannot trigger new analyses until the period resets.

The unit of measurement is **one company analysis** — not LLM calls or tokens. One analysis may involve multiple LLM steps internally, but that complexity is hidden from the user.

## Security Considerations

### Session Invalidation
When an admin disables a user via the Admin Panel, the user's active session must be invalidated immediately (via Spring Security's `SessionRegistry`), not left to expire naturally.

### Logout Flow
Logging out of Oraculum should clear the IdP session for Keycloak (back-channel logout). Google SSO typically does not support back-channel logout.

### Identity Matching
Users are matched by **email** (`t_user.email`). For additional robustness, consider also storing the OIDC `sub` (subject ID) claim in the future — emails can change, while `sub` is the stable identifier per OIDC spec. For a homelab, email-only is sufficient.

## Required Dependencies
To implement this in the future, the following dependencies must be added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

## Spring Modulith Integration

The SSO logic should live in a new **`security`** module (`com.oraculum.security`) with sub-packages:
- `domain` — `User` entity, `Role` enum
- `repository` — `UserRepository`
- `service` — `UserService`, custom `OAuth2UserService` interceptor
- `api` — Public API interfaces and DTOs exposed to other modules (e.g., `ui`)
- `config` — `SecurityConfig`, provider properties binding

The `ui` module depends on `security.api` for role checks and user context.

## UI Considerations

### Login View
A custom Vaadin `LoginView` with two distinct buttons:
- **[ Admin Login ]** → Redirects to Keycloak
- **[ Sign in with Google ]** → Redirects to Google

### Access Denied Page
A branded page shown to unauthorized Google users (Scenario B):
> *"You don't have access to Oraculum yet. Please contact the administrator to request access."*
> 
> *Your email: user@gmail.com*

Displaying the email helps the user tell the admin exactly what to whitelist.

### Admin Panel — User Management
A new `UserManagementView` accessible only to `ROLE_ADMIN`:
- **User Grid** — Lists all users (email, first name, last name, provider, role, enabled, analysis limit, last login)
- **Add User** — Form to pre-register a friend by email + role
- **Edit User** — Change role, adjust analysis limit (number + period dropdown, or unlimited toggle)
- **Deactivate / Revoke Access** — Soft-delete a user by toggling `enabled = false` (with immediate session invalidation). Physical deletion is not permitted in order to preserve audit history.

### Usage Counter
For `USER` role, display in the UI header: **"Analyses: 7 / 10"** — showing current period usage against their limit (e.g., day, week, or month).

## Future Considerations

- **Per-user watchlist** — A `t_user_watchlist(user_id, company_id)` join table to let each user "star" companies for a personalized dashboard. SSO unlocks user-specific features.
- **Admin usage dashboard** — Aggregate analysis counts and (optionally) estimated LLM token costs per user per month, leveraging existing `t_llm_execution_log` data joined through `t_company_analysis.requested_by`.
