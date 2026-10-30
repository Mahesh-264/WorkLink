import { useEffect, useState, type FormEvent } from 'react';
import api from '../lib/api';
import axios from 'axios';
import { AlertCircle, CheckCircle2, Star, Award, TrendingUp, DollarSign } from 'lucide-react';
import FreelancerActiveProject from './FreelancerActiveProject';

type Skill = { skillId: number; name: string; importance: string };
type Project = {
    id: number;
    title: string;
    description: string;
    category: string;
    clientCompany: string;
    budgetMin: number;
    budgetMax: number;
    skills: Skill[];
    deliverables?: string | null;
    requiredExperience?: number;
    deadline?: string;
    complexity?: string;
};
type Match = {
    compatibilityScore: number;
    skillMatchScore: number;
    proficiencyScore: number;
    experienceScore: number;
    portfolioRelevanceScore: number;
    budgetCompatibilityScore: number;
    explanation: string;
    matchedSkills: string[];
    missingSkills: string[];
};
type Proposal = {
    id: number;
    projectId: number;
    projectTitle: string;
    proposedBudget: number;
    estimatedDuration: number;
    status: string;
    portfolioProjectId?: number;
    portfolioProjectTitle?: string;
    createdAt: string;
};
type PortfolioProject = {
    id: number;
    title: string;
};

export default function FreelancerMarketplaceSection() {
    const [projects, setProjects] = useState<Project[]>([]);
    const [proposals, setProposals] = useState<Proposal[]>([]);
    const [portfolio, setPortfolio] = useState<PortfolioProject[]>([]);
    const [acceptedProjects, setAcceptedProjects] = useState<Project[]>([]);
    const [openWorkspaceId, setOpenWorkspaceId] = useState<number | null>(null);
    
    const [selected, setSelected] = useState<Project | null>(null);
    const [match, setMatch] = useState<Match | null>(null);
    
    // Form state
    const [cover, setCover] = useState('');
    const [budget, setBudget] = useState('');
    const [days, setDays] = useState('');
    const [selectedPortfolioId, setSelectedPortfolioId] = useState('');
    
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [detailsWarning, setDetailsWarning] = useState('');
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);

    const load = async () => {
        setLoading(true);
        setError('');
        setDetailsWarning('');
        try {
            // The published-project list is the primary marketplace data. It must
            // render even if a portfolio or history request fails independently.
            const projRes = await api.get<Project[]>('/projects');
            setProjects(projRes.data);
        } catch (requestError: unknown) {
            setProjects([]);
            setError(messageFrom(requestError, 'Could not load published projects.'));
        } finally {
            setLoading(false);
        }

        const [proposalResult, portfolioResult, acceptedResult] = await Promise.allSettled([
            api.get<Proposal[]>('/freelancer/proposals'),
            api.get<PortfolioProject[]>('/freelancer/portfolio'),
            api.get<Project[]>('/projects/accepted'),
        ]);
        if (proposalResult.status === 'fulfilled') setProposals(proposalResult.value.data);
        if (portfolioResult.status === 'fulfilled') setPortfolio(portfolioResult.value.data);
        if (acceptedResult.status === 'fulfilled') setAcceptedProjects(acceptedResult.value.data);
        if ([proposalResult, portfolioResult, acceptedResult].some(result => result.status === 'rejected')) {
            setDetailsWarning('Some request history or portfolio details could not be loaded. Published projects are still available.');
        }
    };

    useEffect(() => {
        void load();
    }, []);

    const open = async (p: Project) => {
        setSelected(p);
        setMatch(null);
        setMessage('');
        setError('');
        setCover('');
        setBudget('');
        setDays('');
        setSelectedPortfolioId('');
        
        try {
            const res = await api.get<Match>(`/freelancer/projects/${p.id}/match`);
            setMatch(res.data);
        } catch {
            // It's okay if match fails, we just don't show it
        }
    };

    const submit = async (e: FormEvent) => {
        e.preventDefault();
        if (!selected) return;
        
        setSubmitting(true);
        setError('');
        setMessage('');
        
        try {
            await api.post(`/projects/${selected.id}/proposals`, {
                coverLetter: cover,
                proposedBudget: Number(budget),
                estimatedDuration: Number(days),
                portfolioProjectId: selectedPortfolioId ? Number(selectedPortfolioId) : null
            });
            setMessage('Proposal submitted successfully.');
            setCover('');
            setBudget('');
            setDays('');
            setSelectedPortfolioId('');
            void load();
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                setError(err.response?.data?.message || 'Proposal could not be submitted. You may already have an active proposal.');
            } else {
                setError('Proposal could not be submitted. You may already have an active proposal.');
            }
        } finally {
            setSubmitting(false);
        }
    };

    const withdraw = async (id: number) => {
        if (!window.confirm("Are you sure you want to withdraw this proposal?")) return;
        try {
            await api.patch(`/freelancer/proposals/${id}/withdraw`);
            void load();
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                alert(err.response?.data?.message || "Could not withdraw proposal");
            } else {
                alert("Could not withdraw proposal");
            }
        }
    };

    const hasActiveProposal = (projectId: number) => {
        return proposals.some(p => p.projectId === projectId && ['SUBMITTED', 'UNDER_REVIEW', 'SHORTLISTED', 'ACCEPTED'].includes(p.status));
    };

    return (
        <section className="mt-8 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="text-xl font-bold">Marketplace</h2>
            <p className="mt-1 text-sm text-slate-500">Explore published opportunities and submit proposals.</p>
            
            {error && !selected && <div className="mt-4 rounded bg-rose-50 p-3 text-sm text-rose-700"><p>{error}</p><button type="button" onClick={() => void load()} className="mt-2 font-semibold underline">Retry</button></div>}
            {detailsWarning && <p className="mt-4 rounded bg-amber-50 p-3 text-sm text-amber-800">{detailsWarning}</p>}
            
            {loading ? (
                <p className="mt-5 text-sm text-slate-500">Loading opportunities…</p>
            ) : (
                <div className="mt-5 grid gap-4 md:grid-cols-2">
                    {projects.length === 0 && <p className="rounded-xl border border-dashed border-slate-300 p-6 text-sm text-slate-500 md:col-span-2">No published projects are available right now.</p>}
                    {projects.map((p) => (
                        <article key={p.id} className="rounded-xl border border-slate-200 p-5 hover:border-violet-300 transition-colors bg-slate-50">
                            <p className="text-xs font-semibold text-violet-700 uppercase tracking-wider">{p.category}</p>
                            <h3 className="mt-1 text-lg font-bold text-slate-900">{p.title}</h3>
                            <p className="text-sm font-medium text-slate-600">{p.clientCompany} · ${p.budgetMin}–${p.budgetMax}</p>
                            <p className="mt-3 line-clamp-2 text-sm text-slate-600">{p.description}</p>
                            
                            <div className="mt-4 flex items-center justify-between">
                                {hasActiveProposal(p.id) ? (
                                    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800">
                                        <CheckCircle2 className="h-3.5 w-3.5" />
                                        Proposal Submitted
                                    </span>
                                ) : (
                                    <button onClick={() => void open(p)} className="inline-flex items-center text-sm font-semibold text-violet-700 hover:text-violet-800">
                                        View & Request
                                    </button>
                                )}
                            </div>
                        </article>
                    ))}
                </div>
            )}
            <h3 className="mt-10 text-lg font-bold">Active Projects / Workspaces</h3>
            <p className="mt-1 text-sm text-slate-500">Projects where your request has been accepted.</p>
            {acceptedProjects.length === 0 ? <p className="mt-4 text-sm text-slate-500">No active projects yet.</p> : (
                <div className="mt-4 grid gap-6">
                    {acceptedProjects.map((project) => (
                        <article key={project.id} className="rounded-xl border border-emerald-200 bg-emerald-50 p-5">
                            <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-2">
                                <div>
                                    <h4 className="font-bold text-lg">{project.title}</h4>
                                    <p className="text-sm text-emerald-800 font-medium">Client: {project.clientCompany}</p>
                                </div>
                                <div className="flex flex-col items-end gap-2">
                                    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800 self-start">
                                        <CheckCircle2 className="h-3.5 w-3.5" /> Accepted
                                    </span>
                                    <button
                                        onClick={() => setOpenWorkspaceId(openWorkspaceId === project.id ? null : project.id)}
                                        className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700 transition"
                                    >
                                        {openWorkspaceId === project.id ? 'Close Workspace' : 'Open Workspace'}
                                    </button>
                                </div>
                            </div>
                            <p className="text-sm text-slate-600">${project.budgetMin}-${project.budgetMax} · Due {project.deadline && new Date(`${project.deadline}T00:00:00`).toLocaleDateString()}</p>
                            {openWorkspaceId === project.id && (
                                <div className="mt-6 pt-6 border-t border-emerald-200">
                                    <FreelancerActiveProject projectId={project.id} />
                                </div>
                            )}
                        </article>
                    ))}
                </div>
            )}
            
            <h3 className="mt-10 text-lg font-bold">My Proposals</h3>
            <p className="mt-1 text-sm text-slate-500">Track the status of your submitted proposals.</p>
            
            {proposals.length === 0 ? (
                <p className="mt-5 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-6 text-center text-sm text-slate-500">
                    You have not submitted any proposals yet.
                </p>
            ) : (
                <div className="mt-5 space-y-4">
                    {proposals.map((x) => (
                        <div key={x.id} className="flex flex-col gap-4 rounded-xl border border-slate-200 p-5 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                                <h4 className="font-bold text-slate-900">{x.projectTitle}</h4>
                                <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-slate-600">
                                    <span>${x.proposedBudget}</span>
                                    <span className="h-1 w-1 rounded-full bg-slate-300"></span>
                                    <span>{x.estimatedDuration} days</span>
                                    {x.portfolioProjectTitle && (
                                        <>
                                            <span className="h-1 w-1 rounded-full bg-slate-300"></span>
                                            <span className="truncate max-w-[200px]" title={x.portfolioProjectTitle}>Ref: {x.portfolioProjectTitle}</span>
                                        </>
                                    )}
                                </div>
                                <p className="mt-2 text-sm font-medium text-slate-500">
                                    Submitted on {new Date(x.createdAt).toLocaleDateString()}
                                </p>
                            </div>
                            
                            <div className="flex flex-col items-end gap-3 sm:items-center sm:flex-row">
                                <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold
                                    ${x.status === 'ACCEPTED' ? 'bg-emerald-100 text-emerald-800' : 
                                      x.status === 'REJECTED' || x.status === 'WITHDRAWN' ? 'bg-rose-100 text-rose-800' : 
                                      x.status === 'SHORTLISTED' ? 'bg-amber-100 text-amber-800' : 
                                      'bg-blue-100 text-blue-800'}`}>
                                    {x.status.replace('_', ' ')}
                                </span>
                                
                                {!['ACCEPTED', 'REJECTED', 'WITHDRAWN'].includes(x.status) && (
                                    <button onClick={() => void withdraw(x.id)} className="text-sm font-medium text-rose-600 hover:text-rose-700">
                                        Withdraw
                                    </button>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
            
            {/* Modal */}
            {selected && (
                <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-slate-900/60 p-4 backdrop-blur-sm">
                    <div className="relative w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white p-6 shadow-xl sm:p-8">
                        <button 
                            onClick={() => setSelected(null)} 
                            className="absolute right-6 top-6 text-slate-400 hover:text-slate-600"
                        >
                            <span className="sr-only">Close</span>
                            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                        
                        <div className="pr-8">
                            <p className="text-sm font-semibold text-violet-700 uppercase tracking-wider">{selected.category}</p>
                            <h3 className="mt-1 text-2xl font-bold text-slate-900">{selected.title}</h3>
                            <p className="mt-1 text-sm font-medium text-slate-600">{selected.clientCompany} · Budget: ${selected.budgetMin}–${selected.budgetMax}</p>
                        </div>
                        
                        <div className="mt-6 prose prose-slate max-w-none text-sm text-slate-600">
                            <p className="whitespace-pre-line">{selected.description}</p>
                            <p><strong>Deliverables:</strong> {selected.deliverables || 'To be agreed with the client'}</p>
                            <p><strong>Experience:</strong> {selected.requiredExperience ?? 0}+ years · <strong>Complexity:</strong> {selected.complexity}</p>
                            <p><strong>Deadline:</strong> {selected.deadline && new Date(`${selected.deadline}T00:00:00`).toLocaleDateString()}</p>
                            <p><strong>Required skills:</strong> {selected.skills.map(skill => `${skill.name} (${skill.importance})`).join(', ') || 'None listed'}</p>
                        </div>
                        
                        {match && (
                            <div className="mt-6 rounded-xl border border-violet-100 bg-violet-50 p-5 shadow-sm">
                                <div className="flex items-center justify-between border-b border-violet-200/60 pb-4">
                                    <div className="flex items-center gap-3">
                                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-violet-100 text-violet-700">
                                            <AlertCircle className="h-6 w-6" />
                                        </div>
                                        <div>
                                            <b className="text-lg font-black text-violet-900">{match.compatibilityScore}% Match</b>
                                            <p className="text-xs font-semibold uppercase tracking-wider text-violet-700">Project Compatibility</p>
                                        </div>
                                    </div>
                                </div>
                                <p className="mt-4 text-sm font-medium italic text-violet-800">"{match.explanation}"</p>
                                
                                <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-5">
                                    <div className="flex flex-col items-center rounded-lg bg-white p-2 text-center shadow-sm">
                                        <CheckCircle2 className="mb-1 h-4 w-4 text-violet-600" />
                                        <span className="text-sm font-bold text-violet-900">{match.skillMatchScore}%</span>
                                        <span className="text-[10px] font-semibold uppercase text-slate-500">Skills</span>
                                    </div>
                                    <div className="flex flex-col items-center rounded-lg bg-white p-2 text-center shadow-sm">
                                        <Star className="mb-1 h-4 w-4 text-violet-600" />
                                        <span className="text-sm font-bold text-violet-900">{match.proficiencyScore}%</span>
                                        <span className="text-[10px] font-semibold uppercase text-slate-500">Proficiency</span>
                                    </div>
                                    <div className="flex flex-col items-center rounded-lg bg-white p-2 text-center shadow-sm">
                                        <TrendingUp className="mb-1 h-4 w-4 text-violet-600" />
                                        <span className="text-sm font-bold text-violet-900">{match.experienceScore}%</span>
                                        <span className="text-[10px] font-semibold uppercase text-slate-500">Experience</span>
                                    </div>
                                    <div className="flex flex-col items-center rounded-lg bg-white p-2 text-center shadow-sm">
                                        <Award className="mb-1 h-4 w-4 text-violet-600" />
                                        <span className="text-sm font-bold text-violet-900">{match.portfolioRelevanceScore}%</span>
                                        <span className="text-[10px] font-semibold uppercase text-slate-500">Portfolio</span>
                                    </div>
                                    <div className="flex flex-col items-center rounded-lg bg-white p-2 text-center shadow-sm">
                                        <DollarSign className="mb-1 h-4 w-4 text-violet-600" />
                                        <span className="text-sm font-bold text-violet-900">{match.budgetCompatibilityScore}%</span>
                                        <span className="text-[10px] font-semibold uppercase text-slate-500">Budget</span>
                                    </div>
                                </div>

                                <div className="mt-5 grid gap-4 rounded-lg bg-white p-4 shadow-sm sm:grid-cols-2">
                                    <div>
                                        <h6 className="mb-2 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-emerald-700">
                                            <CheckCircle2 className="h-4 w-4" /> Matched Skills
                                        </h6>
                                        <div className="flex flex-wrap gap-1.5">
                                            {match.matchedSkills.length === 0 ? <span className="text-sm text-slate-500">None</span> : 
                                             match.matchedSkills.map(s => <span key={s} className="rounded bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">{s}</span>)}
                                        </div>
                                    </div>
                                    <div>
                                        <h6 className="mb-2 flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-rose-700">
                                            <AlertCircle className="h-4 w-4" /> Missing Skills
                                        </h6>
                                        <div className="flex flex-wrap gap-1.5">
                                            {match.missingSkills.length === 0 ? <span className="text-sm text-slate-500">None</span> : 
                                             match.missingSkills.map(s => <span key={s} className="rounded bg-rose-100 px-2 py-0.5 text-xs font-medium text-rose-800">{s}</span>)}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                        
                        <hr className="my-8 border-slate-200" />
                        
                        <div>
                            <h4 className="text-lg font-bold text-slate-900">Request Project</h4>
                            {message && <p className="mt-3 rounded-lg bg-emerald-50 p-3 text-sm font-medium text-emerald-800">{message}</p>}
                            {error && <p className="mt-3 rounded-lg bg-rose-50 p-3 text-sm font-medium text-rose-800">{error}</p>}
                            
                            {!message && (
                                <form onSubmit={submit} className="mt-5 space-y-4">
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700">How would you approach this project? <span className="text-rose-500">*</span></label>
                                        <textarea 
                                            required 
                                            value={cover} 
                                            onChange={e => setCover(e.target.value)} 
                                            rows={6}
                                            placeholder="Explain your project-specific approach, plan, and delivery steps..." 
                                            className="mt-1.5 w-full rounded-lg border border-slate-300 p-3 text-sm outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100"
                                        />
                                    </div>
                                    
                                    <div className="grid gap-4 sm:grid-cols-2">
                                        <div>
                                            <label className="block text-sm font-medium text-slate-700">Proposed Budget ($) <span className="text-rose-500">*</span></label>
                                            <input 
                                                required 
                                                type="number" 
                                                min="1" 
                                                step="0.01"
                                                value={budget} 
                                                onChange={e => setBudget(e.target.value)} 
                                                placeholder="e.g. 1500" 
                                                className="mt-1.5 w-full rounded-lg border border-slate-300 p-2.5 text-sm outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-slate-700">Estimated Duration (Days) <span className="text-rose-500">*</span></label>
                                            <input 
                                                required 
                                                type="number" 
                                                min="1" 
                                                value={days} 
                                                onChange={e => setDays(e.target.value)} 
                                                placeholder="e.g. 14" 
                                                className="mt-1.5 w-full rounded-lg border border-slate-300 p-2.5 text-sm outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100"
                                            />
                                        </div>
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-medium text-slate-700">Relevant Portfolio Project (Optional)</label>
                                        <select 
                                            value={selectedPortfolioId} 
                                            onChange={e => setSelectedPortfolioId(e.target.value)}
                                            className="mt-1.5 w-full rounded-lg border border-slate-300 p-2.5 text-sm outline-none transition focus:border-violet-500 focus:ring-2 focus:ring-violet-100"
                                        >
                                            <option value="">-- None selected --</option>
                                            {portfolio.map(p => (
                                                <option key={p.id} value={p.id}>{p.title}</option>
                                            ))}
                                        </select>
                                    </div>
                                    
                                    <div className="mt-8 flex justify-end gap-3">
                                        <button 
                                            type="button"
                                            onClick={() => setSelected(null)}
                                            className="rounded-lg px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100"
                                        >
                                            Cancel
                                        </button>
                                        <button 
                                            type="submit"
                                            disabled={submitting}
                                            className="rounded-lg bg-violet-600 px-5 py-2 text-sm font-semibold text-white hover:bg-violet-700 disabled:opacity-50"
                                        >
                                            {submitting ? 'Sending request...' : 'Request Project'}
                                        </button>
                                    </div>
                                </form>
                            )}
                        </div>
                    </div>
                </div>
            )}
        </section>
    );
}

function messageFrom(error: unknown, fallback: string) {
    return axios.isAxiosError<{ message?: string }>(error) ? error.response?.data?.message ?? fallback : fallback;
}
