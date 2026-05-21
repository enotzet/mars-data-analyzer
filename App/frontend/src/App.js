import React, { useState, useEffect, useCallback } from 'react';
import { S } from './styles';
import { getChatMessages, saveChat, unsaveChat } from './api';
import { useJobs } from './hooks/useJobs';
import { useChats } from './hooks/useChats';
import Sidebar from './components/Sidebar';
import ChatView from './components/ChatView';
import JobsPanel from './components/JobsPanel';
import BenchmarkPanel from './components/BenchmarkPanel';

const newSessionId = () =>
    crypto.randomUUID?.() ?? Math.random().toString(36).slice(2);

const toMessages = (history) => {
    const msgs = [];
    history.forEach(h => {
        msgs.push({ role: 'user', text: h.question });
        msgs.push({
            role: 'assistant',
            text: h.answer,
            sources: h.sources ?? [],
            jobId: h.jobId,
            promptVersionId: h.promptVersionId,
        });
    });
    return msgs;
};

export default function App() {
    const [sessionId, setSessionId] = useState(newSessionId);
    const [ragEnabled, setRagEnabled] = useState(true);
    const [panel, setPanel] = useState(null);
    const [chatKey, setChatKey] = useState(0);
    const [initialMessages, setInitialMessages] = useState([]);

    const { jobs, jobsBusy, fetchJobs, ingest, retryJob, activeJobs } = useJobs();
    const { chats, refresh: refreshChats } = useChats();

    const handleSelectChat = useCallback(async (sid) => {
        try {
            const history = await getChatMessages(sid);
            setSessionId(sid);
            setInitialMessages(toMessages(history));
            setChatKey(k => k + 1);
            setPanel(null);
            window.history.replaceState({}, '', `?chat=${sid}`);
        } catch (_) { /* silent */ }
    }, []);

    // Load shared chat from URL on first mount
    useEffect(() => {
        const sharedId = new URLSearchParams(window.location.search).get('chat');
        if (sharedId) handleSelectChat(sharedId);
    }, [handleSelectChat]);

    const handleIngest = async (source) => {
        await ingest(source);
        setPanel('jobs');
    };

    const handleNewChat = () => {
        setSessionId(newSessionId());
        setInitialMessages([]);
        setChatKey(k => k + 1);
        setPanel(null);
        window.history.replaceState({}, '', window.location.pathname);
    };

    const handleToggleSave = async (sid, currentlySaved) => {
        try {
            if (currentlySaved) {
                await unsaveChat(sid);
            } else {
                await saveChat(sid);
            }
            await refreshChats();
        } catch (_) { /* silent */ }
    };

    const handleShareChat = async (sid) => {
        const url = `${window.location.origin}${window.location.pathname}?chat=${sid}`;
        try {
            await navigator.clipboard.writeText(url);
        } catch (_) {
            window.prompt('Copy this share link:', url);
        }
    };

    return (
        <div style={S.shell}>
            <Sidebar
                onNewChat={handleNewChat}
                onIngest={handleIngest}
                jobsBusy={jobsBusy}
                panel={panel}
                setPanel={setPanel}
                activeJobsCount={activeJobs.length}
                ragEnabled={ragEnabled}
                setRagEnabled={setRagEnabled}
                sessionId={sessionId}
                chats={chats}
                currentSessionId={sessionId}
                onSelectChat={handleSelectChat}
                onToggleSave={handleToggleSave}
                onShareChat={handleShareChat}
            />

            <main style={S.main}>
                <header style={S.topbar}>
                    <span style={{ fontWeight: 600 }}>Mars Data Analyzer</span>
                    <span style={{ fontSize: 12, color: '#888' }}>
                        {ragEnabled ? 'RAG + Hybrid Reranking' : 'General knowledge'}
                    </span>
                </header>

                {panel && (
                    <div style={S.panelBackdrop} onClick={() => setPanel(null)}>
                        <div style={S.panelSheet} onClick={e => e.stopPropagation()}>
                            {panel === 'jobs' && (
                                <JobsPanel jobs={jobs} retryJob={retryJob} fetchJobs={fetchJobs} />
                            )}
                            {panel === 'benchmark' && (
                                <BenchmarkPanel sessionId={sessionId} />
                            )}
                        </div>
                    </div>
                )}

                <ChatView
                    key={chatKey}
                    sessionId={sessionId}
                    ragEnabled={ragEnabled}
                    initialMessages={initialMessages}
                    onMessageSent={refreshChats}
                />
            </main>
        </div>
    );
}
