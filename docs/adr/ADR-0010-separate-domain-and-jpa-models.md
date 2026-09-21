# ADR-0010: Separate Domain Model from JPA Persistence Model

- Status: Accepted
- Date: 2026-09-20

## Context

Assessment and Group contain lifecycle rules, immutable value objects, multiple Aggregate Roots, retained history and cross-Aggregate workflows. Mapping these directly as JPA entities would make Hibernate concerns influence Aggregate design and business behavior.

## Decision

Keep Domain models as plain Java and create separate JPA persistence entities under Infrastructure. Domain Repository abstractions are implemented by JPA adapters; persistence mappers translate between the models.

## Consequences

### Benefits

- Domain behavior is independent of Hibernate/Spring Data.
- Aggregate boundaries are not defined by ORM associations.
- Domain tests need no Spring or database.
- Database/JSON schemas may evolve separately from Domain refactors.

### Costs

- Additional mappers and adapter code.
- Persistence reconstruction requires explicit design.
