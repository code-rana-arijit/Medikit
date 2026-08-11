import { useEffect, useState } from 'react';
import DashboardLayout, { adminNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Card, Button, Input, Select, Spinner, EmptyState, Alert, StatusBadge } from '../../components/ui';
import { Gift, Copy, Check } from 'lucide-react';
import { useCopy } from '../../lib/analytics';

const emptyForm = { userId: '', discountType: 'PERCENTAGE', discountAmount: 100, percentage: 10, title: '', validForDays: 30, firstOrderOnly: false };

export default function AdminDiscounts() {
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [result, setResult] = useState(null);
  const [recent, setRecent] = useState([]);
  const [copied, copy] = useCopy();

  const loadRecent = () => {
    api.get('/discounts/my')
      .then((r) => setRecent(r?.content || []))
      .catch(() => {});
  };

  useEffect(loadRecent, []);

  const issue = async () => {
    setSaving(true); setError(''); setOk(''); setResult(null);
    try {
      const res = await api.post('/discounts/issue', {
        userId: form.userId,
        discountType: form.discountType,
        discountAmount: Number(form.discountAmount),
        percentage: Number(form.percentage),
        title: form.title || undefined,
        firstOrderOnly: form.firstOrderOnly,
        validForDays: Number(form.validForDays),
      });
      setResult(res);
      setOk('Coupon issued to user');
      loadRecent();
    } catch (e) { setError(e.message); } finally { setSaving(false); }
  };

  return (
    <DashboardLayout title="Issue Coupons" subtitle="Platform control" navItems={adminNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="mb-4 flex items-center gap-2 font-bold text-slate-900"><Gift className="h-5 w-5 text-brand-600" /> Issue a discount code</h2>
          <div className="space-y-3">
            <Input label="User ID (UUID)" value={form.userId} onChange={(e) => setForm({ ...form, userId: e.target.value })} placeholder="Target customer user id" />
            <Select label="Discount type" value={form.discountType} onChange={(e) => setForm({ ...form, discountType: e.target.value })}>
              <option value="PERCENTAGE">Percentage</option>
              <option value="FIXED">Fixed amount</option>
            </Select>
            {form.discountType === 'PERCENTAGE'
              ? <Input label="Percentage" type="number" value={form.percentage} onChange={(e) => setForm({ ...form, percentage: e.target.value })} />
              : <Input label="Discount amount (₹)" type="number" value={form.discountAmount} onChange={(e) => setForm({ ...form, discountAmount: e.target.value })} />}
            <Input label="Title (shown to customer)" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} placeholder="e.g. Customer support credit" />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Valid for (days)" type="number" value={form.validForDays} onChange={(e) => setForm({ ...form, validForDays: e.target.value })} />
              <label className="flex items-end gap-2 pb-2.5 text-sm font-medium text-slate-700">
                <input type="checkbox" checked={form.firstOrderOnly} onChange={(e) => setForm({ ...form, firstOrderOnly: e.target.checked })} className="h-4 w-4 rounded accent-brand-600" />
                First order only
              </label>
            </div>
            <Button className="w-full" loading={saving} onClick={issue}>Issue coupon</Button>
          </div>
          {result && (
            <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
              <p className="text-sm text-emerald-800">Code <code className="rounded bg-white px-2 py-0.5 font-mono font-bold">{result.code}</code></p>
              <p className="mt-1 text-xs text-emerald-700">
                {result.discountType === 'PERCENTAGE' ? `${result.percentage}% off` : `${fmtINR(result.discountAmount)} off`}
                {' · status '}<StatusBadge status={result.status} />
                {result.expiresAt && <> · expires {new Date(result.expiresAt).toLocaleString('en-IN')}</>}
              </p>
              <Button size="sm" variant="secondary" className="mt-2" onClick={() => copy(result.code)}>{copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />} Copy</Button>
            </div>
          )}
        </Card>

        <div>
          <h2 className="mb-4 font-bold text-slate-900">Your issued codes</h2>
          {recent.length === 0 ? (
            <EmptyState icon={Gift} title="No codes yet" subtitle="Codes issued here will be listed for the target user." />
          ) : (
            <div className="space-y-2">
              {recent.slice(0, 20).map((c) => (
                <Card key={c.code} className="flex items-center justify-between p-4">
                  <div>
                    <code className="font-mono text-sm font-bold text-slate-800">{c.code}</code>
                    <p className="text-xs text-slate-500">
                      {c.discountType === 'PERCENTAGE' ? `${c.percentage}% off` : `${fmtINR(c.discountAmount)} off`} · {c.title || '—'}
                    </p>
                  </div>
                  <StatusBadge status={c.status} />
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
