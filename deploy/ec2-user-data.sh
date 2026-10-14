#!/bin/bash
# EC2 user-data script for Amazon Linux 2023. Paste this into the "User data" field when
# launching the instance (EC2 console -> Advanced details -> User data). Installs Docker and
# the Docker Compose plugin, and starts the Docker daemon. It does NOT start the app -- that's
# a manual step after you've copied the repo and .env over (see docs/deploy.md), so you get a
# chance to review what's running before it does.
set -euo pipefail

dnf update -y
dnf install -y docker git

systemctl enable docker
systemctl start docker
usermod -aG docker ec2-user

mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
