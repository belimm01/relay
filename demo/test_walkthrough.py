import threading
import unittest
import urllib.error
import urllib.request
from collections import deque

from walkthrough import MAX_BODY_BYTES, PAYLOADS, Receiver, wait_for_delivery


class DeliveredClient:
    def request(self, method, path):
        if path.startswith("/api/endpoints/"):
            return {"items": [{"id": f"event-{index}"} for index in range(len(PAYLOADS))]}
        return {"deliveries": [{"status": "DELIVERED"}]}


class RecordedReceiver:
    def __init__(self, deliveries):
        self.deliveries = deque(deliveries)
        self.delivery_lock = threading.Lock()


class WaitForDeliveryTest(unittest.TestCase):
    def test_accepts_complete_payloads(self):
        receiver = RecordedReceiver(reversed(PAYLOADS))

        wait_for_delivery(DeliveredClient(), "endpoint", receiver, timeout=0.1, poll_interval=0.001)

    def test_rejects_payloads_with_only_matching_order_ids(self):
        receiver = RecordedReceiver({"orderId": item["orderId"]} for item in PAYLOADS)

        with self.assertRaisesRegex(RuntimeError, "not delivered"):
            wait_for_delivery(DeliveredClient(), "endpoint", receiver, timeout=0.01, poll_interval=0.001)


class ReceiverTest(unittest.TestCase):
    def test_rejects_oversized_delivery(self):
        receiver = Receiver(("127.0.0.1", 0))
        thread = threading.Thread(target=receiver.serve_forever)
        thread.start()
        request = urllib.request.Request(
            f"http://127.0.0.1:{receiver.server_port}/webhooks/orders",
            data=b"x" * (MAX_BODY_BYTES + 1),
            method="POST",
        )

        try:
            with self.assertRaises(urllib.error.HTTPError) as error:
                urllib.request.urlopen(request, timeout=2)
            self.assertEqual(error.exception.code, 413)
        finally:
            receiver.shutdown()
            receiver.server_close()
            thread.join(timeout=2)

        self.assertFalse(thread.is_alive())


if __name__ == "__main__":
    unittest.main()