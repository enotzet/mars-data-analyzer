import React, { useState } from 'react';

function App() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const runAnalysis = async () => {
        setLoading(true);
        setError(null);
        setData(null);

        try {

            const response = await fetch('http://localhost:8080/api/mars/analyze');

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            setData(result);
        } catch (e) {
            setError(e.toString());
        } finally {
            setLoading(false);
        }
    };

    const styles = {
        container: { maxWidth: '800px', margin: '2rem auto', padding: '1rem', fontFamily: 'sans-serif' },
        box: { background: '#f4f4f4', padding: '15px', borderRadius: '5px', marginTop: '10px', whiteSpace: 'pre-wrap' },
        gptBox: { background: '#e3f2fd', padding: '15px', borderRadius: '5px', marginTop: '10px', whiteSpace: 'pre-wrap' },
        button: { padding: '10px 20px', fontSize: '1.2rem', cursor: 'pointer', background: '#007bff', color: 'white', border: 'none', borderRadius: '5px' }
    };

    return (
        <div style={styles.container}>
            <h1>🚀 Mars Data Analyzer (React)</h1>
            <p>Receive data analyze from GPT.</p>

            <button onClick={runAnalysis} style={styles.button} disabled={loading}>
                {loading ? 'Loading...' : 'Analyze'}
            </button>

            {error && <div style={{color: 'red', marginTop: '1rem'}}>Error: {error}</div>}

            {data && (
                <div>
                    <h3>🤖 GPT Analysis:</h3>
                    <div style={styles.gptBox}>{data.gptAnalysis}</div>

                    <h3>🛰️ Raw NASA Data (Snippet):</h3>
                    <div style={styles.box}>{data.nasaRawData.substring(0, 500)}...</div>
                </div>
            )}
        </div>
    );
}

export default App;