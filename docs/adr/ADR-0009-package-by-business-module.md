# ADR-0009: Package by Business Module Inside a Single Spring Boot Build

- Status: Accepted
- Date: 2026-09-20

## Context

The MVP is a Modular Monolith with Identity, Assessment, Group and AI Integration responsibilities. A global `controller/service/repository/entity` package structure would hide domain ownership, while Maven/Gradle multi-module decomposition would add build complexity before physical isolation is needed.

## Decision

Use one Spring Boot build/deployable and organize top-level code by business module. Each business module may contain its own API, Domain, Application, Infrastructure and Web packages. Cross-module workflows use a narrow orchestration layer and module public contracts.

Use architecture tests to protect the dependency direction.

## Consequences

### Benefits

- Code structure reflects documented domain ownership.
- Modular Monolith boundaries are visible without multi-module build overhead.
- Future physical extraction remains possible.

### Costs

- Package boundaries are logical rather than compiler/build isolation boundaries.
- Architecture tests and review discipline are required.
