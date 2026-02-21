---
name: compose-architect
description: Jetpack Compose and Compose Multiplatform architecture guidance for Kotlin Multiplatform apps targeting Android 16 (SDK 36, minimum SDK 26) with Material 3 and Kotlin 2. Use when designing, refactoring, or reviewing Compose UI, ViewModels, UiState and UiEvent flows, Koin annotation-based dependency injection, and type-safe Compose Navigation with minimal-impact code changes.
---

# Compose Architect

Follow these rules when implementing or refactoring Compose screens in this project.

## Workflow

1. Inspect the existing screen, ViewModel, navigation route, and shared UI usage before editing.
2. Keep changes minimal and local; avoid broad rewrites unless required to fix architecture violations.
3. Move business logic out of composables into `AbstractViewModel` or shared `@Single` components.
4. Decompose large composables into small, single-purpose, reusable components.
5. Verify state flow, event flow, side effects, navigation, and previews before finishing.

## Architecture Rules

- Use MVVM with `AbstractViewModel` for state and business logic ownership.
- Represent screen state with `UiState` and `MutableStateFlow`.
- Name mutable state directly without underscore prefixes, for example `val uiState = MutableStateFlow(UiState())`.
- Handle user interactions and one-off effects via `UiEvent` patterns.
- Keep composables focused on rendering and dispatching events.
- Extract shared business logic into Koin `@Single` components.
- Use Koin annotation-based dependency injection setup.
- Use Compose Navigation with type-safe navigation patterns that start from a `Route` and define a `Screen` per view.

## Compose UI Rules

- Use modern Compose Multiplatform 1.9 and Material 3 APIs first.
- Apply Android API availability checks when using SDK 36-specific features.
- Prefer composition over deep or complex UI hierarchies.
- Use reusable components from `shared/src/commonMain/kotlin/app/ui/core` when building new UI, starting from the preview app patterns.
- Keep components independent so they can be reused across screens.
- Add `@Preview` functions at the end of each Compose file.
- Wrap each preview with `AppThemePreview`.
- Avoid unnecessary abstractions and over-engineered patterns.
- Avoid writing comments or extra documentation in code.

## State, Performance, and Side Effects

- Drive UI from observable state and collect flows in composables appropriately.
- Use `remember`, `mutableStateOf`, and `derivedStateOf` to reduce unnecessary recomposition.
- Use `LaunchedEffect` and `LaunchedFor` for side effects and coroutine launches tied to UI lifecycle.
- Keep side effects out of pure UI rendering paths.

## Quality Gate

- Confirm each component is independent and reusable.
- Confirm data flow is unidirectional and consistent across `UiState` and `UiEvent`.
- Confirm business logic is in ViewModel or injected `@Single` components, not composables.
- Confirm navigation is type-safe and consistent with `Route`/`Screen` patterns.
- Confirm changes are minimal and do not introduce unrelated refactors.
