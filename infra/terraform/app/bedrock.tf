# Japan-only Bedrock inference for the assessment clarification adapter.
# Permission is opt-in; runtime enablement is a separate deployment decision.
#
# The JP inference profile routes only to Tokyo/Osaka for the selected model.
# Do not broaden this policy to the APAC or Global inference profiles.

data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

locals {
  bedrock_jp_inference_profile_arn = "arn:${data.aws_partition.current.partition}:bedrock:${var.aws_region}:${data.aws_caller_identity.current.account_id}:inference-profile/${var.bedrock_jp_inference_profile_id}"

  bedrock_jp_foundation_model_arns = [
    for region in ["ap-northeast-1", "ap-northeast-3"] :
    "arn:${data.aws_partition.current.partition}:bedrock:${region}::foundation-model/${var.bedrock_foundation_model_id}"
  ]
}

data "aws_iam_policy_document" "ecs_bedrock_jp" {
  statement {
    sid    = "InvokeApprovedJapanInferenceProfile"
    effect = "Allow"

    actions = [
      "bedrock:InvokeModel"
    ]

    resources = [
      local.bedrock_jp_inference_profile_arn
    ]
  }

  statement {
    sid    = "InvokeJapanFoundationModelsOnlyViaApprovedProfile"
    effect = "Allow"

    actions = [
      "bedrock:InvokeModel"
    ]

    resources = local.bedrock_jp_foundation_model_arns

    condition {
      test     = "StringEquals"
      variable = "bedrock:InferenceProfileArn"
      values   = [local.bedrock_jp_inference_profile_arn]
    }
  }

  statement {
    sid    = "DescribeApprovedJapanInferenceProfile"
    effect = "Allow"

    actions = [
      "bedrock:GetInferenceProfile"
    ]

    resources = [
      local.bedrock_jp_inference_profile_arn
    ]
  }
}

resource "aws_iam_role_policy" "ecs_bedrock_jp" {
  count = var.bedrock_task_access_enabled ? 1 : 0

  name = "${local.name_prefix}-bedrock-jp"
  role = aws_iam_role.ecs_task.id

  policy = data.aws_iam_policy_document.ecs_bedrock_jp.json
}
