#!/usr/bin/env python3
"""Mechastron Watchman & Webhook Monitor.

Monitors long-running builds, agents, or CLI runs in the background.
When the job finishes or if an error is encountered, it triggers a webhook alert
instead of active conversational polling loops, saving tokens and compute.
"""

import argparse
import json
import os
import subprocess
import sys
import time
import urllib.request


def send_webhook(webhook_url: str, event_type: str, title: str, details: dict):
    payload = {
        "event": event_type,
        "title": title,
        "timestamp": int(time.time()),
        "details": details,
    }
    data = json.dumps(payload).encode("utf-8")

    # Always log alert locally for zero-loss audit
    log_dir = os.path.expanduser("~/.hermes")
    os.makedirs(log_dir, exist_ok=True)
    with open(os.path.join(log_dir, "watchman_alerts.jsonl"), "a") as f:
        f.write(json.dumps(payload) + "\n")

    if webhook_url:
        try:
            req = urllib.request.Request(
                webhook_url,
                data=data,
                headers={"Content-Type": "application/json", "User-Agent": "Mechastron-Watchman/1.0"},
            )
            with urllib.request.urlopen(req, timeout=10) as resp:
                print(f"[Watchman] Webhook delivered: {resp.status}")
        except Exception as e:
            print(f"[Watchman] Webhook delivery failed: {e}", file=sys.stderr)
    else:
        print(f"[Watchman] Event [{event_type}]: {title}")


def monitor_process(pid: int, webhook_url: str, task_name: str):
    print(f"[Watchman] Watching PID {pid} for task '{task_name}'...")
    start_time = time.time()

    while True:
        try:
            # Check if process is still running
            os.kill(pid, 0)
        except OSError:
            # Process has finished
            duration = round(time.time() - start_time, 2)
            send_webhook(
                webhook_url,
                "PROCESS_EXITED",
                f"Task '{task_name}' completed",
                {"pid": pid, "duration_seconds": duration},
            )
            break
        time.sleep(2)


def run_and_notify(command: str, webhook_url: str, task_name: str):
    print(f"[Watchman] Executing and watching: {command}")
    start_time = time.time()
    proc = subprocess.Popen(
        command,
        shell=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    stdout, stderr = proc.communicate()
    duration = round(time.time() - start_time, 2)

    if proc.returncode == 0:
        send_webhook(
            webhook_url,
            "TASK_COMPLETED_SUCCESS",
            f"✓ {task_name} Finished Successfully",
            {"exit_code": 0, "duration": duration, "stdout_tail": stdout[-300:]},
        )
    else:
        send_webhook(
            webhook_url,
            "TASK_FAILED_ERROR",
            f"✗ {task_name} Failed (Exit {proc.returncode})",
            {"exit_code": proc.returncode, "duration": duration, "stderr_tail": stderr[-500:]},
        )
    sys.exit(proc.returncode)


def main():
    parser = argparse.ArgumentParser(description="Mechastron Watchman & Webhook Alert Service")
    parser.add_argument("--pid", type=int, help="PID of background process to watch")
    parser.add_argument("--exec", dest="cmd", type=str, help="Shell command to run and alert on completion")
    parser.add_argument("--webhook", type=str, default=os.getenv("WATCHMAN_WEBHOOK_URL", ""), help="Webhook destination URL")
    parser.add_argument("--name", type=str, default="Mechastron Task", help="Human-readable task label")

    args = parser.parse_args()

    if args.pid:
        monitor_process(args.pid, args.webhook, args.name)
    elif args.cmd:
        run_and_notify(args.cmd, args.webhook, args.name)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
