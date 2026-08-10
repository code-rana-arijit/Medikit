import { useState } from 'react';
import { api } from '../lib/api';
import { Card, Button, Input, Badge, Alert, Spinner, cx } from '../components/ui';
import { HeartPulse, Pill, Stethoscope, MessageSquare, FileText, AlertTriangle, Send } from 'lucide-react';

const severities = {
  CONTRAINDICATED: 'bg-rose-100 text-rose-700',
  MAJOR: 'bg-orange-100 text-orange-700',
  MODERATE: 'bg-amber-100 text-amber-700',
  MINOR: 'bg-yellow-50 text-yellow-700',
};

export default function Health() {
  const [tab, setTab] = useState('interactions');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);

  const [interactionDrugs, setInteractionDrugs] = useState('');
  const [symptomInput, setSymptomInput] = useState('');
  const [prescriptionText, setPrescriptionText] = useState('');
  const [chat, setChat] = useState('');
  const [chatHistory, setChatHistory] = useState([]);

  const parseList = (s) => s.split(',').map((x) => x.trim()).filter(Boolean);

  const run = async (path, body) => {
    setLoading(true);
    setError('');
    try {
      const res = await api.post(path, body);
      setResult(res);
    } catch (e) {
      setError(e.message);
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  const sendChat = async (e) => {
    e.preventDefault();
    if (!chat.trim()) return;
    const userMsg = chat;
    setChat('');
    setChatHistory((h) => [...h, { role: 'user', text: userMsg }]);
    setLoading(true);
    try {
      const res = await api.post('/health/assistant/chat', { message: userMsg, contextDrugs: [] });
      setChatHistory((h) => [...h, { role: 'assistant', text: res.summary || res.message || res.answer, raw: res }]);
    } catch (e) {
      setChatHistory((h) => [...h, { role: 'assistant', text: 'Sorry, the assistant is unavailable right now.' }]);
    } finally {
      setLoading(false);
    }
  };

  const tabs = [
    { id: 'interactions', label: 'Drug interactions', icon: Pill },
    { id: 'symptoms', label: 'Symptom analysis', icon: Stethoscope },
    { id: 'prescription', label: 'Prescription scan', icon: FileText },
    { id: 'chat', label: 'Health assistant', icon: MessageSquare },
  ];

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-900">AI Health Intelligence</h1>
        <p className="text-sm text-slate-500">Free, instant, rule-based health guidance powered by MediKit.</p>
      </div>

      <div className="mb-6 flex flex-wrap gap-2">
        {tabs.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => { setTab(id); setResult(null); setError(''); }}
            className={cx('flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition', tab === id ? 'bg-brand-600 text-white shadow-sm' : 'bg-white text-slate-600 ring-1 ring-slate-200 hover:bg-slate-50')}
          >
            <Icon className="h-4 w-4" /> {label}
          </button>
        ))}
      </div>

      {error && <Alert type="error" onClose={() => setError('')}>{error}</Alert>}

      <Card className="p-6">
        {tab === 'interactions' && (
          <div>
            <h2 className="font-bold text-slate-900">Check drug interactions</h2>
            <p className="mt-1 text-sm text-slate-500">Enter medicines (brands or salts), comma separated.</p>
            <div className="mt-4 flex gap-3">
              <Input value={interactionDrugs} onChange={(e) => setInteractionDrugs(e.target.value)} placeholder="e.g. Dolo 650, Metformin, Aspirin" className="flex-1" />
              <Button loading={loading} onClick={() => run('/health/interactions/check', { drugs: parseList(interactionDrugs) })}>Check</Button>
            </div>
            {loading && <div className="mt-6 flex justify-center"><Spinner /></div>}
            {result && (
              <div className="mt-6">
                <div className="mb-3 flex items-center gap-3">
                  <Badge color={result.hasCriticalInteraction ? 'red' : 'green'}>
                    {result.hasCriticalInteraction ? 'Critical interactions found' : 'No critical interactions'}
                  </Badge>
                  <span className="text-sm text-slate-500">{result.totalInteractions} interactions · {result.normalizedDrugs?.join(', ')}</span>
                </div>
                <div className="space-y-3">
                  {(result.interactions || []).map((it, i) => (
                    <div key={i} className="rounded-xl border border-slate-200 p-4">
                      <div className="flex items-center gap-2">
                        <AlertTriangle className="h-4 w-4 text-amber-500" />
                        <span className="font-semibold text-slate-900">{it.drugA} + {it.drugB}</span>
                        <span className={cx('rounded-full px-2 py-0.5 text-xs font-bold', severities[it.severity] || 'bg-slate-100 text-slate-600')}>{it.severity}</span>
                      </div>
                      {it.effect && <p className="mt-2 text-sm text-slate-600">{it.effect}</p>}
                      {it.recommendation && <p className="mt-1 text-sm font-medium text-brand-700">Advice: {it.recommendation}</p>}
                    </div>
                  ))}
                </div>
                <p className="mt-4 text-xs text-slate-400">Always consult a licensed pharmacist or doctor.</p>
              </div>
            )}
          </div>
        )}

        {tab === 'symptoms' && (
          <div>
            <h2 className="font-bold text-slate-900">Analyze symptoms</h2>
            <p className="mt-1 text-sm text-slate-500">Describe symptoms, comma separated.</p>
            <div className="mt-4 flex gap-3">
              <Input value={symptomInput} onChange={(e) => setSymptomInput(e.target.value)} placeholder="e.g. fever, headache, fatigue" className="flex-1" />
              <Button loading={loading} onClick={() => run('/health/symptoms/analyze', { symptoms: parseList(symptomInput) })}>Analyze</Button>
            </div>
            {loading && <div className="mt-6 flex justify-center"><Spinner /></div>}
            {result && (
              <div className="mt-6">
                {result.urgentActionRequired && (
                  <Alert type="warning"><b>Urgent:</b> Symptoms suggest you should seek medical attention immediately.</Alert>
                )}
                <div className="mt-4 space-y-3">
                  {(result.conditions || []).map((c, i) => (
                    <div key={i} className="rounded-xl border border-slate-200 p-4">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-slate-900">{c.condition}</span>
                        <span className="text-sm font-bold text-brand-600">{Math.round(c.score * 100)}% match</span>
                      </div>
                      {c.matchedSymptoms?.length > 0 && (
                        <p className="mt-1 text-xs text-slate-500">Matched: {c.matchedSymptoms.join(', ')}</p>
                      )}
                      {c.referralNote && <p className="mt-2 text-sm text-slate-600">{c.referralNote}</p>}
                      {c.remedies?.length > 0 && (
                        <div className="mt-2 flex flex-wrap gap-2">
                          {c.remedies.map((r, j) => (
                            <Badge key={j} color={r.otc ? 'green' : 'amber'}>{r.medicine}</Badge>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
                <p className="mt-4 text-xs text-slate-400">{result.disclaimer}</p>
              </div>
            )}
          </div>
        )}

        {tab === 'prescription' && (
          <div>
            <h2 className="font-bold text-slate-900">Scan a prescription</h2>
            <p className="mt-1 text-sm text-slate-500">Paste the medicine names from your prescription.</p>
            <div className="mt-4 space-y-3">
              <textarea
                value={prescriptionText}
                onChange={(e) => setPrescriptionText(e.target.value)}
                rows={4}
                placeholder="e.g. Tab. Dolo 650 — 1-0-1, Cap. Amoxiclav 625 — 1-0-1…"
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:border-brand-500 focus:ring-brand-200"
              />
              <Button loading={loading} onClick={() => run('/health/prescriptions/analyze', { text: prescriptionText, orderItems: [] })}>
                Analyze prescription
              </Button>
            </div>
            {loading && <div className="mt-6 flex justify-center"><Spinner /></div>}
            {result && (
              <div className="mt-6 space-y-4">
                {(result.extractedDrugs || []).map((d, i) => (
                  <div key={i} className="rounded-xl border border-slate-200 p-4">
                    <p className="font-semibold text-slate-900">
                      {d.rawTerm}
                      {d.canonicalName && d.canonicalName !== d.rawTerm && <span className="text-sm font-normal text-slate-500"> → {d.canonicalName}</span>}
                    </p>
                    {d.matchedOrderItem && <Badge color="green" className="mt-1">In your order</Badge>}
                  </div>
                ))}
                {result.interactions?.length > 0 && (
                  <Alert type="warning">
                    {(result.interactions || []).map((it) => `${it.drugA} + ${it.drugB} (${it.severity})`).join('; ')}
                  </Alert>
                )}
                {result.orderDiscrepancies?.length > 0 && (
                  <Alert type="error">Discrepancies with your order: {result.orderDiscrepancies.join('; ')}</Alert>
                )}
                {result.disclaimer && <p className="text-xs text-slate-400">{result.disclaimer}</p>}
              </div>
            )}
          </div>
        )}

        {tab === 'chat' && (
          <div>
            <h2 className="font-bold text-slate-900">Health assistant</h2>
            <p className="mt-1 text-sm text-slate-500">Ask anything about medicines, dosage or conditions.</p>
            <div className="mt-4 max-h-96 space-y-3 overflow-y-auto rounded-xl bg-slate-50 p-4">
              {chatHistory.length === 0 && (
                <p className="py-8 text-center text-sm text-slate-400">Start a conversation below.</p>
              )}
              {chatHistory.map((m, i) => (
                <div key={i} className={cx('max-w-[85%] rounded-2xl px-4 py-2.5 text-sm', m.role === 'user' ? 'ml-auto bg-brand-600 text-white' : 'bg-white text-slate-700 ring-1 ring-slate-200')}>
                  <p className="whitespace-pre-wrap">{m.text}</p>
                  {m.raw?.references?.length > 0 && (
                    <p className="mt-1 text-xs text-slate-400">Refs: {m.raw.references.join(', ')}</p>
                  )}
                </div>
              ))}
              {loading && <div className="flex justify-center py-2"><Spinner /></div>}
            </div>
            <form onSubmit={sendChat} className="mt-4 flex gap-2">
              <Input value={chat} onChange={(e) => setChat(e.target.value)} placeholder="Ask the health assistant…" className="flex-1" />
              <Button type="submit" disabled={loading || !chat.trim()}><Send className="h-4 w-4" /> Send</Button>
            </form>
          </div>
        )}
      </Card>
    </div>
  );
}
