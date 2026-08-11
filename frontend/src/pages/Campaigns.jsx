import { useEffect, useState } from 'react';
import { api, fmtINR } from '../lib/api';
import { Card, Badge, Alert, Spinner, EmptyState, Button } from '../components/ui';
import { Ticket, TrendingDown, Gift, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function Campaigns() {
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/campaigns')
      .then((r) => setCampaigns((r?.content || []).filter((c) => c.active)))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6">
      <div className="mb-6 flex items-center gap-2">
        <Ticket className="h-6 w-6 text-brand-600" />
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Active Offers</h1>
          <p className="text-sm text-slate-500">Auto-issued coupon campaigns for your next order</p>
        </div>
      </div>

      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}

      {loading ? (
        <div className="flex justify-center py-24"><Spinner className="h-8 w-8" /></div>
      ) : campaigns.length === 0 ? (
        <EmptyState icon={Ticket} title="No active campaigns" subtitle="Check back soon for new offers." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          {campaigns.map((c) => (
            <Card key={c.id} className="border-l-4 border-l-brand-500 p-5">
              <div className="flex items-center gap-3">
                <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
                  {c.firstOrderOnly ? <Gift className="h-5 w-5" /> : <TrendingDown className="h-5 w-5" />}
                </span>
                <div>
                  <p className="font-bold text-slate-900">
                    {c.discountType === 'PERCENTAGE' ? `${c.percentage}% off` : `${fmtINR(c.discountAmount)} off`}
                  </p>
                  <p className="text-xs text-slate-500">{c.name}</p>
                </div>
              </div>
              <p className="mt-3 text-sm text-slate-600">{c.description || 'Save on your medication order.'}</p>
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <Badge color={c.firstOrderOnly ? 'amber' : 'green'}>{c.firstOrderOnly ? 'First order' : 'All orders'}</Badge>
                <Badge color="slate">valid {c.validForDays} days</Badge>
                {c.issuedCodes < c.totalCodes && <Badge color="blue">{c.totalCodes - c.issuedCodes} codes left</Badge>}
              </div>
              <div className="mt-4">
                <Link to="/discounts"><Button variant="secondary" size="sm">My coupons <ArrowRight className="h-3.5 w-3.5" /></Button></Link>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
