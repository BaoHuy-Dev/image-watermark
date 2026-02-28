import React, { createContext, useState, useContext } from 'react';

const CartContext = createContext();

export function CartProvider({ children }) {
    const [items, setItems] = useState([]);

    const addToCart = (product) => setItems([...items, product]);
    const removeFromCart = (id) => setItems(items.filter(item => item.id !== id));
    const clearCart = () => setItems([]);

    const count = items.length;
    const total = items.reduce((sum, item) => sum + item.price, 0);

    return (
        <CartContext.Provider value={{ items, addToCart, removeFromCart, clearCart, count, total }}>
            {children}
        </CartContext.Provider>
    );
}

export const useCart = () => useContext(CartContext);
