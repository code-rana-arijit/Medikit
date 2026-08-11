import { useState } from 'react';

export function useCopy() {
  const [copied, setCopied] = useState(false);
  const copy = async (text) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch { /* clipboard unavailable */ }
  };
  return [copied, copy];
}

export const STATUS_COLORS = {
  PENDING: '#f59e0b',
  CONFIRMED: '#0ea5e9',
  PROCESSING: '#0ea5e9',
  SHIPPED: '#8b5cf6',
  IN_TRANSIT: '#8b5cf6',
  DELIVERED: '#10b981',
  COMPLETED: '#10b981',
  CANCELLED: '#f43f5e',
  FAILED: '#f43f5e',
  CLAIMED: '#f59e0b',
  PICKED_UP: '#0ea5e9',
  REFUNDED: '#64748b',
  PAID: '#10b981',
  PLACED: '#f59e0b',
  OUT_FOR_DELIVERY: '#8b5cf6',
  CREATED: '#f59e0b',
  PENDING_PAYMENT: '#f59e0b',
};

export function groupBy(arr, keyFn) {
  return arr.reduce((acc, item) => {
    const k = keyFn(item);
    acc[k] = (acc[k] || 0) + 1;
    return acc;
  }, {});
}

export function sumBy(arr, keyFn) {
  return arr.reduce((s, item) => s + Number(keyFn(item) || 0), 0);
}

export function dailyTrend(arr, dateKey, valueKey) {
  const map = {};
  arr.forEach((item) => {
    const day = new Date(item[dateKey]).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
    map[day] = (map[day] || 0) + Number(item[valueKey] || 0);
  });
  return Object.entries(map).map(([date, value]) => ({ date, value }));
}

export function topItems(items, valueKey, labelKey, n = 5) {
  const map = {};
  items.forEach((item) => {
    const label = item[labelKey] || 'Unknown';
    map[label] = (map[label] || 0) + Number(item[valueKey] || 0);
  });
  return Object.entries(map)
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, n);
}
