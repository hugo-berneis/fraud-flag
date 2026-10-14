"""Scores the service's decisions against the generator's ground truth.

Joins tools/generator/output/ground_truth.jsonl (never sent to the service) against the
`decisions` table on transaction_id, then prints precision/recall for the is_fraud call plus
auto-resolve and escalation rates.

Usage:
    python score.py
    python score.py --ground-truth ../tools/generator/output/ground_truth.jsonl
"""

import argparse
import json
import os
from pathlib import Path

import psycopg


def load_ground_truth(path: Path) -> dict:
    ground_truth = {}
    with path.open() as f:
        for line in f:
            row = json.loads(line)
            ground_truth[row["transactionId"]] = row
    return ground_truth


def fetch_decisions(conn, transaction_ids: list[str]) -> dict:
    decisions = {}
    with conn.cursor() as cur:
        cur.execute(
            "select transaction_id, is_fraud, route from decisions where transaction_id = ANY(%s)",
            (transaction_ids,),
        )
        for transaction_id, is_fraud, route in cur.fetchall():
            decisions[transaction_id] = {"isFraud": is_fraud, "route": route}
    return decisions


def score(ground_truth: dict, decisions: dict) -> dict:
    tp = fp = fn = tn = 0
    auto_resolved = escalated = 0
    scored = 0

    for transaction_id, truth in ground_truth.items():
        decision = decisions.get(transaction_id)
        if decision is None:
            continue
        scored += 1

        actual_fraud = truth["isFraud"]
        predicted_fraud = bool(decision["isFraud"])

        if predicted_fraud and actual_fraud:
            tp += 1
        elif predicted_fraud and not actual_fraud:
            fp += 1
        elif not predicted_fraud and actual_fraud:
            fn += 1
        else:
            tn += 1

        if decision["route"] == "REVIEW":
            escalated += 1
        else:
            auto_resolved += 1

    precision = tp / (tp + fp) if (tp + fp) else None
    recall = tp / (tp + fn) if (tp + fn) else None

    return {
        "scored": scored,
        "tp": tp,
        "fp": fp,
        "fn": fn,
        "tn": tn,
        "precision": precision,
        "recall": recall,
        "auto_resolve_rate": auto_resolved / scored if scored else None,
        "escalation_rate": escalated / scored if scored else None,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--ground-truth", default="../tools/generator/output/ground_truth.jsonl")
    parser.add_argument("--db-host", default="localhost")
    parser.add_argument("--db-port", default="5433")
    parser.add_argument("--db-name", default=os.environ.get("POSTGRES_DB", "fraudflag"))
    parser.add_argument("--db-user", default=os.environ.get("POSTGRES_USER", "fraudflag"))
    parser.add_argument("--db-password", default=os.environ.get("POSTGRES_PASSWORD", "changeme"))
    args = parser.parse_args()

    ground_truth = load_ground_truth(Path(args.ground_truth))
    if not ground_truth:
        print(f"No ground truth rows found in {args.ground_truth}")
        return

    conn = psycopg.connect(
        host=args.db_host, port=args.db_port, dbname=args.db_name, user=args.db_user, password=args.db_password
    )
    try:
        decisions = fetch_decisions(conn, list(ground_truth.keys()))
    finally:
        conn.close()

    result = score(ground_truth, decisions)

    print(f"Scored {result['scored']} of {len(ground_truth)} labeled transactions")
    print(f"  TP={result['tp']} FP={result['fp']} FN={result['fn']} TN={result['tn']}")
    print(f"  precision: {fmt(result['precision'])}")
    print(f"  recall: {fmt(result['recall'])}")
    print(f"  auto-resolve rate: {fmt(result['auto_resolve_rate'])}")
    print(f"  escalation rate: {fmt(result['escalation_rate'])}")


def fmt(value):
    return "n/a" if value is None else f"{value:.2%}"


if __name__ == "__main__":
    main()
