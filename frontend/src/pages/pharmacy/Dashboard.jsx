import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import DashboardLayout, { pharmacyNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { useAuth } from '../../lib/auth';
import { StatCard, Card, StatusBadge, Spinner, Button, Alert, Input } from '../../components/ui';
import { ClipboardList, Package, Activity, ArrowRight, Save, TrendingUp, Layers, ShoppingBag, Clock } from 'lucide-react';
import { StatusPie, TrendArea, CategoryBar } from '../../components/Charts';
import { groupBy, dailyTrend, topItems } from '../../lib/analytics';

export default function PharmacyDashboard() {
  const { user } = useAuth();
  const [pharmacies, setPharmacies] = useState([]);
  const [selected, setSelected] = useState('');
  const [orders, setOrders] = useState([]);
  const [stock, setStock] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/pharmacies')
      .then((r) => {
        const mine = (r?.content || []).filter((p) => p.ownerUserId === user?.id);
        setPharmacies(mine.length ? mine : r?.content || []);
        if (mine.length) setSelected(mine[0].id);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [user?.id]);

  const loadData = async () => {
    if (!selected) return;
    setLoading(true);
    try {
      const [ord, inv] = await Promise.all([
        api.get(`/orders/pharmacy/${selected}`).catch(() => ({ content: [] })),
        api.get(`/inventory/stock/bulk`, { pharmacyId: selected, productIds: [] }).catch(() => []),
      ]);
      setOrders(ord?.content || []);
      setStock(inv || []);
    } catch (e) { setError(e.message); }
    setLoading(false);
  };

  useEffect(() => { loadData(); }, [selected]);

  const saveStock = async (s) => {
    try {
      await api.put('/inventory/stock', {
        productId: s.productId,
        pharmacyId: selected,
        quantityAvailable: s.quantityAvailable,
        minStockLevel: s.minStockLevel,
        maxStockLevel: s.maxStockLevel,
        active: s.active,
      });
      loadData();
    } catch (e) { setError(e.message); }
  };

  const activeOrders = orders.filter((o) => ['CREATED', 'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'OUT_FOR_DELIVERY'].includes(o.status));
  const revenue = orders.filter((o) => o.status === 'DELIVERED').reduce((s, o) => s + Number(o.total), 0);
  const lowStock = stock.filter((s) => s.availableQuantity <= s.minStockLevel);

  const statusData = Object.entries(groupBy(orders, (o) => o.status)).map(([name, value]) => ({ name, value }));
  const trendData = dailyTrend(orders, 'createdAt', 'total');
  const topItemsData = topItems(
    orders.flatMap((o) => o.items || []),
    (i) => i.lineTotal,
    (i) => i.productName,
    5,
  );
  const revenueData = trendData;

  return (
    <DashboardLayout title="Pharmacy admin" subtitle="Store operations" navItems={pharmacyNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}

      <div className="mb-6 flex flex-wrap items-center gap-3">
        <Input
          label="Pharmacy"
          value={selected}
          onChange={(e) => setSelected(e.target.value)}
          placeholder="Pharmacy ID (UUID)"
          className="w-72"
        />
        <div className="mt-5 flex flex-wrap gap-2">
          {pharmacies.map((p) => (
            <button key={p.id} onClick={() => setSelected(p.id)}
              className={`rounded-full px-3 py-1.5 text-xs font-semibold ${selected === p.id ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>
              {p.name}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : (
        <>
          <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard label="Total orders" value={orders.length} icon={ClipboardList} />
            <StatCard label="Active orders" value={activeOrders.length} icon={Activity} accent="blue" />
            <StatCard label="Low stock items" value={lowStock.length} icon={Package} accent="amber" />
            <StatCard label="Revenue" value={fmtINR(revenue)} icon={Save} accent="violet" />
          </div>

          <div className="mb-6 grid gap-6 lg:grid-cols-2">
            <Card className="p-6">
              <div className="mb-4 flex items-center gap-2"><TrendingUp className="h-5 w-5 text-brand-600" /><h3 className="font-bold text-slate-900">Sales trend</h3></div>
              {revenueData.length ? <TrendArea data={revenueData} name="Sales" /> : <p className="py-10 text-center text-sm text-slate-400">No order data yet</p>}
            </Card>
            <Card className="p-6">
              <div className="mb-4 flex items-center gap-2"><Layers className="h-5 w-5 text-brand-600" /><h3 className="font-bold text-slate-900">Order status</h3></div>
              {statusData.length ? <StatusPie data={statusData} /> : <p className="py-10 text-center text-sm text-slate-400">No order data yet</p>}
            </Card>
          </div>

          <div className="mb-6 grid gap-6 lg:grid-cols-2">
            <Card className="p-6">
              <div className="mb-4 flex items-center gap-2"><ShoppingBag className="h-5 w-5 text-brand-600" /><h3 className="font-bold text-slate-900">Top products</h3></div>
              {topItemsData.length ? <CategoryBar data={topItemsData} color="#0ea5e9" /> : <p className="py-10 text-center text-sm text-slate-400">No sales data yet</p>}
            </Card>
            <Card className="p-6">
              <div className="mb-4 flex items-center justify-between">
                <h3 className="font-bold text-slate-900">Stock alerts</h3>
                <Link to="/pharmacy/inventory" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">Manage <ArrowRight className="h-4 w-4" /></Link>
              </div>
              {lowStock.length === 0 ? (
                <p className="py-10 text-center text-sm text-slate-400">No low stock items.</p>
              ) : (
                <div className="space-y-2">
                  {lowStock.slice(0, 6).map((s) => (
                    <div key={s.productId} className="flex items-center justify-between rounded-xl bg-rose-50 p-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-800">{s.productId.slice(0, 8)}</p>
                        <p className="text-xs text-slate-500">{s.productId}</p>
                      </div>
                      <p className="text-xs font-bold text-rose-600">{s.availableQuantity} left (min {s.minStockLevel})</p>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card className="p-6">
              <div className="mb-4 flex items-center justify-between">
                <h3 className="font-bold text-slate-900">Recent orders</h3>
                <Link to="/pharmacy/orders" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">View all <ArrowRight className="h-4 w-4" /></Link>
              </div>
              {orders.length === 0 ? (
                <p className="py-6 text-center text-sm text-slate-400">No orders for this pharmacy yet.</p>
              ) : (
                <div className="space-y-3">
                  {orders.slice(0, 5).map((o) => (
                    <div key={o.id} className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-800">{o.orderNumber}</p>
                        <p className="text-xs text-slate-500">{o.items.length} item(s) · {fmtINR(o.total)}</p>
                      </div>
                      <StatusBadge status={o.status} />
                    </div>
                  ))}
                </div>
              )}
            </Card>

            <Card className="p-6">
              <div className="mb-4 flex items-center justify-between">
                <h3 className="font-bold text-slate-900">Quick actions</h3>
              </div>
              <div className="grid gap-3">
                <Link to="/pharmacy/slots"><Button variant="secondary" className="w-full justify-start"><Clock className="h-4 w-4" /> Manage delivery slots</Button></Link>
                <Link to="/pharmacy/inventory"><Button variant="secondary" className="w-full justify-start"><Package className="h-4 w-4" /> Update inventory</Button></Link>
                <Link to="/pharmacy/orders"><Button variant="secondary" className="w-full justify-start"><ClipboardList className="h-4 w-4" /> Review orders</Button></Link>
              </div>
            </Card>
          </div>
        </>
      )}
    </DashboardLayout>
  );
}
