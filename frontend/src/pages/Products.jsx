import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../lib/api';
import { SearchInput, Select, EmptyState, Button, Spinner, Badge } from '../components/ui';
import ProductCard from '../components/ProductCard';
import { SearchX, SlidersHorizontal } from 'lucide-react';

export default function Products() {
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState(params.get('q') || '');
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [filters, setFilters] = useState({
    category: params.get('category') || '',
    type: params.get('type') || '',
    rx: params.get('rx') === '1',
    sort: 'relevance',
  });

  const buildQuery = (p) => {
    const sp = new URLSearchParams();
    if (query) sp.set('q', query);
    if (filters.category) sp.set('categoryId', filters.category);
    if (filters.type) sp.set('type', filters.type);
    sp.set('page', String(p));
    sp.set('size', '12');
    return sp.toString();
  };

  const load = async (p = 0) => {
    setLoading(true);
    try {
      const data = await api.get(`/products/search?${buildQuery(p)}`);
      let list = data.content || [];
      if (filters.rx) list = list.filter((x) => x.prescriptionRequired);
      if (filters.sort === 'price_asc') list = [...list].sort((a, b) => a.sellingPrice - b.sellingPrice);
      if (filters.sort === 'price_desc') list = [...list].sort((a, b) => b.sellingPrice - a.sellingPrice);
      if (filters.sort === 'rating') list = [...list].sort((a, b) => b.rating - a.rating);
      setProducts(list);
      setTotalPages(data.totalPages || 1);
      setTotal(data.totalElements || 0);
      setPage(p);
    } catch {
      setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    api.get('/categories').then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    load(0);
  }, [filters]);

  const runSearch = () => load(0);

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Medicines</h1>
        <p className="text-sm text-slate-500">Search by medicine, brand or salt name</p>
      </div>

      <div className="mb-6 max-w-2xl">
        <SearchInput value={query} onChange={setQuery} onSearch={runSearch} />
      </div>

      <div className="mb-6 flex flex-wrap items-end gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex items-center gap-1.5 pr-1 text-sm font-semibold text-slate-700">
          <SlidersHorizontal className="h-4 w-4" /> Filters
        </div>
        <Select
          className="w-44"
          value={filters.category}
          onChange={(e) => setFilters({ ...filters, category: e.target.value })}
        >
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </Select>
        <Select
          className="w-40"
          value={filters.type}
          onChange={(e) => setFilters({ ...filters, type: e.target.value })}
        >
          <option value="">All types</option>
          <option value="OTC">OTC</option>
          <option value="PRESCRIPTION">Prescription</option>
          <option value="EQUIPMENT">Equipment</option>
          <option value="PERSONAL_CARE">Personal care</option>
        </Select>
        <Select
          className="w-40"
          value={filters.sort}
          onChange={(e) => setFilters({ ...filters, sort: e.target.value })}
        >
          <option value="relevance">Relevance</option>
          <option value="price_asc">Price: low to high</option>
          <option value="price_desc">Price: high to low</option>
          <option value="rating">Top rated</option>
        </Select>
        <label className="flex items-center gap-2 rounded-xl border border-slate-300 px-3 py-2.5 text-sm">
          <input
            type="checkbox"
            checked={filters.rx}
            onChange={(e) => setFilters({ ...filters, rx: e.target.checked })}
            className="h-4 w-4 rounded accent-brand-600"
          />
          Prescription only
        </label>
      </div>

      <p className="mb-4 text-sm text-slate-500">
        {total > 0 ? `${total} products found` : 'Searching…'}
        {filters.rx && <Badge color="amber" className="ml-2">Rx only</Badge>}
      </p>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : products.length === 0 ? (
        <EmptyState
          icon={SearchX}
          title="No medicines found"
          subtitle="Try a different name, brand or remove filters."
        />
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
          {products.map((p) => <ProductCard key={p.id} product={p} />)}
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-8 flex items-center justify-center gap-2">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => load(page - 1)}>Prev</Button>
          <span className="text-sm text-slate-600">Page {page + 1} of {totalPages}</span>
          <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => load(page + 1)}>Next</Button>
        </div>
      )}
    </div>
  );
}
