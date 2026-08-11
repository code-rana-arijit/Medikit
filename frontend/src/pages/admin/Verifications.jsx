import { useEffect, useState } from 'react';
import DashboardLayout, { adminNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Card, Button, Select, Spinner, EmptyState, Alert, StatusBadge } from '../../components/ui';
import { ShieldCheck, Check, X, Eye } from 'lucide-react';

export default function AdminVerifications() {
  const [list, setList] = useState([]);
  const [status, setStatus] = useState('PENDING');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [reviewing, setReviewing] = useState(null);

  const load = () => {
    setLoading(true);
    api.get(`/admin/pharmacists/verifications?status=${status}`)
      .then((r) => setList(r?.content || []))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(load, [status]);

  const review = async (v, approved) => {
    setReviewing(v.id); setError(''); setOk('');
    try {
      await api.post('/admin/pharmacists/verifications/review', {
        verificationId: v.id,
        approved,
        rejectionReason: approved ? null : 'License could not be validated',
      });
      setOk(`${v.fullName} ${approved ? 'approved' : 'rejected'}`);
      load();
    } catch (e) { setError(e.message); }
    setReviewing(null);
  };

  return (
    <DashboardLayout title="Pharmacist Verifications" subtitle="Platform control" navItems={adminNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="mb-4 flex items-center gap-3">
        <Select value={status} onChange={(e) => setStatus(e.target.value)} className="w-48">
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
          <option value="ALL">All</option>
        </Select>
        <Button variant="secondary" onClick={load}>Refresh</Button>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : list.length === 0 ? (
        <EmptyState icon={ShieldCheck} title="No verifications" subtitle="No pharmacist license submissions in this state." />
      ) : (
        <div className="space-y-3">
          {list.map((v) => (
            <Card key={v.id} className="p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex items-start gap-4">
                  <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><ShieldCheck className="h-5 w-5" /></span>
                  <div>
                    <p className="font-semibold text-slate-900">{v.fullName} <StatusBadge status={v.status} /></p>
                    <p className="mt-0.5 text-xs text-slate-500">User {v.userId}</p>
                    <p className="mt-1 text-sm text-slate-700">
                      License <span className="font-mono font-semibold">{v.licenseNumber}</span> · {v.licenseState}
                    </p>
                    <p className="text-xs text-slate-400">Submitted {new Date(v.createdAt).toLocaleString('en-IN')}</p>
                    {v.rejectionReason && <p className="mt-1 text-xs text-rose-600">Rejected: {v.rejectionReason}</p>}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  {v.licenseDocumentUrl && (
                    <a href={v.licenseDocumentUrl} target="_blank" rel="noreferrer">
                      <Button size="sm" variant="secondary"><Eye className="h-4 w-4" /> Document</Button>
                    </a>
                  )}
                  {v.status === 'PENDING' && (
                    <>
                      <Button size="sm" loading={reviewing === v.id} onClick={() => review(v, true)}><Check className="h-4 w-4" /> Approve</Button>
                      <Button size="sm" variant="danger" loading={reviewing === v.id} onClick={() => review(v, false)}><X className="h-4 w-4" /> Reject</Button>
                    </>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </DashboardLayout>
  );
}
