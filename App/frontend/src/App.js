import React, { useState, useEffect, useRef, useCallback } from 'react';

const API = 'http://localhost:8080/api';

/* ------------------------------------------------------------------ */
/*  Tiny SVG icons (no dependency needed)                             */
/* ------------------------------------------------------------------ */
const Icon = {
    plus:     <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><line x1="8" y1="3" x2="8" y2="13"/><line x1="3" y1="8" x2="13" y2="8"/></svg>,
    send:     <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="8" y1="13" x2="8" y2="3"/><polyline points="3,7 8,3 13,7"/></svg>,
    rocket:   <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"><path d="M8 1c0 0-5 3-5 9l2 3 3-2 3 2 2-3c0-6-5-9-5-9z"/><circle cx="8" cy="7" r="1.2"/></svg>,
    chart:    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"><rect x="2" y="8" width="2.5" height="6"/><rect x="6.75" y="4" width="2.5" height="10"/><rect x="11.5" y="6" width="2.5" height="8"/></svg>,
    link:     <svg width="12" height="12" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M6 10l4-4"/><path d="M9 3h4v4"/><path d="M13 9v3a1 1 0 01-1 1H4a1 1 0 01-1-1V5a1 1 0 011-1h3"/></svg>,
    chevron:  <svg width="12" height="12" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="4,6 8,10 12,6"/></svg>,
    retry:    <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M2 8a6 6 0 0111.5-2.3"/><polyline points="14,2 14,6 10,6"/><path d="M14 8a6 6 0 01-11.5 2.3"/><polyline points="2,14 2,10 6,10"/></svg>,
    stop:     <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor"><rect x="4" y="4" width="8" height="8" rx="1"/></svg>,
};

/* ------------------------------------------------------------------ */
/*  App                                                               */
/* ------------------------------------------------------------------ */
export default function App() {
    /* --- state --- */
    const [sessionId] = useState(() => crypto.randomUUID?.() ?? Math.random().toString(36).slice(2));
    const [ragEnabled, setRagEnabled] = useState(true);

    // chat
    const [messages, setMessages] = useState([]);
    const [input, setInput] = useState('');
    const [streaming, setStreaming] = useState(false);
    const endRef = useRef(null);
    const inputRef = useRef(null);

    // sidebar panel: null | 'jobs' | 'benchmark'
    const [panel, setPanel] = useState(null);

    // jobs
    const [jobs, setJobs] = useState([]);
    const [jobsBusy, setJobsBusy] = useState(false);

    // benchmark
    const [benchBaseline, setBenchBaseline] = useState(null);
    const [benchReranked, setBenchReranked] = useState(null);
    const [benchRunning, setBenchRunning] = useState(null); // null | 'baseline' | 'reranked'

    /* --- auto-scroll --- */
    useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages, streaming]);

    /* --- jobs polling --- */
    const fetchJobs = useCallback(async () => {
        try {
            const r = await fetch(`${API}/mars/jobs`);
            if (r.ok) setJobs(await r.json());
        } catch (_) { /* silent */ }
    }, []);
    useEffect(() => { fetchJobs(); const t = setInterval(fetchJobs, 4000); return () => clearInterval(t); }, [fetchJobs]);

    /* --- actions --- */
    const send = async (e) => {
        e?.preventDefault();
        const q = input.trim();
        if (!q || streaming) return;
        setInput('');
        setMessages(prev => [...prev, { role: 'user', text: q }]);
        setStreaming(true);
        try {
            const r = await fetch(`${API}/mars/chat`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ question: q, sessionId, ragEnabled, userId: 'default' }),
            });
            const d = await r.json();
            setMessages(prev => [...prev, {
                role: 'assistant',
                text: d.answer,
                sources: d.sources ?? [],
                jobId: d.jobId,
                promptVersionId: d.promptVersionId,
            }]);
        } catch (err) {
            setMessages(prev => [...prev, { role: 'assistant', text: 'Something went wrong: ' + err }]);
        } finally {
            setStreaming(false);
            setTimeout(() => inputRef.current?.focus(), 50);
        }
    };

    const ingest = async (source) => {
        setJobsBusy(true);
        try {
            await fetch(`${API}/mars/ingest`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ source }),
            });
            await fetchJobs();
            setPanel('jobs');
        } catch (_) { /* silent */ }
        setJobsBusy(false);
    };

    const retryJob = async (id) => {
        await fetch(`${API}/mars/ingest/${id}/retry`, { method: 'POST' });
        fetchJobs();
    };

    const bench = async (mode) => {
        setBenchRunning(mode);
        try {
            const r = await fetch(`${API}/benchmark/run?mode=${mode}&sessionId=${sessionId}`, { method: 'POST' });
            const d = await r.json();
            if (mode === 'baseline') setBenchBaseline(d); else setBenchReranked(d);
        } catch (_) { /* silent */ }
        setBenchRunning(null);
    };

    const newChat = () => { setMessages([]); setPanel(null); inputRef.current?.focus(); };

    /* --- running jobs indicator --- */
    const activeJobs = jobs.filter(j => j.status === 'RUNNING' || j.status === 'PENDING');

    /* ================================================================ */
    /*  RENDER                                                          */
    /* ================================================================ */
    return (
        <div style={S.shell}>
            {/* -------- SIDEBAR -------- */}
            <aside style={S.sidebar}>
                {/* new-chat button */}
                <button style={S.newChat} onClick={newChat}>
                    {Icon.plus}<span>New chat</span>
                </button>

                {/* ingestion section */}
                <div style={S.sideSection}>
                    <div style={S.sideLabel}>Data Ingestion</div>
                    <button style={S.sideBtn} disabled={jobsBusy} onClick={() => ingest('all')}>
                        {Icon.rocket}<span>Analyze All Sources</span>
                    </button>
                    <button style={S.sideBtn} disabled={jobsBusy} onClick={() => ingest('nasa-image-library')}>
                        <span style={S.dot('#a78bfa')} />NASA Image Library
                    </button>
                    <button style={S.sideBtn} disabled={jobsBusy} onClick={() => ingest('mars-rover-photos')}>
                        <span style={S.dot('#fb923c')} />Mars Rover Photos
                    </button>
                </div>

                {/* jobs / benchmark toggles */}
                <div style={S.sideSection}>
                    <button
                        style={{ ...S.sideBtn, ...(panel === 'jobs' ? S.sideBtnActive : {}) }}
                        onClick={() => setPanel(panel === 'jobs' ? null : 'jobs')}
                    >
                        <span>Job History</span>
                        {activeJobs.length > 0 && <span style={S.badge}>{activeJobs.length}</span>}
                    </button>
                    <button
                        style={{ ...S.sideBtn, ...(panel === 'benchmark' ? S.sideBtnActive : {}) }}
                        onClick={() => setPanel(panel === 'benchmark' ? null : 'benchmark')}
                    >
                        {Icon.chart}<span>Benchmark</span>
                    </button>
                </div>

                {/* footer */}
                <div style={S.sideFooter}>
                    <div style={S.ragToggle}>
                        <span style={{ fontSize: 13 }}>RAG retrieval</span>
                        <button style={ragEnabled ? S.toggleOn : S.toggleOff} onClick={() => setRagEnabled(v => !v)}>
                            <span style={{ ...S.toggleKnob, marginLeft: ragEnabled ? 16 : 2 }} />
                        </button>
                    </div>
                    <div style={{ fontSize: 11, color: '#555', marginTop: 6 }}>session {sessionId.slice(0, 8)}</div>
                </div>
            </aside>

            {/* -------- MAIN -------- */}
            <main style={S.main}>
                {/* top bar (minimal, like ChatGPT) */}
                <header style={S.topbar}>
                    <span style={{ fontWeight: 600 }}>Mars Data Analyzer</span>
                    <span style={{ fontSize: 12, color: '#888' }}>
                        {ragEnabled ? 'RAG + Hybrid Reranking' : 'General knowledge'}
                    </span>
                </header>

                {/* ---------- SIDE PANEL OVERLAY ---------- */}
                {panel && (
                    <div style={S.panelBackdrop} onClick={() => setPanel(null)}>
                        <div style={S.panelSheet} onClick={e => e.stopPropagation()}>
                            {panel === 'jobs' && <JobsPanel jobs={jobs} retryJob={retryJob} fetchJobs={fetchJobs} />}
                            {panel === 'benchmark' && (
                                <BenchmarkPanel
                                    baseline={benchBaseline}
                                    reranked={benchReranked}
                                    running={benchRunning}
                                    onRun={bench}
                                />
                            )}
                        </div>
                    </div>
                )}

                {/* ---------- MESSAGES ---------- */}
                <div style={S.chatScroll}>
                    {messages.length === 0 && (
                        <div style={S.empty}>
                            <div style={S.emptyIcon}>
                                <svg width="32" height="32" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"><path d="M8 1c0 0-5 3-5 9l2 3 3-2 3 2 2-3c0-6-5-9-5-9z"/><circle cx="8" cy="7" r="1.2"/></svg>
                            </div>
                            <h2 style={{ fontSize: 22, fontWeight: 600, margin: '16px 0 8px' }}>Mars Data Analyzer</h2>
                            <p style={{ color: '#888', maxWidth: 420, lineHeight: 1.5, fontSize: 14 }}>
                                Ask questions about the Martian surface, geology, climate, and rover observations.
                                Enable RAG to ground answers in analyzed NASA imagery.
                            </p>
                        </div>
                    )}

                    {messages.map((m, i) => (
                        <MessageRow key={i} msg={m} />
                    ))}

                    {streaming && (
                        <div style={{ ...S.msgRow, background: '#212121' }}>
                            <div style={S.msgInner}>
                                <div style={S.avatarBot}>M</div>
                                <div className="mars-dots" style={S.thinkDots}><span /><span /><span /></div>
                            </div>
                        </div>
                    )}
                    <div ref={endRef} />
                </div>

                {/* ---------- INPUT ---------- */}
                <div style={S.inputWrap}>
                    <form onSubmit={send} style={S.inputBar}>
                        <textarea
                            ref={inputRef}
                            style={S.textarea}
                            rows={1}
                            value={input}
                            onChange={e => { setInput(e.target.value); e.target.style.height = 'auto'; e.target.style.height = Math.min(e.target.scrollHeight, 160) + 'px'; }}
                            onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
                            placeholder="Message Mars AI..."
                            disabled={streaming}
                        />
                        <button type="submit" style={S.sendBtn} disabled={streaming || !input.trim()}>
                            {streaming ? Icon.stop : Icon.send}
                        </button>
                    </form>
                    <div style={S.disclaimer}>Mars AI can make mistakes. Verify scientific claims independently.</div>
                </div>
            </main>
        </div>
    );
}

/* ================================================================== */
/*  Message row                                                       */
/* ================================================================== */
function MessageRow({ msg }) {
    const [open, setOpen] = useState(false);
    const isUser = msg.role === 'user';
    const hasSources = msg.sources?.length > 0;

    return (
        <div style={{ ...S.msgRow, background: isUser ? 'transparent' : '#212121' }}>
            <div style={S.msgInner}>
                {isUser
                    ? <div style={S.avatarUser}>Y</div>
                    : <div style={S.avatarBot}>M</div>
                }
                <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={S.msgText}>{msg.text}</div>

                    {/* source evidence toggle */}
                    {hasSources && (
                        <div style={{ marginTop: 10 }}>
                            <button style={S.srcToggle} onClick={() => setOpen(v => !v)}>
                                <span style={{ transform: open ? 'rotate(180deg)' : 'rotate(0)', transition: 'transform .15s', display: 'flex' }}>{Icon.chevron}</span>
                                <span>{msg.sources.length} source{msg.sources.length > 1 ? 's' : ''} cited</span>
                            </button>
                            {open && (
                                <div style={S.srcList}>
                                    {msg.sources.map((s, j) => <SourceCard key={j} s={s} n={j + 1} />)}
                                </div>
                            )}
                        </div>
                    )}

                    {/* provenance line */}
                    {msg.jobId && (
                        <div style={S.provenance}>
                            job {msg.jobId.slice(0, 8)} &middot; prompt {msg.promptVersionId?.slice(0, 8) ?? '?'}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

/* ================================================================== */
/*  Source card                                                        */
/* ================================================================== */
function SourceCard({ s, n }) {
    return (
        <div style={S.srcCard}>
            <div style={S.srcHeader}>
                <span style={S.srcBadge}>{n}</span>
                <span style={S.srcName}>{s.sourceName || 'NASA'}</span>
                <span style={S.srcScore}>{(s.relevanceScore * 100).toFixed(0)}% match</span>
            </div>
            <div style={S.srcSnippet}>{s.snippet}</div>
            {s.url && (
                <a href={s.url} target="_blank" rel="noopener noreferrer" style={S.srcLink}>
                    {Icon.link} <span>View image</span>
                </a>
            )}
        </div>
    );
}

/* ================================================================== */
/*  Jobs panel                                                         */
/* ================================================================== */
function JobsPanel({ jobs, retryJob, fetchJobs }) {
    return (
        <div style={S.panelInner}>
            <div style={S.panelHead}>
                <h2 style={S.panelTitle}>Ingestion Jobs</h2>
                <button style={S.panelRefresh} onClick={fetchJobs}>Refresh</button>
            </div>
            {jobs.length === 0
                ? <div style={S.panelEmpty}>No jobs yet. Start an analysis from the sidebar.</div>
                : jobs.map(j => <JobRow key={j.id} job={j} retryJob={retryJob} />)
            }
        </div>
    );
}

function JobRow({ job, retryJob }) {
    const pct = job.totalItems > 0 ? Math.round((job.processedItems / job.totalItems) * 100) : 0;
    const color = { COMPLETED: '#22c55e', RUNNING: '#3b82f6', FAILED: '#ef4444', PENDING: '#a78bfa' }[job.status] || '#888';
    const isActive = job.status === 'RUNNING' || job.status === 'PENDING';

    return (
        <div style={S.jobCard}>
            <div style={S.jobTop}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    {isActive && <span style={S.pulse(color)} />}
                    <span style={{ fontWeight: 500, fontSize: 13 }}>{job.source}</span>
                </div>
                <span style={{ ...S.statusPill, background: color }}>{job.status}</span>
            </div>

            {/* progress bar */}
            <div style={S.track}>
                <div style={{ ...S.bar, width: `${pct}%`, background: color }} />
            </div>
            <div style={S.jobMeta}>
                <span>{job.processedItems}/{job.totalItems} images{job.failedItems > 0 ? ` \u00b7 ${job.failedItems} failed` : ''}</span>
                {job.retryCount > 0 && <span>retry #{job.retryCount}</span>}
            </div>

            {job.errorMessage && <div style={S.jobErr}>{job.errorMessage}</div>}

            {job.status === 'FAILED' && (
                <button style={S.retryBtn} onClick={() => retryJob(job.id)}>
                    {Icon.retry}<span>Retry</span>
                </button>
            )}

            <div style={{ fontSize: 10, color: '#555', marginTop: 6 }}>{job.id}</div>
        </div>
    );
}

/* ================================================================== */
/*  Benchmark panel                                                    */
/* ================================================================== */
function BenchmarkPanel({ baseline, reranked, running, onRun }) {
    return (
        <div style={S.panelInner}>
            <div style={S.panelHead}>
                <h2 style={S.panelTitle}>Retrieval Benchmark</h2>
            </div>
            <p style={{ fontSize: 13, color: '#999', marginBottom: 16 }}>
                Runs 20 predefined Mars queries and measures Precision@K, latency, and consistency.
            </p>
            <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                <button style={S.benchBtn} disabled={!!running} onClick={() => onRun('baseline')}>
                    {running === 'baseline' ? 'Running...' : 'Run Baseline'}
                </button>
                <button style={{ ...S.benchBtn, background: '#3b82f6' }} disabled={!!running} onClick={() => onRun('reranked')}>
                    {running === 'reranked' ? 'Running...' : 'Run Reranked'}
                </button>
            </div>

            {(baseline || reranked) && (
                <>
                    {/* summary cards */}
                    <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
                        {baseline && <MetricCard label="Baseline" data={baseline} color="#a78bfa" />}
                        {reranked && <MetricCard label="Reranked" data={reranked} color="#3b82f6" />}
                    </div>

                    {/* detail table */}
                    <div style={{ overflowX: 'auto' }}>
                        <table style={S.table}>
                            <thead>
                                <tr>
                                    <th style={S.th}>Query</th>
                                    {baseline && <><th style={S.thNum}>P@K</th><th style={S.thNum}>ms</th></>}
                                    {reranked && <><th style={S.thNum}>P@K</th><th style={S.thNum}>ms</th></>}
                                </tr>
                            </thead>
                            <tbody>
                                {(reranked ?? baseline).queryResults.map((_, i) => (
                                    <tr key={i} style={{ borderBottom: '1px solid #2a2a2a' }}>
                                        <td style={S.tdQuery}>{(reranked ?? baseline).queryResults[i].query}</td>
                                        {baseline && <>
                                            <td style={S.tdNum}>{(baseline.queryResults[i]?.precisionAtK * 100).toFixed(0)}%</td>
                                            <td style={S.tdNum}>{baseline.queryResults[i]?.latencyMs}</td>
                                        </>}
                                        {reranked && <>
                                            <td style={S.tdNum}>{(reranked.queryResults[i]?.precisionAtK * 100).toFixed(0)}%</td>
                                            <td style={S.tdNum}>{reranked.queryResults[i]?.latencyMs}</td>
                                        </>}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </>
            )}
        </div>
    );
}

function MetricCard({ label, data, color }) {
    return (
        <div style={{ flex: 1, background: '#1a1a1a', borderRadius: 10, padding: 14, borderTop: `3px solid ${color}` }}>
            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 10 }}>{label}</div>
            <div style={S.metricGrid}>
                <span style={S.metricLabel}>Precision@K</span>
                <span style={S.metricValue}>{(data.avgPrecisionAtK * 100).toFixed(1)}%</span>
                <span style={S.metricLabel}>Avg latency</span>
                <span style={S.metricValue}>{data.avgLatencyMs.toFixed(0)} ms</span>
                <span style={S.metricLabel}>Consistency</span>
                <span style={S.metricValue}>{(data.consistencyScore * 100).toFixed(1)}%</span>
                <span style={S.metricLabel}>Queries</span>
                <span style={S.metricValue}>{data.totalQueries}</span>
            </div>
        </div>
    );
}

/* ================================================================== */
/*  STYLES                                                             */
/* ================================================================== */
const S = {
    /* layout */
    shell:     { display: 'flex', height: '100vh', background: '#212121', color: '#ececf1', fontFamily: "'Inter', system-ui, sans-serif" },

    /* sidebar */
    sidebar:   { width: 260, background: '#171717', display: 'flex', flexDirection: 'column', padding: '12px 10px', gap: 2, overflowY: 'auto', borderRight: '1px solid #2a2a2a' },
    newChat:   { display: 'flex', alignItems: 'center', gap: 10, width: '100%', padding: '12px 14px', background: 'transparent', border: '1px solid #333', borderRadius: 10, color: '#ececf1', cursor: 'pointer', fontSize: 14, marginBottom: 8 },
    sideSection: { display: 'flex', flexDirection: 'column', gap: 2, marginTop: 12 },
    sideLabel: { fontSize: 11, fontWeight: 600, color: '#777', textTransform: 'uppercase', letterSpacing: '.06em', padding: '4px 14px' },
    sideBtn:   { display: 'flex', alignItems: 'center', gap: 8, width: '100%', padding: '9px 14px', background: 'transparent', border: 'none', borderRadius: 8, color: '#ccc', cursor: 'pointer', fontSize: 13, textAlign: 'left', transition: 'background .12s' },
    sideBtnActive: { background: '#2a2a2a', color: '#fff' },
    badge:     { background: '#3b82f6', color: '#fff', fontSize: 10, borderRadius: 10, padding: '1px 7px', marginLeft: 'auto', fontWeight: 600 },
    dot:       (c) => ({ display: 'inline-block', width: 7, height: 7, borderRadius: '50%', background: c, flexShrink: 0 }),
    sideFooter:{ marginTop: 'auto', padding: '12px 10px', borderTop: '1px solid #2a2a2a' },
    ragToggle: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
    toggleOn:  { width: 36, height: 20, borderRadius: 10, background: '#22c55e', border: 'none', cursor: 'pointer', position: 'relative', transition: 'background .15s' },
    toggleOff: { width: 36, height: 20, borderRadius: 10, background: '#555', border: 'none', cursor: 'pointer', position: 'relative', transition: 'background .15s' },
    toggleKnob:{ display: 'block', width: 16, height: 16, borderRadius: '50%', background: '#fff', transition: 'margin .15s', marginTop: 2 },

    /* main */
    main:      { flex: 1, display: 'flex', flexDirection: 'column', position: 'relative', minWidth: 0 },
    topbar:    { height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, borderBottom: '1px solid #2a2a2a', flexShrink: 0, fontSize: 14 },

    /* chat */
    chatScroll:{ flex: 1, overflowY: 'auto', paddingBottom: 140 },
    empty:     { display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', textAlign: 'center', padding: 24, color: '#aaa' },
    emptyIcon: { width: 64, height: 64, borderRadius: 16, background: '#2a2a2a', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#888', marginBottom: 4 },

    msgRow:    { padding: '24px 0' },
    msgInner:  { maxWidth: 768, margin: '0 auto', padding: '0 24px', display: 'flex', gap: 16 },
    avatarUser:{ width: 30, height: 30, borderRadius: '50%', background: '#fff', color: '#000', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 600, flexShrink: 0 },
    avatarBot: { width: 30, height: 30, borderRadius: '50%', background: '#10a37f', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 600, flexShrink: 0 },
    msgText:   { whiteSpace: 'pre-wrap', lineHeight: 1.65, fontSize: 15 },
    provenance:{ fontSize: 10, color: '#555', marginTop: 8 },

    /* thinking dots */
    thinkDots: { display: 'flex', gap: 4, alignItems: 'center', height: 24 },

    /* sources */
    srcToggle: { display: 'flex', alignItems: 'center', gap: 6, background: 'none', border: '1px solid #333', borderRadius: 8, padding: '5px 12px', color: '#aaa', cursor: 'pointer', fontSize: 12 },
    srcList:   { display: 'flex', flexDirection: 'column', gap: 6, marginTop: 8 },
    srcCard:   { background: '#1a1a1a', borderRadius: 10, padding: '10px 14px', border: '1px solid #2a2a2a' },
    srcHeader: { display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 },
    srcBadge:  { width: 20, height: 20, borderRadius: '50%', background: '#3b82f6', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 600, flexShrink: 0 },
    srcName:   { fontSize: 13, fontWeight: 500, color: '#ccc' },
    srcScore:  { marginLeft: 'auto', fontSize: 11, color: '#888' },
    srcSnippet:{ fontSize: 12, color: '#999', lineHeight: 1.45 },
    srcLink:   { display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: 12, color: '#60a5fa', textDecoration: 'none', marginTop: 6 },

    /* input */
    inputWrap: { position: 'absolute', bottom: 0, left: 0, right: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '12px 24px 20px', background: 'linear-gradient(transparent, #212121 20%)' },
    inputBar:  { display: 'flex', alignItems: 'flex-end', gap: 8, width: '100%', maxWidth: 768, background: '#303030', borderRadius: 16, border: '1px solid #444', padding: '8px 12px 8px 18px' },
    textarea:  { flex: 1, background: 'transparent', border: 'none', color: '#fff', fontSize: 15, outline: 'none', resize: 'none', lineHeight: 1.5, maxHeight: 160, fontFamily: 'inherit' },
    sendBtn:   { width: 34, height: 34, borderRadius: '50%', background: '#fff', color: '#000', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, transition: 'opacity .12s' },
    disclaimer:{ fontSize: 12, color: '#666', marginTop: 6, textAlign: 'center' },

    /* side panel (slide-over) */
    panelBackdrop: { position: 'absolute', inset: 0, zIndex: 20, background: 'rgba(0,0,0,.45)', display: 'flex', justifyContent: 'flex-end' },
    panelSheet:    { width: 520, maxWidth: '90%', background: '#1a1a1a', borderLeft: '1px solid #2a2a2a', overflowY: 'auto', height: '100%' },
    panelInner:    { padding: 24 },
    panelHead:     { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
    panelTitle:    { fontSize: 18, fontWeight: 600, margin: 0 },
    panelRefresh:  { background: '#2a2a2a', border: 'none', color: '#ccc', borderRadius: 8, padding: '6px 14px', cursor: 'pointer', fontSize: 12 },
    panelEmpty:    { color: '#666', textAlign: 'center', marginTop: 40 },

    /* job card */
    jobCard:   { background: '#212121', borderRadius: 10, padding: 14, marginBottom: 10, border: '1px solid #2a2a2a' },
    jobTop:    { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
    statusPill:{ fontSize: 10, fontWeight: 600, padding: '2px 10px', borderRadius: 10, color: '#fff', textTransform: 'uppercase', letterSpacing: '.04em' },
    track:     { height: 6, borderRadius: 3, background: '#333', overflow: 'hidden', marginBottom: 6 },
    bar:       { height: '100%', borderRadius: 3, transition: 'width .4s' },
    jobMeta:   { display: 'flex', justifyContent: 'space-between', fontSize: 11, color: '#888' },
    jobErr:    { fontSize: 12, color: '#f87171', marginTop: 6, padding: '6px 10px', background: '#2a1a1a', borderRadius: 6 },
    retryBtn:  { display: 'inline-flex', alignItems: 'center', gap: 6, marginTop: 8, padding: '5px 14px', background: '#333', border: 'none', borderRadius: 8, color: '#ccc', cursor: 'pointer', fontSize: 12 },
    pulse:     (c) => ({ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', background: c, boxShadow: `0 0 6px ${c}`, animation: 'pulse 1.4s infinite' }),

    /* benchmark */
    benchBtn:  { flex: 1, padding: '10px 0', borderRadius: 10, border: 'none', background: '#a78bfa', color: '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' },
    metricGrid:{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '4px 12px', fontSize: 13 },
    metricLabel:{ color: '#888' },
    metricValue:{ fontWeight: 600, textAlign: 'right' },
    table:     { width: '100%', borderCollapse: 'collapse', fontSize: 12, marginTop: 8 },
    th:        { textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #333', color: '#888', fontWeight: 500 },
    thNum:     { textAlign: 'right', padding: '8px 10px', borderBottom: '1px solid #333', color: '#888', fontWeight: 500, whiteSpace: 'nowrap' },
    tdQuery:   { padding: '7px 10px', maxWidth: 260, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: '#ccc' },
    tdNum:     { padding: '7px 10px', textAlign: 'right', fontVariantNumeric: 'tabular-nums' },
};
