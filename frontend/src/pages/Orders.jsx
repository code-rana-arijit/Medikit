import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, fmtINR } from '../lib/api';
import { Card, StatusBadge, Spinner, EmptyState, Select } from '../components/ui';
import { Package } from 'lucide-react';

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async (s = '') => {
    setLoading(true);
    try {
      const data = await api.get('/orders');
      let list = data.content || [];
      if (s) list = list.filter((o) => o.status === s);
      setOrders(list);
    } catch {
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">My orders</h1>
          <p className="text-sm text-slate-500">Track and manage your orders</p>
        </div>
        <Select className="w-44" value={status} onChange={(e) => { setStatus(e.target.value); load(e.target.value); }}>
          <option value="">All statuses</option>
          <option value="PENDING">Pending</option>
          <option value="CONFIRMED">Confirmed</option>
          <option value="SHIPPED">Shipped</option>
          <option value="DELIVERED">Delivered</option>
          <option value="CANCELLED">Cancelled</option>
        </Select>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : orders.length === 0 ? (
        <EmptyState icon={Package} title="No orders yet" subtitle="Once you place an order it will show up here." />
      ) : (
        <div className="space-y-4">
          {orders.map((o) => (
            <Link key={o.id} to={`/orders/${o.id}`}>
              <Card className="p-5 transition hover:border-brand-300 hover:shadow-md">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <p className="text-sm font-bold text-slate-900">{o.orderNumber}</p>
                    <p className="text-xs text-slate-500">{new Date(o.createdAt).toLocaleString('en-IN')}</p>
                  </div>
                  <StatusBadge status={o.status} />
                  <div className="text-right">
                    <p className="text-lg font-bold text-slate-900">{fmtINR(o.total)}</p>
                    <p className="text-xs text-slate-500">{o.items.length} item{o.items.length > 1 ? 's' : ''}</p>
                  </div>
                </div>
                <div className="mt-3 flex flex-wrap gap-2 border-t border-slate-100 pt-3">
                  {o.items.slice(0, 4).map((i) => (
                    <span key={i.productId} className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">{i.productName}</span>
                  ))}
                  {o.items.length > 4 && <span className="text-xs text-slate-400">+{o.items.length - 4} more</span>}
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
