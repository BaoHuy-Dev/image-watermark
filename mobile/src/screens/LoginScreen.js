import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { login, register } from '../api';
import { useAuth } from '../AuthContext';

export default function LoginScreen({ navigation }) {
    const [isReg, setIsReg] = useState(false);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [fullName, setFullName] = useState('');
    const [loading, setLoading] = useState(false);

    const { loginUser } = useAuth();

    const handleSubmit = async () => {
        if (!email || !password || (isReg && !fullName)) {
            Alert.alert('Error', 'Please fill all fields');
            return;
        }
        setLoading(true);
        try {
            let data;
            if (isReg) {
                data = await register(email, password, fullName);
                if (data.error) throw new Error(data.error);
            } else {
                data = await login(email, password);
            }
            loginUser({
                userId: data.userId, email: data.email,
                fullName: data.fullName, role: data.role
            }, data.token);
            navigation.goBack();
        } catch (e) {
            Alert.alert('Error', isReg ? 'Registration failed' : 'Invalid email/password');
        } finally {
            setLoading(false);
        }
    };

    return (
        <View style={styles.container}>
            <Text style={styles.logo}>💎</Text>
            <Text style={styles.title}>{isReg ? 'Create Account' : 'Welcome Back'}</Text>

            <View style={styles.form}>
                {isReg && (
                    <TextInput
                        style={styles.input}
                        placeholder="Full Name"
                        value={fullName} onChangeText={setFullName}
                    />
                )}
                <TextInput
                    style={styles.input}
                    placeholder="Email"
                    keyboardType="email-address"
                    autoCapitalize="none"
                    value={email} onChangeText={setEmail}
                />
                <TextInput
                    style={styles.input}
                    placeholder="Password"
                    secureTextEntry
                    value={password} onChangeText={setPassword}
                />
                <TouchableOpacity style={styles.btn} onPress={handleSubmit} disabled={loading}>
                    {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.btnText}>{isReg ? 'Register' : 'Sign In'}</Text>}
                </TouchableOpacity>

                <TouchableOpacity onPress={() => setIsReg(!isReg)} style={{ marginTop: 20 }}>
                    <Text style={styles.toggleText}>
                        {isReg ? 'Already have an account? Sign In' : 'Need an account? Register'}
                    </Text>
                </TouchableOpacity>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#f6f7f7', justifyContent: 'center', padding: 30 },
    logo: { fontSize: 60, textAlign: 'center', marginBottom: 10 },
    title: { fontSize: 28, fontWeight: '800', color: '#1f2838', textAlign: 'center', marginBottom: 30 },
    form: { backgroundColor: '#fff', padding: 25, borderRadius: 20, shadowColor: '#000', shadowOpacity: 0.1, shadowRadius: 10, elevation: 5 },
    input: { height: 50, borderBottomWidth: 1, borderColor: '#eee', marginBottom: 20, fontSize: 16 },
    btn: { backgroundColor: '#1f2838', height: 55, borderRadius: 12, alignItems: 'center', justifyContent: 'center', marginTop: 10 },
    btnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
    toggleText: { color: '#1f2838', textAlign: 'center', fontWeight: '600' }
});
