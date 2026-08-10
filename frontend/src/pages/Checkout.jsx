import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { api, fmtINR } from '../lib/api';
import { useCart } from '../lib/cart';
import { Button, Card, Input, Select, Alert, Spinner, EmptyState } from '../components/ui';
import { Truck, MapPin, CreditCard } from 'lucide-react';
import { useAuth } from '../lib/auth';

export default function Checkout() {
  const { cart, clear, load } = useCart();
  const { user, loadMe } = useAuth();
  const navigate = useNavigate();
  const [slots, setSlots] = useState([]);
  const [addresses, setAddresses] = useState([]);
  const [address, setAddress] = useState('');
  const [lat, setLat] = useState(null);
  const [lng, setLng] = useState(null);
  const [slotId, setSlotId] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CARD');
  const [discountCode, setDiscountCode] = useState('');
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState('');
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const init = async () => {
      try {
        const addrs = await api.get('/users/me/addresses');
        setAddresses(addrs || []);
        if (addrs?.length) {
          const a = addrs[0];
          setAddress(a.addressLine1 + (a.addressLine2 ? ', ' + a.addressLine2 : '') + ', ' + a.city + ' ' + a.pincode);
          setLat(a.latitude);
          setLng(a.longitude);
        }
      } catch { /* ignore */ }

      try {
        if (cart?.pharmacyId) {
          const from = new Date();
          const to = new Date(Date.now() + 2 * 86400000);
          const s = await api.get(`/delivery/slots?pharmacyId=${cart.pharmacyId}&from=${from.toISOString()}&to=${to.toISOString()}`);
          setSlots(s || []);
          if (s?.length) setSlotId(s[0].id);
        }
      } catch { /* ignore */ }
      setLoaded(true);
    };
    init();
  }, [cart?.pharmacyId]);

  if (!loaded) return <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>;
  if (!cart || cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16">
        <EmptyState title="Nothing to checkout" subtitle="Your cart is empty." action={<Link to="/products"><Button>Browse medicines</Button></Link>} />
      </div>
    );
  }

  const placeOrder = async () => {
    setPlacing(true);
    setError('');
    try {
      const res = await api.post('/orders', {
        pharmacyId: cart.pharmacyId,
        items: cart.items.map((i) => ({
          productId: i.productId,
          productName: i.productName,
          quantity: i.quantity,
          unitPrice: i.unitPrice,
          mrp: i.mrp,
          prescriptionRequired: i.prescriptionRequired,
        })),
        address: { address, latitude: lat, longitude: lng },
        paymentMethod,
        deliverySlotId: slotId || undefined,
        discountCode: discountCode || undefined,
      });
      await clear();
      await load();
      navigate(`/orders/${res.id}`);
    } catch (e) {
      setError(e.message);
    } finally {
      setPlacing(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Checkout</h1>
      {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Card className="p-6">
            <div className="flex items-center gap-2"><MapPin className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Delivery address</h2></div>
            <div className="mt-4 space-y-3">
              <Select value={address} onChange={(e) => {
                const a = addresses.find((x) => (x.addressLine1 + (x.addressLine2 ? ', ' + x.addressLine2 : '') + ', ' + x.city + ' ' + x.pincode) === e.target.value);
                setAddress(e.target.value);
                setLat(a?.latitude ?? null);
                setLng(a?.longitude ?? null);
              }}>
                {addresses.map((a) => (
                  <option key={a.id} value={a.addressLine1 + (a.addressLine2 ? ', ' + a.addressLine2 : '') + ', ' + a.city + ' ' + a.pincode}>
                    {a.addressLine1}, {a.city}
                  </option>
                ))}
              </Select>
              <Input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="Or enter delivery address" />
            </div>
          </Card>

          <Card className="p-6">
            <div className="flex items-center gap-2"><Truck className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Delivery slot</h2></div>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              {slots.length === 0 && <p className="text-sm text-slate-500">No upcoming slots available right now.</p>}
              {slots.slice(0, 6).map((s) => {
                const start = new Date(s.startTime);
                const available = s.capacity - s.booked;
                return (
                  <button
                    key={s.id}
                    onClick={() => setSlotId(s.id)}
                    className={`rounded-2xl border p-3 text-left transition ${slotId === s.id ? 'border-brand-500 bg-brand-50 ring-1 ring-brand-300' : 'border-slate-200 hover:border-slate-300'}`}
                  >
                    <p className="text-sm font-semibold text-slate-900">
                      {start.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric' })} · {start.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">{Math.max(available, 0)} slots left</p>
                  </button>
                );
              })}
            </div>
          </Card>

          <Card className="p-6">
            <div className="flex items-center gap-2"><CreditCard className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Payment</h2></div>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              {['CARD', 'UPI', 'COD'].map((m) => (
                <button
                  key={m}
                  onClick={() => setPaymentMethod(m)}
                  className={`rounded-2xl border p-3 text-sm font-semibold transition ${paymentMethod === m ? 'border-brand-500 bg-brand-50 text-brand-700 ring-1 ring-brand-300' : 'border-slate-200 text-slate-700 hover:border-slate-300'}`}
                >
                  {m === 'CARD' ? 'Card' : m === 'UPI' ? 'UPI' : 'Cash on delivery'}
                </button>
              ))}
            </div>
          </Card>
        </div>

        <div>
          <Card className="p-6">
            <h2 className="text-lg font-bold text-slate-900">Order summary</h2>
            <div className="mt-3 max-h-60 space-y-2 overflow-y-auto text-sm">
              {cart.items.map((i) => (
                <div key={i.productId} className="flex justify-between gap-2">
                  <span className="truncate text-slate-600">{i.productName} × {i.quantity}</span>
                  <span className="shrink-0 font-medium">{fmtINR(i.unitPrice * i.quantity)}</span>
                </div>
              ))}
            </div>
            <div className="mt-4 space-y-2 border-t border-slate-200 pt-3 text-sm">
              <div className="flex justify-between text-slate-600"><span>Subtotal</span><span>{fmtINR(cart.subtotal)}</span></div>
              {cart.discount > 0 && <div className="flex justify-between text-emerald-600"><span>Discount</span><span>-{fmtINR(cart.discount)}</span></div>}
            </div>
            <div className="mt-3">
              <Input value={discountCode} onChange={(e) => setDiscountCode(e.target.value)} placeholder="Discount code (optional)" />
            </div>
            <div className="mt-4 flex justify-between border-t border-slate-200 pt-3 text-base font-bold">
              <span>Total</span><span>{fmtINR(cart.total)}</span>
            </div>
            <Button className="mt-5 w-full" size="lg" loading={placing} onClick={placeOrder}>
              Place order
            </Button>
            <p className="mt-3 text-center text-xs text-slate-400">Total includes delivery fee. Cancel anytime before dispatch.</p>
          </Card>
        </div>
      </div>
    </div>
  );
}
