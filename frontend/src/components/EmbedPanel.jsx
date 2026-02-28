import { useState, useRef } from 'react';

export default function EmbedPanel() {
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null);
    const [userId, setUserId] = useState('');
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [dragOver, setDragOver] = useState(false);
    const inputRef = useRef();

    const handleFile = (f) => {
        if (!f) return;
        setFile(f);
        setResult(null);
        const reader = new FileReader();
        reader.onload = (e) => setPreview(e.target.result);
        reader.readAsDataURL(f);
    };

    const handleDrop = (e) => {
        e.preventDefault();
        setDragOver(false);
        handleFile(e.dataTransfer.files[0]);
    };

    const handleSubmit = async () => {
        if (!file || !userId) return;
        setLoading(true);
        try {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('userId', userId);
            formData.append('userEmail', email);

            const res = await fetch('/api/watermark/embed', {
                method: 'POST',
                body: formData,
            });

            if (!res.ok) throw new Error('Embed failed: ' + res.status);

            const blob = await res.blob();
            const url = URL.createObjectURL(blob);
            setResult({ url, size: blob.size });
        } catch (err) {
            alert('Lỗi: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const downloadResult = () => {
        if (!result) return;
        const a = document.createElement('a');
        a.href = result.url;
        a.download = 'watermarked.png';
        a.click();
    };

    return (
        <div className="card">
            <h2><span className="icon">🖼️</span> Nhúng Watermark Ẩn</h2>

            {/* Dropzone */}
            <div
                className={`dropzone ${dragOver ? 'drag-over' : ''}`}
                onClick={() => inputRef.current?.click()}
                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={handleDrop}
            >
                <div className="drop-icon">📁</div>
                <div className="drop-text">Kéo thả ảnh vào đây hoặc click để chọn</div>
                <div className="drop-hint">Hỗ trợ: PNG, BMP (lossless)</div>
                <input
                    ref={inputRef}
                    type="file"
                    accept="image/png,image/bmp"
                    onChange={(e) => handleFile(e.target.files[0])}
                />
            </div>

            {file && (
                <div className="file-selected">
                    ✅ {file.name} ({(file.size / 1024).toFixed(1)} KB)
                </div>
            )}

            {/* Form fields */}
            <div className="form-group" style={{ marginTop: '1.5rem' }}>
                <label>User ID *</label>
                <input
                    type="text"
                    placeholder="vd: user-12345"
                    value={userId}
                    onChange={(e) => setUserId(e.target.value)}
                />
            </div>
            <div className="form-group">
                <label>Email</label>
                <input
                    type="text"
                    placeholder="vd: user@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                />
            </div>

            {/* Submit */}
            <button
                className={`btn-primary ${loading ? 'loading' : ''}`}
                onClick={handleSubmit}
                disabled={!file || !userId || loading}
            >
                {loading ? <><div className="spinner" /> Đang xử lý...</> : '🔒 Nhúng Watermark'}
            </button>

            {/* Result */}
            {result && (
                <div className="result-panel">
                    <h3>✨ Kết quả</h3>
                    <div className="result-images">
                        <div className="img-box">
                            <span className="img-label">Ảnh gốc</span>
                            {preview && <img src={preview} alt="Original" />}
                        </div>
                        <div className="img-box">
                            <span className="img-label">Đã nhúng WM</span>
                            <img src={result.url} alt="Watermarked" />
                        </div>
                    </div>
                    <button className="download-btn" onClick={downloadResult}>
                        ⬇️ Tải ảnh ({(result.size / 1024).toFixed(1)} KB)
                    </button>
                </div>
            )}
        </div>
    );
}
