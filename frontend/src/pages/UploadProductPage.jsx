import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { uploadFile, createNewProduct } from '../api';
import { useAuth } from '../AuthContext';

function UploadProductPage() {
    const { user, isLoggedIn } = useAuth();
    const navigate = useNavigate();

    const [title, setTitle] = useState('');
    const [author, setAuthor] = useState('');
    const [description, setDescription] = useState('');
    const [price, setPrice] = useState('');
    const [originalPrice, setOriginalPrice] = useState('');
    const [category, setCategory] = useState('');
    const [productType, setProductType] = useState('PDF');

    const [file, setFile] = useState(null);
    const [thumbnail, setThumbnail] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!isLoggedIn || (user?.role !== 'ADMIN' && user?.role !== 'SELLER')) {
            navigate('/');
        }
    }, [isLoggedIn, user]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!file || !thumbnail) {
            setError('Both main file and thumbnail are required.');
            return;
        }
        setError('');
        setLoading(true);

        try {
            // Upload files first
            const fileUrl = await uploadFile(file);
            const thumbnailUrl = await uploadFile(thumbnail);

            // Create product
            await createNewProduct({
                title, author, description,
                price: parseFloat(price),
                originalPrice: originalPrice ? parseFloat(originalPrice) : null,
                category, productType, fileUrl, thumbnailUrl,
                rating: 50, reviewCount: 0, featured: false
            });

            navigate('/dashboard');
        } catch (err) {
            setError('Failed to create product. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="mx-auto max-w-4xl px-6 py-10 md:px-10">
            <div className="flex items-center gap-3 mb-8 text-sm">
                <Link to="/dashboard" className="text-primary/40 hover:text-primary flex items-center gap-1">
                    <span className="material-symbols-outlined text-sm">arrow_back</span>
                    Back to Dashboard
                </Link>
            </div>

            <div className="mb-8">
                <h1 className="text-4xl font-extrabold text-primary">Upload New Product</h1>
                <p className="text-primary/60 mt-2">Add a new digital asset to your marketplace.</p>
            </div>

            {error && (
                <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-xl mb-6 flex items-center gap-2">
                    <span className="material-symbols-outlined text-sm">error</span> {error}
                </div>
            )}

            <form onSubmit={handleSubmit} className="bg-white rounded-2xl border border-primary/5 shadow-sm p-8 flex flex-col gap-8">

                {/* Basic Info */}
                <div>
                    <h2 className="text-lg font-bold text-primary mb-4 flex items-center gap-2">
                        <span className="material-symbols-outlined">info</span> Basic Information
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="md:col-span-2">
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Title</label>
                            <input type="text" value={title} onChange={e => setTitle(e.target.value)} required placeholder="e.g. Masterclass in Photography"
                                className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm focus:ring-2 focus:ring-primary/20" />
                        </div>
                        <div>
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Author</label>
                            <input type="text" value={author} onChange={e => setAuthor(e.target.value)} required placeholder="Creator Name"
                                className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm focus:ring-2 focus:ring-primary/20" />
                        </div>
                        <div>
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Category</label>
                            <input type="text" value={category} onChange={e => setCategory(e.target.value)} required placeholder="e.g. Education, Art"
                                className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm focus:ring-2 focus:ring-primary/20" />
                        </div>
                        <div className="md:col-span-2">
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Description</label>
                            <textarea value={description} onChange={e => setDescription(e.target.value)} required rows="4" placeholder="Describe your product..."
                                className="w-full rounded-xl border border-primary/10 p-4 text-sm focus:ring-2 focus:ring-primary/20"></textarea>
                        </div>
                    </div>
                </div>

                <hr className="border-primary/5" />

                {/* Pricing & Type */}
                <div>
                    <h2 className="text-lg font-bold text-primary mb-4 flex items-center gap-2">
                        <span className="material-symbols-outlined">sell</span> Pricing & Format
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div>
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Price ($)</label>
                            <input type="number" step="0.01" min="0" value={price} onChange={e => setPrice(e.target.value)} required placeholder="19.99"
                                className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm font-medium text-primary focus:ring-2 focus:ring-primary/20" />
                        </div>
                        <div>
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Original Price (Optional)</label>
                            <input type="number" step="0.01" min="0" value={originalPrice} onChange={e => setOriginalPrice(e.target.value)} placeholder="29.99"
                                className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm font-medium text-primary/50 focus:ring-2 focus:ring-primary/20" />
                        </div>
                        <div>
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Product Type</label>
                            <select value={productType} onChange={e => setProductType(e.target.value)}
                                className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm focus:ring-2 focus:ring-primary/20 bg-white">
                                <option value="PDF">eBook (PDF)</option>
                                <option value="IMAGE">Artwork (PNG/JPG)</option>
                            </select>
                        </div>
                    </div>
                </div>

                <hr className="border-primary/5" />

                {/* Files */}
                <div>
                    <h2 className="text-lg font-bold text-primary mb-4 flex items-center gap-2">
                        <span className="material-symbols-outlined">folder_open</span> Digital Assets
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div>
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Main File (Downloadable)</label>
                            <div className="border-2 border-dashed border-primary/20 rounded-xl p-6 text-center hover:bg-primary/5 transition-colors cursor-pointer relative">
                                <input type="file" onChange={e => setFile(e.target.files[0])} required
                                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" accept={productType === 'PDF' ? '.pdf' : 'image/*'} />
                                <span className="material-symbols-outlined text-primary/30 text-4xl mb-2 block">cloud_upload</span>
                                <p className="text-sm font-bold text-primary">{file ? file.name : 'Click or drag file here'}</p>
                                <p className="text-xs text-primary/40 mt-1">{productType === 'PDF' ? 'PDF formats only' : 'High-res images only'}</p>
                            </div>
                        </div>
                        <div>
                            <label className="text-sm font-bold text-primary/60 uppercase tracking-widest block mb-2">Thumbnail (Cover Image)</label>
                            <div className="border-2 border-dashed border-primary/20 rounded-xl p-6 text-center hover:bg-primary/5 transition-colors cursor-pointer relative">
                                <input type="file" onChange={e => setThumbnail(e.target.files[0])} required accept="image/*"
                                    className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" />
                                <span className="material-symbols-outlined text-primary/30 text-4xl mb-2 block">image</span>
                                <p className="text-sm font-bold text-primary">{thumbnail ? thumbnail.name : 'Click or drag cover here'}</p>
                                <p className="text-xs text-primary/40 mt-1">Recommended 3:4 or 4:5 ratio</p>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="pt-4 flex justify-end gap-4">
                    <Link to="/dashboard" className="h-14 px-8 rounded-xl font-bold text-primary/50 hover:bg-primary/5 flex items-center justify-center transition-all">Cancel</Link>
                    <button type="submit" disabled={loading}
                        className="bg-primary text-white h-14 px-10 rounded-xl font-bold text-lg hover:shadow-lg hover:shadow-primary/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50">
                        {loading ? <span className="material-symbols-outlined animate-spin text-sm">sync</span> : <span className="material-symbols-outlined text-sm">publish</span>}
                        {loading ? 'Uploading & Creating...' : 'Publish Product'}
                    </button>
                </div>

            </form>
        </div>
    );
}

export default UploadProductPage;
