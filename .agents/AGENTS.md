# Oraculum Architecture & Execution Contract

You **MUST** enforce every rule in this file. Before completing any coding turn, run the **Mandatory Self-Correction Loop** in Section 8.

## 1. Phase-Gated Workflow (Human-in-the-Loop)

For any multi-file change, new feature, or refactoring:
1. **PHASE 1 (Research & Plan)**: Inspect affected modules and present a concise implementation plan (or artifact with `RequestFeedback: true`). **STOP and wait for user approval** before modifying production code.
2. **PHASE 2 (Execute & Closed-Loop Verify)**: Use an agentic test-driven or test-backed loop (write/update unit tests alongside or before implementation so behavior is immediately verifiable), then execute the Section 8 verification loop until all checks and tests are 100% green.
3. **PHASE 3 (Review)**: Summarize the changes, report executed test commands, and confirm compliance with all rules below.

## 2. Spring Modulith Boundaries

- Active modules under `com.oraculum`: `analyst`, `audit`, `common`, `company`, `database`, `economy`, `harvester`, `llm`, `load`, `security`, `ui`, `user`, `util`.
- **Strict Isolation**: Do **not** create tangled cross-module dependencies or inject internal repositories/services from another module.
- **Inter-Module Communication**: Communicate across modules exclusively via clean, public module API interfaces (`com.oraculum.<module>.api.*`) or Spring Application Events (`@ApplicationModuleListener` / `@Async` `@EventListener`).
- **Verification**: Any new cross-package import MUST pass `./mvnw test -Dtest=ModularityTests`.

## 3. Control Flow: Guard Clauses & Flat Nesting

- **Early Returns First**: Handle edge cases, empty states, and preconditions at the very top of a method using flat guard clauses (`if (input.isEmpty()) return List.of();`).
- **No Nested `if` Pyramids**: Never write deeply nested `if / else` blocks (max 1 level of conditional nesting inside a method). Invert conditions and return early, or extract a helper method.
- **Avoid `else` After `return`**: If an `if` block ends with `return` or `throw`, do not wrap the rest of the method in an `else` block—keep the happy path at the root indentation level.

## 4. Null-Safety by Design (Boundary Validation Only)

- **NO Endless Enterprise Null Guards**: Do **not** clutter internal services, domain logic, or private helpers with paranoid `if (x != null)` checks or `Objects.requireNonNull()` noise.
- **Validate at the Boundary Only**: Validate and sanitize external inputs once at the entry boundary (external API clients, file loaders, controllers, or UI inputs).
- **Non-Null Domain Guarantees**: Once data passes the boundary, design records, DTOs, and domain objects so their fields are guaranteed non-null (use sensible defaults, empty collections `List.of()`, or empty strings where appropriate) so downstream code never needs null checks.
- **Pragmatic `Optional` Usage**:
  - Use `Optional<T>` **only as a return type** when the absence of a value is a meaningful domain outcome (e.g., `findByTicker(...)`).
  - **Do NOT overuse `Optional`**: Never use `Optional` for class/record fields, method parameters, or collections (always return `List.of()` / `Set.of()` instead of `Optional<List<T>>` or `null`).

## 5. Simplicity Over Abstraction (Rule of Three) & Size Caps

- **KISS & Rule of Three (No Premature Abstraction)**: Prefer simple, concrete, straightforward code over generic frameworks, deep inheritance hierarchies, or single-implementation interfaces. **Wait until a pattern is duplicated 2–3 times** before extracting a shared abstraction or utility.
- **Method Size Cap**: Target **15–20 lines** per method; hard cap is **25–30 lines** (excluding blank lines/comments). If a method exceeds 30 lines, extract logical steps into private helper methods.
- **Class Size Cap**: Target under **250 lines**; hard cap is **350 lines** for production classes. If a class grows beyond 350 lines, split it by responsibility (e.g., separate calculation, mapping, or orchestration).
- **Modern Stack Idioms (Java 25 & Spring Boot 4 / Jackson 3)**:
  - Use `record` for immutable DTOs and value objects, pattern-matching `switch` expressions, `Stream.toList()`, and `SequencedCollection` methods (`.getFirst()`, `.getLast()`).
  - Use `@RequiredArgsConstructor` with `private final` fields for constructor injection (never `@Autowired` field injection).
  - Use Jackson 3 (`tools.jackson.databind.*`) instead of legacy `com.fasterxml.jackson.databind.*`.

## 6. Clean Imports (NO Inline Fully Qualified Names or Wildcards)

- **NEVER** use inline fully qualified class names in code bodies, parameters, or return types.
  - ❌ **BAD**: `java.util.Comparator.comparing(MyClass::getId)` or `java.time.LocalDate.now()`
  - ✅ **GOOD**: Add explicit `import` statements at the top of the file and reference `Comparator.comparing(MyClass::getId)`.
- **No Wildcard Imports**: Avoid `import foo.bar.*;` in production code—always use explicit single-class imports.
- If a naming collision occurs, rename your local DTO/helper or extract a small mapper so both files use clean imports.

## 7. Testing Coverage (Agentic TDD / Test-Backed Loop)

- Maintain high unit test coverage for all new or modified business logic using **JUnit 5**, **Mockito** (`@ExtendWith(MockitoExtension.class)`), and **AssertJ** (`assertThat(...)`).
- Agents should use a test-first or tight test-alongside loop to self-verify happy paths, early-return guard clauses, and boundary validation without requiring manual user testing first.
- Follow the `methodName_whenCondition_expectedOutcome` test naming convention.

## 8. Mandatory Self-Correction Loop (Run Before Stopping)

Before ending your turn after editing any `.java` file, you **MUST** execute this inner verification loop (enforced automatically by `.agents/hooks.json` with a max-2-retry circuit breaker):
1. **Scan for Inline FQNs & Wildcards**: Verify zero inline package paths (`java.util.`, `org.springframework.`, etc.) and zero wildcard imports exist in any modified `.java` file.
2. **Check Control Flow, Size Caps & Null Noise**: Verify every modified method is <= 30 lines, production classes are <= 350 lines, control flow uses flat early-return guard clauses, and there are no redundant internal null guards or `Optional` parameters/fields.
3. **Run Unit Tests**: Execute `./mvnw test -Dtest=<AffectedTestClasses>` and fix any failures.
4. **Run Modulith Check**: If cross-package imports changed, execute `./mvnw test -Dtest=ModularityTests`.

*Do not hand control back to the user with failing tests or rule violations—fix them inside the loop first.*
