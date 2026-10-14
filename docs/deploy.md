# Deploying fraud-flag to AWS (single EC2 instance + docker compose)

This is the cheapest option in the spec: one small EC2 instance running the same
`docker-compose.yml` you use locally, plus a `docker-compose.prod.yml` override that ships logs
to CloudWatch. ECS Fargate is a stretch goal, not covered here.

**Claude writes the configs below. Hugo runs every command that creates a billable resource.**
Before doing anything else:

1. **Set a billing alarm.** AWS Console -> Billing -> Budgets -> create a budget alert (e.g. $10).
   This instance is small but leaving it running for weeks will still cost money.

## 1. IAM role for the instance

The app's container needs permission to write to CloudWatch Logs. Create a role so you don't have
to put AWS credentials inside the instance or the repo.

1. IAM Console -> Roles -> Create role -> AWS service -> EC2.
2. Skip the AWS-managed policy list, attach an inline policy instead, and paste in
   [`deploy/iam-cloudwatch-policy.json`](../deploy/iam-cloudwatch-policy.json).
3. Name it `fraud-flag-ec2-role` and create it.

## 2. Launch the instance

- AMI: Amazon Linux 2023
- Instance type: `t3.small` (Postgres + Kafka + the app JVM together need more than the free-tier
  `t3.micro`'s 1 GB RAM; `t3.small`'s 2 GB is the realistic minimum for a demo)
- IAM instance profile: `fraud-flag-ec2-role` (from step 1)
- Security group: allow inbound TCP 22 (SSH, restrict to your IP) and TCP 8080 (the app) from
  your IP or 0.0.0.0/0 for a demo. Don't expose 5433/9092 publicly -- Postgres and Kafka only
  need to be reachable from the app container, which happens over the docker-compose network.
- Storage: default 8 GB gp3 is enough.
- User data: paste [`deploy/ec2-user-data.sh`](../deploy/ec2-user-data.sh) into Advanced details ->
  User data. It installs Docker and the Compose plugin; it does not start the app.

## 3. Copy the repo and secrets over

From your machine, once the instance has a public IP/DNS:

```bash
scp -r -i <your-key>.pem . ec2-user@<instance-public-dns>:~/fraud-flag
# then, since .env is gitignored and wasn't copied by git, copy it explicitly:
scp -i <your-key>.pem .env ec2-user@<instance-public-dns>:~/fraud-flag/.env
```

(A `git clone` of the repo plus a separate `scp` of just `.env` works just as well if the repo is
pushed somewhere the instance can reach.)

## 4. Start the stack

SSH in (`ssh -i <your-key>.pem ec2-user@<instance-public-dns>`), then:

```bash
cd fraud-flag
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Give it a minute, then confirm:

```bash
curl localhost:8080/health
```

## 5. Confirm logs are in CloudWatch

```bash
aws logs tail /fraud-flag/app --follow
```

or AWS Console -> CloudWatch -> Log groups -> `/fraud-flag/app`. You should see the same JSON
lines (`txnId`, `route`, `confidence`, `latencyMs`) you see locally in `docker compose logs app`.

## 6. Run the generator against the deployed instance

From your machine (not the instance), point the generator at the instance's public IP and the
Kafka port you opened (if you choose to expose it for a demo -- otherwise run the generator from
inside the instance over SSH, which is simpler and doesn't require opening 9092 publicly):

```bash
ssh -i <your-key>.pem ec2-user@<instance-public-dns>
cd fraud-flag/tools/generator
python3 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt
python generator.py --rate 5 --count 100
```

## Tearing it down

Run these yourself -- all of them cost money until they're gone:

```bash
# on the instance
sudo docker compose -f docker-compose.yml -f docker-compose.prod.yml down -v

# then, from your machine:
# 1. EC2 Console -> Instances -> select the instance -> Terminate
# 2. CloudWatch Console -> Log groups -> delete /fraud-flag/app (logs stop being billed once deleted)
# 3. IAM Console -> Roles -> delete fraud-flag-ec2-role (free, but good hygiene)
# 4. Double-check EC2 Console -> Volumes / Elastic IPs for anything left over and still billing
```

## Known limitations

- Single instance, no redundancy -- this is a demo deploy, not production.
- Kafka and Postgres data live only on the instance's local disk (the `pgdata` named volume);
  terminating the instance loses them. Fine for a demo, not for anything you need to keep.
- `docker-compose.prod.yml`'s `awslogs` driver only ships the `app` container's logs, not
  Postgres's or Kafka's -- those aren't the service Project 4 needs to monitor.
