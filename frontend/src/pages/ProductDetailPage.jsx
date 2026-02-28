import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getProduct, getProducts } from '../api';
import { useCart } from '../CartContext';

function ProductDetailPage() {
    const { id } = useParams();
    const [product, setProduct] = useState(null);
    const [related, setRelated] = useState([]);
    const { addToCart, items } = useCart();
    const isInCart = items.some(p => p.id === id);

    useEffect(() => {
        getProduct(id).then(p => {
            setProduct(p);
            getProducts(p.productType).then(all =>
                setRelated(all.filter(r => r.id !== id).slice(0, 4))
            );
        });
    }, [id]);

    if (!product) return <div className="text-center py-20 text-primary/50">Loading...</div>;

    const fmt = (price) => `$${Number(price).toFixed(2)}`;
    const isPdf = product.productType === 'PDF';

    const renderStars = (rating) => {
        const full = Math.floor(rating / 10);
        const half = (rating % 10) >= 5 ? 1 : 0;
        return (
            <div className="flex text-amber-500">
                {Array(full).fill(0).map((_, i) => <span key={i} className="material-symbols-outlined text-sm">star</span>)}
                {half > 0 && <span className="material-symbols-outlined text-sm">star_half</span>}
            </div>
        );
    };

    return (
        <div className="mx-auto max-w-7xl px-6 py-10 md:px-10">
            {/* Breadcrumb */}
            <div className="flex items-center gap-3 mb-8 text-sm">
                <Link to="/" className="text-primary/40 hover:text-primary">Home</Link>
                <span className="text-primary/20">/</span>
                <Link to={`/?type=${product.productType}`} className="text-primary/40 hover:text-primary">
                    {isPdf ? 'eBooks' : 'Gallery'}
                </Link>
                <span className="text-primary/20">/</span>
                <span className="font-medium">{product.title}</span>
            </div>

            <div className="grid grid-cols-1 gap-16 lg:grid-cols-2">
                {/* Product Image */}
                <div>
                    <div className="aspect-[3/4] w-full rounded-xl bg-gradient-to-br from-primary/10 to-primary/5 overflow-hidden shadow-lg flex items-center justify-center">
                        {product.thumbnailUrl ? (
                            <img
                                src={product.thumbnailUrl.startsWith('http') ? product.thumbnailUrl : `http://localhost:8080/api/products/images/${product.thumbnailUrl}`}
                                alt={product.title}
                                className="w-full h-full object-cover"
                            />
                        ) : (
                            <span className="material-symbols-outlined text-primary/20" style={{ fontSize: '120px' }}>
                                {isPdf ? 'menu_book' : 'image'}
                            </span>
                        )}
                    </div>
                    <div className="flex gap-4 mt-6">
                        <button className="flex-1 flex items-center justify-center gap-2 h-14 rounded-xl border-2 border-primary/10 hover:bg-primary/5 transition-colors font-medium">
                            <span className="material-symbols-outlined">visibility</span>
                            Sample Pages
                        </button>
                        <button className="flex-1 flex items-center justify-center gap-2 h-14 rounded-xl border-2 border-primary/10 hover:bg-primary/5 transition-colors font-medium">
                            <span className="material-symbols-outlined">rate_review</span>
                            Read Reviews
                        </button>
                    </div>
                </div>

                {/* Product Info */}
                <div className="flex flex-col gap-6">
                    {product.category && (
                        <span className="inline-flex w-fit rounded-full bg-primary px-3 py-1 text-xs font-bold text-white uppercase tracking-wider">
                            {isPdf ? 'Premium Edition' : '4K High-Res'}
                        </span>
                    )}
                    <h1 className="text-4xl font-extrabold tracking-tight text-primary lg:text-5xl" style={{ fontFamily: 'serif' }}>
                        {product.title}
                    </h1>
                    <p className="text-primary/60">by <span className="font-bold text-primary">{product.author}</span></p>

                    <div className="flex items-center gap-4">
                        {renderStars(product.rating)}
                        <span className="text-sm text-primary/50">{(product.rating / 10).toFixed(1)} ({product.reviewCount} Reviews)</span>
                        <span className="text-primary/30">|</span>
                        <span className="text-sm text-green-600 font-medium flex items-center gap-1">
                            <span className="material-symbols-outlined text-sm">check_circle</span> Instant Download
                        </span>
                    </div>

                    <div className="flex items-baseline gap-3">
                        <span className="text-4xl font-extrabold text-primary">{fmt(product.price)}</span>
                        {product.originalPrice && (
                            <span className="text-xl text-primary/30 line-through">{fmt(product.originalPrice)}</span>
                        )}
                    </div>

                    <blockquote className="border-l-4 border-primary/10 pl-6 py-4 italic text-primary/70 leading-relaxed">
                        "{product.description}"
                    </blockquote>

                    <div className="flex gap-4 items-center">
                        <button onClick={() => !isInCart && addToCart(product)}
                            className={`flex-1 ${isInCart ? 'bg-primary/20 text-primary' : 'bg-primary text-white hover:shadow-lg hover:shadow-primary/20'} h-14 px-8 rounded-xl font-bold text-lg transition-all flex items-center justify-center gap-2`}>
                            <span className="material-symbols-outlined">{isInCart ? 'check' : 'lock'}</span>
                            {isInCart ? 'In Cart' : 'Add to Cart'}
                        </button>
                        <button className="border-2 border-primary/20 text-primary h-14 px-8 rounded-xl font-bold text-lg hover:bg-primary/5 transition-all">
                            Buy Now
                        </button>
                        <button className="h-14 w-14 rounded-xl border-2 border-primary/10 flex items-center justify-center hover:bg-primary/5 transition-colors">
                            <span className="material-symbols-outlined">favorite</span>
                        </button>
                    </div>

                    {/* Technical Specs */}
                    <div className="mt-4 border-t border-primary/5 pt-6">
                        <h4 className="text-xs font-bold text-primary/40 uppercase tracking-widest mb-4">Technical Specifications</h4>
                        <div className="grid grid-cols-3 gap-4">
                            <div>
                                <p className="text-xs text-primary/40 uppercase">Format</p>
                                <p className="font-bold flex items-center gap-1">
                                    <span className="material-symbols-outlined text-sm">{isPdf ? 'picture_as_pdf' : 'image'}</span>
                                    {isPdf ? 'Interactive PDF' : '4K PNG'}
                                </p>
                            </div>
                            <div>
                                <p className="text-xs text-primary/40 uppercase">File Size</p>
                                <p className="font-bold flex items-center gap-1">
                                    <span className="material-symbols-outlined text-sm">data_usage</span>
                                    {isPdf ? '48.5 MB' : '12.8 MB'}
                                </p>
                            </div>
                            <div>
                                <p className="text-xs text-primary/40 uppercase">{isPdf ? 'Pages' : 'Resolution'}</p>
                                <p className="font-bold flex items-center gap-1">
                                    <span className="material-symbols-outlined text-sm">{isPdf ? 'description' : 'aspect_ratio'}</span>
                                    {isPdf ? '214 Pages' : '3840 × 2160'}
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* License */}
                    <div className="bg-primary/5 rounded-xl p-6 mt-2">
                        <div className="flex items-center gap-3 mb-2">
                            <span className="material-symbols-outlined text-primary">info</span>
                            <span className="font-bold">Commercial Usage License</span>
                        </div>
                        <p className="text-sm text-primary/60">
                            All digital downloads include a personal and standard professional license.
                            Downloads are watermark-protected for content security.
                        </p>
                    </div>
                </div>
            </div>

            {/* Related */}
            {related.length > 0 && (
                <section className="mt-24 mb-16">
                    <div className="flex items-center justify-between mb-8">
                        <h2 className="text-2xl font-bold text-primary">Related Curations</h2>
                        <Link to={`/?type=${product.productType}`} className="text-primary font-semibold flex items-center gap-1 hover:underline text-sm">
                            View library <span className="material-symbols-outlined text-sm">arrow_forward</span>
                        </Link>
                    </div>
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
                        {related.map(p => (
                            <Link key={p.id} to={`/products/${p.id}`} className="group">
                                <div className="aspect-[3/4] rounded-xl bg-gradient-to-br from-primary/15 to-primary/5 overflow-hidden shadow-md mb-3 flex items-center justify-center">
                                    {p.thumbnailUrl ? (
                                        <img
                                            src={p.thumbnailUrl.startsWith('http') ? p.thumbnailUrl : `http://localhost:8080/api/products/images/${p.thumbnailUrl}`}
                                            alt={p.title}
                                            className="w-full h-full object-cover"
                                        />
                                    ) : (
                                        <span className="material-symbols-outlined text-primary/20" style={{ fontSize: '60px' }}>
                                            {p.productType === 'PDF' ? 'menu_book' : 'image'}
                                        </span>
                                    )}
                                </div>
                                <h3 className="font-bold text-primary group-hover:underline">{p.title}</h3>
                                <p className="text-sm text-primary/50">{p.author}</p>
                            </Link>
                        ))}
                    </div>
                </section>
            )}
        </div>
    );
}

export default ProductDetailPage;
