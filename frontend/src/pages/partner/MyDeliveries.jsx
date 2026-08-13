import { useEffect, useState } from 'react';
import DashboardLayout, { deliveryPartnerNav } from '../../components/DashboardLayout';
import { api } from '../../lib/api';
import { Card, StatusBadge, Spinner, Alert, EmptyState, Select, Button, Input } from '../../components/ui';
import { ClipboardList, MapPin, Timer, PackageCheck, RefreshCw, Share2, Crosshair } from 'lucide-react';

const NEXT_STATUS = {
  ASSIGNED: 'PICKED_UP',
  PICKED_UP: 'IN_TRANSIT',
  IN_TRANSIT: 'DELIVERED',
};

const ACTIVE = ['ASSIGNED', 'PICKED_UP', 'IN_TRANSIT'];

export default function PartnerDeliveries() {
  const [items, setItems] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [locationDrafts, setLocationDrafts] = useState({});
  const [sendingLoc, setSendingLoc] = useState(null);
  const [locError, setLocError] = useState('');

  const load = async () => {
    setError('');
    try {
      const q = statusFilter ? `?status=${encodeURIComponent(statusFilter)}` : '';
      const r = await api.get(`/delivery/partner${q}`);
      setItems(r?.content || []);
    } catch (e) { setError(e.message); setItems([]); }
    setLoading(false);
  };

  useEffect(() => { load(); }, [statusFilter]);

  const sendLocation = async (d) => {
    const draft = locationDrafts[d.id];
    if (!draft || draft.latitude === '' || draft.longitude === '') return;
    const latitude = Number(draft.latitude);
    const longitude = Number(draft.longitude);
    if (Number.isNaN(latitude) || Number.isNaN(longitude)) { setLocError('Enter valid coordinates'); return; }
    setSendingLoc(d.id); setLocError('');
    try {
      await api.put(`/delivery/${d.orderId}/location`, { latitude, longitude });
      setNotice(`Live location shared for order ${d.orderId.slice(0, 8)}`);
      setLocationDrafts((prev) => ({ ...prev, [d.id]: undefined }));
      await load();
    } catch (e) { setLocError(e.message); }
    setSendingLoc(null);
  };

  const useMyLocation = (d) => {
    if (!navigator.geolocation) { setLocError('Geolocation not supported in this browser'); return; }
    setLocError('');
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setLocationDrafts((prev) => ({
          ...prev,
          [d.id]: {
            latitude: pos.coords.latitude.toFixed(6),
            longitude: pos.coords.longitude.toFixed(6),
          },
        }));
      },
      () => setLocError('Could not fetch your location. Enter coordinates manually.'),
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  const advance = async (d) => {
    const next = NEXT_STATUS[d.status];
    if (!next) return;
    setUpdating(d.id);
    setError(''); setNotice('');
    try {
      await api.put(`/delivery/${d.orderId}/status`, {
        status: next,
        coordinates: next === 'DELIVERED' ? { latitude: d.partnerLatitude ?? null, longitude: d.partnerLongitude ?? null } : null,
      });
      setNotice(`Delivery marked as ${next}`);
      await load();
    } catch (e) { setError(e.message); }
    setUpdating(null);
  };

  const filtered = statusFilter ? items : items.filter((d) => ACTIVE.includes(d.status) || d.status === 'DELIVERED');

  return (
    <DashboardLayout title="My deliveries" subtitle="Delivery partner" navItems={deliveryPartnerNav}>
      {error && <Alert type="error" className="mb-4" onClose={() => setError('')}>{error}</Alert>}
      {notice && <Alert type="success" className="mb-4" onClose={() => setNotice('')}>{notice}</Alert>}
      {locError && <Alert type="error" className="mb-4" onClose={() => setLocError('')}>{locError}</Alert>}

      <div className="mb-4 flex flex-wrap items-end gap-3">
        <Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="w-48">
          <option value="">Active & delivered</option>
          <option value="ASSIGNED">ASSIGNED</option>
          <option value="PICKED_UP">PICKED_UP</option>
          <option value="IN_TRANSIT">IN_TRANSIT</option>
          <option value="DELIVERED">DELIVERED</option>
          <option value="CANCELLED">CANCELLED</option>
        </Select>
        <Button variant="secondary" size="sm" onClick={load}><RefreshCw className="h-4 w-4" /> Refresh</Button>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Spinner className="h-8 w-8" /></div>
      ) : filtered.length === 0 ? (
        <EmptyState icon={ClipboardList} title="No deliveries" subtitle="Deliveries you have claimed will appear here." />
      ) : (
        <div className="space-y-3">
          {filtered.map((d) => (
            <Card key={d.id} className="p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="font-semibold text-slate-900">Order {d.orderId.slice(0, 8)}</p>
                    <StatusBadge status={d.status} />
                  </div>
                  <div className="mt-1 grid gap-1 text-xs text-slate-500 sm:grid-cols-2">
                    <span className="font-mono">Order: {d.orderId}</span>
                    <span className="flex items-center gap-1"><Timer className="h-3.5 w-3.5" /> {d.estimatedMinutes} min estimate</span>
                    <span className="flex items-center gap-1 font-mono"><MapPin className="h-3.5 w-3.5" /> Customer {d.customerLatitude?.toFixed?.(4) ?? '—'}, {d.customerLongitude?.toFixed?.(4) ?? '—'}</span>
                    <span>Pharmacy: <span className="font-mono">{d.pharmacyId?.slice(0, 8)}</span></span>
                  </div>
                  {d.deliveredAt && (
                    <p className="mt-1 flex items-center gap-1 text-xs text-emerald-600">
                      <PackageCheck className="h-3.5 w-3.5" /> Delivered {new Date(d.deliveredAt).toLocaleString('en-IN')}
                    </p>
                  )}
                </div>
                <div className="flex flex-col items-end gap-2">
                  {ACTIVE.includes(d.status) && (
                    <div className="flex items-center gap-2 rounded-xl bg-slate-50 px-3 py-2">
                      <Share2 className="h-4 w-4 text-brand-600" />
                      <Input
                        className="w-24 px-2 py-1 text-xs"
                        placeholder="Lat"
                        value={locationDrafts[d.id]?.latitude ?? ''}
                        onChange={(e) => setLocationDrafts((prev) => ({ ...prev, [d.id]: { ...(prev[d.id] || {}), latitude: e.target.value } }))}
                      />
                      <Input
                        className="w-24 px-2 py-1 text-xs"
                        placeholder="Lng"
                        value={locationDrafts[d.id]?.longitude ?? ''}
                        onChange={(e) => setLocationDrafts((prev) => ({ ...prev, [d.id]: { ...(prev[d.id] || {}), longitude: e.target.value } }))}
                      />
                      <Button size="sm" variant="secondary" onClick={() => useMyLocation(d)} title="Use my current location"><Crosshair className="h-4 w-4" /></Button>
                      <Button size="sm" onClick={() => sendLocation(d)} disabled={sendingLoc === d.id} loading={sendingLoc === d.id}>
                        Share
                      </Button>
                    </div>
                  )}
                  {NEXT_STATUS[d.status] && (
                    <Button onClick={() => advance(d)} disabled={updating === d.id} loading={updating === d.id} size="sm">
                      Mark {NEXT_STATUS[d.status]}
                    </Button>
                  )}
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}
    </DashboardLayout>
  );
}
