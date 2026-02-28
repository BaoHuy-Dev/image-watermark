import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, SafeAreaView, ActivityIndicator, Linking } from 'react-native';
import { getMyOrders, BASE } from '../api';
import { useAuth } from '../AuthContext';

export default function LibraryScreen({ navigation }) {
    const { user, isLoggedIn, logout } = useAuth();
    const [assets, setAssets] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const unsubscribe = navigation.addListener('focus', () => {
            if (isLoggedIn) fetchLibrary();
        });
        if (isLoggedIn) fetchLibrary();
        return unsubscribe;
    }, [isLoggedIn, navigation]);

    const fetchLibrary = () => {
        setLoading(true);
        getMyOrders().then(orders => {
            const all = [];
            const seen = new Set();
            orders.forEach(o => o.items.forEach(i => {
                if (!seen.has(i.productId)) {
                    seen.add(i.productId);
                    all.push(i);
                }
            }));
            setAssets(all);
            setLoading(false);
        }).catch(() => setLoading(false));
    };

    if (!isLoggedIn) {
        return (
            <View style={styles.center}>
                <Text style={styles.emptyText}>Sign in to view your library</Text>
                <TouchableOpacity style={styles.btn} onPress={() => navigation.navigate('Login')}>
                    <Text style={styles.btnText}>Sign In</Text>
                </TouchableOpacity>
            </View>
        );
    }

    const renderItem = ({ item }) => (
        <View style={styles.card}>
            <View style={styles.imagePlaceholder}>
                <Text style={styles.iconText}>{item.productType === 'PDF' ? '📖' : '🖼️'}</Text>
            </View>
            <View style={styles.cardInfo}>
                <Text style={styles.title} numberOfLines={1}>{item.title}</Text>
                <TouchableOpacity
                    style={styles.downloadBtn}
                    // Simple download link using linking - opens device browser to download endpoint
                    onPress={() => Linking.openURL(`${BASE}/api/downloads/${item.productId}`)}
                >
                    <Text style={styles.downloadText}>Download</Text>
                </TouchableOpacity>
            </View>
        </View>
    );

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <View style={styles.profile}>
                    <View style={styles.avatar}><Text style={{ fontSize: 24 }}>👤</Text></View>
                    <View>
                        <Text style={styles.name}>{user.fullName}</Text>
                        <Text style={styles.subtitle}>{assets.length} Purchases</Text>
                    </View>
                </View>
                <TouchableOpacity onPress={() => logout()} style={styles.logoutBtn}>
                    <Text style={styles.logoutText}>Sign Out</Text>
                </TouchableOpacity>
            </View>
            {loading ? (
                <ActivityIndicator size="large" color="#1f2838" style={{ marginTop: 50 }} />
            ) : (
                <FlatList
                    data={assets}
                    keyExtractor={(item) => item.productId}
                    renderItem={renderItem}
                    numColumns={2}
                    contentContainerStyle={styles.list}
                    columnWrapperStyle={styles.row}
                    ListEmptyComponent={<Text style={[styles.emptyText, { textAlign: 'center', marginTop: 50 }]}>No items in library</Text>}
                />
            )}
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f6f7f7' },
    center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 20, paddingTop: 40, backgroundColor: '#fff', borderBottomWidth: 1, borderColor: '#eee' },
    profile: { flexDirection: 'row', alignItems: 'center' },
    avatar: { width: 50, height: 50, borderRadius: 25, backgroundColor: '#f0f2f5', alignItems: 'center', justifyContent: 'center', marginRight: 15 },
    name: { fontSize: 20, fontWeight: '800', color: '#1f2838' },
    subtitle: { fontSize: 13, color: '#888' },
    logoutBtn: { padding: 10 },
    logoutText: { color: 'red', fontWeight: 'bold' },
    list: { padding: 15 },
    row: { justifyContent: 'space-between' },
    card: {
        width: '48%', backgroundColor: '#fff', borderRadius: 12,
        marginBottom: 15, padding: 10,
        shadowColor: '#000', shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05, shadowRadius: 4, elevation: 2
    },
    imagePlaceholder: {
        backgroundColor: '#f0f2f5', height: 120, borderRadius: 8,
        alignItems: 'center', justifyContent: 'center', marginBottom: 10
    },
    iconText: { fontSize: 40 },
    cardInfo: { gap: 8 },
    title: { fontSize: 14, fontWeight: '700', color: '#1f2838' },
    downloadBtn: { backgroundColor: '#1f2838', paddingVertical: 8, borderRadius: 8, alignItems: 'center' },
    downloadText: { color: '#fff', fontSize: 12, fontWeight: 'bold' },
    emptyText: { fontSize: 18, color: '#888', marginBottom: 20 },
    btn: { backgroundColor: '#1f2838', paddingHorizontal: 30, paddingVertical: 15, borderRadius: 10 },
    btnText: { color: '#fff', fontWeight: 'bold' }
});
