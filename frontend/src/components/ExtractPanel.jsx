import { useState, useRef } from 'react';

export default function ExtractPanel() {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [dragOver, setDragOver] = useState(false);
    const inputRef = useRef();

    const handleFile = (f) => {
        if (!f) return;
        setFile(f);
        setResult(null);
    };

    const handleDrop = (e) => {
        e.preventDefault();
        setDragOver(false);
        handleFile(e.dataTransfer.files[0]);
    };

    const handleExtract = async () => {
        if (!file) return;
        setLoading(true);
        try {
            const formData = new FormData();
            formData.append('file', file);

            const res = await fetch('/api/watermark/extract', {
                method: 'POST',
                body: formData,
            });

            const data = await res.json();
            setResult(data);
        } catch (err) {
            alert('Lỗi: ' + err.message);
        } finally {
            setLoading(false);
        }
    };

    const formatJson = (text) => {
        try {
            return JSON.stringify(JSON.parse(text), null, 2);
        } catch {
            return text;
        }
    };

    return (
        <div className="card">
            <h2><span className="icon">🔍</span> Trích Xuất Watermark</h2>

            {/* Dropzone */}
            <div
                className={`dropzone ${dragOver ? 'drag-over' : ''}`}
                onClick={() => inputRef.current?.click()}
                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={handleDrop}
            >
                <div className="drop-icon">🔎</div>
                <div className="drop-text">Upload ảnh cần kiểm tra watermark</div>
                <div className="drop-hint">Kéo thả hoặc click để chọn file</div>
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

            <button
                className={`btn-primary ${loading ? 'loading' : ''}`}
                onClick={handleExtract}
                disabled={!file || loading}
            >
                {loading ? <><div className="spinner" /> Đang phân tích...</> : '🔍 Trích Xuất Watermark'}
            </button>

            {/* Result */}
            {result && (
                <div className="extract-result">
                    {result.found ? (
                        <div className="extract-found">
                            <div className="label">✅ Tìm thấy watermark ẩn</div>
                            <pre>{formatJson(result.watermark)}</pre>
                        </div>
                    ) : (
                        <div className="extract-not-found">
                            ⚠️ Không tìm thấy watermark trong ảnh này.
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
