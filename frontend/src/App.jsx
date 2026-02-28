import { BrowserRouter, Routes, Route, Link, useNavigate, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './AuthContext';
import { CartProvider, useCart } from './CartContext';
import LandingPage from './pages/LandingPage';
import ProductDetailPage from './pages/ProductDetailPage';
import LoginPage from './pages/LoginPage';
import CartPage from './pages/CartPage';
import LibraryPage from './pages/LibraryPage';
import DashboardPage from './pages/DashboardPage';
import UploadProductPage from './pages/UploadProductPage';
import VerifyPage from './pages/VerifyPage';
import './index.css';

function Navbar() {
    const { user, logout, isLoggedIn } = useAuth();
    const { count } = useCart();

    return (
        <header className="sticky top-0 z-50 w-full border-b border-primary/10 bg-bg-light/80 backdrop-blur-md px-6 md:px-10 lg:px-20 py-4">
            <div className="mx-auto flex max-w-7xl items-center justify-between">
                <div className="flex items-center gap-8">
                    <Link to="/" className="flex items-center gap-2">
                        <span className="material-symbols-outlined text-primary text-3xl">diamond</span>
                        <span className="text-lg font-bold tracking-tight text-primary">Aura Digital</span>
                    </Link>
                    <nav className="hidden md:flex items-center gap-6">
                        <Link to="/?type=PDF" className="text-sm font-medium hover:text-primary/70 transition-colors">eBooks</Link>
                        <Link to="/?type=IMAGE" className="text-sm font-medium hover:text-primary/70 transition-colors">Gallery</Link>
                        {isLoggedIn && (
                            <Link to="/library" className="text-sm font-medium hover:text-primary/70 transition-colors">My Library</Link>
                        )}
                    </nav>
                </div>
                <div className="flex items-center gap-4">
                    <Link to="/cart" className="relative flex h-10 w-10 items-center justify-center rounded-xl bg-primary/5 hover:bg-primary/10 transition-colors">
                        <span className="material-symbols-outlined text-primary">shopping_cart</span>
                        {count > 0 && (
                            <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-primary text-white text-[10px] font-bold">
                                {count}
                            </span>
                        )}
                    </Link>
                    {isLoggedIn ? (
                        <div className="flex items-center gap-3">
                            <Link to="/library" className="hidden sm:flex items-center gap-2">
                                <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
                                    <span className="material-symbols-outlined text-primary text-sm">person</span>
                                </div>
                                <span className="text-sm font-medium">{user.fullName}</span>
                            </Link>
                            {(user.role === 'ADMIN' || user.role === 'SELLER') && (
                                <Link to="/dashboard" className="hidden sm:flex items-center gap-2 h-8 px-3 rounded-lg bg-emerald-500/10 text-emerald-600 hover:bg-emerald-500/20 transition-colors">
                                    <span className="material-symbols-outlined text-sm">dashboard</span>
                                    <span className="text-xs font-bold uppercase tracking-wider">Seller Dashboard</span>
                                </Link>
                            )}
                            <button onClick={logout}
                                className="flex h-8 items-center justify-center rounded-lg bg-primary/5 px-3 text-xs font-medium hover:bg-primary/10 transition-colors">
                                Sign Out
                            </button>
                        </div>
                    ) : (
                        <Link to="/login"
                            className="bg-primary text-white h-10 px-5 rounded-xl font-bold text-sm hover:shadow-lg hover:shadow-primary/20 transition-all flex items-center">
                            Sign In
                        </Link>
                    )}
                </div>
            </div>
        </header>
    );
}

function Footer() {
    return (
        <footer className="bg-primary text-white py-16">
            <div className="mx-auto max-w-7xl px-6 md:px-10">
                <div className="grid grid-cols-1 gap-12 lg:grid-cols-4">
                    <div className="flex flex-col gap-6">
                        <Link to="/" className="flex items-center gap-2">
                            <span className="material-symbols-outlined text-3xl">diamond</span>
                            <span className="text-2xl font-bold tracking-tight">Aura Digital</span>
                        </Link>
                        <p className="text-slate-400 text-sm leading-relaxed">
                            The world's premier marketplace for high-end digital books and masterwork imagery. Curated for the modern creative.
                        </p>
                    </div>
                    <div>
                        <h5 className="font-bold mb-6 text-lg">Shop</h5>
                        <ul className="flex flex-col gap-4 text-sm text-slate-400">
                            <li><Link to="/?type=PDF" className="hover:text-white transition-colors">eBook Library</Link></li>
                            <li><Link to="/?type=IMAGE" className="hover:text-white transition-colors">Art Gallery</Link></li>
                        </ul>
                    </div>
                    <div>
                        <h5 className="font-bold mb-6 text-lg">Support</h5>
                        <ul className="flex flex-col gap-4 text-sm text-slate-400">
                            <li><a className="hover:text-white transition-colors" href="#">Privacy Policy</a></li>
                            <li><a className="hover:text-white transition-colors" href="#">Terms of Service</a></li>
                        </ul>
                    </div>
                    <div>
                        <h5 className="font-bold mb-6 text-lg">Newsletter</h5>
                        <p className="text-sm text-slate-400 mb-4">Get notified about new weekly drops.</p>
                        <form className="flex flex-col gap-2" onSubmit={e => e.preventDefault()}>
                            <input className="bg-white/10 border-none rounded-xl px-4 h-12 focus:ring-1 focus:ring-white/50 text-white placeholder:text-slate-500" placeholder="Email address" type="email" />
                            <button className="bg-white text-primary font-bold h-12 rounded-xl hover:bg-slate-200 transition-colors">Subscribe</button>
                        </form>
                    </div>
                </div>
                <div className="mt-16 pt-8 border-t border-white/10 flex items-center justify-between">
                    <p className="text-sm text-slate-500">© 2024 Aura Digital Marketplace. All rights reserved.</p>
                    <div className="flex items-center gap-6 opacity-30 grayscale">
                        <span className="material-symbols-outlined text-4xl">payments</span>
                        <span className="material-symbols-outlined text-4xl">credit_card</span>
                    </div>
                </div>
            </div>
        </footer>
    );
}

function AppRoutes() {
    const { user, isLoggedIn } = useAuth();

    return (
        <div className="relative flex min-h-screen w-full flex-col">
            <Navbar />
            <main className="flex-1">
                <Routes>
                    <Route path="/" element={<LandingPage />} />
                    <Route path="/products/:id" element={<ProductDetailPage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/cart" element={<CartPage />} />
                    <Route path="/library" element={<LibraryPage />} />
                    <Route
                        path="/dashboard"
                        element={isLoggedIn && ['ADMIN', 'SELLER'].includes(user?.role) ? <DashboardPage /> : <Navigate to="/" />}
                    />
                    <Route
                        path="/upload"
                        element={isLoggedIn && ['ADMIN', 'SELLER'].includes(user?.role) ? <UploadProductPage /> : <Navigate to="/" />}
                    />
                    <Route
                        path="/verify"
                        element={isLoggedIn && ['ADMIN', 'SELLER'].includes(user?.role) ? <VerifyPage /> : <Navigate to="/" />}
                    />
                </Routes>
            </main>
            <Footer />
        </div>
    );
}

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <CartProvider>
                    <AppRoutes />
                </CartProvider>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;
