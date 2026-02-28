import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, ActivityIndicator } from 'react-native';
import { getProduct } from '../api';
import { useCart } from '../CartContext';

export default function DetailScreen({ route, navigation }) {
    const { id } = route.params;
    const [product, setProduct] = useState(null);
    const { addToCart, items } = useCart();

    const isInCart = items.some(p => p.id === id);

    useEffect(() => {
        getProduct(id).then(setProduct);
    }, [id]);

    if (!product) return <ActivityIndicator size="large" color="#1f2838" style={{ marginTop: 50 }} />;

    return (
        <ScrollView style={styles.container}>
            <View style={styles.imagePlaceholder}>
                <Text style={styles.iconText}>{product.productType === 'PDF' ? '📖' : '🖼️'}</Text>
            </View>
            <View style={styles.content}>
                <View style={styles.badge}>
                    <Text style={styles.badgeText}>{product.productType === 'PDF' ? 'PREMIUM EDITION' : '4K HIGH-RES'}</Text>
                </View>
                <Text style={styles.title}>{product.title}</Text>
                <Text style={styles.author}>by {product.author}</Text>

                <View style={styles.priceRow}>
                    <Text style={styles.price}>${product.price.toFixed(2)}</Text>
                    {product.originalPrice && (
                        <Text style={styles.originalPrice}>${product.originalPrice.toFixed(2)}</Text>
                    )}
                </View>

                <Text style={styles.description}>{product.description}</Text>

                <TouchableOpacity
                    style={[styles.button, isInCart && styles.buttonOutline]}
                    onPress={() => !isInCart && addToCart(product)}
                >
                    <Text style={[styles.buttonText, isInCart && styles.buttonTextOutline]}>
                        {isInCart ? '✓ IN CART' : 'ADD TO CART'}
                    </Text>
                </TouchableOpacity>

                <View style={styles.specs}>
                    <Text style={styles.specsTitle}>TECHNICAL SPECS</Text>
                    <Text style={styles.specItem}>Format: {product.productType === 'PDF' ? 'Interactive PDF' : '4K PNG'}</Text>
                    <Text style={styles.specItem}>License: Commercial Usage Included</Text>
                </View>
            </View>
        </ScrollView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f6f7f7' },
    imagePlaceholder: {
        height: 350, backgroundColor: '#e5e7eb',
        alignItems: 'center', justifyContent: 'center'
    },
    iconText: { fontSize: 80 },
    content: { padding: 25, backgroundColor: '#fff', borderTopLeftRadius: 25, borderTopRightRadius: 25, marginTop: -20 },
    badge: { alignSelf: 'flex-start', backgroundColor: '#1f2838', paddingHorizontal: 10, paddingVertical: 4, borderRadius: 12, marginBottom: 15 },
    badgeText: { color: '#fff', fontSize: 10, fontWeight: '800' },
    title: { fontSize: 28, fontWeight: '800', color: '#1f2838', marginBottom: 5 },
    author: { fontSize: 15, color: '#666', marginBottom: 20 },
    priceRow: { flexDirection: 'row', alignItems: 'baseline', marginBottom: 25 },
    price: { fontSize: 32, fontWeight: '800', color: '#1f2838', marginRight: 10 },
    originalPrice: { fontSize: 18, color: '#aaa', textDecorationLine: 'line-through' },
    description: { fontSize: 15, color: '#444', lineHeight: 24, marginBottom: 30 },
    button: { backgroundColor: '#1f2838', padding: 18, borderRadius: 12, alignItems: 'center', marginBottom: 30 },
    buttonText: { color: '#fff', fontSize: 16, fontWeight: '800' },
    buttonOutline: { backgroundColor: 'transparent', borderWidth: 2, borderColor: '#1f2838' },
    buttonTextOutline: { color: '#1f2838' },
    specs: { borderTopWidth: 1, borderColor: '#eee', paddingTop: 20 },
    specsTitle: { fontSize: 12, fontWeight: '800', color: '#888', marginBottom: 10 },
    specItem: { fontSize: 14, color: '#333', marginBottom: 5, fontWeight: '500' }
});
