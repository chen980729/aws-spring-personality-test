output "terraform_state_bucket" {
  description = "S3 bucket storing Terraform remote state"
  value       = aws_s3_bucket.terraform_state.bucket
}
