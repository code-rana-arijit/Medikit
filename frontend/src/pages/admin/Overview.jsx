import { useEffect, useState } from 'react';
import DashboardLayout, { adminNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { StatCard, Card, Spinner, Alert, StatusBadge, EmptyState } from '../../components/ui';
import { Users, Pill, Building2, Truck, CalendarClock, Ticket, Gift, ShieldCheck } from 'lucide-react';

export default function AdminOverview() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const init = async () => {
      try {
        const [products, pharmacies, campaigns, verifs] = await Promise.all([
          api.get('/products?size=1').catch(() => null),
          api.get('/pharmacies?size=1').catch(() => null),
          api.get('/campaigns?size=1').catch(() => null),
          api.get('/admin/pharmacists/verifications?status=PENDING&size=1').catch(() => ({ totalElements: 0 })),
        ]);
        setData({
          products: products?.totalElements ?? 0,
          pharmacies: pharmacies?.totalElements ?? 0,
          campaigns: campaigns?.totalElements ?? 0,
          pendingVerifs: verifs?.totalElements ?? 0,
        });
      } catch (e) { setError(e.message); }
      setLoading(false);
    };
    init();
  }, []);

  if (loading) return <DashboardLayout title="Platform Overview" subtitle="Platform control" navItems={adminNav}><div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div></DashboardLayout>;

  return (
    <DashboardLayout title="Platform Overview" subtitle="Platform control" navItems={adminNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Products" value={data.products} icon={Pill} />
        <StatCard label="Pharmacies" value={data.pharmacies} icon={Building2} accent="blue" />
        <StatCard label="Campaigns" value={data.campaigns} icon={Ticket} accent="violet" />
        <StatCard label="Pending verifications" value={data.pendingVerifs} icon={ShieldCheck} accent="amber" />
      </div>

      <Card className="p-6">
        <h2 className="mb-4 font-bold text-slate-900">Platform status</h2>
        <div className="grid gap-3 text-sm sm:grid-cols-2">
          <div className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
            <span className="flex items-center gap-2 text-slate-600"><Users className="h-4 w-4 text-brand-600" /> User service</span>
            <StatusBadge status="ACTIVE" />
          </div>
          <div className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
            <span className="flex items-center gap-2 text-slate-600"><Pill className="h-4 w-4 text-brand-600" /> Product service</span>
            <StatusBadge status="ACTIVE" />
          </div>
          <div className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
            <span className="flex items-center gap-2 text-slate-600"><Building2 className="h-4 w-4 text-brand-600" /> Pharmacy registry</span>
            <StatusBadge status="ACTIVE" />
          </div>
          <div className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
            <span className="flex items-center gap-2 text-slate-600"><Truck className="h-4 w-4 text-brand-600" /> Delivery service</span>
            <StatusBadge status="ACTIVE" />
          </div>
          <div className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
            <span className="flex items-center gap-2 text-slate-600"><CalendarClock className="h-4 w-4 text-brand-600" /> Inventory service</span>
            <StatusBadge status="ACTIVE" />
          </div>
          <div className="flex items-center justify-between rounded-xl bg-slate-50 p-4">
            <span className="flex items-center gap-2 text-slate-600"><Gift className="h-4 w-4 text-brand-600" /> Loyalty & discount</span>
            <StatusBadge status="ACTIVE" />
          </div>
        </div>
      </Card>
    </DashboardLayout>
  );
}
