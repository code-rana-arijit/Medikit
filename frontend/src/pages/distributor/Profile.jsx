import { useEffect, useState } from 'react';
import DashboardLayout, { distributorNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Button, Card, Input, Alert, Spinner } from '../../components/ui';
import { useAuth } from '../../lib/auth';
import { Store, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function DistributorProfile() {
  const { loadMe } = useAuth();
  const [me, setMe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ shopName: '', licenseNumber: '', address: '', city: '' });
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');

  useEffect(() => {
    api.get('/distributors/me')
      .then((p) => { setMe(p); setForm({ shopName: p.shopName, licenseNumber: p.licenseNumber, address: p.address, city: p.city }); })
      .catch(() => setMe(null))
      .finally(() => setLoading(false));
  }, []);

  const upgradeRole = async () => {
    try {
      await api.put('/users/me/role', { role: 'DISTRIBUTOR' });
      setOk('Role upgraded to DISTRIBUTOR');
      await loadMe();
    } catch (e) { setError(e.message); }
  };

  const register = async () => {
    setSaving(true); setError(''); setOk('');
    try {
      const res = await api.post('/distributors/register', form);
      setMe(res);
      setOk('Distributor profile registered');
      await loadMe();
    } catch (e) { setError(e.message); } finally { setSaving(false); }
  };

  const update = async () => {
    setSaving(true); setError(''); setOk('');
    try {
      const res = await api.put('/distributors/me', form);
      setMe(res);
      setOk('Profile updated');
    } catch (e) { setError(e.message); } finally { setSaving(false); }
  };

  if (loading) return <DashboardLayout title="Shop Profile" subtitle="Wholesale supply" navItems={distributorNav}><div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div></DashboardLayout>;

  return (
    <DashboardLayout title="Shop Profile" subtitle="Wholesale supply" navItems={distributorNav}>
      {!me ? (
        <Card className="mx-auto max-w-xl p-8 text-center">
          <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-50 text-brand-600"><Store className="h-7 w-7" /></span>
          <h2 className="mt-4 text-xl font-bold text-slate-900">Register as a distributor</h2>
          <p className="mt-2 text-sm text-slate-500">
            Become a wholesale supplier — sell stock to other medical shops and fulfill retail customer orders alongside your own store.
          </p>
          <div className="mt-6 grid gap-3 text-left">
            <Input label="Shop name" value={form.shopName} onChange={(e) => setForm({ ...form, shopName: e.target.value })} />
            <Input label="License number" value={form.licenseNumber} onChange={(e) => setForm({ ...form, licenseNumber: e.target.value })} />
            <Input label="Address" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
            <Input label="City" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
          </div>
          {error && <Alert type="error" className="mt-4" onClose={() => setError('')}>{error}</Alert>}
          {ok && <Alert type="success" className="mt-4" onClose={() => setOk('')}>{ok}</Alert>}
          <div className="mt-6 flex flex-col gap-2">
            <Button loading={saving} onClick={register}>Register distributor profile</Button>
            <Button variant="secondary" onClick={upgradeRole}>Ensure DISTRIBUTOR role</Button>
          </div>
          <p className="mt-4 text-xs text-slate-400">
            Not sure about the role? <Link to="/account" className="font-semibold text-brand-600">Manage roles in account</Link>
          </p>
        </Card>
      ) : (
        <Card className="mx-auto max-w-xl p-8">
          <div className="mb-5 flex items-center gap-3">
            <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-600"><Store className="h-6 w-6" /></span>
            <div>
              <h2 className="text-lg font-bold text-slate-900">{me.shopName}</h2>
              <p className="text-sm text-slate-500">{me.city} · {me.licenseNumber}</p>
            </div>
          </div>
          <div className="space-y-3">
            <Input label="Shop name" value={form.shopName} onChange={(e) => setForm({ ...form, shopName: e.target.value })} />
            <Input label="License number" value={form.licenseNumber} onChange={(e) => setForm({ ...form, licenseNumber: e.target.value })} />
            <Input label="Address" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
            <Input label="City" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} />
          </div>
          {error && <Alert type="error" className="mt-4" onClose={() => setError('')}>{error}</Alert>}
          {ok && <Alert type="success" className="mt-4" onClose={() => setOk('')}>{ok}</Alert>}
          <Button className="mt-6" loading={saving} onClick={update}>Save changes</Button>
          <Link to="/distributor/catalog" className="mt-4 flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-700">
            Next: add wholesale catalog items <ArrowRight className="h-4 w-4" />
          </Link>
        </Card>
      )}
    </DashboardLayout>
  );
}
