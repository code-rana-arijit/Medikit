import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Zap, ShieldCheck, HeartPulse, Truck, ArrowRight, Sparkles,
  Search, PackageCheck, Building2, Store,
} from 'lucide-react';
import { api } from '../lib/api';
import { SearchInput, Card, Badge } from '../components/ui';
import ProductCard from '../components/ProductCard';
import { useAuth } from '../lib/auth';

export default function Home() {
  const [query, setQuery] = useState('');
  const [trending, setTrending] = useState([]);
  const [categories, setCategories] = useState([]);
  const { user } = useAuth();

  useEffect(() => {
    api.get('/products/trending').then(setTrending).catch(() => {});
    api.get('/categories').then(setCategories).catch(() => {});
  }, []);

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden bg-gradient-to-br from-brand-50 via-white to-sky-50">
        <div className="absolute -right-24 -top-24 h-96 w-96 rounded-full bg-brand-200/40 blur-3xl" />
        <div className="absolute -bottom-32 -left-24 h-96 w-96 rounded-full bg-sky-200/40 blur-3xl" />
        <div className="relative mx-auto max-w-7xl px-4 py-20 sm:px-6 lg:py-28">
          <div className="mx-auto max-w-3xl text-center">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-white px-4 py-1.5 text-sm font-medium text-brand-700 shadow-sm ring-1 ring-brand-100">
              <Sparkles className="h-4 w-4" /> 15-minute medicine delivery
            </span>
            <h1 className="mt-6 text-4xl font-extrabold tracking-tight text-slate-900 sm:text-6xl">
              Medicine, delivered <span className="text-brand-600">in minutes</span>
            </h1>
            <p className="mx-auto mt-5 max-w-xl text-lg text-slate-600">
              Browse, order and track genuine medicines from licensed pharmacies — with AI health checks, loyalty rewards and instant wholesale supply for partners.
            </p>
            <div className="mx-auto mt-8 max-w-xl">
              <SearchInput value={query} onChange={setQuery} onSearch={() => {
                window.location.href = `/products?q=${encodeURIComponent(query)}`;
              }} />
            </div>
            <div className="mt-6 flex flex-wrap items-center justify-center gap-3 text-sm text-slate-500">
              {['Paracetamol', 'Azithromycin', 'Vitamin D3', 'Dolo 650'].map((s) => (
                <Link key={s} to={`/products?q=${s}`} className="rounded-full bg-white px-3 py-1 shadow-sm ring-1 ring-slate-200 hover:ring-brand-300 hover:text-brand-700">
                  {s}
                </Link>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Value props */}
      <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {[
            { icon: Zap, title: 'Instant delivery', desc: '15-minute express delivery at your doorstep' },
            { icon: ShieldCheck, title: '100% genuine', desc: 'Verified licensed partner pharmacies' },
            { icon: HeartPulse, title: 'AI health checks', desc: 'Drug interactions & symptom analysis' },
            { icon: Truck, title: 'B2B supply', desc: 'Distributor portal for wholesale stock' },
          ].map(({ icon: Icon, title, desc }) => (
            <Card key={title} className="p-6">
              <span className="inline-flex rounded-xl bg-brand-50 p-3 text-brand-600"><Icon className="h-6 w-6" /></span>
              <h3 className="mt-4 font-semibold text-slate-900">{title}</h3>
              <p className="mt-1 text-sm text-slate-500">{desc}</p>
            </Card>
          ))}
        </div>
      </section>

      {/* Categories */}
      {categories.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-6 sm:px-6">
          <div className="mb-5 flex items-center justify-between">
            <h2 className="text-xl font-bold text-slate-900">Shop by category</h2>
            <Link to="/products" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">
              View all <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6">
            {categories.slice(0, 12).map((c) => (
              <Link key={c.id} to={`/products?category=${c.id}`} className="group flex flex-col items-center rounded-2xl border border-slate-200 bg-white p-5 text-center shadow-sm transition hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-md">
                <span className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 text-brand-600 transition group-hover:bg-brand-100">
                  <PackageCheck className="h-6 w-6" />
                </span>
                <span className="mt-3 text-sm font-semibold text-slate-700">{c.name}</span>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Trending */}
      <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6">
        <div className="mb-5 flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-slate-900">Trending medicines</h2>
            <p className="text-sm text-slate-500">Most ordered right now</p>
          </div>
          <Link to="/products" className="flex items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700">
            View all <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
        {trending.length === 0 ? (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-5">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-72 animate-pulse rounded-2xl bg-slate-200" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-5">
            {trending.slice(0, 10).map((p) => <ProductCard key={p.id} product={p} />)}
          </div>
        )}
      </section>

      {/* Partner CTA */}
      <section className="mx-auto max-w-7xl px-4 pb-16 sm:px-6">
        <div className="grid gap-4 md:grid-cols-2">
          <div className="overflow-hidden rounded-3xl bg-slate-900 p-8 text-white">
            <span className="inline-flex rounded-xl bg-brand-500/20 p-3 text-brand-300"><Store className="h-6 w-6" /></span>
            <h3 className="mt-4 text-2xl font-bold">Own a medical shop?</h3>
            <p className="mt-2 text-slate-300">
              Become a distributor. Sell wholesale stock to other shops and fulfill retail orders — all from one portal.
            </p>
            <Link to="/distributor" className="mt-5 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-3 font-semibold text-white hover:bg-brand-500">
              Open distributor portal <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
          <div className="overflow-hidden rounded-3xl bg-brand-600 p-8 text-white">
            <span className="inline-flex rounded-xl bg-white/15 p-3 text-white"><Building2 className="h-6 w-6" /></span>
            <h3 className="mt-4 text-2xl font-bold">Partner pharmacy</h3>
            <p className="mt-2 text-brand-100">
              Manage pharmacy orders and inventory. Join the network reaching thousands of customers daily.
            </p>
            <Link to="/pharmacy" className="mt-5 inline-flex items-center gap-2 rounded-xl bg-white px-5 py-3 font-semibold text-brand-700 hover:bg-brand-50">
              Open pharmacy admin <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
