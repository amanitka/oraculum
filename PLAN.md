# Plan for Vaadin UI Reimplementation

This document outlines the plan to create a modern Vaadin-based UI for the Oraculum project, inspired by the existing Python Streamlit console. The plan is divided into two main parts:

1.  **Analysis UI**: A view to trigger company analyses and browse their history.
2.  **Company Data Browser**: An optional but recommended view for exploring fundamental company data, news, and share price history.

We will use modern Vaadin components to create a clean, material-inspired, and user-friendly interface, backed by clean, maintainable code.

---

## Part 0: Code Quality & Design Philosophy

Across all UI implementation, the following principles are paramount:

-   **Don't Repeat Yourself (DRY)**: Avoid code duplication. If a set of components or a piece of logic is used more than once, extract it into a reusable private method or a dedicated UI component class.
-   **Keep It Simple, Stupid (KISS)**: Favor clarity and simplicity over unnecessary complexity.
-   **Small, Focused Methods**: Each method should have a single, clear responsibility. Aim to keep methods under 20 lines of code. This improves readability and makes the code easier to test and refactor.
-   **Consistent Styling**: A `UIStyle` utility class should be created to hold shared Lumo utility class names for consistent padding, margins, and card styling.

---

## Part 1: Analysis UI (`AnalysisView.java`)

This view will be the main hub for interacting with the AI analyst. It will allow users to request new analyses for specific companies and to view the results of past and ongoing analyses.

### Step 1: API Adjustments

To build an efficient and responsive UI, we should first enhance the `CompanyApi`.

1.  **Add `getCompaniesByMarket` to `CompanyApi`**: The current `getAllCompanies()` could be inefficient. A new method to fetch tickers filtered by market is recommended.
    ```java
    // In com.oraculum.company.api.CompanyApi
    List<CompanyDto> getCompaniesByMarket(String market);
    ```
2.  **Confirm `CompanyAnalysisRequest` `defaultVariant`**: The `CompanyAnalysisRequest` requires a `StatementVariant`. We will add a UI control to select this, defaulting to `TTM`.

### Step 2: View Layout & Componentization

The `AnalysisView` constructor should be lean, primarily orchestrating the assembly of components built by smaller, focused methods.

-   **Main Layout**: A `SplitLayout` will be used to create the resizable master-detail interface.
-   **Component Methods**:
    -   `createTriggerCard()`: Builds the entire "Run new analysis" card.
    -   `createHistoryGrid()`: Configures and returns the `Grid` for analysis history.
    -   `createDetailView()`: Builds the layout for displaying the details of a selected analysis.

### Step 3: Analysis Trigger Form (`createTriggerCard`)

This method will build the form for triggering an analysis.

-   **Components**:
    -   `ComboBox<MarketDto>` for Market Selection.
    -   `ComboBox<CompanyDto>` for Ticker Selection.
    -   `ComboBox<StatementVariant>` for Statement Variant.
    -   `Button` ("Analyze").
-   **Logic**: The "Analyze" button's click listener will call a separate private method, `triggerAnalysis()`, to handle the validation, request creation, and notification logic.

### Step 4: Analysis History Grid (`createHistoryGrid`)

This method will configure the main grid.

-   **Data Binding**: Bind the grid to a `CallbackDataProvider` that fetches paginated data from `companyAnalysisApi.getCompanyAnalysisList()`.
-   **Column Renderers**: Use `ComponentRenderer` for columns that need special styling, like the `Status` column (using a `Badge`). Extract the renderer logic into its own small method, e.g., `createStatusBadgeRenderer()`.
-   **Selection Listener**: The grid's selection listener will update the detail view by calling `updateDetailView(Optional<CompanyAnalysisDto> analysis)`.

### Step 5: Analysis Detail View (`createDetailView` & `updateDetailView`)

-   `createDetailView()`: This method will initialize the layout and all its child components (for the header, status, metrics, report, etc.) in a disabled/empty state.
-   `updateDetailView(Optional<CompanyAnalysisDto> analysis)`: This method is responsible for populating the components created in `createDetailView`. It will take an `Optional<CompanyAnalysisDto>`:
    -   If present, it populates the fields from the DTO.
    -   If empty, it resets the view to its initial placeholder state.
    -   Helper methods like `renderReport(String markdown)` and `createMetric(String title, String value)` will be used to keep this method clean.

---

## Part 2: Company Data Browser (Optional)

This second view (`CompanyDataView.java`) would provide a "read-only" dashboard for exploring all the data available for a company. The same principles of small, focused methods will apply.

### Step 1: View Layout

A `SplitLayout` providing a master-detail view.

-   `createCompanyListGrid()`: Builds the searchable grid of companies on the "master" side.
-   `createCompanyDashboard()`: Builds the tabbed dashboard on the "detail" side.

### Step 2: Company Dashboard (`createCompanyDashboard`)

This method will create the `Tabs` and the content for each tab.

-   **Tab Content Methods**: Each tab's content will be generated by its own method:
    -   `createOverviewTab(CompanyDto company)`
    -   `createFinancialsTab(CompanyDto company, StatementType type)`
    -   `createNewsTab(CompanyDto company)`
-   **Chart Creation**: The share price chart within the overview tab will be built by a dedicated `createSharePriceChart(int companyId)` method.

---

## Part 3: Project Structure & Dependencies

1.  **Package Structure**: All new Vaadin views should be placed in `src/main/java/com/oraculum/ui/views/`. A new package `com.oraculum.ui.components` can be created for any reusable UI components.
2.  **Dependencies**: Ensure `pom.xml` includes `vaadin-core` and `vaadin-charts-flow`.
3.  **Routing**: Each view class should be annotated with `@Route`.
