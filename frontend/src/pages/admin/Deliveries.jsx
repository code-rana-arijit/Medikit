import { useEffect, useState } from 'react';
import DashboardLayout, { adminNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Card, StatCard, Select, StatusBadge, Spinner, Alert, EmptyState, Pagination } from '../../components/ui';
import { Truck, PackageCheck, MapPin, XCircle, Timer, RefreshCw, Activity } from 'lucide-react';

const STATUSES = ['', 'PENDING', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED'];

export default function AdminDeliveries() {
  const [stats, setStats] = useState(null);
  const [items, setItems] = useState([]);
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadStats = async () => {
    try {
      const s = await api.get('/delivery/admin/stats');
      setStats(s || {});
    } catch { /* stats optional */ }
  };

  const load = async (p = page, s = status) => {
    setLoading(true);
    setError('');
    try {
      const q = s ? `?status=${encodeURIComponent(s)}&page=${p}&size=20` : `?page=${p}&size=20`;
      const r = await api.get(`/delivery/admin${q}`);
      setItems(r?.content || []);
      setTotal(r?.totalElements ?? 0);
    } catch (e) { setError(e.message); setItems([]); }
    setLoading(false);
  };

  useEffect(() => {
    loadStats();
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const changeFilter = (s) => {
    setStatus(s);
    setPage(0);
    load(0, s);
  };

  const goPage = (p) => {
    setPage(p);
    load(p);
  };

  const totalPages = Math.max(1, Math.ceil(total / 20));
  const statCards = stats ? [
    { label: 'Pending', value: stats.PENDING ?? 0, icon: Timer, accent: 'amber' },
    { label: 'Assigned', value: stats.ASSIGNED ?? 0, icon: Truck, accent: 'blue' },
    { label: 'In transit', value: stats.IN_TRANSIT ?? 0, icon: MapPin, accent: 'violet' },
    { label: 'Delivered', value: stats.DELIVERED ?? 0, icon: PackageCheck, accent: 'emerald' },
    { label: 'Cancelled', value: stats.CANCELLED ?? 0, icon: XCircle, accent: 'rose' },
  ] : [];

  return (
    <DashboardLayout title="Delivery network" subtitle="Platform control" navItems={adminNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        {statCards.map((s) => (
          <StatCard key={s.label} label={s.label} value={s.value} icon={s.icon} accent={s.accent} />
        ))}
      </div>

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <Select value={status} onChange={(e) => changeFilter(e.target.value)} className="w-48">
          {STATUSES.map((s) => <option key={s} value={s}>{s || 'All statuses'}</option>)}
        </Select>
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <Activity className="h-4 w-4" /> {total} deliveries
        </div>
        <div className="ml-auto">
          <button onClick={() => { loadStats(); load(); }} className="flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
            <RefreshCw className="h-4 w-4" /> Refresh
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : items.length === 0 ? (
        <EmptyState icon={Truck} title="No deliveries" subtitle="Delivery records across the platform will appear here." />
      ) : (
        <>
          <Card className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-xs uppercase tracking-wide text-slate-400">
                  <th className="px-4 py-3">Order</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Partner</th>
                  <th className="px-4 py-3">Pharmacy</th>
                  <th className="px-4 py-3">Est. min</th>
                  <th className="px-4 py-3">Delivered</th>
                </tr>
              </thead>
              <tbody>
                {items.map((d) => (
                  <tr key={d.id} className="border-b border-slate-50 last:border-0 hover:bg-slate-50/60">
                    <td className="px-4 py-3">
                      <p className="font-semibold text-slate-900">{d.orderId.slice(0, 8)}</p>
                      <p className="font-mono text-xs text-slate-400">{d.orderId}</p>
                    </td>
                    <td className="px-4 py-3"><StatusBadge status={d.status} /></td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-500">{d.partnerId ? d.partnerId.slice(0, 8) : '—'}</td>
                    <td className="px-4 py-3 font-mono text-xs text-slate-500">{d.pharmacyId ? d.pharmacyId.slice(0, 8) : '—'}</td>
                    <td className="px-4 py-3 text-slate-600">{d.estimatedMinutes}</td>
                    <td className="px-4 py-3 text-xs text-slate-500">
                      {d.deliveredAt ? new Date(d.deliveredAt).toLocaleString('en-IN') : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </Card>
          <div className="mt-4 flex justify-center">
            <Pagination page={page} totalPages={totalPages} onChange={goPage} />
          </div>
        </>
      )}
    </DashboardLayout>
  );
}
