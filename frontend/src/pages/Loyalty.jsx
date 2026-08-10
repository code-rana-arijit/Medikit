import { useEffect, useState } from 'react';
import { api, fmtINR } from '../lib/api';
import { Card, Button, Input, Badge, Alert, Spinner, StatCard } from '../components/ui';
import { Gift, Sparkles, Copy, Check, ArrowRight, TrendingUp } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Loyalty() {
  const [balance, setBalance] = useState(null);
  const [txns, setTxns] = useState([]);
  const [referral, setReferral] = useState(null);
  const [redeemPts, setRedeemPts] = useState(100);
  const [referralCode, setReferralCode] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [ok, setOk] = useState('');
  const [copied, setCopied] = useState(false);
  const [loading, setLoading] = useState(true);

  const load = async () => {
    setLoading(true);
    try {
      const [b, t, r] = await Promise.all([
        api.get('/loyalty/balance').catch(() => null),
        api.get('/loyalty/transactions').catch(() => null),
        api.get('/loyalty/referral').catch(() => null),
      ]);
      setBalance(b);
      setTxns(t?.content || t || []);
      setReferral(r);
    } catch { /* ignore */ }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const redeem = async () => {
    setError(''); setOk(''); setResult(null);
    try {
      const res = await api.post('/loyalty/redeem', { points: redeemPts });
      setResult(res);
      setOk('Discount code generated!');
      load();
    } catch (e) { setError(e.message); }
  };

  const registerReferral = async () => {
    setError(''); setOk('');
    try {
      const res = await api.post('/loyalty/referrals/register', { referralCode: referralCode.trim() });
      setOk(res?.message || 'Referral registered. Your referrer earns a bonus on your first order.');
    } catch (e) { setError(e.message); }
  };

  const copyCode = () => {
    navigator.clipboard?.writeText(referral?.referralCode || '');
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  if (loading) return <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>;

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex items-center gap-2">
        <Gift className="h-6 w-6 text-brand-600" />
        <h1 className="text-2xl font-bold text-slate-900">Loyalty & Rewards</h1>
      </div>

      {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}
      {ok && <Alert type="success" onClose={() => setOk('')}>{ok}</Alert>}

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Balance" value={balance?.balancePoints ?? 0} sub="points" icon={Sparkles} />
        <StatCard label="Tier" value={balance?.tier || 'BRONZE'} icon={TrendingUp} accent="blue" />
        <StatCard label="Earn multiplier" value={`${balance?.earnMultiplier ?? 1}×`} sub="per ₹ spent" icon={Gift} accent="amber" />
        <StatCard label="Lifetime earned" value={balance?.lifetimeEarned ?? 0} sub="points" icon={Sparkles} accent="violet" />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Redeem points</h2>
          <p className="mt-1 text-sm text-slate-500">100 points = ₹10 discount. Minimum 100 points.</p>
          <div className="mt-4 flex items-center gap-3">
            <Input type="number" value={redeemPts} onChange={(e) => setRedeemPts(Number(e.target.value))} className="w-36" min={100} />
            <Button onClick={redeem}>Redeem</Button>
          </div>
          {result && (
            <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
              <p className="text-sm text-emerald-800">
                Code <span className="rounded bg-white px-2 py-0.5 font-mono font-bold text-emerald-700">{result.code}</span> — worth {fmtINR(result.discountAmount)}
              </p>
              <p className="mt-1 text-xs text-emerald-700">Use it at checkout. Remaining balance: {result.remainingBalance} pts</p>
            </div>
          )}
        </Card>

        <Card className="p-6">
          <h2 className="font-bold text-slate-900">Refer & earn</h2>
          <p className="mt-1 text-sm text-slate-500">Share your code — your referrer earns a bonus when their first order is confirmed.</p>
          {referral ? (
            <div className="mt-4 flex items-center gap-2">
              <code className="flex-1 rounded-xl bg-slate-100 px-4 py-3 font-mono text-lg font-bold text-brand-700">{referral.referralCode}</code>
              <Button variant="secondary" onClick={copyCode}>{copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}</Button>
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-400">Referral code unavailable.</p>
          )}
          <div className="mt-5 border-t border-slate-100 pt-4">
            <p className="text-sm font-medium text-slate-700">Have a referral code from a friend?</p>
            <div className="mt-2 flex gap-2">
              <Input value={referralCode} onChange={(e) => setReferralCode(e.target.value)} placeholder="REF-XXXXXX" className="flex-1" />
              <Button variant="secondary" onClick={registerReferral}>Apply</Button>
            </div>
          </div>
        </Card>
      </div>

      <Card className="mt-6 p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-bold text-slate-900">Transaction history</h2>
          <Link to="/discounts" className="hidden text-sm font-semibold text-brand-600 hover:text-brand-700">My coupons</Link>
        </div>
        {txns.length === 0 ? (
          <p className="py-6 text-center text-sm text-slate-400">No transactions yet. Your points will appear after confirmed orders.</p>
        ) : (
          <div className="divide-y divide-slate-100">
            {txns.map((t, i) => (
              <div key={i} className="flex items-center justify-between py-3">
                <div>
                  <p className="text-sm font-medium text-slate-800">{t.description || t.type}</p>
                  <p className="text-xs text-slate-400">{new Date(t.createdAt).toLocaleString('en-IN')}</p>
                </div>
                <span className={`font-bold ${t.points >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                  {t.points >= 0 ? '+' : ''}{t.points} pts
                </span>
              </div>
            ))}
          </div>
        )}
      </Card>

      <div className="mt-6">
        <Link to="/discounts" className="inline-flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-700">
          View my coupons <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
    </div>
  );
}
