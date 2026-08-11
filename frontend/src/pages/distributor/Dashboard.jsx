import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout, { distributorNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { StatCard, Card, StatusBadge, Spinner, Button, EmptyState } from '../../components/ui';
import { Package, ShoppingCart, Truck, Boxes, Store, ArrowRight, Plus, TrendingUp, Layers } from 'lucide-react';
import { StatusPie, TrendArea, CategoryBar } from '../../components/Charts';
import { groupBy, sumBy, dailyTrend, topItems } from '../../lib/analytics';

export default function DistributorDashboard() {
  const [me, setMe] = useState(null);
  const [catalog, setCatalog] = useState([]);
  const [orders, setOrders] = useState([]);
  const [outgoing, setOutgoing] = useState([]);
  const [fulfillments, setFulfillments] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const init = async () => {
      try {
        const [profile, cat, ord, out, ful] = await Promise.all([
          api.get('/distributors/me').catch(() => null),
          api.get('/distributors/me/catalog').catch(() => []),
          api.get('/distributor-orders/distributor?size=200').catch(() => ({ content: [] })),
          api.get('/distributor-orders?size=200').catch(() => ({ content: [] })),
          api.get('/fulfillments').catch(() => []),
        ]);
        setMe(profile);
        setCatalog(cat || []);
        setOrders(ord?.content || []);
        setOutgoing(out?.content || []);
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
  const catalogValue = catalog.reduce((s, c) => s + Number(c.unitPrice) * c.stockQty, 0);
  const pendingOrders = orders.filter((o) => ['PENDING', 'CONFIRMED', 'SHIPPED'].includes(o.status));
  const activeFulfillments = fulfillments.filter((f) => ['CLAIMED', 'PICKED_UP', 'IN_TRANSIT'].includes(f.status));
  const revenue = orders.filter((o) => o.status === 'DELIVERED').reduce((s, o) => s + Number(o.totalAmount), 0);
  const spend = sumBy(outgoing, (o) => o.totalAmount);

  const statusData = Object.entries(groupBy(orders, (o) => o.status)).map(([name, value]) => ({ name, value }));
  const trendData = dailyTrend([...orders, ...outgoing], 'createdAt', 'totalAmount');
  const lowStock = catalog.filter((c) => c.stockQty <= 5);
  const topItemsData = topItems(
    orders.flatMap((o) => o.items || []),
    (i) => i.subtotal,
    (i) => i.productName,
    5,
  );
  const categoryData = topItems(catalog, (c) => c.stockQty, (c) => c.productName, 5);

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
        <StatCard label="Catalog items" value={catalog.length} sub={`${inStock} units · ${fmtINR(catalogValue)} value`} icon={Package} />
        <StatCard label="Active supply orders" value={pendingOrders.length} icon={ShoppingCart} accent="blue" />
        <StatCard label="Active fulfillments" value={activeFulfillments.length} icon={Truck} accent="amber" />
        <StatCard label="B2B revenue" value={fmtINR(revenue)} sub={`Spent ${fmtINR(spend)}`} icon={Store} accent="violet" />
      </div>

      <div className="mb-6 grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <div className="mb-4 flex items-center gap-2"><TrendingUp className="h-5 w-5 text-brand-600" /><h3 className="font-bold text-slate-900">Transaction trend</h3></div>
          {trendData.length ? <TrendArea data={trendData} name="Revenue" /> : <p className="py-10 text-center text-sm text-slate-400">No transaction data yet</p>}
        </Card>
        <Card className="p-6">
          <div className="mb-4 flex items-center gap-2"><Layers className="h-5 w-5 text-brand-600" /><h3 className="font-bold text-slate-900">Order status distribution</h3></div>
          {statusData.length ? <StatusPie data={statusData} /> : <p className="py-10 text-center text-sm text-slate-400">No order data yet</p>}
        </Card>
      </div>

      <div className="mb-6 grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-bold text-slate-900">Top selling products</h3>
            <span className="text-xs text-slate-400">by order value</span>
          </div>
          {topItemsData.length ? <CategoryBar data={topItemsData} color="#7c3aed" /> : <p className="py-10 text-center text-sm text-slate-400">No sales data yet</p>}
        </Card>
        <Card className="p-6">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-bold text-slate-900">Low stock alerts</h3>
            <Link to="/distributor/catalog" className="text-sm font-semibold text-brand-600 hover:text-brand-700">Restock</Link>
          </div>
          {lowStock.length === 0 ? (
            <p className="py-10 text-center text-sm text-slate-400">All catalog items are well stocked.</p>
          ) : (
            <div className="space-y-2">
              {lowStock.slice(0, 6).map((c) => (
                <div key={c.id} className="flex items-center justify-between rounded-xl bg-rose-50 px-4 py-3">
                  <p className="text-sm font-semibold text-slate-800">{c.productName}</p>
                  <p className="text-xs font-bold text-rose-600">{c.stockQty} left</p>
                </div>
              ))}
            </div>
          )}
        </Card>
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
