# ADR-0015: Maven, Concrete Root Package, and Local PostgreSQL Baseline

> **Status:** Accepted  
> **Date:** 2026-09-21

## Context

The accepted backend design intentionally left two implementation placeholders open:

- the build tool was described as Maven/Gradle;
- the Java root package was described conceptually as `com.<project>.portfolio`.

Phase 1A also needs a reproducible local PostgreSQL baseline before business implementation begins.

## Decision

For the MVP implementation:

- use **Maven** as the single backend build module;
- use **Maven Wrapper** for reproducible local/CI builds;
- use `dev.springawsportfolio.portfolio` as the concrete Java root package;
- use **PostgreSQL 18** for local development through Docker Compose;
- mount PostgreSQL 18 persistent storage at `/var/lib/postgresql`, matching the official PostgreSQL 18+ image layout.

## Why Maven

Maven fits the current single-module Spring Boot application well:

- dependency and plugin configuration is explicit and conventional;
- the project does not currently need advanced Gradle build logic;
- the learning value is concentrated on Spring/domain/cloud architecture rather than build DSL complexity;
- Maven Wrapper gives developers and CI a consistent Maven runtime.

Gradle remains a valid alternative, but switching build tools does not currently solve a project problem.

## Why this root package

`dev.springawsportfolio.portfolio`:

- avoids leaving `com.example` in a public portfolio project;
- reflects the project identity without implying ownership of an external commercial domain;
- keeps `PortfolioApplication` above all business-module packages for standard Spring component scanning.

If a long-term public namespace is later tied to a personal domain or GitHub identity, the package can be renamed before the repository/API becomes externally depended upon.

## Consequences

- all production packages live below `dev.springawsportfolio.portfolio`;
- ArchUnit imports and package rules use this root;
- Maven becomes part of the implementation baseline and CI/CD design;
- PostgreSQL 18-specific Docker storage layout must be respected;
- this ADR should be revisited only if a concrete build-system or namespace requirement appears.
