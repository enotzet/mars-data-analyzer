import React, { useState } from 'react';
import { Icon } from '../icons';
import { S } from '../styles';

export default function MessageRow({ msg }) {
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

                    {hasSources && (
                        <div style={{ marginTop: 10 }}>
                            <button style={S.srcToggle} onClick={() => setOpen(v => !v)}>
                                <span style={{ transform: open ? 'rotate(180deg)' : 'rotate(0)', transition: 'transform .15s', display: 'flex' }}>
                                    {Icon.chevron}
                                </span>
                                <span>{msg.sources.length} source{msg.sources.length > 1 ? 's' : ''} cited</span>
                            </button>
                            {open && (
                                <div style={S.srcList}>
                                    {msg.sources.map((s, j) => <SourceCard key={j} s={s} n={j + 1} />)}
                                </div>
                            )}
                        </div>
                    )}

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
