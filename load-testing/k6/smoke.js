import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// Medikit e-commerce load profile simulating the shopping journey:
// browse catalog -> search -> view product -> add to cart -> checkout.
//
// Base URL is overridable, e.g.:
//   k6 run -e BASE_URL=http://api.medikit.local/api/v1 load-testing/k6/smoke.js

const BASE = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

const CATEGORY_IDS = new SharedArray('categories', () => [
  '9a2b3c4d-0000-0000-0000-000000000001',
  '9a2b3c4d-0000-0000-0000-000000000002',
  '9a2b3c4d-0000-0000-0000-000000000003',
  '9a2b3c4d-0000-0000-0000-000000000004',
  '9a2b3c4d-0000-0000-0000-000000000005',
]);

const PRODUCT_IDS = new SharedArray('products', () => [
  '9a2b3c4d-0000-0000-0000-000000000101',
  '9a2b3c4d-0000-0000-0000-000000000102',
  '9a2b3c4d-0000-0000-0000-000000000103',
  '9a2b3c4d-0000-0000-0000-000000000104',
  '9a2b3c4d-0000-0000-0000-000000000105',
]);

const SEARCH_TERMS = new SharedArray('terms', () => [
  'paracetamol', 'amoxicillin', 'vitamin', 'insulin', 'ibuprofen', 'cough',
]);

function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function authHeaders() {
  return {
    headers: { 'Content-Type': 'application/json', 'X-User-Id': 'test-user' },
  };
}

export const options = {
  stages: [
    { duration: '1m', target: 50 },   // ramp up to 50 VUs
    { duration: '3m', target: 200 },  // climb to 200 VUs
    { duration: '5m', target: 500 },  // sustain peak 500 VUs
    { duration: '2m', target: 200 },  // scale down
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  // 1. Browse catalog
  const browse = http.get(`${BASE}/products?page=0&size=20`, authHeaders());
  check(browse, { 'browse 200': (r) => r.status === 200 });

  // 2. Search (60% of traffic)
  if (Math.random() < 0.6) {
    const q = randomItem(SEARCH_TERMS);
    const search = http.get(`${BASE}/search/products?q=${q}&page=0&size=10`, authHeaders());
    check(search, { 'search 200': (r) => r.status === 200 });
  }

  // 3. View a product (30%)
  if (Math.random() < 0.3) {
    const product = http.get(`${BASE}/products/${randomItem(PRODUCT_IDS)}`, authHeaders());
    check(product, { 'product 200': (r) => r.status === 200 });
  }

  // 4. Add to cart (10% -> ~50 add-to-cart/sec at peak)
  if (Math.random() < 0.1) {
    const add = http.post(
      `${BASE}/cart/items`,
      JSON.stringify({ productId: randomItem(PRODUCT_IDS), quantity: 1 }),
      authHeaders(),
    );
    check(add, { 'add-to-cart accepted': (r) => r.status === 201 || r.status === 200 });
  }

  sleep(1);
}
