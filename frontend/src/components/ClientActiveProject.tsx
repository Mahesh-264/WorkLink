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

export default function ClientActiveProject({ projectId }: { projectId: number }) {
    const [milestones, setMilestones] = useState<Milestone[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Review form state
    const [reviewingId, setReviewingId] = useState<number | null>(null);
    const [feedback, setFeedback] = useState('');
    const [reviewAction, setReviewAction] = useState<'APPROVE' | 'REQUEST_CHANGES' | 'REJECT'>('APPROVE');
    const [submittingReview, setSubmittingReview] = useState(false);

    // New milestone form state
    const [showNewMilestone, setShowNewMilestone] = useState(false);
    const [mTitle, setMTitle] = useState('');
    const [mDescription, setMDescription] = useState('');
    const [mAmount, setMAmount] = useState('');
    const [mDeadline, setMDeadline] = useState('');
    const [submittingNew, setSubmittingNew] = useState(false);

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

    const submitReview = async (e: FormEvent) => {
        e.preventDefault();
        if (!reviewingId) return;
        setSubmittingReview(true);
        try {
            await api.patch(`/projects/${projectId}/milestones/${reviewingId}/review`, {
                action: reviewAction,
                feedback
            });
            setReviewingId(null);
            setFeedback('');
            void load();
        } catch (err) {
            alert('Could not submit review.');
        } finally {
            setSubmittingReview(false);
        }
    };

    const submitNewMilestone = async (e: FormEvent) => {
        e.preventDefault();
        setSubmittingNew(true);
        try {
            await api.post(`/projects/${projectId}/milestones`, {
                title: mTitle,
                description: mDescription,
                amount: Number(mAmount),
                deadline: mDeadline
            });
            setShowNewMilestone(false);
            setMTitle('');
            setMDescription('');
            setMAmount('');
            setMDeadline('');
            void load();
        } catch (err) {
            alert('Could not create milestone.');
        } finally {
            setSubmittingNew(false);
        }
    };

    if (loading) return <div className="mt-4 text-sm text-slate-500">Loading workspace...</div>;
    if (error) return <div className="mt-4 text-sm text-rose-600">{error}</div>;

    const allApproved = milestones.length > 0 && milestones.every(m => m.status === 'APPROVED');

    return (
        <div className="mt-4 rounded-xl border border-indigo-200 bg-indigo-50/50 p-5">
            <div className="flex items-center justify-between mb-4">
                <h4 className="font-bold text-slate-900">Project Workspace</h4>
                {allApproved ? (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-semibold text-emerald-800">
                        <CheckCircle2 className="h-4 w-4" /> Project Completed
                    </span>
                ) : (
                    <button onClick={() => setShowNewMilestone(!showNewMilestone)} className="text-sm font-semibold text-indigo-700 hover:underline">
                        + Add Milestone
                    </button>
                )}
            </div>

            {showNewMilestone && (
                <form onSubmit={submitNewMilestone} className="mb-6 rounded-xl border border-indigo-100 bg-white p-4 shadow-sm">
                    <h5 className="font-semibold text-sm mb-3">New Milestone</h5>
                    <div className="grid gap-3 sm:grid-cols-2">
                        <label className="block text-sm">
                            Title *
                            <input required value={mTitle} onChange={e => setMTitle(e.target.value)} className="mt-1 w-full rounded border px-3 py-1.5" />
                        </label>
                        <label className="block text-sm">
                            Deadline *
                            <input type="date" required value={mDeadline} onChange={e => setMDeadline(e.target.value)} className="mt-1 w-full rounded border px-3 py-1.5" />
                        </label>
                        <label className="block text-sm sm:col-span-2">
                            Description
                            <input value={mDescription} onChange={e => setMDescription(e.target.value)} className="mt-1 w-full rounded border px-3 py-1.5" />
                        </label>
                        <label className="block text-sm">
                            Amount ($) *
                            <input type="number" min="0" step="0.01" required value={mAmount} onChange={e => setMAmount(e.target.value)} className="mt-1 w-full rounded border px-3 py-1.5" />
                        </label>
                    </div>
                    <div className="mt-4 flex gap-2">
                        <button disabled={submittingNew} type="submit" className="rounded bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700">Save</button>
                        <button type="button" onClick={() => setShowNewMilestone(false)} className="px-4 py-2 text-sm font-medium hover:underline">Cancel</button>
                    </div>
                </form>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-2 space-y-3">
                    <h5 className="font-semibold text-slate-800 mb-2">Milestones & Deliverables</h5>
                    {milestones.length === 0 && <p className="text-sm text-slate-500">No milestones yet. Create one to get started.</p>}
                    {milestones.map(m => (
                        <div key={m.id} className="rounded-xl border border-slate-200 bg-white p-4">
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
                            
                            {m.status === 'SUBMITTED' && (
                                <div className="mt-4 rounded-lg bg-amber-50 p-4 border border-amber-100">
                                    <h6 className="font-semibold text-amber-800 text-sm">Submission Review</h6>
                                    <p className="mt-1 text-sm text-slate-700 whitespace-pre-line">{m.submissionNotes}</p>
                                    {m.submissionUrl && <a href={m.submissionUrl} target="_blank" rel="noreferrer" className="mt-2 inline-flex items-center gap-1 text-sm font-semibold text-indigo-700 hover:underline"><FileText className="h-4 w-4" /> View Attachment</a>}
                                    
                                    {reviewingId === m.id ? (
                                        <form onSubmit={submitReview} className="mt-4">
                                            <label className="block text-sm font-medium text-slate-700">Action</label>
                                            <select value={reviewAction} onChange={(e) => setReviewAction(e.target.value as any)} className="mt-1 block w-full rounded border p-2">
                                                <option value="APPROVE">Approve</option>
                                                <option value="REQUEST_CHANGES">Request Changes</option>
                                                <option value="REJECT">Reject</option>
                                            </select>
                                            <label className="block text-sm font-medium text-slate-700 mt-3">Feedback (Optional)</label>
                                            <textarea value={feedback} onChange={e => setFeedback(e.target.value)} className="mt-1 block w-full rounded border p-2" rows={2} />
                                            <div className="mt-3 flex gap-2">
                                                <button disabled={submittingReview} type="submit" className="rounded bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white">Submit Review</button>
                                                <button type="button" onClick={() => setReviewingId(null)} className="px-3 py-1.5 text-sm hover:underline">Cancel</button>
                                            </div>
                                        </form>
                                    ) : (
                                        <button onClick={() => setReviewingId(m.id)} className="mt-4 rounded bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700">Review Submission</button>
                                    )}
                                </div>
                            )}
                            
                            {(m.status === 'REVISION_REQUESTED' || m.status === 'REJECTED') && (
                                <div className="mt-3 rounded-lg bg-rose-50 p-3 text-sm border border-rose-100">
                                    <span className="font-semibold text-rose-800">Your Feedback: </span>
                                    <span className="text-slate-700">{m.clientFeedback}</span>
                                </div>
                            )}
                        </div>
                    ))}
                    
                    {allApproved && (
                        <div className="mt-6 border-t border-indigo-200 pt-4 text-center">
                            <button className="rounded-xl bg-emerald-600 px-6 py-3 font-bold text-white hover:bg-emerald-700 transition">Leave a Review for Freelancer</button>
                        </div>
                    )}
                </div>

                <div className="lg:col-span-1 flex flex-col h-[600px]">
                    <h5 className="font-semibold text-slate-800 mb-2 shrink-0">Activity & Updates</h5>
                    <div className="flex-1 overflow-hidden min-h-0">
                        <ProjectWorkspaceActivity projectId={projectId} isFreelancer={false} />
                    </div>
                </div>
            </div>
        </div>
    );
}
