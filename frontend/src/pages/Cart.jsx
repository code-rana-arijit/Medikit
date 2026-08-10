import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../lib/cart';
import { fmtINR } from '../lib/api';
import { Button, Card, EmptyState, Spinner } from '../components/ui';
import { Minus, Plus, Trash2, ShoppingCart, ArrowRight, Pill } from 'lucide-react';

export default function Cart() {
  const { cart, updateQty, removeItem, clear, load } = useCart();
  const navigate = useNavigate();

  if (!cart) {
    return <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>;
  }

  if (cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16">
        <EmptyState
          icon={ShoppingCart}
          title="Your cart is empty"
          subtitle="Browse medicines and add items to get started."
          action={<Link to="/products"><Button>Browse medicines</Button></Link>}
        />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Your cart</h1>
        <button className="text-sm font-medium text-rose-600 hover:text-rose-700" onClick={() => clear().then(load)}>
          Clear cart
        </button>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          {cart.items.map((item) => (
            <Card key={item.productId} className="flex items-center gap-4 p-4">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-600">
                {item.imageUrl ? <img src={item.imageUrl} alt={item.productName} className="h-full w-full rounded-xl object-cover" /> : <Pill className="h-7 w-7" />}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate font-semibold text-slate-900">{item.productName}</p>
                <p className="text-sm text-slate-500">
                  {fmtINR(item.unitPrice)} × {item.quantity} = <span className="font-semibold text-slate-800">{fmtINR(item.unitPrice * item.quantity)}</span>
                </p>
                {item.mrp > item.unitPrice && <p className="text-xs text-slate-400 line-through">MRP {fmtINR(item.mrp)}</p>}
              </div>
              <div className="flex items-center gap-2">
                <button className="rounded-lg border border-slate-300 p-2 text-slate-600 hover:text-brand-600" onClick={() => updateQty(item.productId, item.quantity - 1)}>
                  <Minus className="h-4 w-4" />
                </button>
                <span className="w-8 text-center font-semibold">{item.quantity}</span>
                <button className="rounded-lg border border-slate-300 p-2 text-slate-600 hover:text-brand-600" onClick={() => updateQty(item.productId, item.quantity + 1)}>
                  <Plus className="h-4 w-4" />
                </button>
                <button className="ml-2 rounded-lg p-2 text-rose-500 hover:bg-rose-50" onClick={() => removeItem(item.productId)}>
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </Card>
          ))}
        </div>

        <div>
          <Card className="p-6">
            <h2 className="text-lg font-bold text-slate-900">Order summary</h2>
            <div className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between text-slate-600">
                <span>Subtotal ({cart.itemCount} items)</span>
                <span>{fmtINR(cart.subtotal)}</span>
              </div>
              {cart.discount > 0 && (
                <div className="flex justify-between text-emerald-600">
                  <span>Discount</span>
                  <span>-{fmtINR(cart.discount)}</span>
                </div>
              )}
              <div className="flex justify-between border-t border-slate-200 pt-3 text-base font-bold text-slate-900">
                <span>Total</span>
                <span>{fmtINR(cart.total)}</span>
              </div>
            </div>
            <Button className="mt-5 w-full" size="lg" onClick={() => navigate('/checkout')}>
              Proceed to checkout <ArrowRight className="h-4 w-4" />
            </Button>
            <p className="mt-3 text-center text-xs text-slate-400">Delivery slot and fee chosen at checkout</p>
          </Card>
        </div>
      </div>
    </div>
  );
}
