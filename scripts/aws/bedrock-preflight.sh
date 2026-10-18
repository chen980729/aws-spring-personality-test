#!/usr/bin/env bash
# Step 8I-A. Read-only preflight: NO plan/apply, ECS updates, or Bedrock inference.
# Usage: AWS_PROFILE=your-profile bash scripts/aws/bedrock-preflight.sh CLUSTER SERVICE
set -euo pipefail
export AWS_PAGER=""

REGION="ap-northeast-1"
PROFILE_ID="jp.amazon.nova-2-lite-v1:0"
MODEL_ID="amazon.nova-2-lite-v1:0"

if [[ $# -ne 2 || -z "$1" || -z "$2" ]]; then
  echo "Usage: AWS_PROFILE=<profile> bash $0 ECS_CLUSTER ECS_SERVICE" >&2
  exit 2
fi
CLUSTER="$1"
SERVICE="$2"

for tool in aws jq; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Missing required command: $tool" >&2
    exit 2
  fi
done

echo "=== Read-only Nova 2 Lite preflight (Tokyo) ==="
identity="$(aws sts get-caller-identity --output json)"
account="$(jq -er '.Account' <<< "$identity")"
account_suffix="$(printf '%s' "$account" | tail -c 4)"
echo "AWS caller account ends with: ****$account_suffix"
echo "Confirm this is the intended AWS account before any later apply."

profile="$(aws bedrock get-inference-profile \
  --region "$REGION" \
  --inference-profile-identifier "$PROFILE_ID" \
  --output json)"

if ! jq -e \
  --arg profile_id "$PROFILE_ID" \
  --arg model_id "$MODEL_ID" \
  --arg account "$account" \
  '
  .status == "ACTIVE"
  and .type == "SYSTEM_DEFINED"
  and (.inferenceProfileArn | endswith("/" + $profile_id))
  and ((.inferenceProfileArn | split(":")[4]) == $account)
  and ([.models[].modelArn] | length == 2)
  and (([.models[].modelArn | split(":")[3]] | sort)
      == ["ap-northeast-1", "ap-northeast-3"])
  and (([.models[].modelArn | split("/")[1]] | unique)
      == [$model_id])
  ' <<< "$profile" >/dev/null; then
  echo "FAIL: JP profile is unavailable or its account/models/destinations differ." >&2
  echo "STOP: inspect GetInferenceProfile manually before reviewing IAM." >&2
  exit 1
fi
echo "PASS: Nova 2 Lite JP profile ACTIVE; model and Tokyo/Osaka destinations match."

services="$(aws ecs describe-services \
  --region "$REGION" --cluster "$CLUSTER" --services "$SERVICE" \
  --output json)"
if ! jq -e \
  '(.failures | length) == 0 and (.services | length) == 1
   and .services[0].status == "ACTIVE"' \
  <<< "$services" >/dev/null; then
  echo "FAIL: ECS service is missing, not ACTIVE, or lookup failed." >&2
  exit 1
fi

if ! jq -e   '(.services[0].deployments | length) == 1
   and .services[0].deployments[0].status == "PRIMARY"
   and .services[0].deployments[0].rolloutState == "COMPLETED"'   <<< "$services" >/dev/null; then
  echo "FAIL: ECS has an in-progress or incomplete deployment. Wait for stability." >&2
  exit 1
fi

task_arn="$(jq -er '.services[0].taskDefinition' <<< "$services")"
running="$(jq -er '.services[0].runningCount' <<< "$services")"
desired="$(jq -er '.services[0].desiredCount' <<< "$services")"
if (( desired < 1 || running != desired )); then
  echo "FAIL: ECS running/desired task count is not healthy ($running/$desired)." >&2
  exit 1
fi
task="$(aws ecs describe-task-definition \
  --region "$REGION" --task-definition "$task_arn" --output json)"

if ! jq -e \
  '[.taskDefinition.containerDefinitions[] | select(.name == "backend")] | length == 1' \
  <<< "$task" >/dev/null; then
  echo "FAIL: Expected exactly one backend container in active task definition." >&2
  exit 1
fi
task_role="$(jq -er '.taskDefinition.taskRoleArn' <<< "$task")"
execution_role="$(jq -er '.taskDefinition.executionRoleArn' <<< "$task")"
if [[ "$task_role" == "$execution_role" ]]; then
  echo "FAIL: ECS Task Role and Execution Role must be distinct." >&2
  exit 1
fi

read_env() {
  jq -r --arg name "$1" \
    '[.taskDefinition.containerDefinitions[] | select(.name == "backend") |
       .environment[]? | select(.name == $name) | .value][0] // "<unset>"' <<< "$task"
}
feature_flag="$(read_env BEDROCK_CLARIFICATION_ENABLED)"
configured_model="$(read_env BEDROCK_CLARIFICATION_MODEL_ID)"
task_revision="$(printf '%s' "$task_arn" | awk -F: '{print $NF}')"

echo "PASS: ECS service ACTIVE; tasks running: $running/$desired"
echo "Active task revision: $task_revision"
echo "Backend Bedrock flag: $feature_flag"
echo "Backend Bedrock model: $configured_model"
echo "Task Role and Execution Role: distinct"

if [[ "$feature_flag" == "true" ]]; then
  echo "STOP: Bedrock is already enabled on the active task. Investigate first." >&2
  exit 1
fi
if [[ "$configured_model" != "<unset>" && "$configured_model" != "$PROFILE_ID" ]]; then
  echo "STOP: Active task has an unexpected Bedrock model ID." >&2
  exit 1
fi

echo "PASS: Active runtime has no AI enablement."
echo "NEXT: Review Terraform remote-state plan separately."
echo "No AWS resources were modified and no Bedrock inference was called."
