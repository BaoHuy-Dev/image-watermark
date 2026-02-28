import { useState, useRef } from 'react';

export default function App() {
    const [tab, setTab] = useState('embed');

    return (
        <div className="mobile-app">
            <header className="mobile-header">
                <h1>🔒 Watermark</h1>
                <p className="subtitle">Invisible watermark tool</p>
            </header>

            {tab === 'embed' ? <EmbedView /> : <ExtractView />}

            {/* Bottom Navigation */}
            <nav className="bottom-nav">
                <button className={`nav-btn ${tab === 'embed' ? 'active' : ''}`} onClick={() => setTab('embed')}>
                    <span className="nav-icon">🖼️</span>
                    Nhúng
                </button>
                <button className={`nav-btn ${tab === 'extract' ? 'active' : ''}`} onClick={() => setTab('extract')}>
                    <span className="nav-icon">🔍</span>
                    Trích xuất
                </button>
            </nav>
        </div>
    );
}

// ===================== EMBED VIEW =====================
function EmbedView() {
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null);
    const [userId, setUserId] = useState('');
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const inputRef = useRef();

    const handleFile = (f) => {
        if (!f) return;
        setFile(f);
        setResult(null);
        const reader = new FileReader();
        reader.onload = (e) => setPreview(e.target.result);
        reader.readAsDataURL(f);
    };

    const handleSubmit = async () => {
        if (!file || !userId) return;
        setLoading(true);
        try {
            const fd = new FormData();
            fd.append('file', file);
            fd.append('userId', userId);
            fd.append('userEmail', email);
            const res = await fetch('/api/watermark/embed', { method: 'POST', body: fd });
            if (!res.ok) throw new Error('Lỗi ' + res.status);
            const blob = await res.blob();
            setResult({ url: URL.createObjectURL(blob), size: blob.size });
        } catch (err) {
            alert(err.message);
        } finally {
            setLoading(false);
        }
    };

    const download = () => {
        const a = document.createElement('a');
        a.href = result.url;
        a.download = 'watermarked.png';
        a.click();
    };

    return (
        <div className="m-card">
            <h2>🖼️ Nhúng Watermark</h2>

            <div className="m-upload" onClick={() => inputRef.current?.click()}>
                <div className="u-icon">📁</div>
                <div className="u-text">Chạm để chọn ảnh</div>
                <div className="u-hint">PNG / BMP</div>
                <input ref={inputRef} type="file" accept="image/png,image/bmp"
                    onChange={(e) => handleFile(e.target.files[0])} />
            </div>

            {file && <div className="m-file-info">✅ {file.name} ({(file.size / 1024).toFixed(0)} KB)</div>}

            <div className="m-field" style={{ marginTop: '1rem' }}>
                <label>User ID *</label>
                <input type="text" placeholder="vd: user-12345" value={userId}
                    onChange={(e) => setUserId(e.target.value)} />
            </div>
            <div className="m-field">
                <label>Email</label>
                <input type="text" placeholder="vd: user@mail.com" value={email}
                    onChange={(e) => setEmail(e.target.value)} />
            </div>

            <button className="m-btn" onClick={handleSubmit} disabled={!file || !userId || loading}>
                {loading ? <><div className="m-spin" /> Xử lý...</> : '🔒 Nhúng Watermark'}
            </button>

            {result && (
                <div className="m-preview">
                    <h3>✨ Kết quả</h3>
                    <div className="m-img-stack">
                        <div className="m-img-box">
                            <span className="m-img-tag">GỐC</span>
                            {preview && <img src={preview} alt="Gốc" />}
                        </div>
                        <div className="m-img-box">
                            <span className="m-img-tag">ĐÃ NHÚNG WM</span>
                            <img src={result.url} alt="Watermarked" />
                        </div>
                    </div>
                    <button className="m-download" onClick={download}>
                        ⬇️ Tải ảnh ({(result.size / 1024).toFixed(0)} KB)
                    </button>
                </div>
            )}
        </div>
    );
}

// ===================== EXTRACT VIEW =====================
function ExtractView() {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const inputRef = useRef();

    const handleFile = (f) => {
        if (!f) return;
        setFile(f);
        setResult(null);
    };

    const handleExtract = async () => {
        if (!file) return;
        setLoading(true);
        try {
            const fd = new FormData();
            fd.append('file', file);
            const res = await fetch('/api/watermark/extract', { method: 'POST', body: fd });
            setResult(await res.json());
        } catch (err) {
            alert(err.message);
        } finally {
            setLoading(false);
        }
    };

    const formatJson = (t) => {
        try { return JSON.stringify(JSON.parse(t), null, 2); } catch { return t; }
    };

    return (
        <div className="m-card">
            <h2>🔍 Trích Xuất</h2>

            <div className="m-upload" onClick={() => inputRef.current?.click()}>
                <div className="u-icon">🔎</div>
                <div className="u-text">Chạm để chọn ảnh kiểm tra</div>
                <div className="u-hint">PNG / BMP</div>
                <input ref={inputRef} type="file" accept="image/png,image/bmp"
                    onChange={(e) => handleFile(e.target.files[0])} />
            </div>

            {file && <div className="m-file-info">✅ {file.name} ({(file.size / 1024).toFixed(0)} KB)</div>}

            <button className="m-btn" onClick={handleExtract} disabled={!file || loading}>
                {loading ? <><div className="m-spin" /> Phân tích...</> : '🔍 Trích Xuất'}
            </button>

            {result && (
                result.found ? (
                    <div className="m-result-found">
                        <div className="rlabel">✅ Tìm thấy watermark</div>
                        <pre>{formatJson(result.watermark)}</pre>
                    </div>
                ) : (
                    <div className="m-result-empty">⚠️ Không tìm thấy watermark</div>
                )
            )}
        </div>
    );
}
