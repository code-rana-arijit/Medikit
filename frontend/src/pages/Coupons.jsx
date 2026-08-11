import { useEffect, useState } from 'react';
import { api, fmtINR } from '../lib/api';
import { Card, Badge, Alert, Spinner, Button, EmptyState } from '../components/ui';
import { Ticket, Copy, Check, Gift, Link as LinkIcon, TrendingDown } from 'lucide-react';
import { useCopy } from '../lib/analytics';
import { Link } from 'react-router-dom';

export default function Coupons() {
  const [codes, setCodes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copied, copy] = useCopy();

  const load = () => {
    setLoading(true);
    api.get('/discounts/my')
      .then((r) => setCodes(r?.content || []))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const statusColor = (status) =>
    status === 'AVAILABLE' || status === 'ACTIVE' ? 'green' : status === 'USED' || status === 'REDEEMED' ? 'slate' : 'red';

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex items-center gap-2">
        <Ticket className="h-6 w-6 text-brand-600" />
        <div>
          <h1 className="text-2xl font-bold text-slate-900">My Coupons</h1>
          <p className="text-sm text-slate-500">Discount codes issued to your account</p>
        </div>
      </div>

      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>
      ) : codes.length === 0 ? (
        <EmptyState
          icon={Ticket}
          title="No coupons yet"
          subtitle="Coupons from campaigns, referrals and loyalty redemptions will appear here."
          action={
            <div className="flex gap-2">
              <Link to="/loyalty"><Button variant="secondary">Redeem loyalty points</Button></Link>
              <Link to="/campaigns"><Button>Browse offers</Button></Link>
            </div>
          }
        />
      ) : (
        <div className="space-y-4">
          {codes.map((c) => {
            const active = c.status === 'AVAILABLE' || c.status === 'ACTIVE';
            return (
              <Card key={c.code} className="flex flex-wrap items-center justify-between gap-4 border-l-4 border-l-brand-500 p-5">
                <div className="flex items-center gap-4">
                  <span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
                    {c.firstOrderOnly ? <Gift className="h-6 w-6" /> : <TrendingDown className="h-6 w-6" />}
                  </span>
                  <div>
                    <p className="text-sm font-bold text-slate-900">
                      {c.discountType === 'PERCENTAGE'
                        ? `${c.percentage}% off`
                        : `${fmtINR(c.discountAmount)} off`}
                      {c.firstOrderOnly && <span className="ml-2 text-xs font-semibold text-brand-600">First order only</span>}
                    </p>
                    <p className="text-xs text-slate-500">{c.title || `Campaign ${c.campaignId?.slice(0, 8)}`}</p>
                    <p className="mt-1 text-xs text-slate-400">
                      Expires {c.expiresAt ? new Date(c.expiresAt).toLocaleString('en-IN') : 'never'}
                      {c.redeemedOrderId && <> · used on {c.redeemedOrderId.slice(0, 8)}</>}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Badge color={statusColor(c.status)}>{c.status}</Badge>
                  <code className="rounded-lg bg-slate-100 px-3 py-1.5 font-mono text-sm font-bold text-slate-800">{c.code}</code>
                  <Button size="sm" variant={active ? 'primary' : 'secondary'} disabled={!active} onClick={() => copy(c.code)}>
                    {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />} {copied ? 'Copied' : 'Copy'}
                  </Button>
                </div>
              </Card>
            );
          })}
          <p className="flex items-center gap-1.5 text-xs text-slate-400">
            <LinkIcon className="h-3.5 w-3.5" /> Enter the code at checkout to apply the discount.
          </p>
        </div>
      )}
    </div>
  );
}
