import React, { useState } from 'react';
import { S } from '../styles';
import { runBenchmark } from '../api';

export default function BenchmarkPanel({ sessionId }) {
    const [baseline, setBaseline] = useState(null);
    const [reranked, setReranked] = useState(null);
    const [running, setRunning] = useState(null); // null | 'baseline' | 'reranked'

    const run = async (mode) => {
        setRunning(mode);
        try {
            const d = await runBenchmark({ mode, sessionId });
            if (mode === 'baseline') setBaseline(d); else setReranked(d);
        } catch (_) { /* silent */ }
        setRunning(null);
    };

    return (
        <div style={S.panelInner}>
            <div style={S.panelHead}>
                <h2 style={S.panelTitle}>Retrieval Benchmark</h2>
            </div>
            <p style={{ fontSize: 13, color: '#999', marginBottom: 16 }}>
                Runs 20 predefined Mars queries and measures Precision@K, latency, and consistency.
            </p>
            <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                <button style={S.benchBtn} disabled={!!running} onClick={() => run('baseline')}>
                    {running === 'baseline' ? 'Running...' : 'Run Baseline'}
                </button>
                <button style={{ ...S.benchBtn, background: '#3b82f6' }} disabled={!!running} onClick={() => run('reranked')}>
                    {running === 'reranked' ? 'Running...' : 'Run Reranked'}
                </button>
            </div>

            {(baseline || reranked) && (
                <>
                    <div style={{ display: 'flex', gap: 12, marginBottom: 20 }}>
                        {baseline && <MetricCard label="Baseline" data={baseline} color="#a78bfa" />}
                        {reranked && <MetricCard label="Reranked" data={reranked} color="#3b82f6" />}
                    </div>

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
