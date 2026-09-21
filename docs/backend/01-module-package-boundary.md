# Backend Module / Package Boundary

> **Status:** Accepted MVP Detailed Design  
> **Last updated:** 2026-09-20

## 1. Decision

The backend uses:

```text
Single Spring Boot application
+ single Maven build module
+ package-by-business-module
+ layering inside each module
```

Logical business modules remain:

- `identity` — Identity & Access.
- `assessment` — Assessment Core Domain.
- `group` — Group Supporting Domain.
- `aiintegration` — external LLM technical integration.

Two non-domain areas are added:

- `orchestration` — explicit cross-module application workflows.
- `platform` — genuinely cross-cutting technical configuration only.

MVP deliberately does **not** use global top-level packages such as `controller/`, `service/`, `repository/`, `entity/` or `dto/`.

## 2. Root package shape

Conceptually:

```text
dev.springawsportfolio.portfolio
├── PortfolioApplication
├── identity
│   ├── api
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── web
├── assessment
│   ├── api
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── web
├── group
│   ├── api
│   ├── domain
│   ├── application
│   ├── infrastructure
│   └── web
├── aiintegration
│   ├── adapter
│   ├── provider
│   └── config
├── orchestration
│   ├── assessment
│   ├── group
│   └── web
└── platform
    ├── config
    └── web
```

`PortfolioApplication` stays at the root so standard Spring Boot component scanning reaches all modules.

## 3. Standard business-module layers

| Package | Responsibility |
|---|---|
| `api` | Minimal stable contract available to other modules |
| `domain` | Aggregate, Entity, Value Object, Domain rule/policy, repository abstraction |
| `application` | Use cases, transaction boundaries, authorization, aggregate coordination |
| `infrastructure` | PostgreSQL/JPA/security/external technical implementations |
| `web` | HTTP controllers, request/response DTOs and transport mapping |

A module does not create empty layers merely for symmetry.

## 4. Module public API

Cross-module access must use a deliberately small public surface. Typical stable identifiers include:

```text
identity.api.UserId
assessment.api.AssessmentSessionId
```

A module may expose narrow purpose-specific capabilities and DTOs, for example a `ShareableAssessmentResult` contract. It must not expose internal Aggregate objects or persistence types.

Group may retain an `AssessmentSessionId`, but must not depend on:

```text
assessment.domain.AssessmentSession
assessment.infrastructure.*
assessment.web.*
```

Public module APIs are also privacy boundaries: Group never receives questionnaire answers, private AI interaction context, or unrelated Assessment history.

## 5. Domain package organization

Within `domain`, prefer business concept / Aggregate organization over Java type categories.

Example:

```text
assessment.domain
├── definition
├── session
└── repository

group.domain
├── group
├── membership
├── joinrequest
├── share
└── repository
```

Avoid large generic buckets such as `domain/entity`, `domain/valueobject` and `domain/enum` when those buckets hide business ownership.

## 6. Orchestration boundary

Assessment and Group have legitimate workflows that cross module boundaries. Direct two-way application dependencies would eventually create:

```text
Assessment <-> Group
```

Therefore cross-module user workflows are coordinated by `orchestration`.

`orchestration`:

- owns no Aggregate.
- owns no Entity.
- owns no Repository.
- owns no independent business invariant.
- coordinates already-owned module capabilities in one application transaction when required.

Primary cross-module workflows are:

- Start sharing a completed Assessment result.
- Change the shared Assessment result.
- Delete historical Assessment while ending active Group shares referencing it.
- Read Group-visible shared Assessment results.

Stopping a share remains a pure Group operation because no Assessment read is needed.

## 7. AI Integration boundary

AI Integration is not forced into the same shape as a business Domain module.

Assessment owns an outbound port conceptually such as:

```text
ClarificationAssistant
```

AI Integration implements that port. Assessment does not import provider SDK request/response types, and AI Integration never decides Assessment lifecycle rules.

## 8. Platform boundary

`platform` is intentionally small and may contain items such as:

- Jackson configuration.
- generic Spring configuration.
- HTTP error infrastructure.

It must not become a `common` / `shared` / `utils` dumping ground for business concepts.

## 9. Build-module decision

MVP remains one Maven build artifact. The project does not split into `portfolio-assessment`, `portfolio-group`, etc. yet.

Rationale:

- Current system size does not require physical build isolation.
- Package boundaries + narrow APIs + ArchUnit provide most modularity benefits.
- Multi-module build configuration would add ceremony without solving a current problem.
- Clear logical boundaries make later extraction easier if the project genuinely grows.

## 10. Architecture enforcement

ArchUnit should eventually verify at least:

```text
domain does not depend on web or infrastructure
application does not depend on web
web does not access persistence repositories directly
assessment does not access group internals
group does not access assessment internals
orchestration accesses module public APIs rather than internal packages
@Entity classes reside under infrastructure.persistence
@RestController classes reside under web
```

## 11. Core rule

```text
Package by business capability.
Layer inside the capability.
No module reaches into another module's internals.
```
