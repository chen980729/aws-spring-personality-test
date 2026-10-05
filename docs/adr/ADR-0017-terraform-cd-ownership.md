# ADR-0017 — Separate Terraform Infrastructure Ownership from Application CD Revisions

## Status

Accepted.

## Context

After the first AWS deployment, the project needed repeatable application delivery without making every backend/frontend release a Terraform infrastructure change.

The ECS Service is created by Terraform, but every backend release naturally creates a new ECS Task Definition revision with a new immutable ECR image tag. If Terraform continued to own the exact deployed Task Definition revision, a later `terraform plan/apply` could attempt to move the service back toward the bootstrap revision.

At the same time, allowing GitHub Actions to run an unrestricted `terraform apply` for every application release would mix infrastructure provisioning with application delivery and would expose unrelated infrastructure drift to routine deployments.

## Decision

Use an explicit ownership boundary.

Terraform owns:

- networking;
- Security Groups;
- ALB and target group;
- ECS cluster and service baseline;
- bootstrap Task Definition structure;
- RDS;
- ECR repository;
- S3/CloudFront;
- CloudWatch;
- IAM/OIDC;
- infrastructure state.

GitHub Actions CD owns:

- immutable backend image release tags;
- new ECS Task Definition release revisions;
- the ECS Service's currently deployed Task Definition revision;
- frontend build artifacts;
- CloudFront invalidations.

The ECS Service therefore ignores Terraform drift for `task_definition` after the service baseline is created.

CD must not receive broad Terraform or administrator permissions. It receives only the application-deployment AWS actions it needs.

## Consequences

### Positive

- application releases do not require full Terraform apply;
- unrelated Terraform drift cannot block routine application deployment;
- ECR image / Git commit / ECS revision traceability remains clear;
- infrastructure and application delivery responsibilities are explainable;
- CD can use substantially narrower IAM permissions.

### Negative

- Terraform output/state can retain the bootstrap image/Task Definition even while production runs a later CD-created revision;
- operators must understand that AWS runtime revision is authoritative for the currently deployed image;
- changes to Task Definition structure need coordination: Terraform changes the baseline structure, while CD subsequently clones the current deployed revision and replaces the image.

## Alternatives considered

### Terraform deploys every image revision

Rejected for the current project because it couples application releases to infrastructure apply and unrelated provider drift.

### CD directly edits infrastructure broadly

Rejected because it would require excessive IAM permissions and blur infrastructure/application ownership.

### Store a complete Task Definition template only in GitHub Actions

Not selected because it duplicates Terraform-owned runtime configuration and can drift from infrastructure configuration.

## Related operational decision

The first automated ECS rollout also showed that health timing must reflect real Spring Boot startup behavior. The accepted baseline uses a 240-second ECS health-check grace period and a 60-second ALB deregistration delay.

These are operational parameters and can be retuned independently without changing the ownership decision above.
