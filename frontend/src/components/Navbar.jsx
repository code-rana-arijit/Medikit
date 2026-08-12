import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { Pill, ShoppingCart, User, LogOut, HeartPulse, Menu, X } from 'lucide-react';
import { useAuth } from '../lib/auth';
import { Button, cx } from './ui';
import { useCart } from '../lib/cart';

const navLink = ({ isActive }) =>
  cx(
    'rounded-lg px-3 py-2 text-sm font-medium transition',
    isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
  );

export default function Navbar() {
  const { user, logout } = useAuth();
  const { count } = useCart();
  const navigate = useNavigate();
  const [open, setOpen] = useUiState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6">
        <Link to="/" className="flex items-center gap-2">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white"><Pill className="h-5 w-5" /></span>
          <span className="text-lg font-bold tracking-tight text-slate-900">Medi<span className="text-brand-600">Kit</span></span>
        </Link>

        <nav className="hidden items-center gap-1 md:flex">
          <NavLink to="/products" className={navLink}>Medicines</NavLink>
          <NavLink to="/health" className={navLink}>Health Tools</NavLink>
          {user && <NavLink to="/orders" className={navLink}>Orders</NavLink>}
          {user && <NavLink to="/prescriptions" className={navLink}>Prescriptions</NavLink>}
          {user && <NavLink to="/discounts" className={navLink}>Coupons</NavLink>}
          {user?.role === 'DISTRIBUTOR' && <NavLink to="/distributor" className={navLink}>Distributor</NavLink>}
          {user?.role === 'PHARMACIST' && <NavLink to="/pharmacy" className={navLink}>Pharmacy</NavLink>}
          {user?.role === 'DELIVERY_PARTNER' && <NavLink to="/partner" className={navLink}>Partner</NavLink>}
          {user?.role === 'ADMIN' && <NavLink to="/admin" className={navLink}>Admin</NavLink>}
        </nav>

        <div className="flex items-center gap-2">
          {user ? (
            <>
              <Link to="/cart" className="relative rounded-lg p-2 text-slate-600 hover:bg-slate-100">
                <ShoppingCart className="h-5 w-5" />
                {count > 0 && (
                  <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-brand-600 px-1 text-xs font-bold text-white">
                    {count}
                  </span>
                )}
              </Link>
              <div className="hidden items-center gap-2 sm:flex">
                <Link to="/account" className="rounded-lg p-2 text-slate-600 hover:bg-slate-100"><User className="h-5 w-5" /></Link>
                <button onClick={handleLogout} className="rounded-lg p-2 text-slate-500 hover:bg-rose-50 hover:text-rose-600">
                  <LogOut className="h-5 w-5" />
                </button>
              </div>
            </>
          ) : (
            <>
              <Link to="/login"><Button variant="ghost" size="sm">Sign in</Button></Link>
              <Link to="/register"><Button size="sm">Create account</Button></Link>
            </>
          )}
          <button className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 md:hidden" onClick={() => setOpen(!open)}>
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {open && (
        <div className="border-t border-slate-200 bg-white px-4 py-3 md:hidden">
          <div className="flex flex-col gap-1">
            <NavLink to="/products" className={navLink} onClick={() => setOpen(false)}>Medicines</NavLink>
            <NavLink to="/health" className={navLink} onClick={() => setOpen(false)}>Health Tools</NavLink>
            {user && <NavLink to="/orders" className={navLink} onClick={() => setOpen(false)}>Orders</NavLink>}
            {user && <NavLink to="/prescriptions" className={navLink} onClick={() => setOpen(false)}>Prescriptions</NavLink>}
            {user && <NavLink to="/discounts" className={navLink} onClick={() => setOpen(false)}>Coupons</NavLink>}
            {user && <NavLink to="/account" className={navLink} onClick={() => setOpen(false)}>Account</NavLink>}
            {user?.role === 'DISTRIBUTOR' && <NavLink to="/distributor" className={navLink} onClick={() => setOpen(false)}>Distributor</NavLink>}
            {user?.role === 'PHARMACIST' && <NavLink to="/pharmacy" className={navLink} onClick={() => setOpen(false)}>Pharmacy</NavLink>}
            {user?.role === 'DELIVERY_PARTNER' && <NavLink to="/partner" className={navLink} onClick={() => setOpen(false)}>Partner</NavLink>}
            {user?.role === 'ADMIN' && <NavLink to="/admin" className={navLink} onClick={() => setOpen(false)}>Admin</NavLink>}
            {!user && (
              <div className="mt-2 flex gap-2">
                <Link to="/login" className="flex-1"><Button variant="secondary" className="w-full">Sign in</Button></Link>
                <Link to="/register" className="flex-1"><Button className="w-full">Create account</Button></Link>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  );
}

export function Footer() {
  return (
    <footer className="mt-auto border-t border-slate-200 bg-white">
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-12 sm:px-6 md:grid-cols-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-white"><Pill className="h-4 w-4" /></span>
            <span className="text-lg font-bold text-slate-900">Medi<span className="text-brand-600">Kit</span></span>
          </div>
          <p className="mt-3 max-w-xs text-sm text-slate-500">
            Medicine, delivered in minutes. Health intelligence, loyalty rewards and distributor supply in one platform.
          </p>
        </div>
        <div>
          <h4 className="text-sm font-semibold text-slate-900">Shop</h4>
          <ul className="mt-3 space-y-2 text-sm text-slate-500">
            <li><Link to="/products" className="hover:text-brand-600">All medicines</Link></li>
            <li><Link to="/products?type=OTC" className="hover:text-brand-600">OTC</Link></li>
            <li><Link to="/products?rx=1" className="hover:text-brand-600">Prescription</Link></li>
            <li><Link to="/health" className="hover:text-brand-600">Health tools</Link></li>
            <li><Link to="/campaigns" className="hover:text-brand-600">Offers & campaigns</Link></li>
            <li><Link to="/discounts" className="hover:text-brand-600">My coupons</Link></li>
            <li><Link to="/prescriptions" className="hover:text-brand-600">Prescriptions</Link></li>
          </ul>
        </div>
        <div>
          <h4 className="text-sm font-semibold text-slate-900">Partners</h4>
          <ul className="mt-3 space-y-2 text-sm text-slate-500">
            <li><Link to="/distributor" className="hover:text-brand-600">Distributor portal</Link></li>
            <li><Link to="/pharmacy" className="hover:text-brand-600">Pharmacy admin</Link></li>
            <li><Link to="/partner" className="hover:text-brand-600">Delivery partner</Link></li>
          </ul>
        </div>
        <div>
          <h4 className="text-sm font-semibold text-slate-900">About</h4>
          <ul className="mt-3 space-y-2 text-sm text-slate-500">
            <li><HeartPulse className="mr-1 inline h-4 w-4 text-brand-500" />Built for 50K concurrent users</li>
          </ul>
        </div>
      </div>
      <div className="border-t border-slate-100 py-5 text-center text-xs text-slate-400">
        © {new Date().getFullYear()} MediKit. Demo platform — not for real medical purchases.
      </div>
    </footer>
  );
}

function useUiState(initial) {
  const [v, setV] = useState(initial);
  return [v, setV];
}
