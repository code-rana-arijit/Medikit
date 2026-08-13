import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout, { adminNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { StatCard, Card, Spinner, Button, StatusBadge, EmptyState } from '../../components/ui';
import { ShieldCheck, Ticket, Package, Gift, ArrowRight, Truck, Users } from 'lucide-react';

export default function AdminDashboard() {
  const [pendingVerifs, setPendingVerifs] = useState([]);
  const [campaigns, setCampaigns] = useState([]);
  const [products, setProducts] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      try {
        const [ver, camp, prod] = await Promise.all([
          api.get('/admin/pharmacists/verifications?status=PENDING&size=10').catch(() => ({ content: [] })),
          api.get('/campaigns?size=10').catch(() => ({ content: [] })),
          api.get('/products?size=1').catch(() => null),
        ]);
        setPendingVerifs(ver?.content || []);
        setCampaigns(camp?.content || []);
        setProducts(prod);
      } finally { setLoading(false); }
    };
    init();
  }, []);

  if (loading) return <DashboardLayout title="Admin" subtitle="Platform control" navItems={adminNav}><div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div></DashboardLayout>;

  const totalCampaigns = campaigns.length;
  const issuedCodes = campaigns.reduce((s, c) => s + c.issuedCodes, 0);

  return (
    <DashboardLayout title="Admin dashboard" subtitle="Platform control" navItems={adminNav}>
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Pending verifications" value={pendingVerifs.length} icon={ShieldCheck} accent="amber" />
        <StatCard label="Active campaigns" value={totalCampaigns} icon={Ticket} accent="blue" />
        <StatCard label="Coupon codes issued" value={issuedCodes} icon={Gift} accent="violet" />
        <StatCard label="Products in catalog" value={products?.totalElements ?? '—'} icon={Package} accent="rose" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-bold text-slate-900">Pharmacist verification queue</h3>
            <Link to="/admin/verifications" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">Review <ArrowRight className="h-4 w-4" /></Link>
          </div>
          {pendingVerifs.length === 0 ? (
            <EmptyState icon={ShieldCheck} title="Queue is clear" subtitle="No pending pharmacist license verifications." />
          ) : (
            <div className="space-y-3">
              {pendingVerifs.slice(0, 5).map((v) => (
                <div key={v.id} className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-800">{v.fullName}</p>
                    <p className="text-xs text-slate-500">{v.licenseNumber} · {v.licenseState}</p>
                  </div>
                  <StatusBadge status={v.status} />
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-bold text-slate-900">Latest campaigns</h3>
            <Link to="/admin/campaigns" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">Manage <ArrowRight className="h-4 w-4" /></Link>
          </div>
          {campaigns.length === 0 ? (
            <EmptyState icon={Ticket} title="No campaigns" subtitle="Create your first promotion campaign." />
          ) : (
            <div className="space-y-3">
              {campaigns.map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-800">{c.name}</p>
                    <p className="text-xs text-slate-500">
                      {c.discountType === 'PERCENTAGE' ? `${c.percentage}% off` : `₹${c.discountAmount} off`} · {c.issuedCodes}/{c.totalCodes} issued
                    </p>
                  </div>
                  <StatusBadge status={c.active ? 'ACTIVE' : 'INACTIVE'} />
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        <Link to="/admin/verifications"><Button variant="secondary" className="w-full justify-start"><ShieldCheck className="h-4 w-4" /> Verify pharmacists</Button></Link>
        <Link to="/admin/campaigns"><Button variant="secondary" className="w-full justify-start"><Ticket className="h-4 w-4" /> Create campaign</Button></Link>
        <Link to="/admin/discounts"><Button variant="secondary" className="w-full justify-start"><Gift className="h-4 w-4" /> Issue coupon</Button></Link>
      </div>
      <div className="mt-4 grid gap-4 sm:grid-cols-3">
        <Link to="/admin/deliveries"><Button variant="secondary" className="w-full justify-start"><Truck className="h-4 w-4" /> Delivery network</Button></Link>
        <Link to="/admin/users"><Button variant="secondary" className="w-full justify-start"><Users className="h-4 w-4" /> Manage users & roles</Button></Link>
        <Link to="/admin/products"><Button variant="secondary" className="w-full justify-start"><Package className="h-4 w-4" /> Products & categories</Button></Link>
      </div>
    </DashboardLayout>
  );
}
