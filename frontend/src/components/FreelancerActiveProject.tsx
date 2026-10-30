import { useState, useEffect, type FormEvent } from 'react';
import api from '../lib/api';
import { CheckCircle2, FileText } from 'lucide-react';
import ProjectWorkspaceActivity from './ProjectWorkspaceActivity';

type Milestone = {
    id: number;
    title: string;
    description: string | null;
    amount: number;
    deadline: string;
    status: string;
    submissionNotes: string | null;
    submissionUrl: string | null;
    submittedAt: string | null;
    clientFeedback: string | null;
    revisionCount: number;
};

export default function FreelancerActiveProject({ projectId }: { projectId: number }) {
    const [milestones, setMilestones] = useState<Milestone[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Submission form state
    const [submittingId, setSubmittingId] = useState<number | null>(null);
    const [submissionNotes, setSubmissionNotes] = useState('');
    const [submissionUrl, setSubmissionUrl] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const load = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await api.get<Milestone[]>(`/projects/${projectId}/milestones`);
            setMilestones(res.data);
        } catch (err) {
            setError('Could not load milestones.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void load();
    }, [projectId]);

    const submitWork = async (e: FormEvent) => {
        e.preventDefault();
        if (!submittingId) return;
        setIsSubmitting(true);
        try {
            await api.patch(`/projects/${projectId}/milestones/${submittingId}/submit`, {
                submissionNotes,
                submissionUrl
            });
            setSubmittingId(null);
            setSubmissionNotes('');
            setSubmissionUrl('');
            void load();
        } catch (err) {
            alert('Could not submit milestone.');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (loading) return <div className="mt-4 text-sm text-slate-500">Loading workspace...</div>;
    if (error) return <div className="mt-4 text-sm text-rose-600">{error}</div>;

    const allApproved = milestones.length > 0 && milestones.every(m => m.status === 'APPROVED');

    return (
        <div className="mt-4 rounded-xl border border-violet-200 bg-violet-50/50 p-5">
            <div className="flex items-center justify-between mb-4">
                <h4 className="font-bold text-slate-900">Project Workspace</h4>
                {allApproved && (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800">
                        <CheckCircle2 className="h-4 w-4" /> Project Completed
                    </span>
                )}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2 space-y-3">
                    <h5 className="font-semibold text-slate-800 mb-2">Milestones & Deliverables</h5>
                    {milestones.length === 0 && <p className="text-sm text-slate-500">No milestones have been created by the client yet.</p>}
                    {milestones.map(m => (
                        <div key={m.id} className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
                            <div className="flex justify-between items-start">
                                <div>
                                    <div className="flex items-center gap-2">
                                        <h5 className="font-bold">{m.title}</h5>
                                        <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">{m.status.replace('_', ' ')}</span>
                                    </div>
                                    <p className="text-sm text-slate-500 mt-1">${m.amount} · Due {new Date(`${m.deadline}T00:00:00`).toLocaleDateString()}</p>
                                </div>
                            </div>
                            {m.description && <p className="mt-2 text-sm text-slate-700">{m.description}</p>}
                            
                            {(m.status === 'PENDING' || m.status === 'IN_PROGRESS' || m.status === 'REVISION_REQUESTED') && (
                                <div className="mt-4 border-t border-slate-100 pt-3">
                                    {m.status === 'REVISION_REQUESTED' && (
                                        <div className="mb-3 rounded-lg bg-rose-50 p-3 text-sm border border-rose-100">
                                            <span className="font-semibold text-rose-800">Revision Request: </span>
                                            <span className="text-slate-700">{m.clientFeedback}</span>
                                        </div>
                                    )}
                                    
                                    {submittingId === m.id ? (
                                        <form onSubmit={submitWork} className="rounded-lg bg-slate-50 p-4 border border-slate-200">
                                            <h6 className="font-semibold text-sm mb-3">Submit Work for Review</h6>
                                            <label className="block text-sm font-medium text-slate-700">Notes (Optional)</label>
                                            <textarea value={submissionNotes} onChange={e => setSubmissionNotes(e.target.value)} className="mt-1 block w-full rounded border p-2" rows={2} />
                                            
                                            <label className="block text-sm font-medium text-slate-700 mt-3">Link/URL (Optional)</label>
                                            <input type="url" value={submissionUrl} onChange={e => setSubmissionUrl(e.target.value)} placeholder="https://..." className="mt-1 block w-full rounded border p-2" />
                                            
                                            <div className="mt-3 flex gap-2">
                                                <button disabled={isSubmitting} type="submit" className="rounded bg-violet-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-violet-700">Submit Work</button>
                                                <button type="button" onClick={() => setSubmittingId(null)} className="px-3 py-1.5 text-sm hover:underline">Cancel</button>
                                            </div>
                                        </form>
                                    ) : (
                                        <button onClick={() => setSubmittingId(m.id)} className="rounded bg-violet-600 px-4 py-2 text-sm font-semibold text-white hover:bg-violet-700">
                                            Submit Work
                                        </button>
                                    )}
                                </div>
                            )}
                            
                            {(m.status === 'SUBMITTED' || m.status === 'APPROVED') && (
                                <div className="mt-3 rounded-lg bg-slate-50 p-3 text-sm border border-slate-200">
                                    <span className="font-semibold text-slate-800">Your Submission: </span>
                                    <span className="text-slate-700">{m.submissionNotes}</span>
                                    {m.submissionUrl && <div className="mt-2"><a href={m.submissionUrl} target="_blank" rel="noreferrer" className="text-violet-700 hover:underline inline-flex items-center gap-1"><FileText className="h-4 w-4" /> View Link</a></div>}
                                </div>
                            )}
                            
                            {m.status === 'APPROVED' && m.clientFeedback && (
                                <div className="mt-2 rounded-lg bg-emerald-50 p-3 text-sm border border-emerald-100">
                                    <span className="font-semibold text-emerald-800">Client Feedback: </span>
                                    <span className="text-slate-700">{m.clientFeedback}</span>
                                </div>
                            )}
                        </div>
                    ))}
                    
                    {allApproved && (
                        <div className="mt-6 border-t border-violet-200 pt-4 text-center">
                            <button className="rounded-xl bg-emerald-600 px-6 py-3 font-bold text-white hover:bg-emerald-700 transition">Leave a Review for Client</button>
                        </div>
                    )}
                </div>

                <div className="lg:col-span-1 flex flex-col h-[600px]">
                    <h5 className="font-semibold text-slate-800 mb-2 shrink-0">Activity & Updates</h5>
                    <div className="flex-1 overflow-hidden min-h-0">
                        <ProjectWorkspaceActivity projectId={projectId} isFreelancer={true} />
                    </div>
                </div>
            </div>
        </div>
    );
}
