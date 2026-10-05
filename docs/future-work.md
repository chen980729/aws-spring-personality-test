# Future Work

This document separates **implemented current state** from optional hardening and future product work. Items here are not claims about the deployed system.

## 1. Near-term delivery work

- Final live verification of `main CI success → automatic CD` during the next real feature update.
- Continue documenting deployment changes when the infrastructure baseline changes.
- Keep generated Terraform plans/state/local backend configuration out of version control.

## 2. Cloud and security hardening

### Custom domain and TLS

Options:

1. Route 53 domain + ACM certificate + CloudFront custom domain.
2. Add TLS to the CloudFront → ALB origin connection.
3. Retain the CloudFront default domain for the portfolio MVP.

The current default CloudFront domain is functional; custom-domain work is presentation/security hardening rather than a blocker.

### ALB origin protection

Current restriction uses the AWS-managed CloudFront origin-facing prefix list.

Potential hardening:

- custom secret origin header validated by an ALB listener rule;
- AWS WAF where cost/benefit is justified.

### ECS network isolation

Current: public app subnet + public IP + strict SG ingress.

Alternatives:

- private ECS + NAT Gateway;
- private ECS + VPC endpoints;
- hybrid endpoint/NAT design.

A future change should be driven by cost, outbound dependencies and interview/learning value rather than by copying a reference architecture mechanically.

### Availability

Current portfolio choices:

- one ECS desired task;
- Single-AZ RDS.

Future production-oriented options:

- ECS desired count 2+ across AZs;
- ECS autoscaling;
- RDS Multi-AZ;
- stronger backup/final-snapshot policy.

## 3. Observability and operations

Potential additions:

- CloudWatch alarms for ALB 5xx/target health and ECS failures;
- RDS CPU/storage/connections alarms;
- application metrics and dashboard;
- structured correlation IDs;
- deployment notifications;
- documented incident/rollback runbook.

## 4. CI/CD hardening

- Enable automatic CD after its final real feature-change validation.
- Add a GitHub production Environment and optional manual approval.
- Add browser-based deployment smoke coverage.
- Consider image SBOM, signing/attestation and policy checks.
- Add explicit rollback workflow or ECS deployment circuit-breaker strategy.
- Evaluate ECR lifecycle policies to avoid unlimited image growth.

## 5. Terraform improvements

- Resolve the known CloudFront provider/state canonicalization diff instead of masking it prematurely.
- Keep application release revisions out of Terraform ownership.
- Consider reusable modules only when a second environment or meaningful repetition appears.
- Add a dedicated infrastructure CI plan/check workflow if Terraform change frequency increases.
- Avoid introducing workspaces/modules solely for portfolio complexity.

## 6. Product work

Current deferred product capabilities include:

- provider-backed LLM clarification;
- Group / Membership / Sharing backend;
- Group frontend;
- historical Assessment deletion orchestration;
- result/personality content refinement where required;
- any later account-deletion/anonymization policy.

## 7. LLM provider options

The Assessment domain already isolates provider-specific behavior behind an adapter boundary.

Reasonable future provider strategies:

| Option | Benefits | Trade-offs |
|---|---|---|
| AWS Bedrock | Fits the AWS portfolio story, IAM-native access, multiple model providers | AWS-specific integration and regional/model availability |
| Direct provider API | Simple provider features and documentation | Separate credential/vendor management |
| Multi-provider abstraction | Flexibility and fallback | Premature complexity for MVP |

A single provider implementation behind the existing adapter boundary is preferred for the first LLM milestone. Multi-provider routing should wait for a concrete requirement.

## 8. Testing

Potential next tests:

- one browser E2E happy path: register → login → assessment → result;
- production deployment smoke with browser automation;
- Group/share concurrency tests once Group exists;
- provider adapter contract tests once LLM integration exists.

The project should not chase test count for its own sake. New tests should protect cross-boundary or regression-prone behavior.

## 9. Explicit non-priorities

The following remain intentionally outside the near-term scope unless requirements change:

- microservices;
- Kubernetes/EKS;
- Event Sourcing;
- multi-region active-active deployment;
- centralized enterprise IAM platform;
- large frontend design-system framework;
- generalized multi-environment platform engineering.

These would add substantial complexity without improving the current portfolio learning objective proportionally.
