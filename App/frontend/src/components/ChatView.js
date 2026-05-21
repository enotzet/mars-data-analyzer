import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Icon } from '../icons';
import { S } from '../styles';
import { sendChat } from '../api';
import MessageRow from './MessageRow';

export default function ChatView({ sessionId, ragEnabled, initialMessages = [], onMessageSent }) {
    const [messages, setMessages] = useState(initialMessages);
    const [input, setInput] = useState('');
    const [streaming, setStreaming] = useState(false);
    const endRef = useRef(null);
    const inputRef = useRef(null);

    useEffect(() => {
        endRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, streaming]);

    const send = useCallback(async (e) => {
        e?.preventDefault();
        const q = input.trim();
        if (!q || streaming) return;
        setInput('');
        setMessages(prev => [...prev, { role: 'user', text: q }]);
        setStreaming(true);
        try {
            const d = await sendChat({ question: q, sessionId, ragEnabled, userId: 'default' });
            setMessages(prev => [...prev, {
                role: 'assistant',
                text: d.answer,
                sources: d.sources ?? [],
                jobId: d.jobId,
                promptVersionId: d.promptVersionId,
            }]);
            onMessageSent?.();
        } catch (err) {
            setMessages(prev => [...prev, { role: 'assistant', text: 'Something went wrong: ' + err }]);
        } finally {
            setStreaming(false);
            setTimeout(() => inputRef.current?.focus(), 50);
        }
    }, [input, streaming, sessionId, ragEnabled, onMessageSent]);

    return (
        <>
            <div style={S.chatScroll}>
                {messages.length === 0 && <EmptyState />}

                {messages.map((m, i) => <MessageRow key={i} msg={m} />)}

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

            <div style={S.inputWrap}>
                <form onSubmit={send} style={S.inputBar}>
                    <textarea
                        ref={inputRef}
                        style={S.textarea}
                        rows={1}
                        value={input}
                        onChange={e => {
                            setInput(e.target.value);
                            e.target.style.height = 'auto';
                            e.target.style.height = Math.min(e.target.scrollHeight, 160) + 'px';
                        }}
                        onKeyDown={e => {
                            if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault();
                                send();
                            }
                        }}
                        placeholder="Message Mars AI..."
                        disabled={streaming}
                        autoFocus
                    />
                    <button type="submit" style={S.sendBtn} disabled={streaming || !input.trim()}>
                        {streaming ? Icon.stop : Icon.send}
                    </button>
                </form>
                <div style={S.disclaimer}>Mars AI can make mistakes. Verify scientific claims independently.</div>
            </div>
        </>
    );
}

function EmptyState() {
    return (
        <div style={S.empty}>
            <div style={S.emptyIcon}>{Icon.rocketLarge}</div>
            <h2 style={{ fontSize: 22, fontWeight: 600, margin: '16px 0 8px' }}>Mars Data Analyzer</h2>
            <p style={{ color: '#888', maxWidth: 420, lineHeight: 1.5, fontSize: 14 }}>
                Ask questions about the Martian surface, geology, climate, and rover observations.
                Enable RAG to ground answers in analyzed NASA imagery.
            </p>
        </div>
    );
}
