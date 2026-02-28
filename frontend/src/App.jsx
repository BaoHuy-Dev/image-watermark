import { useState } from 'react';
import EmbedPanel from './components/EmbedPanel';
import ExtractPanel from './components/ExtractPanel';

export default function App() {
    const [tab, setTab] = useState('embed');

    return (
        <div className="app">
            <header className="app-header">
                <h1>🔒 Watermark Tool</h1>
                <p>Nhúng &amp; trích xuất watermark ẩn trong ảnh</p>
                <span className="badge">LSB Steganography</span>
            </header>

            <div className="tabs">
                <button
                    className={`tab-btn ${tab === 'embed' ? 'active' : ''}`}
                    onClick={() => setTab('embed')}
                >
                    🖼️ Nhúng Watermark
                </button>
                <button
                    className={`tab-btn ${tab === 'extract' ? 'active' : ''}`}
                    onClick={() => setTab('extract')}
                >
                    🔍 Trích Xuất
                </button>
            </div>

            {tab === 'embed' ? <EmbedPanel /> : <ExtractPanel />}
        </div>
    );
}
