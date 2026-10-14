#!/usr/bin/env python3
"""
GitHub Actions CI Watchdog for Mechastron Android APK Builds.
Monitors workflow runs, tracks progress, and logs completion events to watchman.
"""

import os
import sys
import time
import json
import urllib.request
import urllib.error

ALERT_LOG = os.path.expanduser("~/.hermes/watchman_alerts.jsonl")

def get_token():
    env_file = os.path.expanduser("~/.hermes/.env")
    if os.path.exists(env_file):
        with open(env_file) as f:
            for line in f:
                if line.startswith("GITHUB_TOKEN="):
                    return line.strip().split("=", 1)[1].strip("\"'")
    return os.environ.get("GITHUB_TOKEN")

def log_event(event_type, details):
    os.makedirs(os.path.dirname(ALERT_LOG), exist_ok=True)
    payload = {
        "timestamp": time.time(),
        "event": event_type,
        "details": details
    }
    with open(ALERT_LOG, "a") as f:
        f.write(json.dumps(payload) + "\n")
    print(f"[{time.strftime('%H:%M:%S')}] [{event_type}] {details}")

def main():
    token = get_token()
    if not token:
        print("Error: No GITHUB_TOKEN found.")
        sys.exit(1)

    headers = {
        "Authorization": f"token {token}",
        "User-Agent": "Mechastron-CI-Watchdog",
        "Accept": "application/vnd.github.v3+json"
    }

    print("[Watchdog] Starting Mechastron APK CI Watchdog...")
    current_run_id = None

    while True:
        try:
            req = urllib.request.Request(
                "https://api.github.com/repos/sarvesh173/mechastron/actions/runs?per_page=1",
                headers=headers
            )
            with urllib.request.urlopen(req) as resp:
                data = json.loads(resp.read().decode())
                runs = data.get("workflow_runs", [])
                if not runs:
                    time.sleep(10)
                    continue

                latest = runs[0]
                run_id = latest["id"]
                status = latest["status"]
                conclusion = latest.get("conclusion")
                commit_msg = latest.get("head_commit", {}).get("message", "").split("\n")[0]

                if current_run_id != run_id:
                    current_run_id = run_id
                    log_event("CI_RUN_DETECTED", f"Run #{run_id} ({commit_msg}) - Status: {status}")

                if status == "completed":
                    if conclusion == "success":
                        log_event("APK_BUILD_SUCCESS", f"Run #{run_id} finished successfully! APK artifact is ready.")
                    else:
                        log_event("APK_BUILD_FAILED", f"Run #{run_id} concluded with: {conclusion}")
                    break
                else:
                    print(f"[{time.strftime('%H:%M:%S')}] Run #{run_id} is {status}...")
        except Exception as e:
            print(f"[Watchdog Error] {e}")

        time.sleep(15)

if __name__ == "__main__":
    main()
