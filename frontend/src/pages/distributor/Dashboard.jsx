import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout, { distributorNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { StatCard, Card, StatusBadge, Spinner, Button, EmptyState } from '../../components/ui';
import { Package, ShoppingCart, Truck, Boxes, Store, ArrowRight, Plus } from 'lucide-react';

export default function DistributorDashboard() {
  const [me, setMe] = useState(null);
  const [catalog, setCatalog] = useState([]);
  const [orders, setOrders] = useState([]);
  const [fulfillments, setFulfillments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      try {
        const [profile, cat, ord, ful] = await Promise.all([
          api.get('/distributors/me').catch(() => null),
          api.get('/distributors/me/catalog').catch(() => []),
          api.get('/distributor-orders/distributor').catch(() => ({ content: [] })),
          api.get('/fulfillments').catch(() => []),
        ]);
        setMe(profile);
        setCatalog(cat || []);
        setOrders(ord?.content || []);
        setFulfillments(ful || []);
      } finally {
        setLoading(false);
      }
    };
    init();
  }, []);

  if (loading) return <DashboardLayout title="Distributor" subtitle="Wholesale supply" navItems={distributorNav}><div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div></DashboardLayout>;

  if (!me) {
    return (
      <DashboardLayout title="Distributor" subtitle="Wholesale supply" navItems={distributorNav}>
        <EmptyState
          icon={Store}
          title="Set up your distributor profile"
          subtitle="Register your shop to start selling wholesale and fulfilling retail orders."
          action={<Link to="/distributor/profile"><Button>Register shop profile</Button></Link>}
        />
      </DashboardLayout>
    );
  }

  const inStock = catalog.reduce((s, c) => s + c.stockQty, 0);
  const pendingOrders = orders.filter((o) => ['PENDING', 'CONFIRMED', 'SHIPPED'].includes(o.status));
  const activeFulfillments = fulfillments.filter((f) => ['CLAIMED', 'PICKED_UP', 'IN_TRANSIT'].includes(f.status));
  const revenue = orders.filter((o) => o.status === 'DELIVERED').reduce((s, o) => s + Number(o.totalAmount), 0);

  return (
    <DashboardLayout title="Distributor dashboard" subtitle="Wholesale supply" navItems={distributorNav}>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-bold text-slate-900">{me.shopName}</h2>
          <p className="text-sm text-slate-500">{me.city} · {me.licenseNumber}</p>
        </div>
        <div className="flex gap-2">
          <Link to="/distributor/catalog"><Button variant="secondary" size="sm"><Plus className="h-4 w-4" /> Catalog</Button></Link>
          <Link to="/distributor/purchase"><Button size="sm"><Boxes className="h-4 w-4" /> Buy stock</Button></Link>
        </div>
      </div>

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Catalog items" value={catalog.length} sub={`${inStock} units in stock`} icon={Package} />
        <StatCard label="Active supply orders" value={pendingOrders.length} icon={ShoppingCart} accent="blue" />
        <StatCard label="Active fulfillments" value={activeFulfillments.length} icon={Truck} accent="amber" />
        <StatCard label="B2B revenue" value={fmtINR(revenue)} icon={Store} accent="violet" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-bold text-slate-900">Latest supply orders</h3>
            <Link to="/distributor/orders" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">View all <ArrowRight className="h-4 w-4" /></Link>
          </div>
          {orders.length === 0 ? (
            <p className="py-6 text-center text-sm text-slate-400">No supply orders yet.</p>
          ) : (
            <div className="space-y-3">
              {orders.slice(0, 5).map((o) => (
                <div key={o.id} className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-800">{o.orderNumber}</p>
                    <p className="text-xs text-slate-500">{o.items.length} item(s) · {fmtINR(o.totalAmount)}</p>
                  </div>
                  <StatusBadge status={o.status} />
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-bold text-slate-900">Retail fulfillments</h3>
            <Link to="/distributor/fulfillments" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">Manage <ArrowRight className="h-4 w-4" /></Link>
          </div>
          {fulfillments.length === 0 ? (
            <p className="py-6 text-center text-sm text-slate-400">No retail orders claimed yet.</p>
          ) : (
            <div className="space-y-3">
              {fulfillments.slice(0, 5).map((f) => (
                <div key={f.id} className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-800">Order {f.orderId?.slice(0, 8)}</p>
                    <p className="text-xs text-slate-500">{new Date(f.createdAt).toLocaleString('en-IN')}</p>
                  </div>
                  <StatusBadge status={f.status} />
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </DashboardLayout>
  );
}
