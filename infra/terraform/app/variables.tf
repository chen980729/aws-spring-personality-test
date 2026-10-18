variable "aws_region" {
  description = "Primary AWS region for the application workload"
  type        = string
  default     = "ap-northeast-1"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "spring-aws-portfolio"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "prod"
}

variable "vpc_cidr" {
  description = "CIDR block for the application VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "db_name" {
  description = "Initial PostgreSQL database name"
  type        = string
  default     = "portfolio"
}

variable "db_master_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "portfolio"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "backend_image_tag" {
  description = "Immutable ECR image tag deployed to ECS"
  type        = string
}
# Step 8H: controlled Japan-only Bedrock integration.
# Defaults are non-invoking and grant no Bedrock permissions.

variable "bedrock_task_access_enabled" {
  description = "Attach scoped Japan-only Bedrock inference permissions to the ECS application task role"
  type        = bool
  default     = false
}

variable "bedrock_clarification_enabled" {
  description = "Bootstrap ECS task definition AI switch; promoting to the live CD-owned task revision is a separate operator action"
  type        = bool
  default     = false

  validation {
    condition     = !var.bedrock_clarification_enabled || var.bedrock_task_access_enabled
    error_message = "Bedrock clarification cannot be enabled without scoped ECS task role access."
  }
}

variable "bedrock_jp_inference_profile_id" {
  description = "Japan geo inference profile ID confirmed for the chosen model, not a Global/APAC inference profile"
  type        = string
  default     = "jp.anthropic.claude-haiku-4-5-20251001-v1:0"

  validation {
    condition     = startswith(var.bedrock_jp_inference_profile_id, "jp.") && !strcontains(var.bedrock_jp_inference_profile_id, "*")
    error_message = "Only an explicit jp. inference profile ID is allowed."
  }
}

variable "bedrock_foundation_model_id" {
  description = "Foundation model ID underlying the approved Japan inference profile"
  type        = string
  default     = "anthropic.claude-haiku-4-5-20251001-v1:0"

  validation {
    condition     = var.bedrock_jp_inference_profile_id == "jp.${var.bedrock_foundation_model_id}" && !strcontains(var.bedrock_foundation_model_id, "*")
    error_message = "The JP inference profile must match the exact foundation model ID, with no wildcards."
  }
}

variable "bedrock_clarification_max_tokens" {
  description = "Maximum Bedrock output tokens per clarification turn; must remain compatible with the Backend prompt/tool schema"
  type        = number
  default     = 1024

  validation {
    condition     = var.bedrock_clarification_max_tokens >= 1 && var.bedrock_clarification_max_tokens <= 1024
    error_message = "Bedrock clarification output is bounded to 1..1024 tokens for the current policy."
  }
}
