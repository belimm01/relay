#!/usr/bin/env python3

import argparse
import json
import os
import socket
import threading
import time
import urllib.error
import urllib.request
from collections import deque
from http.server import BaseHTTPRequestHandler, HTTPServer
from typing import Any, cast


ENDPOINT_NAME = "Demo orders"
DESTINATION_NAME = "Demo order processor"
MAX_BODY_BYTES = 64 * 1024
MAX_RECORDED_DELIVERIES = 100
PAYLOADS = [
    {"type": "order.created", "orderId": "order-1042", "amount": 12900, "currency": "EUR"},
    {"type": "order.created", "orderId": "order-1043", "amount": 7450, "currency": "EUR"},
    {"type": "order.created", "orderId": "order-1044", "amount": 21900, "currency": "EUR"},
]


class Receiver(HTTPServer):
    allow_reuse_address = True

    def __init__(self, address):
        self.deliveries = deque(maxlen=MAX_RECORDED_DELIVERIES)
        self.delivery_lock = threading.Lock()
        super().__init__(address, ReceiverHandler)


class ReceiverHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self.send_error(400)
            return
        if length < 0 or length > MAX_BODY_BYTES:
            self.send_error(413)
            return
        self.connection.settimeout(5)
        try:
            body = self.rfile.read(length)
        except socket.timeout:
            self.send_error(408)
            return
        if len(body) != length:
            self.send_error(400)
            return
        try:
            payload = json.loads(body)
        except json.JSONDecodeError:
            self.send_error(400)
            return
        receiver = cast(Receiver, self.server)
        with receiver.delivery_lock:
            receiver.deliveries.append(payload)
        self.send_response(204)
        self.end_headers()

    def log_message(self, format, *args):
        return


class RelayClient:
    def __init__(self, base_url, api_key):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key

    def request(self, method, path, payload=None, authenticated=True) -> Any:
        body = json.dumps(payload).encode() if payload is not None else None
        headers = {"Content-Type": "application/json"}
        if authenticated:
            headers["X-Api-Key"] = self.api_key
        request = urllib.request.Request(
            f"{self.base_url}{path}", data=body, headers=headers, method=method
        )
        try:
            with urllib.request.urlopen(request, timeout=5) as response:
                response_body = response.read()
                return json.loads(response_body) if response_body else None
        except urllib.error.HTTPError as error:
            detail = error.read().decode(errors="replace")
            raise RuntimeError(f"{method} {path} returned {error.code}: {detail}") from error


def wait_for_api(client, timeout=60):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            client.request("GET", "/api/endpoints")
            return
        except (OSError, RuntimeError):
            time.sleep(1)
    raise RuntimeError("Relay API did not become ready within 60 seconds")


def reset_demo(client):
    for endpoint in client.request("GET", "/api/endpoints"):
        if endpoint["name"] == ENDPOINT_NAME:
            client.request("DELETE", f"/api/endpoints/{endpoint['id']}")


def seed_demo(client, target_url):
    reset_demo(client)
    endpoint = client.request("POST", "/api/endpoints", {"name": ENDPOINT_NAME})
    client.request(
        "POST",
        f"/api/endpoints/{endpoint['id']}/destinations",
        {"name": DESTINATION_NAME, "targetUrl": target_url, "filterExpr": ""},
    )
    for payload in PAYLOADS:
        client.request("POST", endpoint["ingestPath"], payload, authenticated=False)
    return endpoint


def wait_for_delivery(client, endpoint_id, receiver, timeout: float = 30, poll_interval: float = 0.5):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        events = client.request("GET", f"/api/endpoints/{endpoint_id}/events?size=10")["items"]
        statuses = []
        for event in events:
            detail = client.request("GET", f"/api/events/{event['id']}")
            statuses.extend(delivery["status"] for delivery in detail["deliveries"])
        with receiver.delivery_lock:
            delivered_payloads = sorted(
                json.dumps(item, sort_keys=True, separators=(",", ":"))
                for item in receiver.deliveries
            )
        expected_payloads = sorted(
            json.dumps(item, sort_keys=True, separators=(",", ":")) for item in PAYLOADS
        )
        if len(events) == len(PAYLOADS) and statuses == ["DELIVERED"] * len(PAYLOADS):
            if delivered_payloads == expected_payloads:
                return
        time.sleep(poll_interval)
    raise RuntimeError("Demo events were not delivered within 30 seconds")


def main():
    parser = argparse.ArgumentParser(description="Seed and verify the Relay product walkthrough")
    parser.add_argument("--exit-after-seed", action="store_true")
    args = parser.parse_args()

    api_url = os.getenv("RELAY_API_URL", "http://localhost:8080")
    dashboard_url = os.getenv("RELAY_DASHBOARD_URL", "http://localhost:5173")
    api_key = os.getenv("RELAY_BOOTSTRAP_API_KEY", "rk_local_dev_key")
    receiver_port = int(os.getenv("RELAY_DEMO_RECEIVER_PORT", "9876"))
    target_url = os.getenv(
        "RELAY_DEMO_TARGET_URL",
        f"http://host.docker.internal:{receiver_port}/webhooks/orders",
    )

    receiver = Receiver(("0.0.0.0", receiver_port))
    receiver_thread = threading.Thread(target=receiver.serve_forever, daemon=True)
    receiver_thread.start()
    client = RelayClient(api_url, api_key)

    try:
        wait_for_api(client)
        endpoint = seed_demo(client, target_url)
        wait_for_delivery(client, endpoint["id"], receiver)
        print(f"Demo ready: {len(PAYLOADS)} orders captured and delivered")
        print(f"Dashboard: {dashboard_url}/endpoints/{endpoint['id']}")
        if not args.exit_after_seed:
            print("Receiver is running for replay; press Ctrl-C to stop")
            while True:
                time.sleep(3600)
    except KeyboardInterrupt:
        pass
    finally:
        receiver.shutdown()
        receiver.server_close()


if __name__ == "__main__":
    main()