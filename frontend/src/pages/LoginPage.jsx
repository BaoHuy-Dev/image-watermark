import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, register } from '../api';
import { useAuth } from '../AuthContext';

function LoginPage() {
    const [isRegister, setIsRegister] = useState(false);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [fullName, setFullName] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { loginUser } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            let data;
            if (isRegister) {
                data = await register(email, password, fullName);
                if (data.error) { setError(data.error); return; }
            } else {
                data = await login(email, password);
            }
            loginUser({
                userId: data.userId, email: data.email,
                fullName: data.fullName, role: data.role
            }, data.token);
            navigate('/');
        } catch (err) {
            setError(isRegister ? 'Registration failed' : 'Invalid email or password');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-[70vh] flex items-center justify-center px-6 py-16">
            <div className="w-full max-w-md">
                <div className="text-center mb-8">
                    <span className="material-symbols-outlined text-primary text-5xl mb-4 block">diamond</span>
                    <h1 className="text-3xl font-extrabold text-primary">
                        {isRegister ? 'Create Account' : 'Welcome Back'}
                    </h1>
                    <p className="text-primary/50 mt-2">
                        {isRegister ? 'Join the digital marketplace' : 'Sign in to your account'}
                    </p>
                </div>

                <div className="bg-white rounded-2xl shadow-xl border border-primary/5 p-8">
                    {error && (
                        <div className="bg-red-50 text-red-600 text-sm p-4 rounded-xl mb-6 flex items-center gap-2">
                            <span className="material-symbols-outlined text-sm">error</span> {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                        {isRegister && (
                            <div>
                                <label className="text-sm font-medium text-primary/60 block mb-1.5">Full Name</label>
                                <input type="text" value={fullName} onChange={e => setFullName(e.target.value)}
                                    placeholder="Alexander Vance"
                                    className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm focus:ring-2 focus:ring-primary/20 focus:border-primary/30 transition-all" />
                            </div>
                        )}
                        <div>
                            <label className="text-sm font-medium text-primary/60 block mb-1.5">Email</label>
                            <input type="email" value={email} onChange={e => setEmail(e.target.value)}
                                placeholder="alex@aura.digital" required
                                className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm focus:ring-2 focus:ring-primary/20 focus:border-primary/30 transition-all" />
                        </div>
                        <div>
                            <label className="text-sm font-medium text-primary/60 block mb-1.5">Password</label>
                            <input type="password" value={password} onChange={e => setPassword(e.target.value)}
                                placeholder="••••••••" required
                                className="w-full h-12 rounded-xl border border-primary/10 px-4 text-sm focus:ring-2 focus:ring-primary/20 focus:border-primary/30 transition-all" />
                        </div>
                        <button type="submit" disabled={loading}
                            className="bg-primary text-white h-14 rounded-xl font-bold text-lg hover:shadow-lg hover:shadow-primary/20 transition-all mt-2 disabled:opacity-50">
                            {loading ? 'Processing...' : isRegister ? 'Create Account' : 'Sign In'}
                        </button>
                    </form>

                    <div className="text-center mt-6 text-sm text-primary/50">
                        {isRegister ? 'Already have an account?' : "Don't have an account?"}
                        <button onClick={() => { setIsRegister(!isRegister); setError(''); }}
                            className="ml-1 text-primary font-bold hover:underline">
                            {isRegister ? 'Sign In' : 'Register'}
                        </button>
                    </div>
                </div>

                <div className="text-center mt-6 text-xs text-primary/30 bg-primary/5 rounded-xl p-4">
                    <p>Demo: <code className="bg-primary/10 px-2 py-0.5 rounded text-primary/60">alex@aura.digital</code> / <code className="bg-primary/10 px-2 py-0.5 rounded text-primary/60">demo123</code></p>
                </div>
            </div>
        </div>
    );
}

export default LoginPage;
