import { useState } from 'react';
import { Search, AlertCircle, X, CheckCircle2, Loader2 } from 'lucide-react';

export function cx(...classes) {
  return classes.filter(Boolean).join(' ');
}

export function Spinner({ className }) {
  return <Loader2 className={cx('h-5 w-5 animate-spin text-brand-600', className)} />;
}

export function Button({ variant = 'primary', size = 'md', className, children, loading, ...props }) {
  const base = 'inline-flex items-center justify-center gap-2 font-semibold rounded-xl transition-all duration-150 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';
  const variants = {
    primary: 'bg-brand-600 text-white hover:bg-brand-700 shadow-sm shadow-brand-600/20',
    secondary: 'bg-white text-slate-700 border border-slate-300 hover:border-slate-400 hover:bg-slate-50',
    outline: 'bg-transparent text-brand-700 border border-brand-300 hover:bg-brand-50',
    danger: 'bg-rose-600 text-white hover:bg-rose-700',
    ghost: 'bg-transparent text-slate-600 hover:bg-slate-100',
    dark: 'bg-slate-900 text-white hover:bg-slate-800',
  };
  const sizes = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2.5 text-sm',
    lg: 'px-6 py-3 text-base',
  };
  return (
    <button
      className={cx(base, variants[variant], sizes[size], className)}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading ? <Spinner className="h-4 w-4 text-current" /> : null}
      {children}
    </button>
  );
}

export function Input({ label, error, className, icon: Icon, ...props }) {
  return (
    <label className="block w-full">
      {label && <span className="mb-1.5 block text-sm font-medium text-slate-700">{label}</span>}
      <div className="relative">
        {Icon && <Icon className="pointer-events-none absolute left-3 top-1/2 h-4.5 w-4.5 -translate-y-1/2 text-slate-400" />}
        <input
          className={cx(
            'w-full rounded-xl border bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 transition focus:outline-none focus:ring-2',
            Icon && 'pl-10',
            error ? 'border-rose-400 focus:ring-rose-200' : 'border-slate-300 focus:border-brand-500 focus:ring-brand-200',
            className,
          )}
          {...props}
        />
      </div>
      {error && <span className="mt-1 block text-xs text-rose-600">{error}</span>}
    </label>
  );
}

export function Select({ label, children, className, ...props }) {
  return (
    <label className="block w-full">
      {label && <span className="mb-1.5 block text-sm font-medium text-slate-700">{label}</span>}
      <select
        className={cx('w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:border-brand-500 focus:ring-brand-200', className)}
        {...props}
      >
        {children}
      </select>
    </label>
  );
}

export function Card({ className, children, ...props }) {
  return (
    <div className={cx('rounded-2xl border border-slate-200 bg-white shadow-sm', className)} {...props}>
      {children}
    </div>
  );
}

export function Badge({ color = 'slate', children, className }) {
  const colors = {
    slate: 'bg-slate-100 text-slate-700',
    green: 'bg-emerald-100 text-emerald-700',
    amber: 'bg-amber-100 text-amber-700',
    red: 'bg-rose-100 text-rose-700',
    blue: 'bg-sky-100 text-sky-700',
    violet: 'bg-violet-100 text-violet-700',
    brand: 'bg-brand-100 text-brand-800',
  };
  return (
    <span className={cx('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold', colors[color], className)}>
      {children}
    </span>
  );
}

export function StatusBadge({ status }) {
  const map = {
    PENDING: ['amber', 'Pending'],
    CONFIRMED: ['blue', 'Confirmed'],
    PROCESSING: ['blue', 'Processing'],
    SHIPPED: ['violet', 'Shipped'],
    IN_TRANSIT: ['violet', 'In Transit'],
    DELIVERED: ['green', 'Delivered'],
    COMPLETED: ['green', 'Completed'],
    CANCELLED: ['red', 'Cancelled'],
    FAILED: ['red', 'Failed'],
    CLAIMED: ['amber', 'Claimed'],
    PICKED_UP: ['blue', 'Picked up'],
    REFUNDED: ['slate', 'Refunded'],
    PAID: ['green', 'Paid'],
    ACTIVE: ['green', 'Active'],
    INACTIVE: ['slate', 'Inactive'],
    EXPIRED: ['red', 'Expired'],
    SUCCESS: ['green', 'Success'],
    UNUSED: ['blue', 'Unused'],
    AVAILABLE: ['green', 'Available'],
  };
  const [color, label] = map[status] || ['slate', status];
  return <Badge color={color}>{label}</Badge>;
}

export function Alert({ type = 'error', children, onClose }) {
  const styles = {
    error: 'border-rose-200 bg-rose-50 text-rose-700',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-700',
    info: 'border-sky-200 bg-sky-50 text-sky-700',
    warning: 'border-amber-200 bg-amber-50 text-amber-700',
  };
  return (
    <div className={cx('flex items-start gap-2.5 rounded-xl border px-4 py-3 text-sm', styles[type])}>
      {type === 'error' && <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />}
      {type === 'success' && <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />}
      <div className="flex-1">{children}</div>
      {onClose && (
        <button onClick={onClose} className="shrink-0 opacity-60 hover:opacity-100"><X className="h-4 w-4" /></button>
      )}
    </div>
  );
}

export function SearchInput({ value, onChange, placeholder = 'Search medicines, brands, salts…', onSearch, className }) {
  return (
    <form
      className={cx('relative flex w-full', className)}
      onSubmit={(e) => { e.preventDefault(); onSearch?.(); }}
    >
      <Search className="pointer-events-none absolute left-3.5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-2xl border border-slate-300 bg-white py-3 pl-11 pr-24 text-sm shadow-sm focus:outline-none focus:ring-2 focus:border-brand-500 focus:ring-brand-200"
      />
      <button type="submit" className="absolute right-1.5 top-1/2 -translate-y-1/2 rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700">
        Search
      </button>
    </form>
  );
}

export function EmptyState({ icon: Icon, title, subtitle, action }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
      {Icon && <div className="mb-4 rounded-2xl bg-slate-100 p-4"><Icon className="h-8 w-8 text-slate-400" /></div>}
      <h3 className="text-lg font-semibold text-slate-800">{title}</h3>
      {subtitle && <p className="mt-1 max-w-sm text-sm text-slate-500">{subtitle}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function StatCard({ label, value, sub, icon: Icon, accent = 'brand' }) {
  const accents = {
    brand: 'bg-brand-50 text-brand-600',
    amber: 'bg-amber-50 text-amber-600',
    blue: 'bg-sky-50 text-sky-600',
    violet: 'bg-violet-50 text-violet-600',
    rose: 'bg-rose-50 text-rose-600',
  };
  return (
    <Card className="p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-slate-500">{label}</p>
          <p className="mt-1 text-2xl font-bold text-slate-900">{value}</p>
          {sub && <p className="mt-1 text-xs text-slate-500">{sub}</p>}
        </div>
        {Icon && <div className={cx('rounded-xl p-2.5', accents[accent])}><Icon className="h-5 w-5" /></div>}
      </div>
    </Card>
  );
}

export function useToggle(initial = false) {
  const [on, setOn] = useState(initial);
  return [on, () => setOn(true), () => setOn(false), () => setOn((v) => !v)];
}

export function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  const pages = [];
  for (let i = 0; i < totalPages; i++) {
    if (i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) pages.push(i);
    else if (pages[pages.length - 1] !== '…') pages.push('…');
  }
  return (
    <div className="flex items-center gap-1.5">
      <button
        className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-40"
        disabled={page === 0}
        onClick={() => onChange(page - 1)}
      >
        Prev
      </button>
      {pages.map((p) =>
        p === '…' ? (
          <span key={`gap-${p}`} className="px-1 text-slate-400">…</span>
        ) : (
          <button
            key={p}
            className={cx(
              'rounded-lg px-3 py-1.5 text-sm font-semibold',
              p === page ? 'bg-brand-600 text-white' : 'border border-slate-200 text-slate-600 hover:bg-slate-50',
            )}
            onClick={() => onChange(p)}
          >
            {p + 1}
          </button>
        ),
      )}
      <button
        className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-50 disabled:opacity-40"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next
      </button>
    </div>
  );
}
