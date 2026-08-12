import { useEffect, useState } from 'react';
import DashboardLayout, { adminNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Card, Input, Button, Select, Spinner, Alert, EmptyState, StatusBadge } from '../../components/ui';
import { Search, UserCog } from 'lucide-react';

const ROLES = ['CUSTOMER', 'DISTRIBUTOR', 'PHARMACIST', 'DELIVERY_PARTNER', 'ADMIN'];

export default function AdminUsers() {
  const [query, setQuery] = useState('');
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = async (q = query) => {
    setError('');
    try {
      const r = await api.get(`/admin/users?q=${encodeURIComponent(q)}&size=100`);
      setUsers(r?.content || []);
    } catch (e) { setError(e.message); setUsers([]); }
    setLoading(false);
  };

  useEffect(() => { load(''); }, []);

  const search = (e) => { e.preventDefault(); setLoading(true); load(query); };

  const setRole = async (user, role) => {
    setSaving(user.id);
    setError(''); setNotice('');
    try {
      const r = await api.put(`/admin/users/${user.id}/role`, { role });
      setNotice(`Role for ${r.fullName} updated to ${r.role}`);
      await load();
    } catch (e) { setError(e.message); }
    setSaving(null);
  };

  return (
    <DashboardLayout title="Users & roles" subtitle="Platform control" navItems={adminNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {notice && <Alert type="success" className="mb-4" onClose={() => setNotice('')}>{notice}</Alert>}

      <form onSubmit={search} className="mb-6 flex flex-wrap items-end gap-3">
        <Input
          label="Search users"
          icon={Search}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Name, email or phone"
          className="w-80"
        />
        <Button type="submit" disabled={loading}>{loading ? 'Searching…' : 'Search'}</Button>
      </form>

      <Card className="overflow-hidden">
        {loading ? (
          <div className="flex justify-center py-16"><Spinner className="h-8 w-8" /></div>
        ) : users.length === 0 ? (
          <div className="py-16"><EmptyState icon={UserCog} title="No users found" subtitle="Search by name, email or phone to manage roles." /></div>
        ) : (
          <div className="divide-y divide-slate-100">
            {users.map((u) => (
              <div key={u.id} className="flex flex-wrap items-center justify-between gap-3 px-5 py-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="font-semibold text-slate-900">{u.fullName}</p>
                    <StatusBadge status={u.role} />
                  </div>
                  <p className="mt-0.5 truncate text-xs text-slate-500">
                    <span className="font-mono">{u.email}</span> · <span className="font-mono">{u.phone || '—'}</span>
                    {u.emailVerified && <span className="ml-2 text-emerald-600">email verified</span>}
                  </p>
                  <p className="mt-0.5 truncate font-mono text-xs text-slate-400">{u.id}</p>
                </div>
                <div className="flex items-center gap-2">
                  <Select
                    className="w-48"
                    value={u.role}
                    onChange={(e) => setRole(u, e.target.value)}
                    disabled={saving === u.id || u.role === 'ADMIN'}
                  >
                    {ROLES.map((r) => <option key={r} value={r} disabled={u.role === 'ADMIN' && r !== 'ADMIN'}>{r}</option>)}
                  </Select>                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </DashboardLayout>
  );
}
