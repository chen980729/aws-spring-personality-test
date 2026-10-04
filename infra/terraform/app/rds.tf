resource "aws_db_subnet_group" "postgres" {
  name = "${local.name_prefix}-postgres"

  subnet_ids = [
    for subnet in aws_subnet.private_db :
    subnet.id
  ]

  tags = {
    Name = "${local.name_prefix}-postgres"
  }
}

resource "aws_db_instance" "postgres" {
  identifier = "${local.name_prefix}-postgres"

  engine         = "postgres"
  engine_version = "18.6"
  instance_class = var.db_instance_class

  db_name  = var.db_name
  username = var.db_master_username
  port     = 5432

  manage_master_user_password = true

  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_subnet_group_name   = aws_db_subnet_group.postgres.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible = false
  multi_az            = false

  backup_retention_period    = 1
  auto_minor_version_upgrade = true

  deletion_protection = false
  skip_final_snapshot = true

  tags = {
    Name = "${local.name_prefix}-postgres"
  }
}