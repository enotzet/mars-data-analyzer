import React from 'react';
import { Icon } from '../icons';
import { S } from '../styles';

export default function JobsPanel({ jobs, retryJob, fetchJobs }) {
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

            <div style={S.track}>
                <div style={{ ...S.bar, width: `${pct}%`, background: color }} />
            </div>
            <div style={S.jobMeta}>
                <span>
                    {job.processedItems}/{job.totalItems} images
                    {job.failedItems > 0 ? ` \u00b7 ${job.failedItems} failed` : ''}
                </span>
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
