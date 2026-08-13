import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { Spinner, cx } from './ui';
import { PackageCheck, Truck, MapPin, Clock, BadgeCheck } from 'lucide-react';

const STEPS = ['PENDING', 'ASSIGNED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED'];
const STEP_LABEL = {
  PENDING: 'Order placed',
  ASSIGNED: 'Delivery partner assigned',
  PICKED_UP: 'Picked up from pharmacy',
  IN_TRANSIT: 'Out for delivery',
  DELIVERED: 'Delivered',
};

export default function DeliveryTimeline({ orderId }) {
  const [delivery, setDelivery] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    api.get(`/delivery/${orderId}`)
      .then(setDelivery)
      .catch(() => setDelivery(null))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    const active = delivery && ['ASSIGNED', 'PICKED_UP', 'IN_TRANSIT'].includes(delivery.status);
    if (!active) return;
    const id = setInterval(load, 8000);
    return () => clearInterval(id);
  }, [orderId, delivery?.status]);

  if (loading) return <div className="flex justify-center py-8"><Spinner /></div>;
  if (!delivery) {
    return (
      <div className="rounded-xl bg-slate-50 p-4 text-sm text-slate-500">
        {error || 'Delivery details not yet created for this order.'}
      </div>
    );
  }

  const currentIdx = STEPS.indexOf(delivery.status);
  const delivered = delivery.status === 'DELIVERED';

  return (
    <div>
      <div className="relative">
        {STEPS.map((step, i) => {
          const done = currentIdx >= i;
          const current = currentIdx === i && !delivered;
          return (
            <div key={step} className="relative flex gap-3 pb-6 last:pb-0">
              {i < STEPS.length - 1 && (
                <span className={cx('absolute left-[11px] top-6 h-full w-0.5', done ? 'bg-emerald-400' : 'bg-slate-200')} />
              )}
              <span className={cx(
                'relative z-10 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2',
                done ? 'border-emerald-500 bg-emerald-500 text-white' : current ? 'border-brand-500 bg-brand-50 text-brand-600' : 'border-slate-200 bg-white text-slate-300',
              )}>
                {done ? <BadgeCheck className="h-3.5 w-3.5" /> : <span className="h-1.5 w-1.5 rounded-full bg-current" />}
              </span>
              <div>
                <p className={cx('text-sm font-semibold', done ? 'text-slate-900' : current ? 'text-brand-700' : 'text-slate-400')}>
                  {STEP_LABEL[step]}
                </p>
                {current && (
                  <p className="mt-0.5 text-xs font-medium text-brand-600">
                    {delivered ? 'Completed' : 'In progress'}
                    {delivery.estimatedMinutes != null && !delivered && (
                      <span className="ml-1 inline-flex items-center gap-1 text-slate-500">
                        <Clock className="h-3 w-3" /> ~{delivery.estimatedMinutes} min
                      </span>
                    )}
                  </p>
                )}
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-4 grid gap-3 rounded-xl bg-slate-50 p-4 text-sm sm:grid-cols-2">
        <div className="flex items-center gap-2 text-slate-600">
          <Truck className="h-4 w-4 text-brand-600" />
          <span>Partner: <span className="font-semibold text-slate-900">{delivery.partnerId ? delivery.partnerId.slice(0, 8) : '—'}</span></span>
        </div>
        <div className="flex items-center gap-2 text-slate-600">
          <PackageCheck className="h-4 w-4 text-brand-600" />
          <span>Slot: <span className="font-semibold text-slate-900">{delivery.slotId ? delivery.slotId.slice(0, 8) : '—'}</span></span>
        </div>
        <div className="flex items-center gap-2 text-slate-600">
          <MapPin className="h-4 w-4 text-brand-600" />
          <span>Customer: {delivery.customerLatitude != null ? `${delivery.customerLatitude.toFixed(4)}, ${delivery.customerLongitude?.toFixed(4)}` : '—'}</span>
        </div>
        <div className="flex items-center gap-2 text-slate-600">
          <MapPin className="h-4 w-4 text-brand-600" />
          <span>Partner: {delivery.partnerLatitude != null ? `${delivery.partnerLatitude.toFixed(4)}, ${delivery.partnerLongitude?.toFixed(4)}` : '—'}</span>
          {['ASSIGNED', 'PICKED_UP', 'IN_TRANSIT'].includes(delivery.status) && (
            <span className="ml-1 inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700">
              <span className="relative flex h-1.5 w-1.5">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75" />
                <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-500" />
              </span>
              Live
            </span>
          )}
        </div>
      </div>

      {delivered && delivery.deliveredAt && (
        <p className="mt-3 text-sm text-emerald-700">Delivered at {new Date(delivery.deliveredAt).toLocaleString('en-IN')}</p>
      )}
    </div>
  );
}
