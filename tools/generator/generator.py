"""Synthetic card transaction generator for fraud-flag.

Publishes plain JSON transaction events to Kafka at a configurable rate, with a
mix of normal activity and four injected fraud patterns. Ground-truth labels
(is this fraud, and what type) are written ONLY to a local JSONL file -- never
sent to the service, since the whole point of the project is that the service
has to figure fraud out from the transaction features, not be told the answer.

Usage:
    python generator.py --rate 5 --duration 60
    python generator.py --rate 2 --count 500 --bootstrap-servers localhost:9092

Fraud patterns injected (see inject_* functions below):
    card_testing            many tiny-amount purchases in quick succession
    account_takeover_geo_jump  a transaction from an unfamiliar country + new device
    sudden_large_purchase    one purchase far above the account's usual amount
    velocity_abuse           a burst of normal-looking purchases in a short window
"""

import argparse
import json
import random
import string
import time
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path

from confluent_kafka import Producer

MERCHANTS = [
    "Coffee Shop",
    "Gas Station",
    "Grocery Store",
    "Bookstore",
    "Electronics Store",
    "Online Marketplace",
    "Hardware Store",
    "Pharmacy",
    "Restaurant",
    "Clothing Store",
]

COUNTRIES = ["US", "CA", "GB", "DE", "FR"]
FRAUD_COUNTRIES = ["RU", "NG", "VN", "BR"]

TOPIC = "transactions.raw"


@dataclass
class Account:
    account_id: str
    home_country: str
    device_id: str
    usual_amount: float


def random_account_id() -> str:
    return "acct-" + "".join(random.choices(string.digits, k=6))


def make_accounts(n: int) -> list[Account]:
    accounts = []
    for _ in range(n):
        accounts.append(
            Account(
                account_id=random_account_id(),
                home_country=random.choice(COUNTRIES),
                device_id="device-" + uuid.uuid4().hex[:8],
                usual_amount=round(random.uniform(15, 120), 2),
            )
        )
    return accounts


def new_transaction_id() -> str:
    return "txn-" + uuid.uuid4().hex


def base_transaction(account: Account, now: datetime) -> dict:
    return {
        "transactionId": new_transaction_id(),
        "accountId": account.account_id,
        "amount": round(random.uniform(0.6, 1.4) * account.usual_amount, 2),
        "currency": "USD",
        "merchant": random.choice(MERCHANTS),
        "occurredAt": now.isoformat().replace("+00:00", "Z"),
        "deviceId": account.device_id,
        "geoCountry": account.home_country,
    }


def inject_card_testing(account: Account, now: datetime) -> list[dict]:
    """Several tiny charges to different merchants within a few seconds."""
    events = []
    for i in range(random.randint(4, 6)):
        txn = base_transaction(account, now + timedelta(seconds=i * 2))
        txn["amount"] = round(random.uniform(0.5, 2.0), 2)
        txn["merchant"] = random.choice(MERCHANTS)
        events.append(txn)
    return events


def inject_account_takeover_geo_jump(account: Account, now: datetime) -> list[dict]:
    """A single purchase from an unfamiliar country on a new device."""
    txn = base_transaction(account, now)
    txn["geoCountry"] = random.choice(FRAUD_COUNTRIES)
    txn["deviceId"] = "device-" + uuid.uuid4().hex[:8]
    txn["amount"] = round(random.uniform(50, 300), 2)
    return [txn]


def inject_sudden_large_purchase(account: Account, now: datetime) -> list[dict]:
    """One purchase far above the account's usual spending."""
    txn = base_transaction(account, now)
    txn["amount"] = round(account.usual_amount * random.uniform(10, 40), 2)
    return [txn]


def inject_velocity_abuse(account: Account, now: datetime) -> list[dict]:
    """A burst of normal-amount purchases in a short window."""
    events = []
    for i in range(random.randint(6, 10)):
        events.append(base_transaction(account, now + timedelta(seconds=i * 3)))
    return events


FRAUD_PATTERNS = {
    "card_testing": inject_card_testing,
    "account_takeover_geo_jump": inject_account_takeover_geo_jump,
    "sudden_large_purchase": inject_sudden_large_purchase,
    "velocity_abuse": inject_velocity_abuse,
}


def delivery_callback(err, msg):
    if err is not None:
        print(f"Delivery failed for {msg.key()}: {err}")


def run(args):
    producer = Producer({"bootstrap.servers": args.bootstrap_servers})
    accounts = make_accounts(args.accounts)
    ground_truth_path = Path(args.ground_truth_file)
    ground_truth_path.parent.mkdir(parents=True, exist_ok=True)

    sent = 0
    start = time.monotonic()
    interval = 1.0 / args.rate

    with ground_truth_path.open("w") as ground_truth_file:
        while True:
            if args.count and sent >= args.count:
                break
            if args.duration and (time.monotonic() - start) >= args.duration:
                break

            account = random.choice(accounts)
            now = datetime.now(timezone.utc)

            if random.random() < args.fraud_rate:
                fraud_type = random.choice(list(FRAUD_PATTERNS))
                events = FRAUD_PATTERNS[fraud_type](account, now)
            else:
                fraud_type = None
                events = [base_transaction(account, now)]

            for event in events:
                producer.produce(
                    TOPIC,
                    key=event["accountId"],
                    value=json.dumps(event),
                    callback=delivery_callback,
                )
                ground_truth_file.write(
                    json.dumps(
                        {
                            "transactionId": event["transactionId"],
                            "accountId": event["accountId"],
                            "isFraud": fraud_type is not None,
                            "fraudType": fraud_type,
                        }
                    )
                    + "\n"
                )
                sent += 1

            producer.poll(0)
            time.sleep(interval)

    producer.flush()
    print(f"Sent {sent} transactions. Ground truth written to {ground_truth_path}")


def parse_args():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--bootstrap-servers", default="localhost:9092")
    parser.add_argument("--rate", type=float, default=2.0, help="transactions/sec (normal ticks; fraud patterns can send bursts)")
    parser.add_argument("--duration", type=float, default=None, help="stop after this many seconds")
    parser.add_argument("--count", type=int, default=200, help="stop after sending this many events (ignored if --duration is set)")
    parser.add_argument("--accounts", type=int, default=20, help="number of synthetic accounts to simulate")
    parser.add_argument("--fraud-rate", type=float, default=0.08, help="probability each tick injects a fraud pattern instead of a normal transaction")
    parser.add_argument("--ground-truth-file", default="output/ground_truth.jsonl")
    return parser.parse_args()


if __name__ == "__main__":
    run(parse_args())
