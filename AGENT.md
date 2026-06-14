# Oraculum Golden Rules for AI Agents

Welcome to the `oraculum` repository! As an AI coding assistant, you **MUST** adhere to the following architectural and stylistic rules when modifying this codebase. 

## 1. Modular Architecture (Spring Modulith)
- This project is structured using **Spring Modulith**.
- Enforce strict logical boundaries between modules (`analyst`, `company`, `database`, `harvester`, `load`, `ui`).
- Do **not** create tangled cross-module dependencies. Inter-module communication should happen via Spring Application Events (`@Async` event listeners) or clean, well-defined API interfaces.

## 2. DRY & KISS Principles
- **DRY (Don't Repeat Yourself)**: Extract common logic into shared utility methods, default interface methods, or abstract base classes. Do not copy-paste code.
- **KISS (Keep It Simple, Stupid)**: Avoid over-engineering. Write straightforward, readable code over "clever" but complex code. 

## 3. SOLID Principles
- Ensure your code follows SOLID principles (Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion).
- Classes should do exactly one thing well. 

## 4. Short Methods & Clean Code
- Methods must be concise. **Keep methods strictly under 15-20 lines where possible.** 
- If a method grows too large, extract logical blocks into private helper methods with descriptive names.

## 5. Proper Imports (NO Inline Fully Qualified Names)
- **NEVER** use inline fully qualified class names.
- ❌ **BAD**: `java.util.Comparator.comparing(MyClass::getId)`
- ✅ **GOOD**: Use proper imports at the top of the file and reference `Comparator.comparing(MyClass::getId)`.

## 6. Testing Coverage
- Maintain high test coverage for all new business logic.
- Services should have corresponding unit tests using **JUnit 5**, **Mockito**, and **AssertJ**.
- Ensure edge cases and exception handling are explicitly tested.

## 7. Defensive Programming
- Use `null` checks where appropriate, but prefer `Optional` for return types.
- Validate incoming data and rely on standard Spring validation or assertion checks (e.g., `Assert.notNull()`).
