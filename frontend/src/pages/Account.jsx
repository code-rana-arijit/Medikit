import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { useAuth } from '../lib/auth';
import { Button, Card, Input, Alert, Badge, Spinner } from '../components/ui';
import { User, MapPin, Plus, Star, Trash2, Building2, Gift } from 'lucide-react';

export default function Account() {
  const { user, loadMe } = useAuth();
  const [profile, setProfile] = useState({ fullName: '', email: '' });
  const [addresses, setAddresses] = useState([]);
  const [newAddress, setNewAddress] = useState({ addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', latitude: 18.5204, longitude: 73.8567, type: 'HOME' });
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    setProfile({ fullName: user.fullName, email: user.email });
    api.get('/users/me/addresses').then(setAddresses).catch(() => {});
    setLoading(false);
  }, [user]);

  const saveProfile = async () => {
    try {
      await api.put('/users/me', profile);
      setOk('Profile updated');
      await loadMe();
      setTimeout(() => setOk(''), 2000);
    } catch (e) { setError(e.message); }
  };

  const addAddress = async () => {
    try {
      const res = await api.post('/users/me/addresses', newAddress);
      setAddresses([...addresses, res]);
      setNewAddress({ addressLine1: '', addressLine2: '', city: '', state: '', pincode: '', latitude: 18.5204, longitude: 73.8567, type: 'HOME' });
      setOk('Address added');
      setTimeout(() => setOk(''), 2000);
    } catch (e) { setError(e.message); }
  };

  const upgrade = async (role) => {
    try {
      await api.put('/users/me/role', { role });
      setOk(`Role upgraded to ${role}. Re-login recommended.`);
      await loadMe();
    } catch (e) { setError(e.message); }
  };

  if (loading) return <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>;

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
      <h1 className="text-2xl font-bold text-slate-900">My account</h1>
      <p className="mb-6 text-sm text-slate-500">Manage your profile, addresses and roles</p>

      {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <div className="flex items-center gap-2"><User className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Profile</h2></div>
          <div className="mt-4 space-y-3">
            <Input label="Full name" value={profile.fullName} onChange={(e) => setProfile({ ...profile, fullName: e.target.value })} />
            <Input label="Email" value={profile.email} onChange={(e) => setProfile({ ...profile, email: e.target.value })} />
            <div className="flex items-center gap-2">
              <span className="text-sm text-slate-500">Role:</span>
              <Badge color={user?.role === 'DISTRIBUTOR' ? 'violet' : user?.role === 'PHARMACIST' ? 'blue' : 'green'}>{user?.role}</Badge>
            </div>
            <Button onClick={saveProfile}>Save changes</Button>
          </div>
        </Card>

        <Card className="p-6">
          <div className="flex items-center gap-2"><Building2 className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Partner roles</h2></div>
          <p className="mt-2 text-sm text-slate-500">
            Upgrade your account to open the distributor or pharmacy portal.
          </p>
          <div className="mt-4 flex gap-3">
            <Button variant={user?.role === 'DISTRIBUTOR' ? 'outline' : 'primary'} disabled={user?.role === 'DISTRIBUTOR'} onClick={() => upgrade('DISTRIBUTOR')}>
              Become a distributor
            </Button>
            <Button variant={user?.role === 'PHARMACIST' ? 'outline' : 'secondary'} disabled={user?.role === 'PHARMACIST'} onClick={() => upgrade('PHARMACIST')}>
              Become a pharmacy
            </Button>
          </div>
          <div className="mt-4 border-t border-slate-100 pt-4">
            <Link to="/loyalty" className="flex items-center gap-2 text-sm font-semibold text-brand-600 hover:text-brand-700">
              <Gift className="h-4 w-4" /> View loyalty & rewards
            </Link>
          </div>
        </Card>

        <Card className="p-6 lg:col-span-2">
          <div className="flex items-center gap-2"><MapPin className="h-5 w-5 text-brand-600" /><h2 className="font-bold text-slate-900">Addresses</h2></div>
          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            {addresses.map((a) => (
              <div key={a.id} className="rounded-xl border border-slate-200 p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <p className="font-semibold text-slate-800">{a.addressLine1}{a.addressLine2 && `, ${a.addressLine2}`}</p>
                    <p className="text-sm text-slate-500">{a.city}, {a.state} {a.pincode}</p>
                  </div>
                  <div className="flex items-center gap-1">
                    {a.isDefault && <Star className="h-4 w-4 fill-amber-400 text-amber-400" />}
                    <button className="p-1 text-slate-400 hover:text-rose-500"><Trash2 className="h-4 w-4" /></button>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-4 grid gap-3 rounded-xl bg-slate-50 p-4 sm:grid-cols-2">
            <Input placeholder="Address line 1" value={newAddress.addressLine1} onChange={(e) => setNewAddress({ ...newAddress, addressLine1: e.target.value })} />
            <Input placeholder="Address line 2" value={newAddress.addressLine2} onChange={(e) => setNewAddress({ ...newAddress, addressLine2: e.target.value })} />
            <Input placeholder="City" value={newAddress.city} onChange={(e) => setNewAddress({ ...newAddress, city: e.target.value })} />
            <Input placeholder="State" value={newAddress.state} onChange={(e) => setNewAddress({ ...newAddress, state: e.target.value })} />
            <Input placeholder="Pincode" value={newAddress.pincode} onChange={(e) => setNewAddress({ ...newAddress, pincode: e.target.value })} />
            <Button variant="secondary" onClick={addAddress}><Plus className="h-4 w-4" /> Add address</Button>
          </div>
        </Card>
      </div>
    </div>
  );
}
