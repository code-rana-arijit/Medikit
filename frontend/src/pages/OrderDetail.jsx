import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { api, fmtINR } from '../lib/api';
import { Button, Card, StatusBadge, Spinner, Alert } from '../components/ui';
import { ArrowLeft, Truck, XCircle, FileText } from 'lucide-react';

export default function OrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => {
    setLoading(true);
    api.get(`/orders/${id}`)
      .then(setOrder)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(load, [id]);

  const cancel = async () => {
    try {
      await api.post(`/orders/${id}/cancel`, { reason: 'Cancelled by customer' });
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  const pay = async () => {
    try {
      await api.post('/payments/initiate', { orderId: id, amount: order.total, method: order.paymentMethod || 'CARD' });
      load();
    } catch (e) {
      setError(e.message);
    }
  };

  if (loading) return <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>;

  if (!order) {
    return <div className="mx-auto max-w-3xl px-4 py-16"><Alert type="error">{error}</Alert></div>;
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
      <button onClick={() => navigate('/orders')} className="mb-4 flex items-center gap-1.5 text-sm font-medium text-slate-500 hover:text-brand-600">
        <ArrowLeft className="h-4 w-4" /> Back to orders
      </button>

      {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}

      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold text-slate-900">{order.orderNumber}</h1>
            <StatusBadge status={order.status} />
          </div>
          <p className="mt-1 text-sm text-slate-500">
            Placed {new Date(order.createdAt).toLocaleString('en-IN')} · {order.paymentMethod} · {order.paymentStatus}
          </p>
        </div>
        <div className="flex gap-2">
          {(order.status === 'PENDING' || order.status === 'CONFIRMED') && (
            <Button variant="danger" onClick={cancel}><XCircle className="h-4 w-4" /> Cancel order</Button>
          )}
          {order.paymentStatus === 'FAILED' && (
            <Button onClick={pay}><FileText className="h-4 w-4" /> Retry payment</Button>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          <Card className="p-6">
            <div className="mb-4 flex items-center gap-2"><Truck className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Items</h2></div>
            <div className="space-y-3">
              {order.items.map((i) => (
                <div key={i.productId} className="flex items-center justify-between rounded-xl bg-slate-50 p-3">
                  <div>
                    <Link to={`/products/${i.productId}`} className="font-semibold text-slate-900 hover:text-brand-600">{i.productName}</Link>
                    <p className="text-xs text-slate-500">{fmtINR(i.unitPrice)} × {i.quantity}</p>
                  </div>
                  <span className="font-bold text-slate-900">{fmtINR(i.lineTotal)}</span>
                </div>
              ))}
            </div>
          </Card>

          <Card className="p-6">
            <h2 className="font-bold text-slate-900">Delivery address</h2>
            <p className="mt-2 text-sm text-slate-600">{order.deliveryAddress}</p>
            {order.deliverySlotId && <p className="mt-1 text-xs text-slate-400">Slot ID: {order.deliverySlotId}</p>}
          </Card>
        </div>

        <div>
          <Card className="p-6">
            <h2 className="text-lg font-bold text-slate-900">Payment summary</h2>
            <div className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between text-slate-600"><span>Subtotal</span><span>{fmtINR(order.subtotal)}</span></div>
              <div className="flex justify-between text-slate-600"><span>Delivery fee</span><span>{fmtINR(order.deliveryFee)}</span></div>
              {order.discount > 0 && <div className="flex justify-between text-emerald-600"><span>Discount</span><span>-{fmtINR(order.discount)}</span></div>}
              <div className="flex justify-between border-t border-slate-200 pt-3 text-lg font-bold text-slate-900"><span>Total</span><span>{fmtINR(order.total)}</span></div>
            </div>
            {order.cancellationReason && (
              <div className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">
                <p className="font-semibold">Cancellation reason</p>
                <p className="mt-0.5">{order.cancellationReason}</p>
              </div>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
