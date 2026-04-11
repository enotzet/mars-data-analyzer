import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

const style = document.createElement('style');
style.textContent = `
  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50%      { opacity: .3; }
  }
  @keyframes dot-bounce {
    0%, 80%, 100% { transform: translateY(0); }
    40%           { transform: translateY(-5px); }
  }
  .mars-dots span {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #888;
    animation: dot-bounce .9s ease-in-out infinite;
  }
  .mars-dots span:nth-child(2) { animation-delay: .15s; }
  .mars-dots span:nth-child(3) { animation-delay: .3s; }
  aside button:hover:not(:disabled) { background: #2a2a2a !important; }
  button:disabled { opacity: .4; cursor: default !important; }
`;
document.head.appendChild(style);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
