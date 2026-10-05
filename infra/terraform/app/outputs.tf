output "vpc_id" {
  value = aws_vpc.main.id
}

output "availability_zones" {
  value = local.availability_zones
}

output "public_app_subnet_ids" {
  value = [
    for subnet in aws_subnet.public_app :
    subnet.id
  ]
}

output "private_db_subnet_ids" {
  value = [
    for subnet in aws_subnet.private_db :
    subnet.id
  ]
}

output "alb_security_group_id" {
  value = aws_security_group.alb.id
}

output "ecs_security_group_id" {
  value = aws_security_group.ecs.id
}

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}

output "rds_address" {
  description = "RDS PostgreSQL hostname"
  value       = aws_db_instance.postgres.address
}

output "rds_port" {
  description = "RDS PostgreSQL port"
  value       = aws_db_instance.postgres.port
}

output "rds_database_name" {
  description = "Application database name"
  value       = aws_db_instance.postgres.db_name
}

output "rds_master_secret_arn" {
  description = "Secrets Manager ARN for the RDS-managed master credentials"
  value       = aws_db_instance.postgres.master_user_secret[0].secret_arn
}

output "backend_ecr_repository_url" {
  description = "ECR repository URL for the Spring Boot backend image"
  value       = aws_ecr_repository.backend.repository_url
}

output "backend_alb_dns_name" {
  description = "DNS name of the backend Application Load Balancer"
  value       = aws_lb.backend.dns_name
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS backend service name"
  value       = aws_ecs_service.backend.name
}

output "backend_image_uri" {
  description = "Bootstrap backend image URI used by Terraform; current release revisions are managed by CD"
  value       = "${aws_ecr_repository.backend.repository_url}:${var.backend_image_tag}"
}

output "backend_log_group_name" {
  description = "CloudWatch log group for the backend container"
  value       = aws_cloudwatch_log_group.backend.name
}