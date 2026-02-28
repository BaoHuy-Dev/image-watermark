import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getFeaturedProducts, getProducts } from '../api';
import { useCart } from '../CartContext';

function LandingPage() {
    const [featured, setFeatured] = useState([]);
    const [searchParams] = useSearchParams();
    const filterType = searchParams.get('type');
    const { addToCart, items } = useCart();
    const isInCart = (id) => items.some(p => p.id === id);

    useEffect(() => {
        if (filterType) {
            getProducts(filterType).then(setFeatured);
        } else {
            getFeaturedProducts().then(setFeatured);
        }
    }, [filterType]);

    const ebooks = featured.filter(p => p.productType === 'PDF');
    const artworks = featured.filter(p => p.productType === 'IMAGE');

    const fmt = (price) => `$${Number(price).toFixed(2)}`;

    const renderStars = (rating) => {
        const full = Math.floor(rating / 10);
        const half = (rating % 10) >= 5 ? 1 : 0;
        const empty = 5 - full - half;
        return (
            <div className="flex text-amber-500">
                {Array(full).fill(0).map((_, i) => <span key={'f' + i} className="material-symbols-outlined text-sm">star</span>)}
                {half > 0 && <span className="material-symbols-outlined text-sm">star_half</span>}
                {Array(empty).fill(0).map((_, i) => <span key={'e' + i} className="material-symbols-outlined text-sm">star_outline</span>)}
            </div>
        );
    };

    return (
        <>
            {/* Hero Section */}
            {!filterType && (
                <section className="mx-auto max-w-7xl px-6 py-16 md:px-10 lg:py-24">
                    <div className="grid grid-cols-1 items-center gap-12 lg:grid-cols-2">
                        <div className="flex flex-col gap-8">
                            <div className="flex flex-col gap-4">
                                <h1 className="text-5xl font-extrabold leading-[1.1] tracking-tight text-primary lg:text-7xl">
                                    Curated Digital Books & Premium Artwork
                                </h1>
                                <p className="text-lg text-primary/60 max-w-lg leading-relaxed">
                                    Experience high-fidelity digital craftsmanship. Instant PDF downloads and master-quality artwork for creators and connoisseurs.
                                </p>
                            </div>
                            <div className="flex flex-wrap gap-4">
                                <Link to="/?type=PDF"
                                    className="bg-primary text-white h-14 px-8 rounded-xl font-bold text-lg hover:shadow-lg hover:shadow-primary/20 transition-all flex items-center justify-center gap-2">
                                    <span className="material-symbols-outlined">auto_stories</span>
                                    Shop PDF Books
                                </Link>
                                <Link to="/?type=IMAGE"
                                    className="border-2 border-primary/20 text-primary h-14 px-8 rounded-xl font-bold text-lg hover:bg-primary/5 transition-all flex items-center justify-center gap-2">
                                    <span className="material-symbols-outlined">image</span>
                                    Browse Images
                                </Link>
                            </div>
                        </div>
                        <div className="relative">
                            <div className="aspect-square w-full rounded-xl bg-gradient-to-br from-primary/10 to-primary/5 overflow-hidden shadow-2xl flex items-center justify-center">
                                <span className="material-symbols-outlined text-primary/20" style={{ fontSize: '200px' }}>auto_stories</span>
                            </div>
                            <div className="absolute -bottom-6 -left-6 rounded-xl bg-white p-6 shadow-xl border border-primary/5 hidden md:block">
                                <div className="flex items-center gap-4">
                                    <div className="h-12 w-12 rounded-full bg-primary flex items-center justify-center">
                                        <span className="material-symbols-outlined text-white">download_done</span>
                                    </div>
                                    <div>
                                        <p className="font-bold text-primary">Instant Access</p>
                                        <p className="text-xs text-primary/50 uppercase tracking-widest">Watermark Protected</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
            )}

            {/* Filter title */}
            {filterType && (
                <div className="mx-auto max-w-7xl px-6 pt-16 md:px-10">
                    <div className="flex items-center gap-3 mb-2">
                        <Link to="/" className="text-primary/40 hover:text-primary transition-colors text-sm">Home</Link>
                        <span className="text-primary/20">/</span>
                        <span className="text-sm font-medium">{filterType === 'PDF' ? 'eBooks' : 'Art Gallery'}</span>
                    </div>
                    <h1 className="text-4xl font-bold tracking-tight text-primary">
                        {filterType === 'PDF' ? 'eBook Library' : 'Premium Art Gallery'}
                    </h1>
                    <p className="text-primary/60 mt-2">
                        {filterType === 'PDF' ? 'Expert knowledge in beautifully formatted PDFs.' : 'Exclusive high-resolution images for your projects.'}
                    </p>
                </div>
            )}

            {/* eBooks Section */}
            {(ebooks.length > 0) && (
                <section className="mx-auto max-w-7xl px-6 py-16 md:px-10 bg-primary/5 rounded-xl mb-16">
                    {!filterType && (
                        <div className="mb-10 flex items-end justify-between">
                            <div>
                                <h2 className="text-3xl font-bold tracking-tight text-primary">Featured eBooks</h2>
                                <p className="text-primary/60">Expert knowledge in beautifully formatted PDFs.</p>
                            </div>
                            <Link to="/?type=PDF" className="text-primary font-semibold flex items-center gap-1 hover:underline">
                                View all <span className="material-symbols-outlined text-sm">arrow_forward</span>
                            </Link>
                        </div>
                    )}
                    <div className="grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-4">
                        {ebooks.map(book => (
                            <div key={book.id} className="group flex flex-col gap-4">
                                <Link to={`/products/${book.id}`}>
                                    <div className="relative aspect-[3/4] w-full overflow-hidden rounded-xl shadow-md transition-all duration-300 group-hover:-translate-y-2 group-hover:shadow-xl bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center">
                                        {book.thumbnailUrl ? (
                                            <img
                                                src={book.thumbnailUrl.startsWith('http') ? book.thumbnailUrl : `http://localhost:8080/api/products/images/${book.thumbnailUrl}`}
                                                alt={book.title}
                                                className="w-full h-full object-cover"
                                            />
                                        ) : (
                                            <span className="material-symbols-outlined text-primary/30" style={{ fontSize: '80px' }}>menu_book</span>
                                        )}
                                        <div className="absolute inset-0 bg-primary/20 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                                            <button onClick={e => { e.preventDefault(); if (!isInCart(book.id)) addToCart(book); }}
                                                className="bg-white text-primary h-10 px-4 rounded-lg font-bold flex items-center gap-2">
                                                <span className="material-symbols-outlined text-xl">add_shopping_cart</span>
                                                {isInCart(book.id) ? 'In Cart' : 'Quick Add'}
                                            </button>
                                        </div>
                                    </div>
                                </Link>
                                <div className="flex flex-col gap-1">
                                    <Link to={`/products/${book.id}`}>
                                        <h3 className="font-bold text-primary text-lg hover:underline">{book.title}</h3>
                                    </Link>
                                    <p className="text-sm text-primary/50">by {book.author}</p>
                                    <div className="flex items-center gap-2 mt-1">
                                        {renderStars(book.rating)}
                                        <span className="text-xs font-bold text-primary/40">({book.reviewCount})</span>
                                        <span className="ml-auto font-bold text-primary">{fmt(book.price)}</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>
            )}

            {/* Artwork Gallery */}
            {(artworks.length > 0) && (
                <section className="mx-auto max-w-7xl px-6 py-16 md:px-10 lg:py-24">
                    {!filterType && (
                        <div className="mb-12 text-center">
                            <h2 className="text-4xl font-bold tracking-tight text-primary">Premium Artwork Gallery</h2>
                            <p className="text-primary/60 mt-2">Exclusive high-resolution images for your commercial or personal projects.</p>
                        </div>
                    )}
                    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
                        {artworks.map(art => (
                            <Link key={art.id} to={`/products/${art.id}`}>
                                <div className="group relative aspect-[4/5] overflow-hidden rounded-xl bg-gradient-to-br from-primary/20 to-primary/10 flex items-center justify-center">
                                    {art.thumbnailUrl ? (
                                        <img
                                            src={art.thumbnailUrl.startsWith('http') ? art.thumbnailUrl : `http://localhost:8080/api/products/images/${art.thumbnailUrl}`}
                                            alt={art.title}
                                            className="absolute inset-0 w-full h-full object-cover"
                                        />
                                    ) : (
                                        <span className="material-symbols-outlined text-primary/20" style={{ fontSize: '100px' }}>image</span>
                                    )}
                                    <div className="absolute inset-0 bg-gradient-to-t from-primary/90 via-primary/30 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100 p-8 flex flex-col justify-end">
                                        <span className="inline-flex w-fit rounded-full bg-primary px-2.5 py-0.5 text-[10px] font-bold text-white uppercase tracking-wider mb-2">4K High-Res</span>
                                        <h3 className="text-xl font-bold text-white mb-1">{art.title}</h3>
                                        <p className="text-white/70 text-sm mb-4">{art.author}</p>
                                        <div className="flex items-center justify-between">
                                            <span className="text-2xl font-bold text-white">{fmt(art.price)}</span>
                                            <button onClick={e => { e.preventDefault(); if (!isInCart(art.id)) addToCart(art); }}
                                                className="h-10 w-10 bg-white rounded-full flex items-center justify-center text-primary hover:scale-110 transition-transform">
                                                <span className="material-symbols-outlined">shopping_cart</span>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </Link>
                        ))}
                    </div>
                </section>
            )}

            {/* Trust Section */}
            {!filterType && (
                <section className="border-y border-primary/5 py-16">
                    <div className="mx-auto max-w-7xl px-6 md:px-10">
                        <div className="grid grid-cols-1 gap-12 md:grid-cols-3">
                            {[
                                { icon: 'cloud_download', title: 'Instant PDF Access', desc: 'Download your library immediately after checkout. High-quality interactive PDF formatting.' },
                                { icon: 'print', title: 'Print-Ready Resolutions', desc: 'All images come in 300 DPI master quality, perfect for large-scale professional printing.' },
                                { icon: 'lock', title: 'Secure Checkout', desc: 'AES-256 encrypted payments. Your data and digital assets are always protected.' }
                            ].map((item, i) => (
                                <div key={i} className="flex flex-col items-center text-center gap-4">
                                    <div className="h-16 w-16 rounded-2xl bg-primary/5 flex items-center justify-center text-primary">
                                        <span className="material-symbols-outlined text-4xl">{item.icon}</span>
                                    </div>
                                    <h4 className="text-xl font-bold text-primary">{item.title}</h4>
                                    <p className="text-primary/60 max-w-xs leading-relaxed">{item.desc}</p>
                                </div>
                            ))}
                        </div>
                    </div>
                </section>
            )}
        </>
    );
}

export default LandingPage;
