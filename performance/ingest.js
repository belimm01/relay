import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    ingest: {
      executor: "constant-arrival-rate",
      rate: Number(__ENV.RATE || 20),
      timeUnit: "1s",
      duration: __ENV.DURATION || "30s",
      preAllocatedVUs: 10,
      maxVUs: 100,
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(99)<250"],
  },
};

const base = __ENV.RELAY_URL || "http://localhost:8080";
const token = __ENV.INGEST_TOKEN;

export default function () {
  if (!token) throw new Error("INGEST_TOKEN is required");
  const response = http.post(`${base}/in/${token}`, JSON.stringify({type: "load.test", sentAt: Date.now()}), {headers: {"Content-Type": "application/json"}});
  check(response, {"accepted": (r) => r.status === 202});
  sleep(0.01);
}
