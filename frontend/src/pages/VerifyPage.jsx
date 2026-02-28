import { useState, useRef } from 'react';
import { Link } from 'react-router-dom';
import { extractWatermark } from '../api';

function VerifyPage() {
    const fileRef = useRef(null);
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null);
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);

    const handleFileChange = (e) => {
        const selected = e.target.files[0];
        if (selected) {
            setFile(selected);
            setPreview(URL.createObjectURL(selected));
            setResult(null);
        }
    };

    const handleVerify = async () => {
        if (!file) return;
        setLoading(true);
        try {
            const data = await extractWatermark(file);
            if (data.found && data.watermark) {
                try {
                    // Try parsing JSON if it's user info
                    const json = JSON.parse(data.watermark);
                    setResult({ found: true, payload: json, raw: data.watermark });
                } catch (e) {
                    setResult({ found: true, raw: data.watermark });
                }
            } else {
                setResult({ found: false });
            }
        } catch (err) {
            alert('Verification failed or server error');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="mx-auto max-w-4xl px-6 py-10 md:px-10">
            {/* Header */}
            <div className="flex items-center gap-3 mb-8">
                <Link to="/dashboard" className="text-primary/40 hover:text-primary transition-colors flex items-center gap-1">
                    <span className="material-symbols-outlined text-sm">arrow_back</span>
                    Back to Dashboard
                </Link>
            </div>

            <div className="mb-10">
                <h1 className="text-4xl font-extrabold tracking-tight text-primary">Verify Watermark</h1>
                <p className="text-primary/60 mt-2">
                    Upload an image asset to decode and verify its invisible LSB watermark signature.
                </p>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-primary/5 p-8">
                <div className="grid grid-cols-1 gap-12 lg:grid-cols-2">
                    {/* Upload Section */}
                    <div>
                        <h3 className="font-bold text-primary mb-4 flex items-center gap-2">
                            <span className="material-symbols-outlined">upload_file</span>
                            Asset Inspector
                        </h3>

                        <input type="file" ref={fileRef} onChange={handleFileChange} className="hidden" accept="image/png, image/bmp, application/pdf" />

                        <div
                            onClick={() => fileRef.current?.click()}
                            className="border-2 border-dashed border-primary/20 rounded-xl p-10 flex flex-col items-center justify-center text-center cursor-pointer hover:bg-primary/5 transition-colors"
                        >
                            {preview ? (
                                file && file.type === 'application/pdf' ? (
                                    <div className="h-48 w-full mb-4 flex items-center justify-center bg-primary/5 rounded-lg">
                                        <span className="material-symbols-outlined text-primary/30 text-6xl">picture_as_pdf</span>
                                    </div>
                                ) : (
                                    <img src={preview} alt="Preview" className="w-full h-48 object-contain rounded-lg mb-4" />
                                )
                            ) : (
                                <div className="h-16 w-16 mb-4 rounded-full bg-primary/5 flex items-center justify-center text-primary/40">
                                    <span className="material-symbols-outlined text-3xl">plagiarism</span>
                                </div>
                            )}
                            <p className="font-bold text-primary">{file ? file.name : 'Click to select asset file'}</p>
                            <p className="text-xs text-primary/40 mt-1">PNG, BMP, or PDF</p>
                        </div>

                        <button
                            onClick={handleVerify}
                            disabled={!file || loading}
                            className="mt-6 w-full bg-primary text-white h-14 rounded-xl font-bold flex items-center justify-center gap-2 disabled:opacity-50 hover:shadow-lg transition-all"
                        >
                            <span className="material-symbols-outlined">
                                {loading ? 'hourglass_top' : 'fingerprint'}
                            </span>
                            {loading ? 'Analyzing Signature...' : 'Extract Watermark'}
                        </button>
                    </div>

                    {/* Result Section */}
                    <div>
                        <h3 className="font-bold text-primary mb-4 flex items-center gap-2">
                            <span className="material-symbols-outlined">analytics</span>
                            Analysis Report
                        </h3>

                        {result ? (
                            <div className={`p-6 rounded-xl border-2 ${result.found ? 'border-green-500/20 bg-green-50' : 'border-red-500/20 bg-red-50'}`}>
                                <div className="flex items-center gap-3 mb-4">
                                    <span className={`material-symbols-outlined text-3xl ${result.found ? 'text-green-600' : 'text-red-600'}`}>
                                        {result.found ? 'verified_user' : 'warning'}
                                    </span>
                                    <h4 className={`text-xl font-bold ${result.found ? 'text-green-800' : 'text-red-800'}`}>
                                        {result.found ? 'Signature Verified' : 'No Signature Found'}
                                    </h4>
                                </div>

                                {result.found && (
                                    <div className="space-y-4">
                                        <div className="bg-white p-4 rounded-lg border border-green-500/20 shadow-sm">
                                            <p className="text-xs text-green-600/70 font-bold uppercase tracking-widest mb-2">Hidden Payload</p>

                                            {result.payload ? (
                                                <div className="grid grid-cols-1 gap-2">
                                                    <div className="flex justify-between border-b border-green-100 pb-2">
                                                        <span className="text-sm font-medium text-green-700">Owner Email</span>
                                                        <span className="text-sm font-bold text-green-900">{result.payload.email}</span>
                                                    </div>
                                                    <div className="flex justify-between border-b border-green-100 pb-2">
                                                        <span className="text-sm font-medium text-green-700">User ID</span>
                                                        <span className="text-sm font-mono text-green-900 truncate max-w-[150px]">{result.payload.userId}</span>
                                                    </div>
                                                    <div className="flex justify-between">
                                                        <span className="text-sm font-medium text-green-700">Timestamp</span>
                                                        <span className="text-sm font-bold text-green-900">{new Date(result.payload.ts).toLocaleString()}</span>
                                                    </div>
                                                </div>
                                            ) : (
                                                <p className="font-mono text-sm text-green-800 break-all">{result.raw}</p>
                                            )}
                                        </div>
                                        <p className="text-xs text-green-700 text-center">
                                            This image contains an embedded Aura Digital license.
                                        </p>
                                    </div>
                                )}

                                {!result.found && (
                                    <p className="text-sm text-red-700 mt-2">
                                        The advanced LSB scanner could not detect any
                                        Aura Digital watermark signature on this file. It may be original or tampered with.
                                    </p>
                                )}
                            </div>
                        ) : (
                            <div className="h-full border-2 border-dashed border-primary/10 rounded-xl p-8 flex flex-col items-center justify-center text-center bg-primary/5">
                                <span className="material-symbols-outlined text-primary/20 text-5xl mb-4">gpp_maybe</span>
                                <p className="text-primary/50 text-sm">Upload an image and click extract to see the embedded license data.</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default VerifyPage;
