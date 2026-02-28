import React, { useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, SafeAreaView, Alert, ActivityIndicator } from 'react-native';
import { useCart } from '../CartContext';
import { useAuth } from '../AuthContext';
import { createOrder } from '../api';

export default function CartScreen({ navigation }) {
    const { items, removeFromCart, clearCart, total } = useCart();
    const { isLoggedIn } = useAuth();
    const [loading, setLoading] = useState(false);

    const fee = total * 0.025;
    const finalTotal = total + fee;

    const handleCheckout = async () => {
        if (!isLoggedIn) {
            navigation.navigate('Login');
            return;
        }
        setLoading(true);
        try {
            await createOrder(items.map(p => p.id));
            clearCart();
            Alert.alert('Success', 'Order completed successfully!');
            navigation.navigate('LibraryTab');
        } catch (e) {
            Alert.alert('Error', 'Checkout failed');
        } finally {
            setLoading(false);
        }
    };

    const renderItem = ({ item }) => (
        <View style={styles.itemRow}>
            <View style={styles.itemImage}>
                <Text style={{ fontSize: 24 }}>{item.productType === 'PDF' ? '📖' : '🖼️'}</Text>
            </View>
            <View style={styles.itemInfo}>
                <Text style={styles.itemTitle}>{item.title}</Text>
                <Text style={styles.itemType}>{item.productType}</Text>
            </View>
            <Text style={styles.itemPrice}>${item.price.toFixed(2)}</Text>
            <TouchableOpacity onPress={() => removeFromCart(item.id)} style={styles.deleteBtn}>
                <Text style={{ color: 'red', fontSize: 18 }}>×</Text>
            </TouchableOpacity>
        </View>
    );

    return (
        <SafeAreaView style={styles.container}>
            <Text style={styles.header}>Checkout</Text>
            {items.length === 0 ? (
                <View style={styles.empty}>
                    <Text style={styles.emptyText}>Your cart is empty</Text>
                    <TouchableOpacity style={styles.btn} onPress={() => navigation.navigate('CatalogTab')}>
                        <Text style={styles.btnText}>Browse Catalog</Text>
                    </TouchableOpacity>
                </View>
            ) : (
                <>
                    <FlatList
                        data={items}
                        renderItem={renderItem}
                        keyExtractor={(item, index) => `${item.id}-${index}`}
                        style={styles.list}
                    />
                    <View style={styles.summary}>
                        <View style={styles.row}>
                            <Text style={styles.label}>Subtotal</Text>
                            <Text style={styles.value}>${total.toFixed(2)}</Text>
                        </View>
                        <View style={styles.row}>
                            <Text style={styles.label}>Processing Fee</Text>
                            <Text style={styles.value}>${fee.toFixed(2)}</Text>
                        </View>
                        <View style={[styles.row, styles.totalRow]}>
                            <Text style={styles.totalLabel}>Total</Text>
                            <Text style={styles.totalValue}>${finalTotal.toFixed(2)}</Text>
                        </View>
                        <TouchableOpacity style={styles.checkoutBtn} onPress={handleCheckout} disabled={loading}>
                            {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.checkoutBtnText}>Complete Purchase</Text>}
                        </TouchableOpacity>
                    </View>
                </>
            )}
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f6f7f7' },
    header: { fontSize: 32, fontWeight: '800', color: '#1f2838', padding: 20, paddingTop: 40 },
    list: { paddingHorizontal: 20 },
    itemRow: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#fff', padding: 15, borderRadius: 12, marginBottom: 10 },
    itemImage: { width: 50, height: 60, backgroundColor: '#f0f2f5', borderRadius: 8, alignItems: 'center', justifyContent: 'center', marginRight: 15 },
    itemInfo: { flex: 1 },
    itemTitle: { fontSize: 16, fontWeight: '700', color: '#1f2838' },
    itemType: { fontSize: 12, color: '#888', marginTop: 4 },
    itemPrice: { fontSize: 16, fontWeight: '800', color: '#1f2838', marginRight: 15 },
    deleteBtn: { padding: 5 },
    summary: { backgroundColor: '#fff', padding: 25, borderTopLeftRadius: 25, borderTopRightRadius: 25 },
    row: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 10 },
    label: { color: '#666', fontSize: 15 },
    value: { color: '#1f2838', fontSize: 15, fontWeight: '500' },
    totalRow: { borderTopWidth: 1, borderColor: '#eee', paddingTop: 15, marginTop: 5, marginBottom: 20 },
    totalLabel: { fontSize: 18, fontWeight: '800', color: '#1f2838' },
    totalValue: { fontSize: 18, fontWeight: '800', color: '#1f2838' },
    checkoutBtn: { backgroundColor: '#1f2838', padding: 18, borderRadius: 12, alignItems: 'center' },
    checkoutBtnText: { color: '#fff', fontSize: 16, fontWeight: '800' },
    empty: { flex: 1, alignItems: 'center', justifyContent: 'center' },
    emptyText: { fontSize: 18, color: '#888', marginBottom: 20 },
    btn: { backgroundColor: '#1f2838', paddingHorizontal: 30, paddingVertical: 15, borderRadius: 10 },
    btnText: { color: '#fff', fontWeight: 'bold' }
});
