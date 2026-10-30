import { useState, useEffect, type FormEvent } from 'react';
import api from '../lib/api';
import { Send, Upload } from 'lucide-react';

export type ProjectActivityType = 'MESSAGE' | 'PROGRESS_UPDATE' | 'FILE_UPLOAD';

export interface ProjectActivity {
    id: number;
    projectId: number;
    senderId: number;
    senderName: string;
    senderRole: string;
    type: ProjectActivityType;
    content: string;
    progressPercentage: number | null;
    createdAt: string;
}

export default function ProjectWorkspaceActivity({ projectId, isFreelancer }: { projectId: number, isFreelancer: boolean }) {
    const [activities, setActivities] = useState<ProjectActivity[]>([]);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState('');
    const [fileUrl, setFileUrl] = useState('');
    const [progress, setProgress] = useState<number>(0);
    const [progressNote, setProgressNote] = useState('');

    const [activeTab, setActiveTab] = useState<'MESSAGE' | 'FILE' | 'PROGRESS'>('MESSAGE');

    const loadActivities = async () => {
        try {
            const res = await api.get<ProjectActivity[]>(`/projects/${projectId}/workspace/activities`);
            setActivities(res.data);
            if (res.data.length > 0) {
                const latestProgress = res.data.reduce((max, act) => {
                    if (act.progressPercentage !== null && act.progressPercentage > max) return act.progressPercentage;
                    return max;
                }, 0);
                setProgress(latestProgress);
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadActivities();
        const intervalId = setInterval(loadActivities, 5000); // Auto-refresh for real-time feel
        return () => clearInterval(intervalId);
    }, [projectId]);

    const postActivity = async (endpoint: string, payload: any) => {
        try {
            await api.post(`/projects/${projectId}/workspace/${endpoint}`, payload);
            setMessage('');
            setFileUrl('');
            setProgressNote('');
            void loadActivities();
        } catch (err) {
            alert('Failed to post update.');
        }
    };

    const handleMessageSubmit = (e: FormEvent) => {
        e.preventDefault();
        if (!message.trim()) return;
        postActivity('messages', { content: message });
    };

    const handleFileSubmit = (e: FormEvent) => {
        e.preventDefault();
        if (!fileUrl.trim()) return;
        postActivity('files', { content: fileUrl });
    };

    const handleProgressSubmit = (e: FormEvent) => {
        e.preventDefault();
        postActivity('progress', { percentage: progress, note: progressNote });
    };

    if (loading && activities.length === 0) return <div className="text-sm text-slate-500">Loading timeline...</div>;

    const myRole = isFreelancer ? 'FREELANCER' : 'CLIENT';

    return (
        <div className="flex flex-col h-full rounded-xl border border-slate-200 bg-white shadow-sm overflow-hidden">
            {/* Timeline */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4 max-h-[500px]">
                {activities.length === 0 && <p className="text-sm text-slate-500 text-center py-8">No activities yet. Send a message to get started.</p>}
                
                {activities.map(act => {
                    const isMe = act.senderRole === myRole;
                    return (
                        <div key={act.id} className={`flex flex-col ${isMe ? 'items-end' : 'items-start'}`}>
                            <div className="flex items-baseline gap-2 mb-1">
                                <span className="text-xs font-semibold text-slate-700">{act.senderName}</span>
                                <span className="text-[10px] text-slate-400">{new Date(act.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                            </div>
                            
                            {act.type === 'MESSAGE' && (
                                <div className={`px-4 py-2 rounded-2xl max-w-[85%] text-sm ${isMe ? 'bg-indigo-600 text-white rounded-br-none' : 'bg-slate-100 text-slate-800 rounded-bl-none'}`}>
                                    {act.content}
                                </div>
                            )}

                            {act.type === 'FILE_UPLOAD' && (
                                <div className={`px-4 py-3 rounded-2xl max-w-[85%] text-sm border ${isMe ? 'bg-indigo-50 border-indigo-200 rounded-br-none' : 'bg-slate-50 border-slate-200 rounded-bl-none'}`}>
                                    <div className="flex items-center gap-2 font-semibold mb-1">
                                        <Upload className="h-4 w-4" /> File Uploaded
                                    </div>
                                    <a href={act.content} target="_blank" rel="noreferrer" className="text-blue-600 hover:underline break-all">{act.content}</a>
                                </div>
                            )}

                            {act.type === 'PROGRESS_UPDATE' && (
                                <div className="w-full my-2 flex justify-center">
                                    <div className="bg-emerald-50 border border-emerald-100 rounded-lg p-3 text-center w-full max-w-sm">
                                        <div className="text-xs font-semibold text-emerald-800 mb-1">Progress Update</div>
                                        <div className="text-2xl font-bold text-emerald-600 mb-2">{act.progressPercentage}%</div>
                                        <div className="w-full bg-emerald-200 rounded-full h-2 mb-2">
                                            <div className="bg-emerald-600 h-2 rounded-full" style={{ width: `${act.progressPercentage}%` }}></div>
                                        </div>
                                        {act.content && <p className="text-sm text-emerald-900 mt-2 italic">"{act.content}"</p>}
                                    </div>
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>

            {/* Input Area */}
            <div className="border-t border-slate-200 bg-slate-50 p-3">
                <div className="flex gap-2 mb-3 border-b border-slate-200 pb-2">
                    <button onClick={() => setActiveTab('MESSAGE')} className={`text-xs font-semibold px-3 py-1 rounded-full ${activeTab === 'MESSAGE' ? 'bg-slate-200 text-slate-800' : 'text-slate-500 hover:bg-slate-100'}`}>Message</button>
                    <button onClick={() => setActiveTab('FILE')} className={`text-xs font-semibold px-3 py-1 rounded-full ${activeTab === 'FILE' ? 'bg-slate-200 text-slate-800' : 'text-slate-500 hover:bg-slate-100'}`}>Upload</button>
                    {isFreelancer && (
                        <button onClick={() => setActiveTab('PROGRESS')} className={`text-xs font-semibold px-3 py-1 rounded-full ${activeTab === 'PROGRESS' ? 'bg-slate-200 text-slate-800' : 'text-slate-500 hover:bg-slate-100'}`}>Update Progress</button>
                    )}
                </div>

                {activeTab === 'MESSAGE' && (
                    <form onSubmit={handleMessageSubmit} className="flex gap-2">
                        <input type="text" value={message} onChange={e => setMessage(e.target.value)} placeholder="Type a message..." className="flex-1 rounded-full border border-slate-300 px-4 py-2 text-sm focus:outline-none focus:border-indigo-500" />
                        <button type="submit" disabled={!message.trim()} className="rounded-full bg-indigo-600 p-2 text-white hover:bg-indigo-700 disabled:opacity-50">
                            <Send className="h-4 w-4" />
                        </button>
                    </form>
                )}

                {activeTab === 'FILE' && (
                    <form onSubmit={handleFileSubmit} className="flex gap-2">
                        <input type="url" value={fileUrl} onChange={e => setFileUrl(e.target.value)} placeholder="Paste file URL here..." className="flex-1 rounded-full border border-slate-300 px-4 py-2 text-sm focus:outline-none focus:border-indigo-500" />
                        <button type="submit" disabled={!fileUrl.trim()} className="rounded-full bg-indigo-600 p-2 text-white hover:bg-indigo-700 disabled:opacity-50">
                            <Upload className="h-4 w-4" />
                        </button>
                    </form>
                )}

                {activeTab === 'PROGRESS' && isFreelancer && (
                    <form onSubmit={handleProgressSubmit} className="space-y-2">
                        <div className="flex items-center gap-3">
                            <input type="range" min="0" max="100" value={progress} onChange={e => setProgress(Number(e.target.value))} className="flex-1" />
                            <span className="font-bold text-sm w-12">{progress}%</span>
                        </div>
                        <div className="flex gap-2">
                            <input type="text" value={progressNote} onChange={e => setProgressNote(e.target.value)} placeholder="Daily update note (optional)" className="flex-1 rounded-full border border-slate-300 px-4 py-1.5 text-sm" />
                            <button type="submit" className="rounded-full bg-emerald-600 px-4 py-1.5 text-sm font-semibold text-white hover:bg-emerald-700">Update</button>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
}
