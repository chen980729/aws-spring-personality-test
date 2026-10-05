data "aws_iam_policy_document" "github_actions_deploy_permissions" {
  statement {
    sid    = "EcrAuthorization"
    effect = "Allow"

    actions = [
      "ecr:GetAuthorizationToken",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "PushBackendImage"
    effect = "Allow"

    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]

    resources = [
      aws_ecr_repository.backend.arn,
    ]
  }

  statement {
    sid    = "DescribeBackendService"
    effect = "Allow"

    actions = [
      "ecs:DescribeServices",
    ]

    resources = [
      aws_ecs_service.backend.id,
    ]
  }

  statement {
    sid    = "DescribeTaskDefinition"
    effect = "Allow"

    actions = [
      "ecs:DescribeTaskDefinition",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "RegisterBackendTaskDefinition"
    effect = "Allow"

    actions = [
      "ecs:RegisterTaskDefinition",
    ]

    resources = [
      "${aws_ecs_task_definition.backend.arn_without_revision}:*",
    ]
  }

  statement {
    sid    = "UpdateBackendService"
    effect = "Allow"

    actions = [
      "ecs:UpdateService",
    ]

    resources = [
      aws_ecs_service.backend.id,
    ]
  }

  statement {
    sid    = "PassEcsTaskRoles"
    effect = "Allow"

    actions = [
      "iam:PassRole",
    ]

    resources = [
      aws_iam_role.ecs_task.arn,
      aws_iam_role.ecs_task_execution.arn,
    ]

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"

      values = [
        "ecs-tasks.amazonaws.com",
      ]
    }
  }

  statement {
    sid    = "ManageFrontendBucket"
    effect = "Allow"

    actions = [
      "s3:GetBucketLocation",
      "s3:ListBucket",
    ]

    resources = [
      aws_s3_bucket.frontend.arn,
    ]
  }

  statement {
    sid    = "DeployFrontendObjects"
    effect = "Allow"

    actions = [
      "s3:PutObject",
      "s3:DeleteObject",
    ]

    resources = [
      "${aws_s3_bucket.frontend.arn}/*",
    ]
  }

  statement {
    sid    = "InvalidateFrontendDistribution"
    effect = "Allow"

    actions = [
      "cloudfront:CreateInvalidation",
      "cloudfront:GetInvalidation",
    ]

    resources = [
      "arn:aws:cloudfront::${data.aws_caller_identity.frontend.account_id}:distribution/*",
    ]

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Project"

      values = [
        "spring-aws-portfolio",
      ]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/Environment"

      values = [
        "prod",
      ]
    }
  }
}

resource "aws_iam_role_policy" "github_actions_deploy" {
  name = "${local.name_prefix}-github-actions-deploy"
  role = aws_iam_role.github_actions_deploy.id

  policy = data.aws_iam_policy_document.github_actions_deploy_permissions.json
}