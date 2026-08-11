import { useEffect, useState } from 'react';
import DashboardLayout, { adminNav } from '../../components/DashboardLayout';
import { api, fmtINR } from '../../lib/api';
import { Card, Button, Input, Select, Spinner, EmptyState, Alert, Badge } from '../../components/ui';
import { Package, Plus, Pencil, Trash2, Tags } from 'lucide-react';

const PRODUCT_TYPES = ['MEDICINE', 'AYURVEDIC', 'HOMEOPATHY', 'HEALTH_CARE', 'VITAMINS', 'DEVICE'];

const emptyProduct = {
  name: '', description: '', saltComposition: '', manufacturer: '',
  mrp: '', sellingPrice: '', prescriptionRequired: false,
  productType: 'MEDICINE', packaging: '', packSize: '', categoryId: '',
  pharmacyId: '', imageUrl: '',
};

const emptyCategory = { name: '', description: '', sortOrder: 0 };

export default function AdminProducts() {
  const [tab, setTab] = useState('products');
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [q, setQ] = useState('');

  const [openProduct, setOpenProduct] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [pform, setPform] = useState(emptyProduct);

  const [openCategory, setOpenCategory] = useState(false);
  const [cform, setCform] = useState(emptyCategory);

  const loadProducts = async (p = 0) => {
    setLoading(true);
    try {
      const data = await api.get(`/products?page=${p}&size=12`);
      let list = data.content || [];
      if (q.trim()) list = list.filter((x) => (x.name || '').toLowerCase().includes(q.trim().toLowerCase()));
      setProducts(list);
      setTotalPages(data.totalPages || 1);
      setPage(p);
    } catch (e) { setError(e.message); }
    setLoading(false);
  };

  const loadCategories = () => {
    api.get('/categories').then(setCategories).catch(() => {});
  };

  useEffect(() => {
    loadProducts(0);
    loadCategories();
  }, []);

  const startCreate = () => { setEditingProduct(null); setPform(emptyProduct); setOpenProduct(true); };
  const startEdit = (p) => {
    setEditingProduct(p.id);
    setPform({
      name: p.name, description: p.description || '', saltComposition: p.saltComposition || '',
      manufacturer: p.manufacturer || '', mrp: p.mrp, sellingPrice: p.sellingPrice,
      prescriptionRequired: p.prescriptionRequired, productType: p.productType,
      packaging: p.packaging || '', packSize: p.packSize || '', categoryId: p.categoryId || '',
      pharmacyId: p.pharmacyId || '', imageUrl: p.imageUrl || '',
    });
    setOpenProduct(true);
  };

  const saveProduct = async () => {
    setError(''); setOk('');
    const payload = {
      ...pform,
      mrp: Number(pform.mrp),
      sellingPrice: Number(pform.sellingPrice),
      categoryId: pform.categoryId || null,
      pharmacyId: pform.pharmacyId || null,
    };
    try {
      if (editingProduct) {
        await api.put(`/products/${editingProduct}`, payload);
        setOk('Product updated');
      } else {
        await api.post('/products', payload);
        setOk('Product created');
      }
      setOpenProduct(false);
      loadProducts(page);
    } catch (e) { setError(e.message); }
  };

  const deactivateProduct = async (id) => {
    setError(''); setOk('');
    try {
      await api.del(`/products/${id}`);
      setOk('Product deactivated');
      loadProducts(page);
    } catch (e) { setError(e.message); }
  };

  const saveCategory = async () => {
    setError(''); setOk('');
    try {
      await api.post('/categories', { ...cform, sortOrder: Number(cform.sortOrder) });
      setOk('Category created');
      setOpenCategory(false);
      setCform(emptyCategory);
      loadCategories();
    } catch (e) { setError(e.message); }
  };

  const deleteCategory = async (id) => {
    setError(''); setOk('');
    try {
      await api.del(`/categories/${id}`);
      setOk('Category deleted');
      loadCategories();
    } catch (e) { setError(e.message); }
  };

  return (
    <DashboardLayout title="Products & Categories" subtitle="Platform control" navItems={adminNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" className="mb-4" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex rounded-xl bg-slate-100 p-1">
          {['products', 'categories'].map((t) => (
            <button key={t} onClick={() => setTab(t)}
              className={`rounded-lg px-4 py-1.5 text-sm font-semibold capitalize transition ${tab === t ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-500 hover:text-slate-700'}`}>
              {t}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-2">
          {tab === 'products' && (
            <>
              <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Filter by name…" className="w-52" />
              <Button onClick={startCreate}><Plus className="h-4 w-4" /> New product</Button>
            </>
          )}
          {tab === 'categories' && <Button onClick={() => setOpenCategory(true)}><Plus className="h-4 w-4" /> New category</Button>}
        </div>
      </div>

      {openProduct && (
        <Card className="mb-6 p-6">
          <h2 className="mb-4 font-bold text-slate-900">{editingProduct ? 'Edit product' : 'New product'}</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            <Input label="Name" value={pform.name} onChange={(e) => setPform({ ...pform, name: e.target.value })} />
            <Input label="Manufacturer" value={pform.manufacturer} onChange={(e) => setPform({ ...pform, manufacturer: e.target.value })} />
            <Input label="Salt composition" value={pform.saltComposition} onChange={(e) => setPform({ ...pform, saltComposition: e.target.value })} />
            <Input label="Packaging" value={pform.packaging} onChange={(e) => setPform({ ...pform, packaging: e.target.value })} />
            <Input label="Pack size" value={pform.packSize} onChange={(e) => setPform({ ...pform, packSize: e.target.value })} />
            <Select label="Type" value={pform.productType} onChange={(e) => setPform({ ...pform, productType: e.target.value })}>
              {PRODUCT_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </Select>
            <Select label="Category" value={pform.categoryId} onChange={(e) => setPform({ ...pform, categoryId: e.target.value })}>
              <option value="">No category</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
            <Input label="Pharmacy ID" value={pform.pharmacyId} onChange={(e) => setPform({ ...pform, pharmacyId: e.target.value })} placeholder="UUID" />
            <Input label="MRP (₹)" type="number" value={pform.mrp} onChange={(e) => setPform({ ...pform, mrp: e.target.value })} />
            <Input label="Selling price (₹)" type="number" value={pform.sellingPrice} onChange={(e) => setPform({ ...pform, sellingPrice: e.target.value })} />
            <Input label="Image URL" value={pform.imageUrl} onChange={(e) => setPform({ ...pform, imageUrl: e.target.value })} className="sm:col-span-2" />
            <Input label="Description" value={pform.description} onChange={(e) => setPform({ ...pform, description: e.target.value })} className="sm:col-span-2" />
            <label className="flex items-end gap-2 pb-2.5 text-sm font-medium text-slate-700">
              <input type="checkbox" checked={pform.prescriptionRequired} onChange={(e) => setPform({ ...pform, prescriptionRequired: e.target.checked })} className="h-4 w-4 rounded accent-brand-600" />
              Prescription required
            </label>
          </div>
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setOpenProduct(false)}>Cancel</Button>
            <Button onClick={saveProduct}>{editingProduct ? 'Save changes' : 'Create product'}</Button>
          </div>
        </Card>
      )}

      {openCategory && (
        <Card className="mb-6 p-6">
          <h2 className="mb-4 font-bold text-slate-900">New category</h2>
          <div className="grid gap-3 sm:grid-cols-3">
            <Input label="Name" value={cform.name} onChange={(e) => setCform({ ...cform, name: e.target.value })} />
            <Input label="Sort order" type="number" value={cform.sortOrder} onChange={(e) => setCform({ ...cform, sortOrder: e.target.value })} />
            <Input label="Description" value={cform.description} onChange={(e) => setCform({ ...cform, description: e.target.value })} />
          </div>
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setOpenCategory(false)}>Cancel</Button>
            <Button onClick={saveCategory}>Create category</Button>
          </div>
        </Card>
      )}

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : tab === 'products' ? (
        products.length === 0 ? (
          <EmptyState icon={Package} title="No products" subtitle="Add products to the catalog." />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            {products.map((p) => (
              <Card key={p.id} className="p-4">
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-3">
                    <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><Package className="h-5 w-5" /></span>
                    <div>
                      <p className="font-semibold text-slate-900">{p.name}</p>
                      <p className="text-xs text-slate-500">{p.manufacturer || '—'} · {p.categoryName || 'No category'}</p>
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <button onClick={() => startEdit(p)} className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-brand-600"><Pencil className="h-4 w-4" /></button>
                    <button onClick={() => deactivateProduct(p.id)} className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600"><Trash2 className="h-4 w-4" /></button>
                  </div>
                </div>
                <div className="mt-3 flex items-center justify-between text-sm">
                  <div>
                    <span className="font-bold text-slate-900">{fmtINR(p.sellingPrice)}</span>
                    <span className="ml-1 text-xs text-slate-400 line-through">{fmtINR(p.mrp)}</span>
                  </div>
                  <div className="flex gap-1.5">
                    {p.prescriptionRequired && <Badge color="amber">Rx</Badge>}
                    <Badge color="slate">{p.productType}</Badge>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        )
      ) : categories.length === 0 ? (
        <EmptyState icon={Tags} title="No categories" subtitle="Create categories to organize the catalog." />
      ) : (
        <div className="space-y-3">
          {categories.map((c) => (
            <Card key={c.id} className="flex items-center justify-between p-4">
              <div className="flex items-center gap-3">
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-50 text-brand-600"><Tags className="h-5 w-5" /></span>
                <div>
                  <p className="font-semibold text-slate-900">{c.name}</p>
                  <p className="text-xs text-slate-500">{c.description || 'No description'} · sort {c.sortOrder}</p>
                </div>
              </div>
              <button onClick={() => deleteCategory(c.id)} className="rounded-lg p-2 text-slate-400 hover:bg-rose-50 hover:text-rose-600"><Trash2 className="h-4 w-4" /></button>
            </Card>
          ))}
        </div>
      )}

      {tab === 'products' && totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-2">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => loadProducts(page - 1)}>Prev</Button>
          <span className="text-sm text-slate-600">Page {page + 1} of {totalPages}</span>
          <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => loadProducts(page + 1)}>Next</Button>
        </div>
      )}
    </DashboardLayout>
  );
}
