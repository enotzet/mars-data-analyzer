import React, { useState } from 'react';
import { S } from '../styles';
import { runBenchmark } from '../api';

const MODE_LABELS = {
    'zero-shot': { label: 'Zero-shot', color: '#9ca3af', desc: 'Direct LLM, no system prompt, no retrieval' },
    'system-prompt': { label: 'System Prompt', color: '#a78bfa', desc: 'Mars-expert system prompt, no retrieval' },
    'rag-baseline': { label: 'Standard RAG', color: '#fb923c', desc: 'Pure dense retrieval + RAG prompt' },
    'rag-reranked': { label: 'RAG + Rerank', color: '#3b82f6', desc: 'Hybrid retrieval (rerank) + RAG prompt' },
};

export default function BenchmarkPanel() {
    const [result, setResult] = useState(null);
    const [running, setRunning] = useState(false);
    const [repeats, setRepeats] = useState(5);

    const run = async () => {
        setRunning(true);
        try {
            const d = await runBenchmark({ repeats });
            setResult(d);
        } catch (_) { /* silent */ }
        setRunning(false);
    };

    return (
        <div style={S.panelInner}>
            <div style={S.panelHead}>
                <h2 style={S.panelTitle}>Retrieval Benchmark</h2>
            </div>
            <p style={{ fontSize: 13, color: '#999', marginBottom: 16 }}>
                Runs 20 queries × N repeats across four modes (Zero-shot → System Prompt → Standard RAG → RAG + Rerank).
                Each generated answer is scored 0–5 by an LLM-as-a-Judge against a manually curated ideal answer.
            </p>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 20 }}>
                <label style={{ fontSize: 13, color: '#aaa' }}>Repeats per query:</label>
                <input
                    type="number"
                    min={1}
                    max={10}
                    value={repeats}
                    disabled={running}
                    onChange={e => setRepeats(parseInt(e.target.value || '5', 10))}
                    style={{ width: 60, padding: 4, background: '#1a1a1a', color: '#fff', border: '1px solid #333', borderRadius: 4 }}
                />
                <button style={S.benchBtn} disabled={running} onClick={run}>
                    {running ? 'Running... (this takes several minutes)' : 'Run Full Benchmark'}
                </button>
            </div>

            {result && (
                <>
                    <div style={{ display: 'flex', gap: 12, marginBottom: 20, flexWrap: 'wrap' }}>
                        {result.modes.map(m => (
                            <MetricCard key={m.mode} mode={m} />
                        ))}
                    </div>

                    <div style={{ overflowX: 'auto' }}>
                        <table style={S.table}>
                            <thead>
                                <tr>
                                    <th style={S.th}>Query</th>
                                    {result.modes.map(m => (
                                        <th key={m.mode} style={S.thNum} colSpan={2}>
                                            {MODE_LABELS[m.mode]?.label || m.mode}
                                        </th>
                                    ))}
                                </tr>
                                <tr>
                                    <th style={S.th}></th>
                                    {result.modes.map(m => (
                                        <React.Fragment key={m.mode + '-h'}>
                                            <th style={S.thNum}>Judge</th>
                                            <th style={S.thNum}>ms</th>
                                        </React.Fragment>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {result.modes[0].queryResults.map((_, i) => (
                                    <tr key={i} style={{ borderBottom: '1px solid #2a2a2a' }}>
                                        <td style={S.tdQuery}>{result.modes[0].queryResults[i].query}</td>
                                        {result.modes.map(m => (
                                            <React.Fragment key={m.mode + '-row-' + i}>
                                                <td style={S.tdNum}>
                                                    {m.queryResults[i]?.meanJudgeScore.toFixed(2)}
                                                    <span style={{ color: '#666', fontSize: 10 }}>±{m.queryResults[i]?.stdJudgeScore.toFixed(2)}</span>
                                                </td>
                                                <td style={S.tdNum}>{Math.round(m.queryResults[i]?.meanLatencyMs)}</td>
                                            </React.Fragment>
                                        ))}
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

function MetricCard({ mode }) {
    const meta = MODE_LABELS[mode.mode] || { label: mode.mode, color: '#666', desc: '' };
    return (
        <div style={{ flex: '1 1 220px', minWidth: 220, background: '#1a1a1a', borderRadius: 10, padding: 14, borderTop: `3px solid ${meta.color}` }}>
            <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 4 }}>{meta.label}</div>
            <div style={{ fontSize: 11, color: '#888', marginBottom: 10 }}>{meta.desc}</div>
            <div style={S.metricGrid}>
                <span style={S.metricLabel}>LLM-Judge (0–5)</span>
                <span style={S.metricValue}>
                    {mode.avgJudgeScore.toFixed(2)}
                    <span style={{ color: '#888', fontSize: 11 }}> ± {mode.stdJudgeScore.toFixed(2)}</span>
                </span>
                <span style={S.metricLabel}>Precision@K</span>
                <span style={S.metricValue}>
                    {Number.isNaN(mode.avgPrecisionAtK) ? 'n/a' : (mode.avgPrecisionAtK * 100).toFixed(1) + '%'}
                </span>
                <span style={S.metricLabel}>Avg latency</span>
                <span style={S.metricValue}>{mode.avgLatencyMs.toFixed(0)} ms</span>
                <span style={S.metricLabel}>Consistency</span>
                <span style={S.metricValue}>{(mode.consistencyScore * 100).toFixed(1)}%</span>
            </div>
        </div>
    );
}
