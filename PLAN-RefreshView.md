# Plan for Refresh UI Redesign

This document outlines a redesign of the existing `RefreshView` to create a more modern, intuitive, and visually consistent user interface using Vaadin. The goal is to move from a simple tabbed form layout to a cleaner, card-based dashboard design, built with clean, maintainable code.

---

## 1. Core Design & Code Quality Principles

-   **DRY & KISS**: Avoid code duplication by extracting repeated UI patterns and logic into small, reusable private methods. Favor simplicity and clarity.
-   **Small, Focused Methods**: Each method will have a single responsibility and be kept short (ideally under 20 lines). For example, instead of one large method for the "Share Price" card, we will have separate methods to build the form fields and to handle the submission logic.
-   **Consistency**: The design will adopt the same visual language (spacing, typography, card styling) as the `AnalysisView`. A shared `UIStyle` utility class should be used for Lumo class names.
-   **Clarity & Efficiency**: A dashboard layout will make all actions visible at once, grouped logically into cards for quick scanning and access.

---

## 2. Proposed Layout: A Dashboard of Cards

The `RefreshView`'s constructor will be a clean composition of methods that build and assemble the dashboard.

### 2.1. Overall Structure

```
RefreshView (VerticalLayout)
|
+-- Header ("Data Refresh Console")
|
+-- buildMetadataSection()
|   +-- createSimpleActionCard("Market Data", "...", ...)
|   +-- createSimpleActionCard("Industry Data", "...", ...)
|
+-- buildCompanySection()
|   +-- createCompanyRefreshCard()
|   +-- createSharePriceRefreshCard()
|
+-- buildStatementsSection()
    +-- createStatementRefreshCard("Income Statements", ...)
    +-- createStatementRefreshCard("Balance Sheets", ...)
    +-- createStatementRefreshCard("Cash Flow Statements", ...)
    +-- createNewsRefreshCard()
```

---

## 3. Reusable Component Methods (DRY)

To avoid repeating code, we will create several generic, reusable methods.

### 3.1. `createCard(String title)`
Creates and returns a styled `VerticalLayout` that serves as the base for all cards. This method handles the shared styling (border, padding, shadow) in one place.

### 3.2. `createSimpleActionCard(String title, String description, Runnable action)`
Builds a complete card for actions without parameters.
-   Calls `createCard(title)`.
-   Adds a `Paragraph` with the description.
-   Adds a "Queue Refresh" `Button` with an icon, and assigns the `action` to its click listener.
-   **Used for**: Market and Industry refreshes.

### 3.3. `createStatementRefreshCard(String title, StatementRequestFactory factory)`
Builds a card for the three financial statement types, which share an identical form structure.
-   Calls `createCard(title)`.
-   Adds the `FormLayout` with fields for Market, Variants, and Templates.
-   Adds a "Queue Refresh" `Button`. The click listener will validate the form and use the provided `factory` to create the specific request object.
-   **Used for**: Income Statements, Balance Sheets, and Cash Flow Statements.

---

## 4. Detailed Implementation Plan

The `RefreshView` will be composed of small, clean methods.

### 4.1. `buildMetadataSection()`
-   Creates a `HorizontalLayout`.
-   Calls `createSimpleActionCard()` twice, once for Markets and once for Industries, providing the specific title, description, and refresh logic (`Runnable`) for each.
-   Returns the layout.

### 4.2. `buildCompanySection()`
-   Creates a `HorizontalLayout`.
-   Calls two new methods:
    -   `createCompanyRefreshCard()`: Builds the specific card for company list refreshes.
    -   `createSharePriceRefreshCard()`: Builds the specific card for share price refreshes.
-   Returns the layout.

### 4.3. `buildStatementsSection()`
-   Creates a responsive `CssGrid` layout.
-   Calls `createStatementRefreshCard()` three times, providing the appropriate title and request factory for each statement type.
-   Calls a final method, `createNewsRefreshCard()`, to build the unique card for news refreshes.
-   Returns the grid.

### 4.4. Event Handling
-   The `publish(HarvesterRequest)` method and the `showSuccess`/`showError` notification methods will be kept as small, focused helper methods.
-   Click listeners within the cards will be short lambdas that delegate to these helper methods.

This structured and component-based approach will ensure the `RefreshView` is not only visually appealing and easy to use but also that its underlying code is clean, maintainable, and easy to understand, perfectly aligning with the project's overall quality goals.
