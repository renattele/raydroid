---
name: datalayer-architect
description: Kotlin Multiplatform data layer and business logic architecture guidance for Kotlin 2.2.0 projects targeting Android (minimum SDK 26) and iOS 15. Use when designing, refactoring, or reviewing repositories, data sources, Koin annotation-based DI, coroutine and Flow usage, Ktor networking, Room persistence, model separation, and error handling with AppLogger.
---

# Datalayer Architect

Follow these rules when implementing or refactoring the data layer and shared business logic.

## Workflow

1. Inspect current repository, data source, model, and DI structure before editing.
2. Keep changes minimal and local; avoid broad rewrites unless required to fix architecture issues.
3. Decompose large classes into focused repositories and data sources.
4. Extract shared business logic into injected `@Single` components.
5. Verify threading, error handling, model boundaries, and source-set placement before finishing.

## Platform and Source Sets

- Use modern Kotlin 2.2.0 language and library features where appropriate.
- Target Android minimum SDK 26 and iOS 15 constraints.
- Follow multiplatform best practices across `commonMain`, `androidMain`, and `iosMain`.
- Place shared data layer components under `src/commonMain/kotlin/app/data`.

## Architecture Rules

- Use repository + data source architecture for data access and business logic boundaries.
- Use suffix `Repository` for repository classes.
- Use suffix `DataSource` for the layer below repositories.
- Keep components small, single-purpose, and reusable.
- Maintain clear separation between data layer and domain/business logic.

## Dependency Injection

- Use Koin annotation-based DI configuration.
- Use `@Single` for focused reusable components.
- Move shared business logic from feature-specific classes into shared injected components.

## Coroutines and Flow

- Use Kotlin Coroutines for asynchronous and background operations.
- Use `Flow` for reactive data pipelines where state and stream semantics are needed.
- Keep coroutine context handling explicit and safe.

## Networking and Persistence

- Use Ktor for networking and remote data fetching.
- Implement robust response parsing and error handling for network calls.
- Use Room for local persistence on supported targets.
- Use suffix `Entity` for Room database models.
- Maintain database migrations and versioning discipline.

## Models and Serialization

- Separate data models from domain models.
- Use `@Serializable` only for classes that require serialization.
- Keep serializable models small and focused.
- Prefer `sealed interface` over `sealed class` when language constraints allow.

## Error Handling and Logging

- Implement explicit error handling and fallback behavior at repository/data-source boundaries.
- Log errors and exceptions using `AppLogger`.
- Avoid swallowing exceptions without mapping or logging.

## Code Style

- Write clean and readable Kotlin aligned with official Kotlin and Android guidance.
- Use descriptive names for components and state properties.
- Avoid unnecessary abstractions or over-engineering.
- Avoid writing comments or extra documentation in code.

## Quality Gate

- Confirm repositories and data sources are independent and reusable.
- Confirm business logic extraction and DI boundaries are correct.
- Confirm coroutine and Flow data paths are coherent and maintainable.
- Confirm model separation and serialization boundaries are correct.
- Confirm Ktor/Room usage follows modern patterns and robust error handling.
- Confirm changes stay minimal and avoid unrelated refactors.

