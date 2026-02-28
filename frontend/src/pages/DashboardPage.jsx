import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getProducts } from '../api';
import { useAuth } from '../AuthContext';

function DashboardPage() {
    const { user, isLoggedIn } = useAuth();
    const navigate = useNavigate();
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!isLoggedIn || (user?.role !== 'ADMIN' && user?.role !== 'SELLER')) {
            navigate('/');
            return;
        }
        getProducts().then(setProducts).finally(() => setLoading(false));
    }, [isLoggedIn, user]);

    if (loading) return <div className="text-center py-20 text-primary/50">Loading Dashboard...</div>;

    const fmt = (price) => `$${Number(price).toFixed(2)}`;

    return (
        <div className="mx-auto max-w-7xl px-6 py-10 md:px-10">
            <div className="flex items-center justify-between mb-10">
                <div>
                    <h1 className="text-3xl font-extrabold text-primary">Seller Dashboard</h1>
                    <p className="text-sm text-primary/50 mt-1">Manage your digital products and listings.</p>
                </div>
                <div className="flex gap-4">
                    <Link to="/verify" className="bg-primary/10 text-primary h-12 px-6 rounded-xl font-bold flex items-center gap-2 hover:bg-primary/20 transition-all">
                        <span className="material-symbols-outlined">verified</span>
                        Verify Asset
                    </Link>
                    <Link to="/upload" className="bg-primary text-white h-12 px-6 rounded-xl font-bold flex items-center gap-2 hover:shadow-lg hover:shadow-primary/20 transition-all">
                        <span className="material-symbols-outlined">add</span>
                        New Product
                    </Link>
                </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-10">
                <div className="bg-white rounded-2xl border border-primary/5 shadow-sm p-6 flex flex-col gap-2">
                    <span className="material-symbols-outlined text-primary/40 text-3xl">inventory_2</span>
                    <h2 className="text-2xl font-bold text-primary">{products.length}</h2>
                    <p className="text-sm text-primary/50 font-medium uppercase tracking-widest">Total Products</p>
                </div>
                <div className="bg-white rounded-2xl border border-primary/5 shadow-sm p-6 flex flex-col gap-2">
                    <span className="material-symbols-outlined text-green-500/40 text-3xl">payments</span>
                    <h2 className="text-2xl font-bold text-primary">{fmt(products.reduce((acc, p) => acc + p.price, 0))}</h2>
                    <p className="text-sm text-primary/50 font-medium uppercase tracking-widest">Total Value</p>
                </div>
                <div className="bg-primary/5 rounded-2xl border border-primary/10 shadow-sm p-6 flex flex-col gap-2 justify-center">
                    <p className="text-primary/70 font-medium">Want to reach more customers?</p>
                    <button className="text-primary font-bold text-sm text-left hover:underline flex items-center gap-1">
                        Upgrade to Pro Seller <span className="material-symbols-outlined text-sm">arrow_forward</span>
                    </button>
                </div>
            </div>

            <h2 className="text-xl font-bold text-primary mb-6">Product Catalog</h2>
            <div className="bg-white rounded-2xl border border-primary/5 shadow-sm overflow-hidden">
                <table className="w-full text-left border-collapse">
                    <thead>
                        <tr className="bg-primary/5 border-b border-primary/10">
                            <th className="p-4 text-xs font-bold text-primary/40 uppercase tracking-widest">Product</th>
                            <th className="p-4 text-xs font-bold text-primary/40 uppercase tracking-widest">Type</th>
                            <th className="p-4 text-xs font-bold text-primary/40 uppercase tracking-widest">Price</th>
                            <th className="p-4 text-xs font-bold text-primary/40 uppercase tracking-widest">Status</th>
                            <th className="p-4 text-xs font-bold text-primary/40 uppercase tracking-widest text-right">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {products.map(p => (
                            <tr key={p.id} className="border-b border-primary/5 last:border-0 hover:bg-primary/5 transition-colors">
                                <td className="p-4">
                                    <div className="flex items-center gap-4">
                                        <div className="h-12 w-10 rounded bg-primary/10 flex items-center justify-center flex-shrink-0">
                                            <span className="material-symbols-outlined text-primary/30 text-sm">
                                                {p.productType === 'PDF' ? 'menu_book' : 'image'}
                                            </span>
                                        </div>
                                        <div>
                                            <p className="font-bold text-primary">{p.title}</p>
                                            <p className="text-xs text-primary/40">{p.author}</p>
                                        </div>
                                    </div>
                                </td>
                                <td className="p-4">
                                    <span className="inline-flex rounded-full bg-primary/10 text-primary px-2.5 py-0.5 text-xs font-bold uppercase">
                                        {p.productType}
                                    </span>
                                </td>
                                <td className="p-4 font-medium text-primary">{fmt(p.price)}</td>
                                <td className="p-4">
                                    <span className="inline-flex rounded-full bg-green-100 text-green-700 px-2.5 py-0.5 text-xs font-bold">
                                        Active
                                    </span>
                                </td>
                                <td className="p-4 text-right">
                                    <button className="h-8 w-8 rounded-lg hover:bg-primary/10 text-primary/50 hover:text-primary transition-colors inline-flex items-center justify-center">
                                        <span className="material-symbols-outlined text-sm">edit</span>
                                    </button>
                                    <button className="h-8 w-8 rounded-lg hover:bg-red-50 text-red-400 hover:text-red-600 transition-colors inline-flex items-center justify-center ml-2">
                                        <span className="material-symbols-outlined text-sm">delete</span>
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                {products.length === 0 && (
                    <div className="p-10 text-center text-primary/40">
                        No products found. Start by adding a new product.
                    </div>
                )}
            </div>
        </div>
    );
}

export default DashboardPage;
