export const API = 'http://localhost:8080/api';

export async function fetchJobs() {
    const r = await fetch(`${API}/mars/jobs`);
    if (!r.ok) throw new Error('Failed to fetch jobs');
    return r.json();
}

export async function sendChat({ question, sessionId, ragEnabled, userId }) {
    const r = await fetch(`${API}/mars/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question, sessionId, ragEnabled, userId }),
    });
    return r.json();
}

export async function submitIngest(source) {
    return fetch(`${API}/mars/ingest`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ source }),
    });
}

export async function retryIngest(jobId) {
    return fetch(`${API}/mars/ingest/${jobId}/retry`, { method: 'POST' });
}

export async function runBenchmark({ mode, sessionId }) {
    const r = await fetch(`${API}/benchmark/run?mode=${mode}&sessionId=${sessionId}`, { method: 'POST' });
    return r.json();
}

export async function listChats(userId = 'default') {
    const r = await fetch(`${API}/mars/chats?userId=${encodeURIComponent(userId)}`);
    if (!r.ok) throw new Error('Failed to list chats');
    return r.json();
}

export async function getChatMessages(sessionId) {
    const r = await fetch(`${API}/mars/chats/${sessionId}`);
    if (!r.ok) throw new Error('Failed to fetch chat messages');
    return r.json();
}

export async function saveChat(sessionId, userId = 'default') {
    const r = await fetch(`${API}/mars/chats/${sessionId}/save?userId=${encodeURIComponent(userId)}`, { method: 'POST' });
    if (!r.ok) throw new Error('Failed to save chat');
}

export async function unsaveChat(sessionId, userId = 'default') {
    const r = await fetch(`${API}/mars/chats/${sessionId}/save?userId=${encodeURIComponent(userId)}`, { method: 'DELETE' });
    if (!r.ok) throw new Error('Failed to unsave chat');
}
