locals {
  name_prefix = "${var.project_name}-${var.environment}"

  availability_zones = slice(
    data.aws_availability_zones.available.names,
    0,
    2
  )

  public_app_subnets = {
    az1 = {
      availability_zone = local.availability_zones[0]
      cidr_block        = "10.0.0.0/24"
    }

    az2 = {
      availability_zone = local.availability_zones[1]
      cidr_block        = "10.0.1.0/24"
    }
  }

  private_db_subnets = {
    az1 = {
      availability_zone = local.availability_zones[0]
      cidr_block        = "10.0.20.0/24"
    }

    az2 = {
      availability_zone = local.availability_zones[1]
      cidr_block        = "10.0.21.0/24"
    }
  }
}