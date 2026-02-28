import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

// Use 10.0.2.2 for Android emulator to access localhost, otherwise use localhost for iOS Simulator/Web
export const BASE = Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';

async function request(url, options = {}) {
    const token = await AsyncStorage.getItem('token');
    const headers = { ...options.headers };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (options.body && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }
    const res = await fetch(BASE + url, { ...options, headers });
    if (res.status === 401) {
        await AsyncStorage.removeItem('token');
        await AsyncStorage.removeItem('user');
        return res;
    }
    return res;
}

export async function login(email, password) {
    const res = await fetch(BASE + '/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
    });
    if (!res.ok) throw new Error('Invalid credentials');
    return res.json();
}

export async function register(email, password, fullName) {
    const res = await fetch(BASE + '/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, fullName })
    });
    return res.json();
}

export async function getProducts(type, category, search) {
    const params = new URLSearchParams();
    if (type) params.set('type', type);
    if (category) params.set('category', category);
    if (search) params.set('search', search);
    const url = '/api/products' + (params.toString() ? '?' + params : '');
    const res = await fetch(BASE + url);
    return res.json();
}

export async function getFeaturedProducts() {
    const res = await fetch(BASE + '/api/products/featured');
    return res.json();
}

export async function getProduct(id) {
    const res = await fetch(BASE + '/api/products/' + id);
    return res.json();
}

export async function createOrder(productIds) {
    const res = await request('/api/orders', {
        method: 'POST',
        body: { productIds }
    });
    return res.json();
}

export async function getMyOrders() {
    const res = await request('/api/orders/my');
    return res.json();
}
