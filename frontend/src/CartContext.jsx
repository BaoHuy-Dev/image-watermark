import { createContext, useContext, useState } from 'react';

const CartContext = createContext(null);

export function CartProvider({ children }) {
    const [items, setItems] = useState([]);

    const addToCart = (product) => {
        setItems(prev => {
            if (prev.find(p => p.id === product.id)) return prev;
            return [...prev, product];
        });
    };

    const removeFromCart = (productId) => {
        setItems(prev => prev.filter(p => p.id !== productId));
    };

    const clearCart = () => setItems([]);

    const total = items.reduce((sum, p) => sum + Number(p.price), 0);
    const count = items.length;

    return (
        <CartContext.Provider value={{ items, addToCart, removeFromCart, clearCart, total, count }}>
            {children}
        </CartContext.Provider>
    );
}

export function useCart() {
    return useContext(CartContext);
}
