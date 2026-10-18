# Step 8I — Nova 2 Lite Preflight, Activation, and Live Validation

> Date: 2026-10-18
> Status: **8I-A preparation only**; no AWS resources changed or live inference performed
> Depends on: merged Step 8H, `docs/deployment/bedrock-deployment.md`
> Selected model: `jp.amazon.nova-2-lite-v1:0` (Tokyo source; Tokyo/Osaka inference)

## 1. Deployment gates and ownership

| Gate | Owner | Evidence required | Stop condition |
|---|---|---|---|
| 8I-A: Preflight | Operator, read-only AWS CLI | Correct AWS account, ACTIVE JP profile and exact model/Regions, healthy current ECS Service, Bedrock off | Anything different, unknown AWS account, access denied |
| 8I-B: IAM preparation | Operator and Terraform remote S3 state | Reviewed Terraform plan; exact Task Role policy only; no infrastructure replacement | Unexpected IAM scope, ECS/VPC/RDS replacement, drift not understood |
| 8I-C: Feature activation | Operator-controlled ECS task promotion | Stable Service, same image/secrets/network/logs/roles, only reviewed AI env vars changed | Task fails health or AI initialization |
| 8I-D: Live integration | Authenticated test account, minimal requests | Tool Calling contract, privacy, lifecycle, latency, cost, rollback confirmed | Unsafe output, invalid domain transitions, cost/timeout concerns |

**Never combine 8I-B, 8I-C and 8I-D into an automatic CI pipeline.**
The normal GitHub Actions CD owns application image deployment and preserves
the currently active Task Definition's environment. A Terraform-only merge or
apply does not activate the live ECS Task Definition by itself.

## 2. 8I-A — Required read-only verification

On the operator's local Git Bash or another trusted AWS CLI environment:

```bash
# Explicitly choose the AWS credential profile you already use.
# Do not paste credential values into chat or into this repository.
export AWS_PROFILE="YOUR_EXISTING_AWS_PROFILE"

aws sts get-caller-identity
aws bedrock get-inference-profile \
  --region ap-northeast-1 \
  --inference-profile-identifier jp.amazon.nova-2-lite-v1:0 \
  --query '{status:status,type:type,arn:inferenceProfileArn,models:models[].modelArn}'

# Name examples derive from the current Terraform default name_prefix.
# Verify your actual names in the ECS Console or existing CD Variables.
bash scripts/aws/bedrock-preflight.sh \
  spring-aws-portfolio-prod-cluster \
  spring-aws-portfolio-prod-backend
```

Prerequisites: AWS CLI v2 and `jq`, working **read-only** IAM permissions
for `sts:GetCallerIdentity`, `bedrock:GetInferenceProfile`,
`ecs:DescribeServices`, and `ecs:DescribeTaskDefinition`.

The script intentionally uses only these read-only calls and exits if:
- the JP inference profile is not ACTIVE or no longer maps **exactly** to
  `amazon.nova-2-lite-v1:0` in Tokyo and Osaka;
- the ECS Service is absent/unhealthy, or its backend container cannot be found;
- the Task Role equals Execution Role; or
- the currently active task is already AI-enabled or uses a different model ID.

Do not share the raw `describe-task-definition` payload, since it can contain
database connection information and secret references. Report only the sanitized
PASS/FAIL lines and task revision produced by the script. The masked account
suffix is only a reminder: independently verify the target account yourself.

**Checkpoint:** Review the preflight output before proceeding. `8I-A PASS`
establishes *model profile and deployed configuration*, not working model
invocation, billing eligibility, or permission to enable an application.

## 3. 8I-B — Terraform plan and permission gate (deferred)

Use the **existing S3 remote state backend**, not `-backend=false`, for any
production plan. Reuse the exact backend configuration, AWS credentials,
workspace, and `backend_image_tag` value from the prior successful Terraform
deployment. Do not invent a new state or guess the tag.

Proposed variable overrides for the later reviewed plan:

```text
bedrock_task_access_enabled = true
bedrock_clarification_enabled = false
bedrock_jp_inference_profile_id = "jp.amazon.nova-2-lite-v1:0"
bedrock_foundation_model_id = "amazon.nova-2-lite-v1:0"
```

The IAM policy must permit only `bedrock:InvokeModel` to the exact profile and
the expected Japan foundation model ARNs with `bedrock:InferenceProfileArn`
conditioning the foundation-model resources. Inspect AWS Organization SCPs;
blocking any destination Region may prevent cross-Region inference.

Plan review is required even if `terraform validate` passed in CI.
Watch especially for a new Terraform *baseline* ECS Task Definition revision
and `ecs_service.backend.task_definition` drift. That resource explicitly
ignores changes to the Service's active Task Definition because GitHub CD owns
release promotions. Never use a plan containing unexplained replacements or
unexpected IAM broadening. **Do not apply until this checkpoint is approved.**

## 4. 8I-C — Controlled Task Definition promotion (deferred)

After the IAM gate passes, the operator must make a new revision cloned from
the **currently deployed** ECS Task Definition rather than from the Terraform
bootstrap revision. Change only these reviewed settings:

```text
AWS_REGION = ap-northeast-1
BEDROCK_CLARIFICATION_ENABLED = true
BEDROCK_CLARIFICATION_MODEL_ID = jp.amazon.nova-2-lite-v1:0
BEDROCK_CLARIFICATION_MAX_TOKENS = 1024
BEDROCK_CLARIFICATION_STRICT_TOOL_SCHEMA = false
```

Preserve existing image URI, database secrets, log configuration, ports, task
and execution role ARNs, CPU/memory, networking, and other environment values.
Registering a Task Definition alone does not change the running ECS Service.
An explicit `ecs update-service`, stabilization wait and deployed-revision
verification are required later.

The rollback plan is to create a **new revision** from the active configuration
with only `BEDROCK_CLARIFICATION_ENABLED=false`, update the Service to that
revision, and verify that it stabilizes. Do not rely on source-code revert or
Terraform apply for an immediate feature shutdown.

## 5. 8I-D — Small live-model acceptance matrix (deferred)

Use only a dedicated, authorized test account and invented example
questionnaire responses; do not reuse private user conversations.

| Scenario | Acceptance evidence |
|---|---|
| Nova 2 Lite `Converse` with `toolChoice=any` | Model emits `ask_followup` or `submit_judgement` with valid schema, single bounded action |
| Last allowed model turn | Only `submit_judgement` is accepted, no extra turn |
| Chinese answers | Neutral Chinese follow-up, understandable judgment; avoid overclaiming psychometric accuracy |
| `RESOLVED` and `UNCLEAR` | Domain-valid persisted result and AI provenance; deterministic scoring unchanged |
| Timeout / model error | Bounded latency, `FAILED_RETRYABLE`, manual Retry/Skip; no infinite loop |
| Refresh / 409 | `GET current-turn` recovers without duplicate `POST` |
| Privacy and cost | No raw answer/prompt logs; note observed latency, request count and token usage |
| Rollback | Flag-off revision restores deterministic fallback without losing Assessment data |

Nova 2 Lite supports **client-side Tool Calling**, not Bedrock native Structured
Outputs. Therefore successful schema-shaped tool use is not a substitute for
Application-side validation, and must be tested on the deployed model.

Before any **paid** request, confirm model terms for your actual intended end
users and current AWS account, establish an appropriate AWS Budget alert,
inspect pricing and quota, and explicitly approve a limited smoke-test budget.

## 6. Evidence log

- PR #23 (Step 8H) merged: Terraform/IAM definitions only.
- 8I-A preflight script prepared: **not yet executed against user's AWS account**.
- AWS Terraform plan/apply: **not executed in this checkpoint**.
- ECS feature activation: **not executed**.
- Live Bedrock inference: **not executed**.

Update this section with actual account-checked results and release revisions
only after each gate. Never describe a plan as an applied production change.

## 7. References

- `docs/deployment/bedrock-deployment.md`
- `docs/deployment/ci-cd.md`
- `docs/backend/18-assessment-llm-http-boundary.md`
- `docs/frontend/09-ai-clarification-checkpoint-8gb.md`
- Amazon Bedrock Nova 2 Lite model card and Geographic cross-Region IAM docs.
