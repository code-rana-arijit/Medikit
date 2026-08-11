import { useEffect, useState } from 'react';
import DashboardLayout, { pharmacyNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Card, Button, Input, Spinner, EmptyState, Alert, Badge } from '../../components/ui';
import { CalendarClock, Plus, Clock } from 'lucide-react';

const emptySlot = { pharmacyId: '', startTime: '', endTime: '', capacity: 10 };

export default function PharmacySlots() {
  const [pharmacyId, setPharmacyId] = useState('');
  const [slots, setSlots] = useState([]);
  const [form, setForm] = useState(emptySlot);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  const load = async () => {
    if (!pharmacyId) { setError('Enter a pharmacy ID first'); return; }
    setError(''); setLoading(true);
    const from = new Date().toISOString();
    const to = new Date(Date.now() + 14 * 24 * 3600 * 1000).toISOString();
    try {
      const r = await api.get(`/delivery/slots?pharmacyId=${pharmacyId}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
      setSlots(r || []);
    } catch (e) { setError(e.message); setSlots([]); }
    setLoading(false);
  };

  useEffect(() => { if (pharmacyId) load(); }, [pharmacyId]);

  const create = async () => {
    setSaving(true); setError(''); setOk('');
    try {
      await api.post('/delivery/slots', {
        pharmacyId: form.pharmacyId,
        startTime: new Date(form.startTime).toISOString(),
        endTime: new Date(form.endTime).toISOString(),
        capacity: Number(form.capacity),
      });
      setOk('Delivery slot created');
      setForm(emptySlot);
      if (form.pharmacyId) setPharmacyId(form.pharmacyId);
      load();
    } catch (e) { setError(e.message); } finally { setSaving(false); }
  };

  const available = slots.filter((s) => s.active);

  return (
    <DashboardLayout title="Delivery Slots" subtitle="Store operations" navItems={pharmacyNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="grid gap-6 lg:grid-cols-3">
        <Card className="p-6 lg:col-span-1">
          <div className="mb-4 flex items-center gap-2"><Plus className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Create slot</h2></div>
          <div className="space-y-3">
            <Input label="Pharmacy ID" value={form.pharmacyId} onChange={(e) => setForm({ ...form, pharmacyId: e.target.value })} placeholder="UUID" />
            <Input label="Start time" type="datetime-local" value={form.startTime} onChange={(e) => setForm({ ...form, startTime: e.target.value })} />
            <Input label="End time" type="datetime-local" value={form.endTime} onChange={(e) => setForm({ ...form, endTime: e.target.value })} />
            <Input label="Capacity" type="number" value={form.capacity} onChange={(e) => setForm({ ...form, capacity: e.target.value })} />
            <Button className="w-full" loading={saving} onClick={create}>Create slot</Button>
          </div>
        </Card>

        <div className="lg:col-span-2">
          <div className="mb-4 flex items-end gap-3">
            <Input label="Pharmacy ID" value={pharmacyId} onChange={(e) => setPharmacyId(e.target.value)} placeholder="UUID" className="max-w-xs" />
            <Button variant="secondary" onClick={load} disabled={loading}><CalendarClock className="mr-1 h-4 w-4" /> Load slots</Button>
          </div>

          {loading ? (
            <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
          ) : slots.length === 0 ? (
            <EmptyState icon={CalendarClock} title="No slots" subtitle="Enter a pharmacy ID and load its delivery slots, or create a new one." />
          ) : (
            <div className="space-y-3">
              {slots.map((s) => (
                <Card key={s.id} className="flex flex-wrap items-center justify-between gap-3 p-4">
                  <div className="flex items-center gap-3">
                    <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><Clock className="h-5 w-5" /></span>
                    <div>
                      <p className="font-semibold text-slate-900">
                        {new Date(s.startTime).toLocaleString('en-IN')} → {new Date(s.endTime).toLocaleTimeString('en-IN')}
                      </p>
                      <p className="text-xs text-slate-500">
                        Capacity {s.capacity} · booked {s.booked}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Badge color={s.active ? 'green' : 'slate'}>{s.active ? 'Active' : 'Inactive'}</Badge>
                    <Badge color={s.booked >= s.capacity ? 'red' : 'blue'}>{s.capacity - s.booked} available</Badge>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
