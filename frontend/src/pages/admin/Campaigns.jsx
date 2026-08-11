import { useEffect, useState } from 'react';
import DashboardLayout, { adminNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Card, Button, Input, Select, Spinner, EmptyState, Alert, StatusBadge } from '../../components/ui';
import { Ticket, Plus, ChevronDown, ChevronUp, Copy, Check } from 'lucide-react';
import { useCopy } from '../../lib/analytics';

const emptyCampaign = {
  name: '',
  description: '',
  discountType: 'PERCENTAGE',
  discountAmount: 100,
  percentage: 10,
  validForDays: 30,
  totalCodes: 100,
  firstOrderOnly: false,
};

export default function AdminCampaigns() {
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(emptyCampaign);
  const [saving, setSaving] = useState(false);
  const [expanded, setExpanded] = useState(null);
  const [codes, setCodes] = useState([]);
  const [copied, copy] = useCopy();

  const load = () => {
    setLoading(true);
    api.get('/campaigns')
      .then((r) => setCampaigns(r?.content || []))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const create = async () => {
    setSaving(true); setError(''); setOk('');
    try {
      await api.post('/campaigns', {
        ...form,
        discountAmount: Number(form.discountAmount),
        percentage: Number(form.percentage),
        validForDays: Number(form.validForDays),
        totalCodes: Number(form.totalCodes),
      });
      setOk('Campaign created — codes will be issued to eligible users');
      setForm(emptyCampaign);
      setOpen(false);
      load();
    } catch (e) { setError(e.message); } finally { setSaving(false); }
  };

  const toggleCodes = async (id) => {
    if (expanded === id) { setExpanded(null); return; }
    setExpanded(id);
    try {
      const r = await api.get(`/campaigns/${id}/codes`);
      setCodes(r?.content || []);
    } catch { setCodes([]); }
  };

  return (
    <DashboardLayout title="Promotions" subtitle="Platform control" navItems={adminNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="mb-4 flex items-center justify-between">
        <p className="text-sm text-slate-500">Create discount campaigns that auto-issue coupon codes to customers.</p>
        <Button onClick={() => setOpen(!open)}>{open ? 'Close' : <><Plus className="h-4 w-4" /> New campaign</>}</Button>
      </div>

      {open && (
        <Card className="mb-6 p-6">
          <h2 className="mb-4 font-bold text-slate-900">Create campaign</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="Campaign name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="e.g. Monsoon Sale" />
            <Select label="Discount type" value={form.discountType} onChange={(e) => setForm({ ...form, discountType: e.target.value })}>
              <option value="PERCENTAGE">Percentage</option>
              <option value="FIXED">Fixed amount</option>
            </Select>
            {form.discountType === 'PERCENTAGE'
              ? <Input label="Percentage" type="number" value={form.percentage} onChange={(e) => setForm({ ...form, percentage: e.target.value })} />
              : <Input label="Discount amount (₹)" type="number" value={form.discountAmount} onChange={(e) => setForm({ ...form, discountAmount: e.target.value })} />}
            <Input label="Valid for (days)" type="number" value={form.validForDays} onChange={(e) => setForm({ ...form, validForDays: e.target.value })} />
            <Input label="Total codes to issue" type="number" value={form.totalCodes} onChange={(e) => setForm({ ...form, totalCodes: e.target.value })} />
            <label className="flex items-end gap-2 pb-2.5 text-sm font-medium text-slate-700">
              <input type="checkbox" checked={form.firstOrderOnly} onChange={(e) => setForm({ ...form, firstOrderOnly: e.target.checked })} className="h-4 w-4 rounded accent-brand-600" />
              First order only
            </label>
            <Input label="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} className="sm:col-span-2" placeholder="Short description shown to customers" />
          </div>
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
            <Button loading={saving} onClick={create}>Create campaign</Button>
          </div>
        </Card>
      )}

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : campaigns.length === 0 ? (
        <EmptyState icon={Ticket} title="No campaigns yet" subtitle="Create a campaign to auto-issue coupon codes." />
      ) : (
        <div className="space-y-3">
          {campaigns.map((c) => (
            <Card key={c.id} className="overflow-hidden">
              <div className="flex flex-wrap items-center justify-between gap-3 p-5">
                <div className="flex items-center gap-4">
                  <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><Ticket className="h-5 w-5" /></span>
                  <div>
                    <p className="font-semibold text-slate-900">{c.name} <StatusBadge status={c.active ? 'ACTIVE' : 'INACTIVE'} /></p>
                    <p className="text-xs text-slate-500">{c.description || 'No description'}</p>
                    <p className="mt-1 text-xs text-slate-400">
                      {c.discountType === 'PERCENTAGE' ? `${c.percentage}% off` : `${fmtINR(c.discountAmount)} off`}
                      {' · '}{c.issuedCodes}/{c.totalCodes} issued · valid {c.validForDays} days
                      {c.firstOrderOnly && ' · first order only'} · created {new Date(c.createdAt).toLocaleDateString('en-IN')}
                    </p>
                  </div>
                </div>
                <Button size="sm" variant="secondary" onClick={() => toggleCodes(c.id)}>
                  {expanded === c.id ? <><ChevronUp className="h-4 w-4" /> Hide codes</> : <><ChevronDown className="h-4 w-4" /> View codes</>}
                </Button>
              </div>
              {expanded === c.id && (
                <div className="border-t border-slate-100 bg-slate-50/60 px-5 py-4">
                  {codes.length === 0 ? (
                    <p className="py-4 text-center text-sm text-slate-400">No codes issued yet.</p>
                  ) : (
                    <div className="max-h-72 space-y-2 overflow-y-auto">
                      {codes.map((code) => (
                        <div key={code.code} className="flex items-center justify-between rounded-xl bg-white px-4 py-2.5">
                          <code className="font-mono text-sm font-bold text-slate-800">{code.code}</code>
                          <div className="flex items-center gap-2">
                            <StatusBadge status={code.status} />
                            <Button size="sm" variant="ghost" onClick={() => copy(code.code)}>{copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}</Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </DashboardLayout>
  );
}
