import { useEffect, useState } from 'react';
import api from '../lib/api';
import axios from 'axios';
import { Check, X, Clock, Eye, AlertCircle } from 'lucide-react';

type Proposal = {
    id: number;
    projectId: number;
    projectTitle: string;
    freelancerId: number;
    freelancerName: string;
    coverLetter: string;
    proposedBudget: number;
    estimatedDuration: number;
    status: string;
    portfolioProjectId?: number;
    portfolioProjectTitle?: string;
    createdAt: string;
};

export default function ClientProjectProposals({ projectId }: { projectId: number }) {
    const [proposals, setProposals] = useState<Proposal[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [actionLoading, setActionLoading] = useState<number | null>(null);

    const load = async () => {
        try {
            const res = await api.get<Proposal[]>(`/client/projects/${projectId}/proposals`);
            setProposals(res.data);
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                setError(err.response?.data?.message || 'Could not load proposals');
            } else {
                setError('Could not load proposals');
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void load();
    }, [projectId]);

    const changeStatus = async (proposalId: number, status: string) => {
        if (status === 'ACCEPTED' && !window.confirm("Are you sure you want to accept this proposal? This may reject others.")) {
            return;
        }

        setActionLoading(proposalId);
        try {
            await api.patch(`/client/projects/${projectId}/proposals/${proposalId}/status`, { status });
            await load();
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                alert(err.response?.data?.message || 'Could not update status');
            } else {
                alert('Could not update status');
            }
        } finally {
            setActionLoading(null);
        }
    };

    if (loading) return <div className="mt-6 border-t border-slate-100 pt-6"><p className="text-sm text-slate-500">Loading proposals...</p></div>;
    if (error) return <div className="mt-6 border-t border-slate-100 pt-6"><p className="text-sm text-rose-600">{error}</p></div>;
    if (proposals.length === 0) return (
        <div className="mt-6 border-t border-slate-100 pt-6">
            <h4 className="font-bold text-slate-900 mb-2">Proposals</h4>
            <p className="text-sm text-slate-500 bg-slate-50 rounded-lg p-4 text-center border border-dashed border-slate-200">No proposals received yet.</p>
        </div>
    );

    return (
        <div className="mt-6 border-t border-slate-100 pt-6">
            <h4 className="font-bold text-slate-900 mb-4 flex items-center gap-2">
                Proposals
                <span className="bg-indigo-100 text-indigo-700 text-xs py-0.5 px-2 rounded-full font-semibold">{proposals.length}</span>
            </h4>
            
            <div className="space-y-4">
                {proposals.map(proposal => (
                    <div key={proposal.id} className={`rounded-xl border p-4 ${proposal.status === 'ACCEPTED' ? 'border-emerald-200 bg-emerald-50' : 'border-slate-200 bg-white'}`}>
                        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                            <div>
                                <div className="flex items-center gap-2">
                                    <h5 className="font-bold text-slate-900">{proposal.freelancerName}</h5>
                                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full
                                        ${proposal.status === 'ACCEPTED' ? 'bg-emerald-100 text-emerald-700' : 
                                          proposal.status === 'REJECTED' ? 'bg-rose-100 text-rose-700' : 
                                          proposal.status === 'SHORTLISTED' ? 'bg-amber-100 text-amber-700' : 
                                          proposal.status === 'WITHDRAWN' ? 'bg-slate-200 text-slate-700' : 
                                          'bg-blue-100 text-blue-700'}`}>
                                        {proposal.status.replace('_', ' ')}
                                    </span>
                                </div>
                                <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm font-medium text-slate-700">
                                    <span>Bid: <span className="text-indigo-700">${proposal.proposedBudget}</span></span>
                                    <span>Time: <span className="text-indigo-700">{proposal.estimatedDuration} days</span></span>
                                    <span className="text-slate-400 text-xs font-normal">
                                        Submitted {new Date(proposal.createdAt).toLocaleDateString()}
                                    </span>
                                </div>
                            </div>
                            
                            {/* Client Actions */}
                            {!['ACCEPTED', 'REJECTED', 'WITHDRAWN'].includes(proposal.status) && (
                                <div className="flex flex-wrap items-center gap-2 sm:shrink-0">
                                    {proposal.status === 'SUBMITTED' && (
                                        <button 
                                            onClick={() => changeStatus(proposal.id, 'UNDER_REVIEW')}
                                            disabled={actionLoading === proposal.id}
                                            className="inline-flex items-center gap-1 rounded bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700 hover:bg-blue-100 disabled:opacity-50"
                                        >
                                            <Eye className="h-3.5 w-3.5" /> Review
                                        </button>
                                    )}
                                    
                                    {(proposal.status === 'SUBMITTED' || proposal.status === 'UNDER_REVIEW') && (
                                        <button 
                                            onClick={() => changeStatus(proposal.id, 'SHORTLISTED')}
                                            disabled={actionLoading === proposal.id}
                                            className="inline-flex items-center gap-1 rounded bg-amber-50 px-2 py-1 text-xs font-semibold text-amber-700 hover:bg-amber-100 disabled:opacity-50"
                                        >
                                            <Clock className="h-3.5 w-3.5" /> Shortlist
                                        </button>
                                    )}
                                    
                                    <button 
                                        onClick={() => changeStatus(proposal.id, 'ACCEPTED')}
                                        disabled={actionLoading === proposal.id}
                                        className="inline-flex items-center gap-1 rounded bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-700 hover:bg-emerald-100 disabled:opacity-50"
                                    >
                                        <Check className="h-3.5 w-3.5" /> Accept
                                    </button>
                                    
                                    <button 
                                        onClick={() => changeStatus(proposal.id, 'REJECTED')}
                                        disabled={actionLoading === proposal.id}
                                        className="inline-flex items-center gap-1 rounded bg-rose-50 px-2 py-1 text-xs font-semibold text-rose-700 hover:bg-rose-100 disabled:opacity-50"
                                    >
                                        <X className="h-3.5 w-3.5" /> Reject
                                    </button>
                                </div>
                            )}
                        </div>
                        
                        <div className="mt-3">
                            <p className="whitespace-pre-wrap text-sm leading-relaxed text-slate-600 bg-slate-50 p-3 rounded-lg border border-slate-100">
                                {proposal.coverLetter}
                            </p>
                        </div>
                        
                        {proposal.portfolioProjectId && (
                            <div className="mt-3 flex items-center gap-2">
                                <AlertCircle className="h-4 w-4 text-indigo-500" />
                                <span className="text-sm font-medium text-slate-700">
                                    Relevant Portfolio Reference: <span className="text-indigo-700">{proposal.portfolioProjectTitle}</span>
                                </span>
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
}
