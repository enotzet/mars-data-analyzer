import React, { useState, useEffect, useRef } from 'react';

function App() {
    const [ingestStatus, setIngestStatus] = useState("");
    const [question, setQuestion] = useState("");
    const [chatHistory, setChatHistory] = useState([]);
    const [loading, setLoading] = useState(false);
    const [chatLoading, setChatLoading] = useState(false);
    const messagesEndRef = useRef(null);

    // Session data
    const [sessionId] = useState(() => crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(7));
    // Random enabling RAG logic
    const [ragEnabled] = useState(() => Math.random() > 0.5);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [chatHistory, chatLoading]);

    const runIngest = async () => {
        setLoading(true);
        setIngestStatus("Downloading & analyzing...");
        try {
            const response = await fetch('http://localhost:8080/api/mars/ingest', { method: 'POST' });
            const result = await response.text();
            setIngestStatus(result);
        } catch (e) {
            setIngestStatus("Error: " + e.toString());
        } finally {
            setLoading(false);
        }
    };

    const askQuestion = async (e) => {
        e?.preventDefault();
        if (!question.trim()) return;

        const userQ = question;
        setQuestion("");
        setChatHistory(prev => [...prev, { role: 'user', content: userQ }]);
        setChatLoading(true);

        try {
            const response = await fetch('http://localhost:8080/api/mars/chat', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    question: userQ,
                    sessionId: sessionId,
                    ragEnabled: ragEnabled
                })
            });
            const data = await response.json();
            setChatHistory(prev => [...prev, { role: 'bot', content: data.answer }]);
        } catch (e) {
            setChatHistory(prev => [...prev, { role: 'bot', content: "Error: " + e.toString() }]);
        } finally {
            setChatLoading(false);
        }
    };

    // Стили под ChatGPT
    const styles = {
        app: { display: 'flex', height: '100vh', backgroundColor: '#212121', color: '#ECECF1', fontFamily: 'Inter, sans-serif' },
        sidebar: { width: '260px', backgroundColor: '#171717', padding: '10px', display: 'flex', flexDirection: 'column' },
        main: { flex: 1, display: 'flex', flexDirection: 'column', position: 'relative' },
        chatArea: { flex: 1, overflowY: 'auto', paddingBottom: '120px' },
        messageRow: { display: 'flex', justifyContent: 'center', padding: '24px 20px', borderBottom: '1px solid rgba(255,255,255,0.1)' },
        userBg: { backgroundColor: '#212121' },
        botBg: { backgroundColor: '#2f2f2f' },
        messageContent: { maxWidth: '800px', width: '100%', display: 'flex', gap: '20px', lineHeight: '1.6' },
        avatar: { width: '30px', height: '30px', borderRadius: '4px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 'bold' },
        inputContainer: { position: 'absolute', bottom: '30px', left: '50%', transform: 'translateX(-50%)', width: '800px', maxWidth: '90%' },
        inputForm: { display: 'flex', backgroundColor: '#2f2f2f', borderRadius: '15px', border: '1px solid #555', padding: '10px 15px', alignItems: 'center' },
        input: { flex: 1, backgroundColor: 'transparent', border: 'none', color: '#fff', fontSize: '16px', outline: 'none' },
        sendBtn: { backgroundColor: '#fff', color: '#000', border: 'none', borderRadius: '8px', padding: '8px 12px', cursor: 'pointer', fontWeight: 'bold' },
        sidebarBtn: { backgroundColor: '#2f2f2f', color: '#fff', border: 'none', padding: '12px', borderRadius: '8px', cursor: 'pointer', marginBottom: '10px', textAlign: 'left', display: 'flex', flexDirection: 'column', gap: '5px' },
        debugBadge: { fontSize: '10px', backgroundColor: '#444', padding: '3px 6px', borderRadius: '4px', alignSelf: 'flex-start' }
    };

    return (
        <div style={styles.app}>
            {/* Sidebar */}
            <div style={styles.sidebar}>
                <button onClick={() => setChatHistory([])} style={styles.sidebarBtn}>
                    + New Chat
                </button>
                <div style={{marginTop: 'auto'}}>
                    {/* A/B Test Debug Info */}
                    <div style={{...styles.sidebarBtn, cursor: 'default', backgroundColor: '#1a1a1a', border: '1px solid #333'}}>
                        <span style={{fontSize: '12px', color: '#888'}}>Session A/B Test:</span>
                        <span style={{fontSize: '14px', color: ragEnabled ? '#4ade80' : '#f87171'}}>
                            RAG is {ragEnabled ? 'ON' : 'OFF'}
                        </span>
                    </div>

                    <button onClick={runIngest} style={styles.sidebarBtn} disabled={loading}>
                        {loading ? '⚙️ Processing...' : '🚀 Ingest NASA Data'}
                    </button>
                    {ingestStatus && <div style={{color: '#aaa', fontSize: '12px', padding: '5px'}}>{ingestStatus}</div>}
                </div>
            </div>

            {/* Chat Area */}
            <div style={styles.main}>
                <div style={styles.chatArea}>
                    {chatHistory.length === 0 ? (
                        <div style={{textAlign: 'center', marginTop: '20vh', color: '#666'}}>
                            <h2>Mars AI Assistant</h2>
                            <p>Ask anything about the Martian surface.</p>
                        </div>
                    ) : (
                        chatHistory.map((msg, i) => (
                            <div key={i} style={{...styles.messageRow, ...(msg.role === 'user' ? styles.userBg : styles.botBg)}}>
                                <div style={styles.messageContent}>
                                    <div style={{...styles.avatar, backgroundColor: msg.role === 'user' ? '#fff' : '#10a37f', color: msg.role === 'user' ? '#000' : '#fff'}}>
                                        {msg.role === 'user' ? 'U' : 'AI'}
                                    </div>
                                    <div style={{flex: 1, whiteSpace: 'pre-wrap'}}>{msg.content}</div>
                                </div>
                            </div>
                        ))
                    )}
                    {chatLoading && (
                        <div style={{...styles.messageRow, ...styles.botBg}}>
                            <div style={styles.messageContent}>
                                <div style={{...styles.avatar, backgroundColor: '#10a37f', color: '#fff'}}>AI</div>
                                <div style={{flex: 1}}>Thinking...</div>
                            </div>
                        </div>
                    )}
                    <div ref={messagesEndRef} />
                </div>

                <div style={styles.inputContainer}>
                    <form onSubmit={askQuestion} style={styles.inputForm}>
                        <input
                            style={styles.input}
                            value={question}
                            onChange={e => setQuestion(e.target.value)}
                            placeholder="Message Mars AI..."
                            disabled={chatLoading}
                        />
                        <button type="submit" style={styles.sendBtn} disabled={chatLoading || !question.trim()}>
                            ↑
                        </button>
                    </form>
                    <div style={{textAlign: 'center', color: '#666', fontSize: '12px', marginTop: '10px'}}>
                        AI can make mistakes. Verify important information.
                    </div>
                </div>
            </div>
        </div>
    );
}

export default App;