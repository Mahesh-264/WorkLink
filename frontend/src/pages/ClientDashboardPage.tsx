import { type FormEvent, type ReactNode, useEffect, useState } from 'react';
import axios from 'axios';
import { BriefcaseBusiness, Building2, Globe, LogOut, Mail, MapPin, Pencil, Save } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { clearSession, getStoredUser } from '../lib/auth';
import api from '../lib/api';
import ClientProjectsSection from '../components/ClientProjectsSection';

interface ClientProfile {
  id: number;
  companyName: string;
  companyDescription: string | null;
  industry: string | null;
  website: string | null;
  location: string | null;
  contactInformation: string;
}

type ClientProfilePayload = Omit<ClientProfile, 'id'>;
interface ApiError { message?: string; fieldErrors?: Record<string, string>; }

const emptyProfile: ClientProfilePayload = {
  companyName: '', companyDescription: '', industry: '', website: '', location: '', contactInformation: '',
};

export default function ClientDashboardPage() {
  const user = getStoredUser();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<ClientProfile | null>(null);
  const [form, setForm] = useState<ClientProfilePayload>(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    api.get<ClientProfile>('/client/profile')
      .then(({ data }) => { setProfile(data); setForm(toPayload(data)); })
      .catch((requestError: unknown) => {
        if (axios.isAxiosError(requestError) && requestError.response?.status === 404) setEditing(true);
        else setError(messageFrom(requestError, 'We could not load your company profile. Please try again.'));
      })
      .finally(() => setLoading(false));
  }, []);

  const updateField = (field: keyof ClientProfilePayload, value: string) => setForm((current) => ({ ...current, [field]: value }));
  const logout = () => { clearSession(); navigate('/', { replace: true }); };
  const startEditing = () => { setError(null); setSuccess(null); setEditing(true); };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null); setSuccess(null);
    if (!form.companyName.trim() || !form.contactInformation.trim()) {
      setError('Company name and contact information are required.'); return;
    }
    if (form.website && !isUrl(form.website)) { setError('Enter a valid website URL, including https://.'); return; }
    setSubmitting(true);
    try {
      const payload = trimPayload(form);
      const { data } = profile
        ? await api.put<ClientProfile>('/client/profile', payload)
        : await api.post<ClientProfile>('/client/profile', payload);
      setProfile(data); setForm(toPayload(data)); setEditing(false);
      setSuccess(profile ? 'Your company profile has been updated.' : 'Your company profile is ready.');
    } catch (requestError) {
      setError(messageFrom(requestError, 'We could not save your profile. Please try again.'));
    } finally { setSubmitting(false); }
  };

  if (!user) return null;
  return <DashboardShell role="Client" name={`${user.firstName} ${user.lastName}`} onLogout={logout}>
    {loading ? <LoadingCard label="Loading your company profile" /> : <>
      {error && <Alert kind="error" message={error} />}
      {success && <Alert kind="success" message={success} />}
      {editing ? <ClientProfileForm form={form} onChange={updateField} onSubmit={submit} submitting={submitting} isNew={!profile} /> : profile ? <ClientProfileView profile={profile} onEdit={startEditing} /> : null}
    </>}
  <ClientProjectsSection /></DashboardShell>;
}

function ClientProfileView({ profile, onEdit }: { profile: ClientProfile; onEdit: () => void }) {
  return <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="border-b border-slate-100 bg-gradient-to-r from-indigo-600 to-violet-600 px-6 py-8 text-white sm:px-8">
      <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-start"><div className="flex gap-4"><div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white/15"><Building2 /></div><div><p className="text-sm font-medium text-indigo-100">Company profile</p><h1 className="mt-1 text-2xl font-bold">{profile.companyName}</h1>{profile.industry && <p className="mt-1 text-indigo-100">{profile.industry}</p>}</div></div><button onClick={onEdit} className="inline-flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-indigo-700 hover:bg-indigo-50"><Pencil className="h-4 w-4" />Edit profile</button></div>
    </div>
    <div className="grid gap-8 p-6 sm:p-8 lg:grid-cols-[1.5fr_1fr]"><div><h2 className="text-base font-semibold text-slate-900">About the company</h2><p className="mt-3 whitespace-pre-line leading-7 text-slate-600">{profile.companyDescription || 'Add a company description to help professionals understand your team and opportunities.'}</p></div><dl className="space-y-4 rounded-xl bg-slate-50 p-5 text-sm"><Detail icon={<MapPin />} label="Location" value={profile.location} /><Detail icon={<Globe />} label="Website" value={profile.website} link /><Detail icon={<Mail />} label="Contact" value={profile.contactInformation} /></dl></div>
  </section>;
}

function ClientProfileForm({ form, onChange, onSubmit, submitting, isNew }: { form: ClientProfilePayload; onChange: (field: keyof ClientProfilePayload, value: string) => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void; submitting: boolean; isNew: boolean }) {
  return <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"><div className="mb-8 flex gap-4"><div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-indigo-100 text-indigo-700"><Building2 className="h-5 w-5" /></div><div><p className="text-sm font-semibold text-indigo-700">{isNew ? 'Set up your workspace' : 'Keep your details current'}</p><h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">{isNew ? 'Create your company profile' : 'Edit company profile'}</h1><p className="mt-2 text-sm leading-6 text-slate-500">A complete profile helps the right specialists understand your business.</p></div></div><form className="space-y-5" onSubmit={onSubmit}><div className="grid gap-5 sm:grid-cols-2"><Field label="Company name" required value={form.companyName} onChange={(value) => onChange('companyName', value)} maxLength={150} /><Field label="Industry" value={form.industry ?? ''} onChange={(value) => onChange('industry', value)} maxLength={100} /></div><Field label="Website" type="url" placeholder="https://example.com" value={form.website ?? ''} onChange={(value) => onChange('website', value)} maxLength={255} /><div className="grid gap-5 sm:grid-cols-2"><Field label="Location" value={form.location ?? ''} onChange={(value) => onChange('location', value)} maxLength={100} /><Field label="Contact information" required value={form.contactInformation} onChange={(value) => onChange('contactInformation', value)} maxLength={255} /></div><Field label="Company description" textarea value={form.companyDescription ?? ''} onChange={(value) => onChange('companyDescription', value)} maxLength={2000} /><SaveButton submitting={submitting} label={isNew ? 'Create company profile' : 'Save changes'} /></form></section>;
}

function DashboardShell({ role, name, onLogout, children }: { role: string; name: string; onLogout: () => void; children: ReactNode }) { return <div className="min-h-screen bg-slate-50 text-slate-900"><header className="border-b border-slate-200 bg-white"><div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6"><div className="flex items-center gap-3"><div className="flex h-9 w-9 items-center justify-center rounded-xl bg-indigo-600 text-white"><BriefcaseBusiness className="h-5 w-5" /></div><span className="text-lg font-bold">WorkLink</span><span className="hidden rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700 sm:inline">{role}</span></div><div className="flex items-center gap-3"><span className="hidden text-sm text-slate-600 sm:inline">{name}</span><button onClick={onLogout} className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900"><LogOut className="h-4 w-4" /> <span className="hidden sm:inline">Log out</span></button></div></div></header><main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-12"><div className="mb-8"><p className="text-sm font-semibold text-indigo-700">{role} workspace</p><h1 className="mt-1 text-3xl font-bold tracking-tight">Welcome, {name.split(' ')[0]}</h1><p className="mt-2 text-slate-600">Manage the profile that introduces you to the WorkLink community.</p></div>{children}</main></div>; }
function Field({ label, value, onChange, required, type = 'text', textarea, maxLength, placeholder }: { label: string; value: string; onChange: (value: string) => void; required?: boolean; type?: string; textarea?: boolean; maxLength: number; placeholder?: string }) { const className = 'mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-900 outline-none transition focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100'; return <label className="block text-sm font-medium text-slate-700">{label}{required && <span className="text-rose-600"> *</span>}{textarea ? <textarea required={required} value={value} onChange={(event) => onChange(event.target.value)} maxLength={maxLength} rows={5} className={className} /> : <input required={required} type={type} value={value} onChange={(event) => onChange(event.target.value)} maxLength={maxLength} placeholder={placeholder} className={className} />}</label>; }
function Detail({ icon, label, value, link }: { icon: ReactNode; label: string; value: string | null; link?: boolean }) { if (!value) return null; return <div className="flex gap-3"><span className="mt-0.5 h-4 w-4 text-indigo-600">{icon}</span><div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</dt><dd className="mt-1 break-all text-slate-700">{link ? <a className="text-indigo-700 hover:underline" href={value} target="_blank" rel="noreferrer">{value}</a> : value}</dd></div></div>; }
function LoadingCard({ label }: { label: string }) { return <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-sm text-slate-500"><div className="mx-auto mb-3 h-7 w-7 animate-spin rounded-full border-2 border-indigo-200 border-t-indigo-600" />{label}…</div>; }
function Alert({ kind, message }: { kind: 'error' | 'success'; message: string }) { return <p role={kind === 'error' ? 'alert' : 'status'} className={`mb-5 rounded-xl px-4 py-3 text-sm ${kind === 'error' ? 'bg-rose-50 text-rose-700' : 'bg-emerald-50 text-emerald-700'}`}>{message}</p>; }
function SaveButton({ submitting, label }: { submitting: boolean; label: string }) { return <button disabled={submitting} className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-3 font-semibold text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:bg-indigo-400"><Save className="h-4 w-4" />{submitting ? 'Saving…' : label}</button>; }
function toPayload(profile: ClientProfile): ClientProfilePayload { const { id: _id, ...payload } = profile; return payload; }
function trimPayload(profile: ClientProfilePayload): ClientProfilePayload { return Object.fromEntries(Object.entries(profile).map(([key, value]) => [key, value?.trim() ?? ''])) as ClientProfilePayload; }
function isUrl(value: string) { try { new URL(value); return true; } catch { return false; } }
function messageFrom(error: unknown, fallback: string) { return axios.isAxiosError<ApiError>(error) ? error.response?.data?.message ?? fallback : fallback; }
