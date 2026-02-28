import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { getMyOrders, downloadProduct } from '../api';
import { useAuth } from '../AuthContext';

function LibraryPage() {
    const { user, isLoggedIn } = useAuth();
    const navigate = useNavigate();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [downloading, setDownloading] = useState(null);
    const [filter, setFilter] = useState('ALL');

    useEffect(() => {
        if (!isLoggedIn) { navigate('/login'); return; }
        getMyOrders().then(setOrders).finally(() => setLoading(false));
    }, [isLoggedIn]);

    const handleDownload = async (productId, title, type) => {
        setDownloading(productId);
        try {
            const blob = await downloadProduct(productId);
            const ext = type === 'PDF' ? '.pdf' : '.png';
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = title.replace(/[^a-zA-Z0-9]/g, '_') + ext;
            a.click();
            URL.revokeObjectURL(url);
        } catch (err) {
            alert('Download failed');
        } finally {
            setDownloading(null);
        }
    };

    // Deduplicate purchased products
    const allProducts = [];
    const seen = new Set();
    orders.forEach(o => o.items.forEach(item => {
        if (!seen.has(item.productId)) {
            seen.add(item.productId);
            allProducts.push({ ...item, orderDate: o.createdAt });
        }
    }));

    const filtered = filter === 'ALL' ? allProducts
        : allProducts.filter(p => p.productType === filter);

    if (loading) return <div className="text-center py-20 text-primary/50">Loading...</div>;

    return (
        <div className="mx-auto max-w-7xl px-6 py-10 md:px-10">
            {/* Profile Header */}
            <div className="flex items-center justify-between mb-10">
                <div className="flex items-center gap-6">
                    <div className="h-20 w-20 rounded-full bg-primary/10 flex items-center justify-center">
                        <span className="material-symbols-outlined text-primary text-4xl">person</span>
                    </div>
                    <div>
                        <h1 className="text-2xl font-extrabold text-primary">{user?.fullName || 'User'}</h1>
                        <p className="text-sm text-primary/50">
                            {allProducts.length} Purchases
                        </p>
                    </div>
                </div>
                <div className="flex gap-3">
                    <button className="h-12 px-6 rounded-xl border-2 border-primary font-bold text-sm">My Library</button>
                    <button className="h-12 px-6 rounded-xl border-2 border-primary/10 text-primary/50 font-bold text-sm">Order History</button>
                </div>
            </div>

            {/* Filter tabs */}
            <div className="flex items-center justify-between mb-8 border-b border-primary/5 pb-4">
                <div className="flex gap-6">
                    {['ALL', 'PDF', 'IMAGE'].map(f => (
                        <button key={f} onClick={() => setFilter(f)}
                            className={`text-sm font-medium pb-2 transition-colors ${filter === f
                                ? 'text-primary border-b-2 border-primary'
                                : 'text-primary/40 hover:text-primary/60'
                                }`}>
                            {f === 'ALL' ? 'All Assets' : f === 'PDF' ? 'eBooks' : 'Artworks'}
                        </button>
                    ))}
                </div>
                <div className="relative hidden sm:block">
                    <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-primary/30 text-xl">search</span>
                    <input className="h-10 w-56 rounded-xl border-none bg-primary/5 pl-10 pr-4 text-sm focus:ring-2 focus:ring-primary/20"
                        placeholder="Search my library..." type="text" />
                </div>
            </div>

            {/* Products Grid */}
            {filtered.length === 0 ? (
                <div className="text-center py-20">
                    <span className="material-symbols-outlined text-primary/20 text-6xl mb-4 block">library_books</span>
                    <p className="text-xl text-primary/50 mb-6">No purchased content yet</p>
                    <Link to="/" className="bg-primary text-white h-14 px-8 rounded-xl font-bold text-lg inline-flex items-center">
                        Browse More Products
                    </Link>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                    {filtered.map(item => (
                        <div key={item.productId} className="flex flex-col">
                            <div className="relative aspect-[3/4] rounded-xl bg-gradient-to-br from-primary/10 to-primary/5 overflow-hidden shadow-md mb-3 flex items-center justify-center">
                                {item.thumbnailUrl ? (
                                    <img
                                        src={item.thumbnailUrl.startsWith('http') ? item.thumbnailUrl : `http://localhost:8080/api/products/images/${item.thumbnailUrl}`}
                                        alt={item.title}
                                        className="w-full h-full object-cover"
                                    />
                                ) : (
                                    <span className="material-symbols-outlined text-primary/20" style={{ fontSize: '60px' }}>
                                        {item.productType === 'PDF' ? 'menu_book' : 'image'}
                                    </span>
                                )}
                                <span className="absolute top-3 right-3 bg-primary/80 text-white text-[10px] font-bold px-2 py-0.5 rounded-full uppercase">
                                    {item.productType === 'PDF' ? 'PDF' : '4K High-Res'}
                                </span>
                            </div>
                            <h3 className="font-bold text-primary">{item.title}</h3>
                            <p className="text-xs text-primary/40 mb-3">
                                Purchased on {new Date(item.orderDate || Date.now()).toLocaleDateString('en-US', {
                                    year: 'numeric', month: 'short', day: 'numeric'
                                })}
                            </p>
                            <button onClick={() => handleDownload(item.productId, item.title, item.productType)}
                                disabled={downloading === item.productId}
                                className="bg-primary text-white h-12 rounded-xl font-bold text-sm hover:shadow-lg hover:shadow-primary/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50">
                                <span className="material-symbols-outlined text-sm">
                                    {downloading === item.productId ? 'hourglass_top' : 'download'}
                                </span>
                                {downloading === item.productId ? 'Processing...' : 'Download Files'}
                            </button>
                            <Link to={`/products/${item.productId}`}
                                className="text-center text-sm text-primary/40 hover:text-primary mt-2 transition-colors">
                                View Details
                            </Link>
                        </div>
                    ))}
                </div>
            )}

            {/* End of library */}
            {filtered.length > 0 && (
                <div className="mt-16 mb-8 border border-dashed border-primary/10 rounded-2xl p-10 text-center">
                    <p className="text-primary/30 mb-4">You've reached the end of your library</p>
                    <Link to="/" className="border-2 border-primary text-primary h-12 px-8 rounded-xl font-bold inline-flex items-center hover:bg-primary hover:text-white transition-all">
                        Browse More Products
                    </Link>
                </div>
            )}
        </div>
    );
}

export default LibraryPage;
