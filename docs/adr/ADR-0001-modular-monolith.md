# ADR-0001: Use a Modular Monolith

- Status: Accepted
- Date: 2026-09-15

## Context

The project needs clear business boundaries and interview-worthy architecture while remaining feasible for one developer to build, test, deploy, and operate.

## Decision

Use one Spring Boot deployable organized as a Modular Monolith with logical modules for Assessment, Identity & Access, Group, and AI Integration. Cross-module use cases are coordinated by the Application layer.

## Consequences

Benefits include lower operational complexity, easier local development, and clear domain boundaries without premature distributed systems. The codebase must actively protect module boundaries to avoid degrading into a layered monolith with unrestricted dependencies.
