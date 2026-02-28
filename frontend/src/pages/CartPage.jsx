import { useNavigate, Link } from 'react-router-dom';
import { useCart } from '../CartContext';
import { useAuth } from '../AuthContext';
import { createOrder } from '../api';
import { useState } from 'react';

function CartPage() {
    const { items, removeFromCart, clearCart, total } = useCart();
    const { isLoggedIn } = useAuth();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);

    const fmt = (price) => `$${Number(price).toFixed(2)}`;
    const processingFee = (total * 0.025).toFixed(2);
    const grandTotal = (total + Number(processingFee)).toFixed(2);

    const handleCheckout = async () => {
        if (!isLoggedIn) { navigate('/login'); return; }
        setLoading(true);
        try {
            await createOrder(items.map(p => p.id));
            clearCart();
            setSuccess(true);
            setTimeout(() => navigate('/library'), 2000);
        } catch (err) {
            alert('Checkout failed');
        } finally {
            setLoading(false);
        }
    };

    if (success) {
        return (
            <div className="min-h-[60vh] flex items-center justify-center">
                <div className="text-center">
                    <span className="material-symbols-outlined text-green-500 text-6xl mb-4 block">check_circle</span>
                    <h2 className="text-3xl font-extrabold text-primary">Order Successful!</h2>
                    <p className="text-primary/50 mt-2">Redirecting to your library...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="mx-auto max-w-7xl px-6 py-16 md:px-10">
            <h1 className="text-4xl font-extrabold text-primary mb-2">Checkout</h1>

            {items.length === 0 ? (
                <div className="text-center py-20">
                    <span className="material-symbols-outlined text-primary/20 text-6xl mb-4 block">shopping_cart</span>
                    <p className="text-xl text-primary/50 mb-6">Your cart is empty</p>
                    <Link to="/" className="bg-primary text-white h-14 px-8 rounded-xl font-bold text-lg inline-flex items-center">
                        Browse Catalog
                    </Link>
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-12 lg:grid-cols-5 mt-8">
                    {/* Cart Items */}
                    <div className="lg:col-span-3">
                        <p className="text-primary/50 mb-6">{items.length} items in cart</p>
                        <div className="flex flex-col gap-6">
                            {items.map(item => (
                                <div key={item.id} className="flex items-center gap-6 pb-6 border-b border-primary/5">
                                    <div className="h-20 w-16 rounded-lg bg-gradient-to-br from-primary/10 to-primary/5 flex items-center justify-center flex-shrink-0">
                                        <span className="material-symbols-outlined text-primary/30">
                                            {item.productType === 'PDF' ? 'menu_book' : 'image'}
                                        </span>
                                    </div>
                                    <div className="flex-1">
                                        <Link to={`/products/${item.id}`} className="font-bold text-primary hover:underline text-lg">{item.title}</Link>
                                        <p className="text-sm text-primary/50">
                                            Digital {item.productType} · Lifetime Access
                                        </p>
                                    </div>
                                    <button onClick={() => removeFromCart(item.id)}
                                        className="h-8 w-8 rounded-lg bg-primary/5 flex items-center justify-center hover:bg-red-50 hover:text-red-500 transition-colors">
                                        <span className="material-symbols-outlined text-sm">delete</span>
                                    </button>
                                    <span className="font-bold text-primary text-lg w-20 text-right">{fmt(item.price)}</span>
                                </div>
                            ))}
                        </div>

                        {/* Totals */}
                        <div className="bg-primary/5 rounded-xl p-6 mt-8">
                            <div className="flex justify-between mb-3">
                                <span className="text-primary/60">Subtotal</span>
                                <span className="font-medium">{fmt(total)}</span>
                            </div>
                            <div className="flex justify-between mb-3">
                                <span className="text-primary/60">Processing Fee</span>
                                <span className="font-medium">${processingFee}</span>
                            </div>
                            <div className="border-t border-primary/10 pt-4 mt-4 flex justify-between">
                                <span className="text-xl font-extrabold text-primary">Total</span>
                                <span className="text-xl font-extrabold text-primary">${grandTotal}</span>
                            </div>
                        </div>
                    </div>

                    {/* Payment */}
                    <div className="lg:col-span-2">
                        <div className="bg-white rounded-2xl border border-primary/5 shadow-lg p-8 sticky top-24">
                            <h2 className="text-xl font-bold text-primary flex items-center gap-2 mb-6">
                                <span className="material-symbols-outlined">credit_card</span> Account & Delivery
                            </h2>
                            <div className="mb-4">
                                <label className="text-xs font-bold text-primary/40 uppercase tracking-widest">Email Address</label>
                                <input type="email" placeholder="alex@example.com"
                                    className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm mt-2 focus:ring-2 focus:ring-primary/20" />
                            </div>
                            <div className="mb-6 bg-primary/5 rounded-xl p-4 text-sm text-primary/50">
                                Not required for digital downloads. Your products will be delivered via email and dashboard access.
                            </div>

                            <h3 className="text-xl font-bold text-primary flex items-center gap-2 mb-4">
                                <span className="material-symbols-outlined">payments</span> Payment Method
                            </h3>
                            <div className="flex gap-3 mb-6">
                                <button className="flex-1 h-14 rounded-xl border-2 border-primary font-bold flex items-center justify-center gap-2">
                                    <span className="material-symbols-outlined text-sm">credit_card</span> Card
                                </button>
                                <button className="flex-1 h-14 rounded-xl border-2 border-primary/10 text-primary/40 font-bold flex items-center justify-center gap-2">
                                    <span className="material-symbols-outlined text-sm">currency_bitcoin</span> Crypto
                                </button>
                            </div>

                            <button onClick={handleCheckout} disabled={loading}
                                className="w-full bg-primary text-white h-16 rounded-xl font-bold text-lg hover:shadow-lg hover:shadow-primary/20 transition-all flex items-center justify-center gap-2 disabled:opacity-50">
                                <span className="material-symbols-outlined">lock</span>
                                {loading ? 'Processing...' : isLoggedIn ? 'Complete Purchase' : 'Sign In to Purchase'}
                            </button>
                            <p className="text-xs text-primary/30 text-center mt-4">
                                By completing your purchase, you agree to our Terms of Service and Digital Content Licensing.
                            </p>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default CartPage;
