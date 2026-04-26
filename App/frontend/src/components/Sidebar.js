import React from 'react';
import { Icon } from '../icons';
import { S } from '../styles';

export default function Sidebar({
    onNewChat,
    onIngest,
    jobsBusy,
    panel,
    setPanel,
    activeJobsCount,
    ragEnabled,
    setRagEnabled,
    sessionId,
    chats,
    currentSessionId,
    onSelectChat,
    onToggleSave,
    onShareChat,
}) {
    return (
        <aside style={S.sidebar}>
            <button style={S.newChat} onClick={onNewChat}>
                {Icon.plus}<span>New chat</span>
            </button>

            <div style={S.sideSection}>
                <div style={S.sideLabel}>Data Ingestion</div>
                <button style={S.sideBtn} disabled={jobsBusy} onClick={() => onIngest('all')}>
                    {Icon.rocket}<span>Analyze All Sources</span>
                </button>
                <button style={S.sideBtn} disabled={jobsBusy} onClick={() => onIngest('nasa-image-library')}>
                    <span style={S.dot('#a78bfa')} />NASA Image Library
                </button>
                <button style={S.sideBtn} disabled={jobsBusy} onClick={() => onIngest('mars-rover-photos')}>
                    <span style={S.dot('#fb923c')} />Mars Rover Photos
                </button>
                <button style={S.sideBtn} disabled={jobsBusy} onClick={() => onIngest('insight-weather')}>
                    <span style={S.dot('#22d3ee')} />InSight Weather
                </button>
            </div>

            <div style={S.sideSection}>
                <button
                    style={{ ...S.sideBtn, ...(panel === 'jobs' ? S.sideBtnActive : {}) }}
                    onClick={() => setPanel(panel === 'jobs' ? null : 'jobs')}
                >
                    <span>Job History</span>
                    {activeJobsCount > 0 && <span style={S.badge}>{activeJobsCount}</span>}
                </button>
                <button
                    style={{ ...S.sideBtn, ...(panel === 'benchmark' ? S.sideBtnActive : {}) }}
                    onClick={() => setPanel(panel === 'benchmark' ? null : 'benchmark')}
                >
                    {Icon.chart}<span>Benchmark</span>
                </button>
            </div>

            <div style={S.sideSection}>
                <div style={S.sideLabel}>Chats</div>
                {chats.length === 0
                    ? <div style={S.chatEmpty}>No previous chats</div>
                    : chats.map(c => (
                        <ChatRow
                            key={c.sessionId}
                            chat={c}
                            active={c.sessionId === currentSessionId}
                            onSelect={onSelectChat}
                            onToggleSave={onToggleSave}
                            onShare={onShareChat}
                        />
                    ))
                }
            </div>

            <div style={S.sideFooter}>
                <div style={S.ragToggle}>
                    <span style={{ fontSize: 13 }}>RAG retrieval</span>
                    <button
                        style={ragEnabled ? S.toggleOn : S.toggleOff}
                        onClick={() => setRagEnabled(v => !v)}
                    >
                        <span style={{ ...S.toggleKnob, marginLeft: ragEnabled ? 16 : 2 }} />
                    </button>
                </div>
                <div style={{ fontSize: 11, color: '#555', marginTop: 6 }}>
                    session {sessionId.slice(0, 8)}
                </div>
            </div>
        </aside>
    );
}

function ChatRow({ chat, active, onSelect, onToggleSave, onShare }) {
    const rowStyle = { ...S.chatRow, ...(active ? S.chatRowActive : {}) };
    const starStyle = { ...S.chatIcon, ...(chat.saved ? S.chatIconActive : {}) };

    return (
        <div style={rowStyle}>
            <button
                onClick={() => onSelect(chat.sessionId)}
                title={chat.title}
                style={{ ...S.chatTitle, background: 'transparent', border: 'none', color: 'inherit', cursor: 'pointer', padding: 0 }}
            >
                {chat.title || '(empty)'}
            </button>
            <button
                onClick={() => onToggleSave(chat.sessionId, chat.saved)}
                style={starStyle}
                title={chat.saved ? 'Unsave' : 'Save'}
                aria-label={chat.saved ? 'Unsave' : 'Save'}
            >
                {chat.saved ? Icon.starFilled : Icon.starOutline}
            </button>
            <button
                onClick={() => onShare(chat.sessionId)}
                style={S.chatIcon}
                title="Copy share link"
                aria-label="Share"
            >
                {Icon.share}
            </button>
        </div>
    );
}
