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