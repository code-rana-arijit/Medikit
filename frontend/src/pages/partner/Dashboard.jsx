import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout, { deliveryPartnerNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { useAuth } from '../../lib/auth';
import { StatCard, Card, StatusBadge, Spinner, Alert, EmptyState, Button } from '../../components/ui';
import { Truck, MapPin, PackageCheck, ClipboardList, ArrowRight, Navigation, Timer } from 'lucide-react';
import { StatusPie, TrendArea } from '../../components/Charts';
import { groupBy } from '../../lib/analytics';

export default function PartnerDashboard() {
  const { user } = useAuth();
  const [mine, setMine] = useState([]);
  const [available, setAvailable] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setError('');
    try {
      const [mineR, availR] = await Promise.all([
        api.get('/delivery/partner').catch(() => ({ content: [] })),
        api.get('/delivery/available').catch(() => ({ content: [] })),
      ]);
      setMine(mineR?.content || []);
      setAvailable(availR?.content || []);
    } catch (e) { setError(e.message); }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const active = mine.filter((d) => ['ASSIGNED', 'PICKED_UP', 'IN_TRANSIT'].includes(d.status));
  const delivered = mine.filter((d) => d.status === 'DELIVERED');
  const cancelled = mine.filter((d) => d.status === 'CANCELLED');
  const statusData = Object.entries(groupBy(mine, (d) => d.status)).map(([name, value]) => ({ name, value }));
  const trendMap = {};
  mine.filter((d) => d.status === 'DELIVERED' && d.deliveredAt).forEach((d) => {
    const day = new Date(d.deliveredAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
    trendMap[day] = (trendMap[day] || 0) + 1;
  });
  const trendData = Object.entries(trendMap).map(([date, value]) => ({ date, value }));

  return (
    <DashboardLayout title="Partner dashboard" subtitle="Delivery partner" navItems={deliveryPartnerNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Active deliveries" value={active.length} icon={Truck} />
            <StatCard label="Available to claim" value={available.length} icon={MapPin} accent="blue" />
            <StatCard label="Delivered" value={delivered.length} icon={PackageCheck} accent="emerald" />
            <StatCard label="Total assigned" value={mine.length} icon={ClipboardList} accent="violet" />
          </div>

          <div className="mb-6 grid gap-6 lg:grid-cols-2">
            <Card className="p-6">
              <div className="mb-4 flex items-center gap-2"><Navigation className="h-5 w-5 text-brand-600" /><h3 className="font-bold text-slate-900">Delivery status</h3></div>
              {statusData.length ? <StatusPie data={statusData} /> : <p className="py-10 text-center text-sm text-slate-400">No deliveries yet</p>}
            </Card>
            <Card className="p-6">
              <div className="mb-4 flex items-center gap-2"><Timer className="h-5 w-5 text-brand-600" /><h3 className="font-bold text-slate-900">Deliveries completed per day</h3></div>
              {trendData.length ? <TrendArea data={trendData} name="Delivered" color="#10b981" /> : <p className="py-10 text-center text-sm text-slate-400">No deliveries yet</p>}
            </Card>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card className="p-6">
              <div className="mb-4 flex items-center justify-between">
                <h3 className="font-bold text-slate-900">Available deliveries</h3>
                <Link to="/partner/available" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">Claim <ArrowRight className="h-4 w-4" /></Link>
              </div>
              {available.length === 0 ? (
                <p className="py-6 text-center text-sm text-slate-400">Nothing waiting to be claimed.</p>
              ) : (
                <div className="space-y-3">
                  {available.slice(0, 5).map((d) => (
                    <div key={d.id} className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-800">Order {d.orderId.slice(0, 8)}</p>
                        <p className="text-xs text-slate-500">{d.estimatedMinutes} min est. · {d.createdAt ? new Date(d.createdAt).toLocaleString('en-IN') : ''}</p>
                      </div>
                      <StatusBadge status={d.status} />
                    </div>
                  ))}
                </div>
              )}
            </Card>

            <Card className="p-6">
              <div className="mb-4 flex items-center justify-between">
                <h3 className="font-bold text-slate-900">My active deliveries</h3>
                <Link to="/partner/deliveries" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">View all <ArrowRight className="h-4 w-4" /></Link>
              </div>
              {active.length === 0 ? (
                <p className="py-6 text-center text-sm text-slate-400">No active deliveries. Claim one now.</p>
              ) : (
                <div className="space-y-3">
                  {active.slice(0, 5).map((d) => (
                    <div key={d.id} className="flex items-center justify-between rounded-xl bg-brand-50 p-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-800">Order {d.orderId.slice(0, 8)}</p>
                        <p className="text-xs text-slate-500">Order {d.orderId}</p>
                      </div>
                      <StatusBadge status={d.status} />
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>
        </>
      )}
    </DashboardLayout>
  );
}
