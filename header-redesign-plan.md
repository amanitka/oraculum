# Header & Administration UI Redesign

Replace the cluttered right-side header elements with a single avatar popover menu, and consolidate the admin-only views (User Management + Data Refresh) into a dialog accessible from that popover.

## User Review Required

> [!IMPORTANT]
> The `/refresh` and `/users` routes will be **removed** as standalone pages. Both become tabs inside an "Administration" dialog, accessible only via the avatar popover for `ADMIN` users. Non-admin users will never see the "Administration" option.

> [!IMPORTANT]
> The Refresh view will be redesigned from a card-grid layout to a compact table/grid layout to better fit inside the dialog tab.

## Open Questions

> [!NOTE]
> **Popover vs ContextMenu**: Vaadin has both `Popover` and `ContextMenu`. I'll use `Popover` anchored to the avatar since it supports richer content (badges, progress bars, dividers) while `ContextMenu` is limited to simple menu items.

> [!NOTE]
> **Refresh table redesign**: The tiles with inline controls (Share Prices checkbox/datepicker, SEC Documents batch/priority fields, Ticker Documents company selector) will become expandable rows or inline components within the table. The trade-off is density vs discoverability — but since this is an admin-only tool used infrequently, density wins.

## Proposed Changes

### Navigation Simplification

#### [MODIFY] [MainLayout.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/MainLayout.java)

**Before:**
```
oraculum  Screener  Analysis  Company  Economy  Refresh    [LP] Analyses:Unlimited  Admin  Ludek Pokorny  Logout
```

**After:**
```
oraculum  Screener  Analysis  Company  Economy                                                          [LP]
```

Changes:
- Remove `RefreshView` from the navigation tabs (remove tab + router link + tabMap entry)
- Replace `createUserProfileGroup()` method entirely — instead of the inline `HorizontalLayout` with avatar + badges + links + logout, render just the `Avatar` component and attach a `UserProfilePopover` to it.
- **Avatar Enhancements**:
  - Add a hover tooltip to the Avatar showing the current usage (e.g., "Analyses: 12 / 50" or "Analyses: Unlimited").
  - If the user has reached their limit (`usage.isExceeded()`), apply a custom CSS class or theme variant to the Avatar to make its background/border red, providing a visual cue.
- Remove imports for `RefreshView`

---

### User Profile Popover

#### [NEW] [UserProfilePopover.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/components/UserProfilePopover.java)

A `Popover` component anchored to the avatar. Constructed with the current user details and analysis usage data.

**Layout:**
```
┌──────────────────────────────┐
│  Ludek Pokorny               │
│  balhazzar@gmail.com         │
│                              │
│  [ADMIN]   Analyses: Unlimited│
│                              │
│  ─────────────────────────── │
│                              │
│  ⚙ Administration            │  ← only visible if role == ADMIN
│                              │
│  ─────────────────────────── │
│                              │
│  ↪ Logout                    │
└──────────────────────────────┘
```

- **Identity section**: Bold display name + secondary-colored email
- **Role badge**: Themed `Span` badge (`ADMIN` = primary, `USER` = contrast)
- **Usage**: `Analyses: Unlimited` success badge, or `12 / 50` with color coding (success/warning/error based on remaining)
- **Administration button**: Opens `AdministrationDialog`. Only rendered if role is `ADMIN`.
- **Logout**: `Anchor` to `/logout` styled with error color

---

### Administration Dialog

#### [NEW] [AdministrationDialog.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/components/AdministrationDialog.java)

A full-screen `Dialog` (`90vw × 90vh`) with a `TabSheet` containing two tabs:

| Tab | Content |
|---|---|
| **Users** | Embedded user management grid (extracted from `UserManagementView`) |
| **Data Refresh** | Embedded refresh console (extracted from `RefreshView`, redesigned as table) |

- Dialog header title: "Administration"
- Footer: "Close" button
- Both tab contents are created lazily when the dialog opens

---

### Refactored Admin Components

#### [MODIFY] [UserManagementView.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/views/UserManagementView.java) → [MOVE/RENAME] [UserManagementComponent.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/components/UserManagementComponent.java)

- Remove `@Route`, `@PageTitle`, `@RolesAllowed` annotations (security is enforced at the dialog level — only admins see the "Administration" button)
- Change from extending `VerticalLayout` as a route target to a plain `VerticalLayout` component
- Keep all existing grid + edit dialog logic intact
- Constructor takes `UserManagementApi` as parameter

#### [MODIFY] [RefreshView.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/views/RefreshView.java) → [MOVE/RENAME] [DataRefreshComponent.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/components/DataRefreshComponent.java)

- Remove `@Route`, `@PageTitle`, `@RolesAllowed` annotations
- **Redesign from card-grid to table layout**:

**New table structure:**

| Operation | Description | Action |
|---|---|---|
| Market Data | Refreshes the list of all supported stock markets | `Refresh` |
| Industry Data | Refreshes the list of all industry classifications | `Refresh` |
| Company List | Refreshes companies across all supported markets | `Refresh` |
| Fundamentals | Refreshes Income/Balance/CashFlow statements | `Refresh` |
| Ticker Documents | SEC filings (US). Select companies or refresh stale | `Refresh` |
| Share Prices | Daily share prices. Incremental + date options | `Refresh` |
| SEC Summaries | LLM processing of pending docs. Batch/priority | `Process` |
| News & Sentiment | Recent news articles and sentiment data | `Refresh` |
| Insider Transactions | Daily insider trading from OpenInsider | `Refresh` |
| Macroeconomic Data | Yield curves, inflation, unemployment from FRED | `Refresh` |
| Materialized Views | Rebuild views and screener cache (async) | `Refresh` |

- Tiles with extra controls (Share Prices, Ticker Documents, SEC Summaries) will use a **details row** pattern: clicking the row or an expand icon reveals the inline controls below that row
- Constructor takes `HarvesterBatchApi`, `ApplicationEventPublisher`, `CompanyMetadataApi` as parameters

---

### Cleanup

#### [DELETE] [RefreshView.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/views/RefreshView.java)
Replaced by `DataRefreshComponent.java` inside the administration dialog.

#### [DELETE] [UserManagementView.java](file:///c:/Git/Personal/oraculum/src/main/java/com/oraculum/ui/views/UserManagementView.java)
Replaced by `UserManagementComponent.java` inside the administration dialog.

---

## Verification Plan

### Automated Tests
- `mvn clean test` — ensure compilation, modularity tests (Spring Modulith), and all existing unit tests pass after the route removals and component moves.

### Manual Verification
- Verify that clicking the avatar opens the popover with correct user info, role badge, and usage stats.
- Verify that non-admin users do NOT see the "Administration" button in the popover.
- Verify that clicking "Administration" opens the dialog with both tabs (Users + Data Refresh).
- Verify that the User Management grid and edit dialog work correctly inside the dialog tab.
- Verify that all refresh operations still fire correctly from the redesigned table layout.
- Verify that the "Logout" link in the popover redirects to `/logout`.
- Verify that navigating between Screener/Analysis/Company/Economy still highlights the correct tab.
