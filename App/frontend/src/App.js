import React, { useState } from 'react';

function App() {
    const [ingestStatus, setIngestStatus] = useState("");
    const [question, setQuestion] = useState("");
    const [chatHistory, setChatHistory] = useState([]);
    const [loading, setLoading] = useState(false);

    const runIngest = async () => {
        setLoading(true);
        setIngestStatus("Analyzing images and saving to Vector DB...");
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

    const askQuestion = async () => {
        if (!question) return;
        const userQ = question;
        setQuestion("");
        setChatHistory(prev => [...prev, { role: 'user', content: userQ }]);

        try {
            const response = await fetch('http://localhost:8080/api/mars/chat', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({ question: userQ })
            });
            const data = await response.json();
            setChatHistory(prev => [...prev, { role: 'bot', content: data.answer }]);
        } catch (e) {
            setChatHistory(prev => [...prev, { role: 'bot', content: "Error: " + e.toString() }]);
        }
    };

    const styles = {
        container: { maxWidth: '800px', margin: '2rem auto', padding: '1rem', fontFamily: 'sans-serif' },
        box: { background: '#f4f4f4', padding: '15px', borderRadius: '5px', marginBottom: '10px' },
        userMsg: { textAlign: 'right', color: 'blue', marginBottom: '5px' },
        botMsg: { textAlign: 'left', color: 'black', background: '#e3f2fd', padding: '10px', borderRadius: '10px', marginBottom: '10px' },
        inputGroup: { display: 'flex', gap: '10px', marginTop: '20px' },
        input: { flex: 1, padding: '10px' },
        button: { padding: '10px 20px', cursor: 'pointer', background: '#007bff', color: 'white', border: 'none', borderRadius: '5px' }
    };

    return (
        <div style={styles.container}>
            <h1>🚀 Mars AI Assistant (RAG)</h1>

            <div style={styles.box}>
                <h3>1. Prepare Data</h3>
                <p>Download NASA images, analyze via GPT-4 Vision, and save to Vector DB.</p>
                <button onClick={runIngest} style={styles.button} disabled={loading}>
                    {loading ? 'Processing...' : 'Ingest & Analyze Images'}
                </button>
                {ingestStatus && <p>{ingestStatus}</p>}
            </div>

            <div style={styles.box}>
                <h3>2. Chat with Data</h3>
                <div style={{maxHeight: '400px', overflowY: 'auto'}}>
                    {chatHistory.map((msg, i) => (
                        <div key={i} style={msg.role === 'user' ? styles.userMsg : styles.botMsg}>
                            <strong>{msg.role === 'user' ? 'You' : 'AI'}:</strong> {msg.content}
                        </div>
                    ))}
                </div>
                <div style={styles.inputGroup}>
                    <input
                        style={styles.input}
                        value={question}
                        onChange={e => setQuestion(e.target.value)}
                        placeholder="Ask about the images (e.g. 'Did you find any water?')"
                    />
                    <button onClick={askQuestion} style={styles.button}>Send</button>
                </div>
            </div>
        </div>
    );
}

export default App;