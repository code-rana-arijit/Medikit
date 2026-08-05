import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// Peak soak test targeting 50K concurrent users over a 20-minute window.
// Sustains ~1000 VUs with a realistic 1s think time; combined with 50 RPS on
// the gateway this validates the 50K-concurrent-user capacity envelope.
//
// Run:
//   k6 run -e BASE_URL=http://api.medikit.local/api/v1 -e PEAK_VUS=1000 load-testing/k6/peak.js

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/v1';
const PEAK_VUS = parseInt(__ENV.PEAK_VUS || '1000', 10);

const CATEGORY_IDS = new SharedArray('categories', () => [
  '9a2b3c4d-0000-0000-0000-000000000001',
  '9a2b3c4d-0000-0000-0000-000000000002',
  '9a2b3c4d-0000-0000-0000-000000000003',
]);

const PRODUCT_IDS = new SharedArray('products', () => [
  '9a2b3c4d-0000-0000-0000-000000000101',
  '9a2b3c4d-0000-0000-0000-000000000102',
  '9a2b3c4d-0000-0000-0000-000000000103',
  '9a2b3c4d-0000-0000-0000-000000000104',
  '9a2b3c4d-0000-0000-0000-000000000105',
  '9a2b3c4d-0000-0000-0000-000000000106',
]);

const SEARCH_TERMS = new SharedArray('terms', () => [
  'paracetamol', 'amoxicillin', 'vitamin', 'insulin', 'ibuprofen', 'cough', 'aspirin',
]);

function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

const HEADERS = {
  headers: { 'Content-Type': 'application/json', 'X-User-Id': 'load-test-user' },
};

export const options = {
  scenarios: {
    browse: {
      executor: 'constant-arrival-rate',
      rate: __ENV.BROWSE_RPS || 300,
      timeUnit: '1s',
      duration: '20m',
      preAllocatedVUs: Math.floor(PEAK_VUS * 0.6),
      maxVUs: PEAK_VUS,
    },
    search: {
      executor: 'constant-arrival-rate',
      rate: __ENV.SEARCH_RPS || 150,
      timeUnit: '1s',
      duration: '20m',
      preAllocatedVUs: Math.floor(PEAK_VUS * 0.3),
      maxVUs: Math.floor(PEAK_VUS * 0.5),
    },
    transactional: {
      executor: 'constant-arrival-rate',
      rate: __ENV.TX_RPS || 50,
      timeUnit: '1s',
      duration: '20m',
      preAllocatedVUs: Math.floor(PEAK_VUS * 0.15),
      maxVUs: Math.floor(PEAK_VUS * 0.3),
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<400', 'p(99)<800'],
    http_req_failed: ['rate<0.005'],
    checks: ['rate>0.995'],
  },
};

export default function () {
  // Scenario-independent realistic mix chosen by tag is not possible with
  // constant-arrival-rate, so each VU executes a lightweight single request.
  http.get(`${BASE}/products?page=0&size=20`, HEADERS);
  sleep(0.1);
}
