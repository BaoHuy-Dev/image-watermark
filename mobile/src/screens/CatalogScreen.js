import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, SafeAreaView, ActivityIndicator, Image } from 'react-native';
import { getProducts } from '../api';

export default function CatalogScreen({ navigation }) {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        getProducts().then(data => {
            setProducts(data);
            setLoading(false);
        }).catch(() => setLoading(false));
    }, []);

    const renderItem = ({ item }) => (
        <TouchableOpacity
            style={styles.card}
            onPress={() => navigation.navigate('Detail', { id: item.id })}
        >
            <View style={styles.imagePlaceholder}>
                {item.thumbnailUrl ? (
                    <Image
                        source={{ uri: item.thumbnailUrl.startsWith('http') ? item.thumbnailUrl : `http://10.0.2.2:8080/api/products/images/${item.thumbnailUrl}` }}
                        style={styles.thumbnailImage}
                    />
                ) : (
                    <Text style={styles.iconText}>{item.productType === 'PDF' ? '📖' : '🖼️'}</Text>
                )}
            </View>
            <View style={styles.cardInfo}>
                <Text style={styles.title} numberOfLines={1}>{item.title}</Text>
                <Text style={styles.author}>{item.author}</Text>
                <Text style={styles.price}>${item.price.toFixed(2)}</Text>
            </View>
        </TouchableOpacity>
    );

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.headerLogo}>💎 Aura Digital</Text>
                <Text style={styles.headerSubtitle}>Curated eBooks & Art</Text>
            </View>
            {loading ? (
                <ActivityIndicator size="large" color="#1f2838" style={{ marginTop: 50 }} />
            ) : (
                <FlatList
                    data={products}
                    keyExtractor={(item) => item.id.toString()}
                    renderItem={renderItem}
                    numColumns={2}
                    contentContainerStyle={styles.list}
                    columnWrapperStyle={styles.row}
                />
            )}
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f6f7f7' },
    header: { padding: 20, paddingTop: 40, backgroundColor: '#fff', borderBottomWidth: 1, borderColor: '#eee' },
    headerLogo: { fontSize: 24, fontWeight: '800', color: '#1f2838', marginBottom: 5 },
    headerSubtitle: { fontSize: 14, color: '#666' },
    list: { padding: 15 },
    row: { justifyContent: 'space-between' },
    card: {
        width: '48%', backgroundColor: '#fff', borderRadius: 12,
        marginBottom: 15, padding: 10,
        shadowColor: '#000', shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.05, shadowRadius: 4, elevation: 2
    },
    imagePlaceholder: {
        backgroundColor: '#f0f2f5', height: 150, borderRadius: 8,
        alignItems: 'center', justifyContent: 'center', marginBottom: 10,
        overflow: 'hidden'
    },
    thumbnailImage: { width: '100%', height: '100%', resizeMode: 'cover' },
    iconText: { fontSize: 40 },
    cardInfo: { gap: 4 },
    title: { fontSize: 15, fontWeight: '700', color: '#1f2838' },
    author: { fontSize: 12, color: '#888' },
    price: { fontSize: 15, fontWeight: '800', color: '#1f2838', marginTop: 4 },
});
