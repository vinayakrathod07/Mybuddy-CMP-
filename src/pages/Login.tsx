import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { GraduationCap, ArrowRight, Sparkles, Lock, Mail } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const { loginWithEmail, loginWithGoogle, loginAsGuest } = useAuth();
  const { success, error } = useToast();
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      error('Please enter both email and password.');
      return;
    }
    setLoading(true);
    try {
      await loginWithEmail(email, password);
      success('Welcome back to AcademiaLife!');
      navigate('/');
    } catch (err: any) {
      error(err.message || 'Invalid email or password. Please check your credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    setLoading(true);
    try {
      await loginWithGoogle();
      success('Signed in with Google successfully!');
      navigate('/');
    } catch (err: any) {
      error(err.message || 'Google sign in was cancelled or failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickStudentLogin = async () => {
    setLoading(true);
    try {
      // Sign in anonymously as a student
      await loginAsGuest();
      success('Logged in as Student with a clean workspace!');
      navigate('/');
    } catch (err: any) {
      error('Failed to log in as guest student: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden antialiased">
      {/* Decorative gradient backdrops */}
      <div className="absolute top-0 -left-40 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 -right-40 w-96 h-96 bg-cyan-500/15 rounded-full blur-3xl pointer-events-none" />

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10 text-center px-4">
        <div className="inline-flex w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-cyan-400 items-center justify-center text-white shadow-xl shadow-indigo-500/25 mb-4">
          <GraduationCap className="w-8 h-8" />
        </div>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          Academia<span className="text-cyan-400">Life</span>
        </h2>
        <p className="mt-1.5 text-xs sm:text-sm text-slate-400">
          AI-Powered Student Academic & Life Assistant • 2026–2027
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md relative z-10 px-4">
        <div className="bg-slate-800/90 backdrop-blur-xl py-8 px-6 sm:px-10 shadow-2xl rounded-3xl border border-slate-700/80">
          {/* Quick 1-Click Student Login */}
          <div className="mb-6 space-y-2.5">
            <button
              type="button"
              onClick={handleQuickStudentLogin}
              disabled={loading}
              className="w-full flex items-center justify-center space-x-2 py-2.5 px-4 rounded-xl text-xs font-semibold text-cyan-300 bg-cyan-950/60 hover:bg-cyan-900/60 border border-cyan-800/60 transition-all shadow-xs"
            >
              <Sparkles className="w-4 h-4 text-cyan-400" />
              <span>Continue as Student (Instant Clean Workspace)</span>
            </button>

            <button
              type="button"
              onClick={handleGoogleLogin}
              disabled={loading}
              className="w-full flex items-center justify-center space-x-2 py-2.5 px-4 rounded-xl text-xs font-semibold text-slate-200 bg-slate-700/60 hover:bg-slate-700 border border-slate-600/70 transition-all shadow-xs"
            >
              <svg className="w-4 h-4" viewBox="0 0 24 24">
                <path
                  fill="#4285F4"
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                />
                <path
                  fill="#34A853"
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                />
                <path
                  fill="#EA4335"
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                />
              </svg>
              <span>Sign in with Google</span>
            </button>
          </div>

          <div className="relative my-4">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-slate-700" />
            </div>
            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-slate-800 px-2 text-slate-400 font-semibold text-[10px]">
                or email sign in
              </span>
            </div>
          </div>

          <form className="space-y-4" onSubmit={handleLogin}>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                University Email Address
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="student@university.edu"
                  required
                  className="w-full bg-slate-900/80 text-white placeholder-slate-500 pl-10 pr-4 py-2.5 rounded-xl border border-slate-700 focus:border-indigo-500 focus:outline-none text-xs transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="w-full bg-slate-900/80 text-white placeholder-slate-500 pl-10 pr-4 py-2.5 rounded-xl border border-slate-700 focus:border-indigo-500 focus:outline-none text-xs transition-colors"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center space-x-2 py-3 px-4 rounded-xl text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 transition-all shadow-lg shadow-indigo-600/30 mt-2"
            >
              <span>{loading ? 'Signing in...' : 'Sign in to Dashboard'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-slate-700/60 text-center">
            <p className="text-xs text-slate-400">
              New to AcademiaLife?{' '}
              <Link to="/register" className="font-semibold text-indigo-400 hover:text-indigo-300">
                Create student account
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
