import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Button, Card, Input, Alert, Spinner, StatusBadge, EmptyState } from '../components/ui';
import { FileText, Upload, Send, ShieldCheck, Clock, XCircle } from 'lucide-react';

const empty = { patientName: '', patientAge: '', doctorName: '', diagnosis: '', imageUrl: '' };

export default function Prescriptions() {
  const [list, setList] = useState([]);
  const [form, setForm] = useState(empty);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [open, setOpen] = useState(false);

  const load = () => {
    setLoading(true);
    api.get('/prescriptions/my')
      .then((r) => setList(r?.content || []))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const upload = async () => {
    setSaving(true); setError(''); setOk('');
    try {
      const res = await api.post('/prescriptions', {
        ...form,
        patientAge: Number(form.patientAge),
        imageUrl: form.imageUrl || undefined,
      });
      setOk(`Prescription uploaded for ${res.patientName}`);
      setForm(empty);
      setOpen(false);
      load();
    } catch (e) { setError(e.message); } finally { setSaving(false); }
  };

  const submit = async (id) => {
    setError(''); setOk('');
    try {
      await api.post(`/prescriptions/${id}/submit`);
      setOk('Prescription submitted for validation');
      load();
    } catch (e) { setError(e.message); }
  };

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <FileText className="h-6 w-6 text-brand-600" />
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Prescriptions</h1>
            <p className="text-sm text-slate-500">Upload doctor prescriptions and track validation</p>
          </div>
        </div>
        <Button onClick={() => setOpen(!open)}>{open ? 'Close' : '+ Upload prescription'}</Button>
      </div>

      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      {open && (
        <Card className="mb-6 p-6">
          <h2 className="mb-4 font-bold text-slate-900">New prescription</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="Patient name" value={form.patientName} onChange={(e) => setForm({ ...form, patientName: e.target.value })} placeholder="e.g. Ravi Kumar" />
            <Input label="Patient age" type="number" value={form.patientAge} onChange={(e) => setForm({ ...form, patientAge: e.target.value })} placeholder="e.g. 32" />
            <Input label="Doctor name" value={form.doctorName} onChange={(e) => setForm({ ...form, doctorName: e.target.value })} placeholder="Dr. ..." />
            <Input label="Diagnosis" value={form.diagnosis} onChange={(e) => setForm({ ...form, diagnosis: e.target.value })} placeholder="e.g. Acute pharyngitis" />
            <Input label="Image URL (optional)" value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} placeholder="https://…/prescription.jpg" className="sm:col-span-2" />
          </div>
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
            <Button loading={saving} onClick={upload}><Upload className="h-4 w-4" /> Upload</Button>
          </div>
        </Card>
      )}

      {loading ? (
        <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>
      ) : list.length === 0 ? (
        <EmptyState icon={FileText} title="No prescriptions" subtitle="Upload a prescription to keep it on file for Rx medicines and pharmacist validation." />
      ) : (
        <div className="space-y-3">
          {list.map((p) => {
            const icon = p.status === 'APPROVED' ? <ShieldCheck className="h-5 w-5 text-emerald-500" />
              : p.status === 'REJECTED' ? <XCircle className="h-5 w-5 text-rose-500" />
              : p.status === 'EXPIRED' ? <Clock className="h-5 w-5 text-slate-400" />
              : <Clock className="h-5 w-5 text-amber-500" />;
            return (
              <Card key={p.id} className="flex flex-wrap items-center justify-between gap-3 p-5">
                <div className="flex items-center gap-4">
                  <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-slate-100">{icon}</span>
                  <div>
                    <p className="font-semibold text-slate-900">
                      {p.patientName} <span className="font-normal text-slate-400">· {p.patientAge} yrs</span>
                    </p>
                    <p className="text-xs text-slate-500">
                      {p.doctorName ? `${p.doctorName} · ` : ''}{p.diagnosis || 'No diagnosis'}
                    </p>
                    <p className="mt-0.5 text-xs text-slate-400">
                      Uploaded {new Date(p.createdAt).toLocaleString('en-IN')}
                      {p.approvedAt && <> · Approved {new Date(p.approvedAt).toLocaleString('en-IN')}</>}
                      {p.expiresAt && <> · Expires {new Date(p.expiresAt).toLocaleString('en-IN')}</>}
                    </p>
                    {p.rejectionReason && <p className="mt-0.5 text-xs text-rose-600">Reason: {p.rejectionReason}</p>}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge status={p.status} />
                  {p.status === 'UPLOADED' && (
                    <Button size="sm" variant="secondary" onClick={() => submit(p.id)}><Send className="h-3.5 w-3.5" /> Submit</Button>
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
