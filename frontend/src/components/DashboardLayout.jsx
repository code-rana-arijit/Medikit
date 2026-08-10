import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { cx } from './ui';
import {
  LayoutDashboard, Store, Package, ShoppingCart, Truck, ClipboardList,
  Boxes, Pill, LogOut,
} from 'lucide-react';

const linkCls = ({ isActive }) =>
  cx(
    'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition',
    isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100',
  );

export default function DashboardLayout({ children, title, navItems, subtitle }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen bg-slate-50">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 flex-col border-r border-slate-200 bg-white lg:flex">
        <div className="flex h-16 items-center gap-2 border-b border-slate-100 px-5">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white"><Pill className="h-5 w-5" /></span>
          <div>
            <p className="text-sm font-bold leading-tight text-slate-900">MediKit</p>
            <p className="text-xs text-slate-500">{subtitle}</p>
          </div>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end} className={linkCls}>
              <item.icon className="h-5 w-5" />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-100 p-3">
          <div className="mb-2 flex items-center gap-3 rounded-xl bg-slate-50 px-3 py-2">
            <span className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-700">
              {(user?.fullName || 'U').charAt(0).toUpperCase()}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-slate-800">{user?.fullName}</p>
              <p className="text-xs text-slate-500">{user?.role}</p>
            </div>
          </div>
          <button
            onClick={() => { logout(); navigate('/'); }}
            className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-rose-600 hover:bg-rose-50"
          >
            <LogOut className="h-5 w-5" /> Sign out
          </button>
        </div>
      </aside>

      <div className="flex min-h-screen w-full flex-col lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-slate-200 bg-white/90 px-4 backdrop-blur sm:px-6">
          <div>
            <h1 className="text-lg font-bold text-slate-900">{title}</h1>
          </div>
          <div className="flex items-center gap-2 lg:hidden">
            <NavLink to="/" className="rounded-lg p-2 text-slate-600 hover:bg-slate-100"><Pill className="h-5 w-5" /></NavLink>
          </div>
        </header>
        <main className="flex-1 p-4 sm:p-6 lg:p-8">{children}</main>
      </div>
    </div>
  );
}

export const distributorNav = [
  { to: '/distributor', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/distributor/profile', label: 'Shop Profile', icon: Store },
  { to: '/distributor/catalog', label: 'Wholesale Catalog', icon: Package },
  { to: '/distributor/orders', label: 'Supply Orders', icon: ShoppingCart },
  { to: '/distributor/fulfillments', label: 'Retail Fulfillment', icon: Truck },
  { to: '/distributor/purchase', label: 'Buy Stock', icon: Boxes },
];

export const pharmacyNav = [
  { to: '/pharmacy', label: 'Dashboard', icon: LayoutDashboard, end: true },
  { to: '/pharmacy/orders', label: 'Orders', icon: ClipboardList },
  { to: '/pharmacy/inventory', label: 'Inventory', icon: Boxes },
];
