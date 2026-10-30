import { type FormEvent, type ReactNode, useEffect, useState } from 'react';
import axios from 'axios';
import { Award, BriefcaseBusiness, Clock3, Code2, ExternalLink, FileText, Globe2, LogOut, MapPin, Pencil, Plus, Save, ShieldCheck, Trash2, UserRound } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { clearSession, getStoredUser } from '../lib/auth';
import api from '../lib/api';
import FreelancerMarketplaceSection from '../components/FreelancerMarketplaceSection';

interface FreelancerProfile { id: number; professionalHeadline: string; bio: string | null; location: string; yearsExperience: number; hourlyRate: number; availability: string; resumeInformation: string | null; profilePhotoUrl: string | null; }
type FreelancerPayload = Omit<FreelancerProfile, 'id'>;
interface ApiError { message?: string; }
interface FreelancerSkill { skillId: number; name: string; category: string | null; proficiency: string; }
interface PortfolioProject { id: number; title: string; description: string | null; projectUrl: string | null; repositoryUrl: string | null; technologiesUsed: string | null; role: string | null; completionDate: string | null; imageUrl: string | null; }
interface TrustComponent { name: string; weightPercent: number; score: number | null; contribution: number | null; availability: string; basis: string; }
interface TrustScore { overallTrustScore: number | null; availability: string; projectsConsidered: number; calculatedAt: string | null; components: TrustComponent[]; interpretation: string; }
interface PerformanceRecord { id: number; projectId: number; projectTitle: string; projectStatus: string; completionDate: string | null; deadline: string | null; onTime: boolean | null; milestoneCount: number; approvedMilestoneCount: number; clientRating: number | null; clientReview: string | null; freelancerActivityCount: number; clientActivityCount: number; activityCount: number; }
const emptyProfile: FreelancerPayload = { professionalHeadline: '', bio: '', location: '', yearsExperience: 0, hourlyRate: 0, availability: 'AVAILABLE', resumeInformation: '', profilePhotoUrl: '' };
const availabilityOptions = ['AVAILABLE', 'LIMITED_AVAILABILITY', 'UNAVAILABLE'];

export default function FreelancerDashboardPage() {
  const user = getStoredUser();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<FreelancerProfile | null>(null);
  const [form, setForm] = useState<FreelancerPayload>(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    api.get<FreelancerProfile>('/freelancer/profile').then(({ data }) => { setProfile(data); setForm(toPayload(data)); })
      .catch((requestError: unknown) => {
        if (axios.isAxiosError(requestError) && requestError.response?.status === 404) setEditing(true);
        else setError(messageFrom(requestError, 'We could not load your professional profile. Please try again.'));
      }).finally(() => setLoading(false));
  }, []);

  const update = <K extends keyof FreelancerPayload>(field: K, value: FreelancerPayload[K]) => setForm((current) => ({ ...current, [field]: value }));
  const logout = () => { clearSession(); navigate('/', { replace: true }); };
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setError(null); setSuccess(null);
    if (!form.professionalHeadline.trim() || !form.location.trim()) { setError('Professional headline and location are required.'); return; }
    if (!Number.isInteger(form.yearsExperience) || form.yearsExperience < 0 || form.hourlyRate < 0) { setError('Experience and hourly rate must be valid non-negative values.'); return; }
    if ((form.resumeInformation && !isUrl(form.resumeInformation)) || (form.profilePhotoUrl && !isUrl(form.profilePhotoUrl))) { setError('Resume and profile photo links must be valid URLs, including https://.'); return; }
    setSubmitting(true);
    try {
      const payload = trimPayload(form);
      const { data } = profile ? await api.put<FreelancerProfile>('/freelancer/profile', payload) : await api.post<FreelancerProfile>('/freelancer/profile', payload);
      setProfile(data); setForm(toPayload(data)); setEditing(false); setSuccess(profile ? 'Your professional profile has been updated.' : 'Your professional profile is ready.');
    } catch (requestError) { setError(messageFrom(requestError, 'We could not save your profile. Please try again.')); } finally { setSubmitting(false); }
  };

  if (!user) return null;
  return <div className="min-h-screen bg-slate-50 text-slate-900"><header className="border-b border-slate-200 bg-white"><div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6"><div className="flex items-center gap-3"><div className="flex h-9 w-9 items-center justify-center rounded-xl bg-violet-600 text-white"><BriefcaseBusiness className="h-5 w-5" /></div><span className="text-lg font-bold">WorkLink</span><span className="hidden rounded-full bg-violet-50 px-2.5 py-1 text-xs font-semibold text-violet-700 sm:inline">Freelancer</span></div><div className="flex items-center gap-3"><span className="hidden text-sm text-slate-600 sm:inline">{user.firstName} {user.lastName}</span><button onClick={logout} className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-100"><LogOut className="h-4 w-4" /><span className="hidden sm:inline">Log out</span></button></div></div></header><main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-12"><div className="mb-8"><p className="text-sm font-semibold text-violet-700">Talent workspace</p><h1 className="mt-1 text-3xl font-bold tracking-tight">Welcome, {user.firstName}</h1><p className="mt-2 text-slate-600">Show clients the experience and availability behind your work.</p></div>{loading ? <Loading label="Loading your professional profile" /> : <>{error && <Alert kind="error" message={error} />}{success && <Alert kind="success" message={success} />}{editing ? <FreelancerForm form={form} update={update} submit={submit} submitting={submitting} isNew={!profile} /> : profile && <ProfileView profile={profile} edit={() => { setError(null); setSuccess(null); setEditing(true); }} />}<FreelancerMarketplaceSection /><TrustPerformanceSection /><FreelancerCapabilities /></>}</main></div>;
}

function TrustPerformanceSection() {
  const [trust, setTrust] = useState<TrustScore | null>(null); const [records, setRecords] = useState<PerformanceRecord[]>([]);
  const [trustLoading, setTrustLoading] = useState(true); const [historyLoading, setHistoryLoading] = useState(true);
  const [trustError, setTrustError] = useState<string | null>(null); const [historyError, setHistoryError] = useState<string | null>(null);
  const loadTrust = async () => { setTrustLoading(true); setTrustError(null); try { const response = await api.get<TrustScore>('/freelancer/trust-score'); setTrust(response.data); } catch (err) { setTrustError(messageFrom(err, 'Could not load your trust score.')); } finally { setTrustLoading(false); } };
  const loadHistory = async () => { setHistoryLoading(true); setHistoryError(null); try { const response = await api.get<PerformanceRecord[]>('/freelancer/performance'); setRecords(response.data); } catch (err) { setHistoryError(messageFrom(err, 'Could not load your performance history.')); } finally { setHistoryLoading(false); } };
  useEffect(() => { void loadTrust(); void loadHistory(); }, []);
  const labels: Record<string, string> = { completionReliability: 'Completion Reliability', deadlineAdherence: 'Deadline Adherence', clientSatisfaction: 'Client Satisfaction', milestonePerformance: 'Milestone Performance', communicationActivity: 'Communication / Activity', recentPerformance: 'Recent Performance' };
  return <section className="mt-8 space-y-5" aria-label="Trust and performance">
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"><div className="flex items-start gap-3"><span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-violet-100 text-violet-700"><ShieldCheck className="h-5 w-5" /></span><div><h2 className="text-xl font-bold">Trust &amp; Performance</h2><p className="mt-1 text-sm text-slate-500">Dynamic Trust Score summarizes recorded performance outcomes. Performance History lists the underlying project records.</p></div></div>
      {trustLoading ? <Loading label="Loading your dynamic trust score" /> : trustError ? <div className="mt-5 rounded-xl bg-rose-50 p-4"><p role="alert" className="text-sm text-rose-700">{trustError}</p><button onClick={() => void loadTrust()} className="mt-2 text-sm font-semibold text-rose-700 hover:underline">Retry trust score</button></div> : trust && (trust.availability === 'NO_COMPLETED_PROJECTS' || trust.overallTrustScore == null) ? <div className="mt-5 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-5"><h3 className="font-semibold text-slate-800">No completed projects yet</h3><p className="mt-1 text-sm text-slate-600">A Dynamic Trust Score will be available when completed project outcomes have been recorded.</p><span className="mt-3 inline-flex rounded-full bg-slate-200 px-2.5 py-1 text-xs font-semibold text-slate-700">{humanize(trust.availability)}</span></div> : trust && <><div className="mt-5 grid gap-4 sm:grid-cols-[auto_1fr] sm:items-center"><div className="rounded-xl bg-violet-50 px-6 py-4 text-center"><p className="text-xs font-bold uppercase tracking-wide text-violet-700">Dynamic Trust Score</p><p className="mt-1 text-3xl font-black text-violet-800">{Number(trust.overallTrustScore).toFixed(1)}%</p></div><div className="text-sm text-slate-600"><span className="inline-flex rounded-full bg-violet-100 px-2.5 py-1 text-xs font-semibold text-violet-800">{humanize(trust.availability)}</span><p className="mt-2">Projects considered: <strong>{trust.projectsConsidered}</strong></p>{trust.calculatedAt && <p className="mt-1">Calculated {new Date(trust.calculatedAt).toLocaleString()}</p>}<p className="mt-2">{trust.interpretation}</p></div></div><div className="mt-5 grid gap-3 sm:grid-cols-2">{trust.components.map(component => <div key={component.name} className="rounded-xl border border-slate-200 p-4"><div className="flex items-center justify-between gap-2"><h3 className="font-semibold text-slate-800">{labels[component.name] || component.name}</h3><span className="text-sm font-bold text-violet-700">{component.score == null ? 'Insufficient data' : Number(component.score).toFixed(1) + '%'}</span></div><p className="mt-1 text-xs text-slate-500">Weight {Number(component.weightPercent).toFixed(0)}%{component.contribution != null ? ' · Contribution ' + Number(component.contribution).toFixed(2) + ' pts' : ''}</p><p className="mt-2 text-sm leading-5 text-slate-600">{component.basis}</p></div>)}</div></>}
    </div>
    <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"><h2 className="text-xl font-bold">Performance History</h2><p className="mt-1 text-sm text-slate-500">Persisted project outcomes used as source records for trust scoring.</p>{historyLoading ? <Loading label="Loading your performance history" /> : historyError ? <div className="mt-5 rounded-xl bg-rose-50 p-4"><p role="alert" className="text-sm text-rose-700">{historyError}</p><button onClick={() => void loadHistory()} className="mt-2 text-sm font-semibold text-rose-700 hover:underline">Retry performance history</button></div> : records.length === 0 ? <div className="mt-5 rounded-xl border border-dashed border-slate-300 bg-slate-50 px-5 py-8 text-center"><p className="font-semibold text-slate-700">No performance history available</p><p className="mt-1 text-sm text-slate-500">Recorded project outcomes will appear here.</p></div> : <div className="mt-5 space-y-3">{records.map(record => <article key={record.id} className="rounded-xl border border-slate-200 p-4"><div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-start"><div><h3 className="font-semibold text-slate-900">{record.projectTitle}</h3><p className="mt-1 text-sm text-slate-500">{humanize(record.projectStatus)}{record.completionDate ? ' · Completed ' + new Date(record.completionDate + 'T00:00:00').toLocaleDateString() : ''}</p></div>{record.onTime != null && <span className={'w-fit rounded-full px-2.5 py-1 text-xs font-semibold ' + (record.onTime ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700')}>{record.onTime ? 'On time' : 'After deadline'}</span>}</div><div className="mt-3 grid grid-cols-2 gap-3 text-sm text-slate-600 sm:grid-cols-4"><p>Deadline: {record.deadline ? new Date(record.deadline + 'T00:00:00').toLocaleDateString() : 'Not recorded'}</p><p>Milestones: {record.approvedMilestoneCount} / {record.milestoneCount} approved</p><p>Client activity: {record.clientActivityCount}</p><p>Your activity: {record.freelancerActivityCount} ({record.activityCount} total)</p></div>{record.clientRating != null && <p className="mt-3 text-sm text-slate-700">Client review: {record.clientRating}/5{record.clientReview ? ' · “' + record.clientReview + '”' : ''}</p>}</article>)}</div>}</div>
  </section>;
}

function ProfileView({ profile, edit }: { profile: FreelancerProfile; edit: () => void }) { const photo = profile.profilePhotoUrl; return <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="bg-gradient-to-r from-violet-600 to-indigo-600 px-6 py-8 text-white sm:px-8"><div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-start"><div className="flex gap-4">{photo ? <img src={photo} alt="Profile" className="h-16 w-16 rounded-2xl object-cover ring-2 ring-white/30" /> : <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/15"><UserRound /></div>}<div><p className="text-sm font-medium text-violet-100">Professional profile</p><h2 className="mt-1 text-2xl font-bold">{profile.professionalHeadline}</h2><p className="mt-1 text-violet-100">{profile.location}</p></div></div><button onClick={edit} className="inline-flex items-center justify-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-violet-700 hover:bg-violet-50"><Pencil className="h-4 w-4" />Edit profile</button></div></div><div className="grid gap-8 p-6 sm:p-8 lg:grid-cols-[1.5fr_1fr]"><div><h3 className="text-base font-semibold text-slate-900">About me</h3><p className="mt-3 whitespace-pre-line leading-7 text-slate-600">{profile.bio || 'Add a short introduction so clients can understand the value you bring to their projects.'}</p>{profile.resumeInformation && <a className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-violet-700 hover:underline" href={profile.resumeInformation} target="_blank" rel="noreferrer"><FileText className="h-4 w-4" />View resume</a>}</div><dl className="space-y-4 rounded-xl bg-slate-50 p-5"><Stat icon={<Award />} label="Experience" value={`${profile.yearsExperience} ${profile.yearsExperience === 1 ? 'year' : 'years'}`} /><Stat icon={<Clock3 />} label="Availability" value={humanize(profile.availability)} /><Stat icon={<Globe2 />} label="Hourly rate" value={`$${Number(profile.hourlyRate).toFixed(2)} / hr`} /><Stat icon={<MapPin />} label="Based in" value={profile.location} /></dl></div></section>; }

function FreelancerForm({ form, update, submit, submitting, isNew }: { form: FreelancerPayload; update: <K extends keyof FreelancerPayload>(field: K, value: FreelancerPayload[K]) => void; submit: (event: FormEvent<HTMLFormElement>) => void; submitting: boolean; isNew: boolean }) { return <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8"><div className="mb-8 flex gap-4"><div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-violet-100 text-violet-700"><UserRound className="h-5 w-5" /></div><div><p className="text-sm font-semibold text-violet-700">{isNew ? 'Build your presence' : 'Keep your details current'}</p><h2 className="mt-1 text-2xl font-bold tracking-tight">{isNew ? 'Create your professional profile' : 'Edit professional profile'}</h2><p className="mt-2 text-sm leading-6 text-slate-500">Give clients a clear, polished view of your expertise and availability.</p></div></div><form className="space-y-5" onSubmit={submit}><Input label="Professional headline" required value={form.professionalHeadline} change={(value) => update('professionalHeadline', value)} maxLength={200} /><Input label="Bio" textarea value={form.bio ?? ''} change={(value) => update('bio', value)} maxLength={3000} /><div className="grid gap-5 sm:grid-cols-2"><Input label="Location" required value={form.location} change={(value) => update('location', value)} maxLength={100} /><Input label="Years of experience" required type="number" min={0} value={String(form.yearsExperience)} change={(value) => update('yearsExperience', Number(value))} /></div><div className="grid gap-5 sm:grid-cols-2"><Input label="Hourly rate (USD)" required type="number" min={0} step="0.01" value={String(form.hourlyRate)} change={(value) => update('hourlyRate', Number(value))} /><label className="block text-sm font-medium text-slate-700">Availability<span className="text-rose-600"> *</span><select value={form.availability} onChange={(event) => update('availability', event.target.value)} className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-900 outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-100">{availabilityOptions.map((option) => <option key={option} value={option}>{humanize(option)}</option>)}</select></label></div><div className="grid gap-5 sm:grid-cols-2"><Input label="Resume link" type="url" placeholder="https://example.com/resume" value={form.resumeInformation ?? ''} change={(value) => update('resumeInformation', value)} maxLength={500} /><Input label="Profile photo link" type="url" placeholder="https://example.com/photo.jpg" value={form.profilePhotoUrl ?? ''} change={(value) => update('profilePhotoUrl', value)} maxLength={500} /></div><button disabled={submitting} className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-violet-600 px-4 py-3 font-semibold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"><Save className="h-4 w-4" />{submitting ? 'Saving…' : isNew ? 'Create professional profile' : 'Save changes'}</button></form></section>; }

function Input({ label, value, change, required, type = 'text', textarea, maxLength, min, step, placeholder }: { label: string; value: string; change: (value: string) => void; required?: boolean; type?: string; textarea?: boolean; maxLength?: number; min?: number; step?: string; placeholder?: string }) { const style = 'mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2.5 text-slate-900 outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100'; return <label className="block text-sm font-medium text-slate-700">{label}{required && <span className="text-rose-600"> *</span>}{textarea ? <textarea rows={5} value={value} onChange={(event) => change(event.target.value)} maxLength={maxLength} className={style} /> : <input required={required} type={type} min={min} step={step} value={value} onChange={(event) => change(event.target.value)} maxLength={maxLength} placeholder={placeholder} className={style} />}</label>; }
function Stat({ icon, label, value }: { icon: ReactNode; label: string; value: string }) { return <div className="flex gap-3"><span className="mt-0.5 h-4 w-4 text-violet-600">{icon}</span><div><dt className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</dt><dd className="mt-1 text-sm text-slate-700">{value}</dd></div></div>; }
function Loading({ label }: { label: string }) { return <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center text-sm text-slate-500"><div className="mx-auto mb-3 h-7 w-7 animate-spin rounded-full border-2 border-violet-200 border-t-violet-600" />{label}…</div>; }
function Alert({ kind, message }: { kind: 'error' | 'success'; message: string }) { return <p role={kind === 'error' ? 'alert' : 'status'} className={`mb-5 rounded-xl px-4 py-3 text-sm ${kind === 'error' ? 'bg-rose-50 text-rose-700' : 'bg-emerald-50 text-emerald-700'}`}>{message}</p>; }
function toPayload(profile: FreelancerProfile): FreelancerPayload { const { id: _id, ...payload } = profile; return payload; }
function trimPayload(profile: FreelancerPayload): FreelancerPayload { return { ...profile, professionalHeadline: profile.professionalHeadline.trim(), bio: profile.bio?.trim() || '', location: profile.location.trim(), availability: profile.availability.trim(), resumeInformation: profile.resumeInformation?.trim() || '', profilePhotoUrl: profile.profilePhotoUrl?.trim() || '' }; }
function humanize(value: string) { return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()); }
function isUrl(value: string) { try { new URL(value); return true; } catch { return false; } }
function messageFrom(error: unknown, fallback: string) { return axios.isAxiosError<ApiError>(error) ? error.response?.data?.message ?? fallback : fallback; }

// ─── Skills & Portfolio ───────────────────────────────────────────────────────

const proficiencyOptions = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'];
interface PortfolioProjectForm { title: string; description: string; projectUrl: string; repositoryUrl: string; technologiesUsed: string; role: string; completionDate: string; imageUrl: string; }
const emptyPortfolioForm: PortfolioProjectForm = { title: '', description: '', projectUrl: '', repositoryUrl: '', technologiesUsed: '', role: '', completionDate: '', imageUrl: '' };

function FreelancerCapabilities() {
  const [skills, setSkills] = useState<FreelancerSkill[]>([]);
  const [portfolio, setPortfolio] = useState<PortfolioProject[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [skillName, setSkillName] = useState('');
  const [proficiency, setProficiency] = useState('INTERMEDIATE');
  const [addingSkill, setAddingSkill] = useState(false);
  const [portfolioForm, setPortfolioForm] = useState<PortfolioProjectForm>(emptyPortfolioForm);
  const [editingPortfolioId, setEditingPortfolioId] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const [currentSkillsRes, currentPortfolioRes] = await Promise.allSettled([
        api.get<FreelancerSkill[]>('/freelancer/skills'),
        api.get<PortfolioProject[]>('/freelancer/portfolio'),
      ]);

      if (currentSkillsRes.status === 'fulfilled') {
        setSkills(currentSkillsRes.value.data);
      } else if (axios.isAxiosError(currentSkillsRes.reason) && currentSkillsRes.reason.response?.status === 404) {
        setSkills([]);
      } else {
        setError(messageFrom(currentSkillsRes.reason, 'Could not load your profile skills.'));
      }

      if (currentPortfolioRes.status === 'fulfilled') {
        setPortfolio(currentPortfolioRes.value.data);
      } else if (axios.isAxiosError(currentPortfolioRes.reason) && currentPortfolioRes.reason.response?.status === 404) {
        setPortfolio([]);
      } else {
        setError(messageFrom(currentPortfolioRes.reason, 'Could not load your portfolio.'));
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  const showNotice = (message: string) => { setError(null); setNotice(message); };

  const addSkill = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = skillName.trim();
    if (!trimmed) return;
    setAddingSkill(true);
    try {
      await api.post('/freelancer/skills/by-name', { name: trimmed, proficiency });
      setSkillName('');
      showNotice(`"${trimmed}" added to your profile.`);
      void load();
    } catch (requestError) {
      setError(messageFrom(requestError, 'We could not add this skill.'));
    } finally {
      setAddingSkill(false);
    }
  };

  const updateSkill = async (current: FreelancerSkill, next: string) => {
    try {
      await api.put(`/freelancer/skills/${current.skillId}`, { skillId: current.skillId, proficiency: next });
      showNotice('Skill proficiency updated.');
      void load();
    } catch (requestError) { setError(messageFrom(requestError, 'We could not update this skill.')); }
  };

  const removeSkill = async (current: FreelancerSkill) => {
    if (!window.confirm(`Remove ${current.name} from your profile?`)) return;
    try {
      await api.delete(`/freelancer/skills/${current.skillId}`);
      showNotice('Skill removed from your profile.');
      void load();
    } catch (requestError) { setError(messageFrom(requestError, 'We could not remove this skill.')); }
  };

  const submitPortfolio = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!portfolioForm.title.trim()) { setError('Portfolio title is required.'); return; }
    if (![portfolioForm.projectUrl, portfolioForm.repositoryUrl, portfolioForm.imageUrl].every((url) => !url || isUrl(url))) { setError('Portfolio links must be valid URLs, including https://.'); return; }
    try {
      const payload = portfolioPayload(portfolioForm);
      if (editingPortfolioId) await api.put(`/freelancer/portfolio/${editingPortfolioId}`, payload);
      else await api.post('/freelancer/portfolio', payload);
      setPortfolioForm(emptyPortfolioForm);
      setEditingPortfolioId(null);
      showNotice(editingPortfolioId ? 'Portfolio project updated.' : 'Portfolio project added.');
      void load();
    } catch (requestError) { setError(messageFrom(requestError, 'We could not save this portfolio project.')); }
  };

  const editPortfolio = (item: PortfolioProject) => {
    setEditingPortfolioId(item.id);
    setPortfolioForm({ title: item.title, description: item.description ?? '', projectUrl: item.projectUrl ?? '', repositoryUrl: item.repositoryUrl ?? '', technologiesUsed: item.technologiesUsed ?? '', role: item.role ?? '', completionDate: item.completionDate ?? '', imageUrl: item.imageUrl ?? '' });
    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
  };

  const deletePortfolio = async (item: PortfolioProject) => {
    if (!window.confirm(`Delete "${item.title}"? This cannot be undone.`)) return;
    try {
      await api.delete(`/freelancer/portfolio/${item.id}`);
      showNotice('Portfolio project deleted.');
      void load();
    } catch (requestError) { setError(messageFrom(requestError, 'We could not delete this portfolio project.')); }
  };

  return (
    <section className="mt-8 space-y-8">
      {error && <Alert kind="error" message={error} />}
      {notice && <Alert kind="success" message={notice} />}
      {loading ? <Loading label="Loading skills and portfolio" /> : (
        <>
          {/* Skills */}
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
            <div>
              <h2 className="text-xl font-bold">Skills &amp; expertise</h2>
              <p className="mt-1 text-sm text-slate-500">Build a capability profile clients and matching tools can understand.</p>
            </div>
            <form onSubmit={addSkill} className="mt-6 grid gap-3 rounded-xl bg-violet-50 p-4 md:grid-cols-[1fr_180px_auto]">
              <input
                required
                id="freelancer-skill-input"
                name="skillName"
                type="text"
                placeholder="e.g. React, Java, PostgreSQL"
                value={skillName}
                onChange={(event) => setSkillName(event.target.value)}
                maxLength={100}
                className="rounded-lg border border-violet-200 bg-white px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-violet-200 placeholder:text-slate-400"
              />
              <select
                value={proficiency}
                onChange={(event) => setProficiency(event.target.value)}
                className="rounded-lg border border-violet-200 bg-white px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-violet-200"
              >
                {proficiencyOptions.map((value) => <option key={value}>{value}</option>)}
              </select>
              <button
                disabled={addingSkill || !skillName.trim()}
                className="inline-flex items-center justify-center gap-2 rounded-lg bg-violet-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-300"
              >
                <Plus className="h-4 w-4" />
                {addingSkill ? 'Adding…' : 'Add skill'}
              </button>
            </form>
            {skills.length === 0
              ? <Empty label="No skills added yet" description="Add the technologies and disciplines that best represent your work." />
              : (
                <div className="mt-5 grid gap-3 sm:grid-cols-2">
                  {skills.map((skill) => (
                    <div key={skill.skillId} className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 p-4">
                      <div>
                        <p className="font-semibold text-slate-800">{skill.name}</p>
                        {skill.category && <p className="mt-1 text-xs text-slate-500">{skill.category}</p>}
                      </div>
                      <div className="flex items-center gap-2">
                        <select
                          aria-label={`${skill.name} proficiency`}
                          value={skill.proficiency}
                          onChange={(event) => updateSkill(skill, event.target.value)}
                          className="rounded-full border border-violet-200 bg-violet-50 px-2 py-1 text-xs font-semibold text-violet-700"
                        >
                          {proficiencyOptions.map((value) => <option key={value}>{value}</option>)}
                        </select>
                        <button
                          aria-label={`Remove ${skill.name}`}
                          onClick={() => removeSkill(skill)}
                          className="rounded-lg p-2 text-slate-400 hover:bg-rose-50 hover:text-rose-600"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
          </section>

          {/* Portfolio */}
          <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
            <div>
              <h2 className="text-xl font-bold">Portfolio</h2>
              <p className="mt-1 text-sm text-slate-500">Share selected work that demonstrates your skills and impact.</p>
            </div>
            {portfolio.length === 0
              ? <Empty label="Your portfolio is ready for its first project" description="Add a project to show clients what you have delivered." />
              : (
                <div className="mt-6 grid gap-5 md:grid-cols-2">
                  {portfolio.map((item) => <PortfolioCard key={item.id} item={item} edit={() => editPortfolio(item)} remove={() => deletePortfolio(item)} />)}
                </div>
              )}
            <PortfolioForm
              form={portfolioForm}
              setForm={setPortfolioForm}
              submit={submitPortfolio}
              editing={editingPortfolioId !== null}
              cancel={() => { setEditingPortfolioId(null); setPortfolioForm(emptyPortfolioForm); }}
            />
          </section>
        </>
      )}
    </section>
  );
}

function portfolioPayload(form: PortfolioProjectForm) { return Object.fromEntries(Object.entries(form).map(([key, value]) => [key, value.trim() || null])); }
function Empty({ label, description }: { label: string; description: string }) { return <div className="mt-5 rounded-xl border border-dashed border-slate-300 bg-slate-50 px-5 py-8 text-center"><p className="font-semibold text-slate-700">{label}</p><p className="mt-1 text-sm text-slate-500">{description}</p></div>; }
function PortfolioCard({ item, edit, remove }: { item: PortfolioProject; edit: () => void; remove: () => void }) { return <article className="overflow-hidden rounded-xl border border-slate-200"><div className="flex min-h-28 items-center justify-center bg-gradient-to-br from-violet-100 to-indigo-100">{item.imageUrl ? <img src={item.imageUrl} alt="" className="h-36 w-full object-cover" /> : <BriefcaseBusiness className="h-8 w-8 text-violet-500" />}</div><div className="p-5"><div className="flex justify-between gap-3"><div><h3 className="font-bold text-slate-900">{item.title}</h3>{item.role && <p className="mt-1 text-sm text-violet-700">{item.role}</p>}</div><div className="flex"><button aria-label={`Edit ${item.title}`} onClick={edit} className="p-2 text-slate-400 hover:text-violet-700"><Pencil className="h-4 w-4" /></button><button aria-label={`Delete ${item.title}`} onClick={remove} className="p-2 text-slate-400 hover:text-rose-600"><Trash2 className="h-4 w-4" /></button></div></div>{item.description && <p className="mt-3 line-clamp-3 text-sm leading-6 text-slate-600">{item.description}</p>}{item.technologiesUsed && <div className="mt-4 flex flex-wrap gap-1.5">{item.technologiesUsed.split(',').filter(Boolean).map((technology) => <span key={technology} className="rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600">{technology.trim()}</span>)}</div>}<div className="mt-4 flex flex-wrap items-center gap-3 text-xs font-semibold">{item.projectUrl && <a href={item.projectUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-violet-700 hover:underline"><ExternalLink className="h-3.5 w-3.5" />Project</a>}{item.repositoryUrl && <a href={item.repositoryUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-violet-700 hover:underline"><Code2 className="h-3.5 w-3.5" />Repository</a>}{item.completionDate && <span className="text-slate-400">Completed {new Date(`${item.completionDate}T00:00:00`).toLocaleDateString(undefined, { month: 'short', year: 'numeric' })}</span>}</div></div></article>; }
function PortfolioForm({ form, setForm, submit, editing, cancel }: { form: PortfolioProjectForm; setForm: (form: PortfolioProjectForm) => void; submit: (event: FormEvent<HTMLFormElement>) => void; editing: boolean; cancel: () => void }) { const set = (field: keyof PortfolioProjectForm, value: string) => setForm({ ...form, [field]: value }); return <form onSubmit={submit} className="mt-8 rounded-xl border border-violet-200 bg-violet-50 p-5"><div className="flex items-center justify-between gap-3"><h3 className="font-bold text-slate-900">{editing ? 'Edit portfolio project' : 'Add a portfolio project'}</h3>{editing && <button type="button" onClick={cancel} className="text-sm font-medium text-violet-700 hover:underline">Cancel</button>}</div><div className="mt-5 grid gap-4 sm:grid-cols-2"><CompactInput label="Title" required value={form.title} change={(value) => set('title', value)} /><CompactInput label="Role" value={form.role} change={(value) => set('role', value)} /><CompactInput label="Project link" type="url" value={form.projectUrl} change={(value) => set('projectUrl', value)} /><CompactInput label="Repository link" type="url" value={form.repositoryUrl} change={(value) => set('repositoryUrl', value)} /><CompactInput label="Technologies used" value={form.technologiesUsed} change={(value) => set('technologiesUsed', value)} placeholder="React, Java, PostgreSQL" /><CompactInput label="Completion date" type="date" value={form.completionDate} change={(value) => set('completionDate', value)} /><CompactInput label="Image link" type="url" value={form.imageUrl} change={(value) => set('imageUrl', value)} /><CompactInput label="Description" textarea value={form.description} change={(value) => set('description', value)} /></div><button className="mt-5 inline-flex items-center gap-2 rounded-lg bg-violet-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-violet-700"><Save className="h-4 w-4" />{editing ? 'Save project' : 'Add project'}</button></form>; }
function CompactInput({ label, value, change, required, type = 'text', textarea, placeholder }: { label: string; value: string; change: (value: string) => void; required?: boolean; type?: string; textarea?: boolean; placeholder?: string }) { return <label className={`block text-sm font-medium text-slate-700 ${textarea ? 'sm:col-span-2' : ''}`}>{label}{required && <span className="text-rose-600"> *</span>}{textarea ? <textarea value={value} onChange={(event) => change(event.target.value)} rows={3} className="mt-1.5 w-full rounded-lg border border-violet-200 bg-white px-3 py-2.5" /> : <input required={required} type={type} value={value} onChange={(event) => change(event.target.value)} placeholder={placeholder} className="mt-1.5 w-full rounded-lg border border-violet-200 bg-white px-3 py-2.5" />}</label>; }
