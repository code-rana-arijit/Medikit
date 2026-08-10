import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../lib/api';
import { useAuth } from '../lib/auth';
import { Button, Input, Card, Alert, Spinner } from '../components/ui';
import { Pill, Mail, Lock } from 'lucide-react';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const data = await login({ identifier, password });
      if (data.user?.role === 'DISTRIBUTOR') navigate('/distributor');
      else if (data.user?.role === 'PHARMACIST') navigate('/pharmacy');
      else navigate('/');
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
          <h1 className="mt-4 text-2xl font-bold text-slate-900">Welcome back</h1>
          <p className="mt-1 text-sm text-slate-500">Sign in to MediKit</p>
        </div>

        <Card className="p-6">
          {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}
          <form onSubmit={submit} className="space-y-4">
            <Input
              label="Email or phone"
              icon={Mail}
              value={identifier}
              onChange={(e) => setIdentifier(e.target.value)}
              placeholder="you@example.com or 9876543210"
              required
            />
            <Input
              label="Password"
              icon={Lock}
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
            <Button type="submit" className="w-full" size="lg" loading={loading}>
              Sign in
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-slate-500">
            New to MediKit? <Link to="/register" className="font-semibold text-brand-600 hover:text-brand-700">Create an account</Link>
          </p>
        </Card>
      </div>
    </div>
  );
}
