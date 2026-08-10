import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';
import { api } from '../lib/api';
import { Button, Input, Card, Alert, Select } from '../components/ui';
import { Pill, Mail, Lock, Phone, User } from 'lucide-react';

export default function Register() {
  const { register, login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', role: 'CUSTOMER' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState(null);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await register({ fullName: form.fullName, email: form.email, phone: form.phone, password: form.password });
      const data = await login({ identifier: form.email, password: form.password });
      if (form.role !== 'CUSTOMER') {
        try {
          await api.put('/users/me/role', { role: form.role });
          await login({ identifier: form.email, password: form.password });
        } catch (err) {
          setError('Account created, but role upgrade failed: ' + err.message);
        }
      }
      setRegisteredEmail(form.email);
      const role = form.role;
      setTimeout(() => navigate(role === 'DISTRIBUTOR' ? '/distributor' : role === 'PHARMACIST' ? '/pharmacy' : '/'), 1200);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-[70vh] items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-600 text-white"><Pill className="h-8 w-8" /></span>
          <h1 className="mt-4 text-2xl font-bold text-slate-900">Create your account</h1>
          <p className="mt-1 text-sm text-slate-500">Join MediKit in seconds</p>
        </div>

        <Card className="p-6">
          {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}
          {registeredEmail && !loading && (
            <Alert type="success">
              Account created for <b>{registeredEmail}</b>. {form.role !== 'CUSTOMER' ? 'Sign in to continue.' : 'You are now signed in.'}
            </Alert>
          )}
          <form onSubmit={submit} className="space-y-4">
            <Input label="Full name" icon={User} value={form.fullName} onChange={set('fullName')} placeholder="Your name" required />
            <Input label="Email" icon={Mail} type="email" value={form.email} onChange={set('email')} placeholder="you@example.com" required />
            <Input label="Phone" icon={Phone} value={form.phone} onChange={set('phone')} placeholder="9876543210" required />
            <Input label="Password" icon={Lock} type="password" value={form.password} onChange={set('password')} placeholder="Min 8 characters" required />
            <Select label="I am a…" value={form.role} onChange={set('role')}>
              <option value="CUSTOMER">Customer</option>
              <option value="PHARMACIST">Medical shop / Pharmacy</option>
              <option value="DISTRIBUTOR">Distributor (wholesale supplier)</option>
            </Select>
            <Button type="submit" className="w-full" size="lg" loading={loading}>
              Create account
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-slate-500">
            Already have an account? <Link to="/login" className="font-semibold text-brand-600 hover:text-brand-700">Sign in</Link>
          </p>
        </Card>
      </div>
    </div>
  );
}
