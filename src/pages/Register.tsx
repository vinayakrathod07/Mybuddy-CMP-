import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { GraduationCap, ArrowRight, Lock, Mail, User, BookOpen } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';

export const Register: React.FC = () => {
  const [formData, setFormData] = useState({
    full_name: '',
    email: '',
    password: '',
    college: 'National Institute of Technology',
    course: 'B.Tech Computer Science & Engineering',
    semester: 'Semester 6',
    academic_year: '2026-2027',
  });
  const [loading, setLoading] = useState(false);
  const { registerWithEmail } = useAuth();
  const { success, error } = useToast();
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.email || !formData.password || !formData.full_name) {
      error('Please fill in all required fields.');
      return;
    }
    if (formData.password.length < 6) {
      error('Password must be at least 6 characters.');
      return;
    }
    setLoading(true);
    try {
      await registerWithEmail(formData.email, formData.password, {
        full_name: formData.full_name,
        college: formData.college,
        course: formData.course,
        semester: formData.semester,
        academic_year: formData.academic_year,
      });
      success(`Welcome to AcademiaLife, ${formData.full_name}! Your student workspace is ready.`);
      navigate('/');
    } catch (err: any) {
      error(err.message || 'Registration failed. Email may already be registered.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden antialiased">
      <div className="absolute top-0 -left-40 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 -right-40 w-96 h-96 bg-cyan-500/15 rounded-full blur-3xl pointer-events-none" />

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10 text-center px-4">
        <div className="inline-flex w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-cyan-400 items-center justify-center text-white shadow-xl shadow-indigo-500/25 mb-4">
          <GraduationCap className="w-8 h-8" />
        </div>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          Register Student Profile
        </h2>
        <p className="mt-1.5 text-xs sm:text-sm text-slate-400">
          Join your personalized 2026–2027 academic hub
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md relative z-10 px-4">
        <div className="bg-slate-800/90 backdrop-blur-xl py-8 px-6 sm:px-10 shadow-2xl rounded-3xl border border-slate-700/80">
          <form className="space-y-4" onSubmit={handleRegister}>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Student Full Name *
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  name="full_name"
                  value={formData.full_name}
                  onChange={handleChange}
                  placeholder="e.g. Alex Sharma"
                  required
                  className="w-full bg-slate-900/80 text-white placeholder-slate-500 pl-10 pr-4 py-2.5 rounded-xl border border-slate-700 focus:border-indigo-500 focus:outline-none text-xs"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                University Email Address *
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  placeholder="alex.s@university.edu"
                  required
                  className="w-full bg-slate-900/80 text-white placeholder-slate-500 pl-10 pr-4 py-2.5 rounded-xl border border-slate-700 focus:border-indigo-500 focus:outline-none text-xs"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Password * (min 6 characters)
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••"
                  required
                  minLength={6}
                  className="w-full bg-slate-900/80 text-white placeholder-slate-500 pl-10 pr-4 py-2.5 rounded-xl border border-slate-700 focus:border-indigo-500 focus:outline-none text-xs"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Semester
                </label>
                <select
                  name="semester"
                  value={formData.semester}
                  onChange={handleChange}
                  className="w-full bg-slate-900/80 text-white px-3 py-2.5 rounded-xl border border-slate-700 focus:border-indigo-500 focus:outline-none text-xs"
                >
                  <option value="Semester 1">Semester 1</option>
                  <option value="Semester 2">Semester 2</option>
                  <option value="Semester 3">Semester 3</option>
                  <option value="Semester 4">Semester 4</option>
                  <option value="Semester 5">Semester 5</option>
                  <option value="Semester 6">Semester 6</option>
                  <option value="Semester 7">Semester 7</option>
                  <option value="Semester 8">Semester 8</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">
                  Academic Year
                </label>
                <input
                  type="text"
                  name="academic_year"
                  value={formData.academic_year}
                  onChange={handleChange}
                  placeholder="2026-2027"
                  className="w-full bg-slate-900/80 text-white px-3 py-2.5 rounded-xl border border-slate-700 focus:border-indigo-500 focus:outline-none text-xs"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Course / Major
              </label>
              <input
                type="text"
                name="course"
                value={formData.course}
                onChange={handleChange}
                placeholder="B.Tech Computer Science & Engineering"
                className="w-full bg-slate-900/80 text-white px-3 py-2.5 rounded-xl border border-slate-700 focus:border-indigo-500 focus:outline-none text-xs"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center space-x-2 py-3 px-4 rounded-xl text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 transition-all shadow-lg shadow-indigo-600/30 mt-3"
            >
              <span>{loading ? 'Registering...' : 'Create Clean Account'}</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-slate-700/60 text-center">
            <p className="text-xs text-slate-400">
              Already have an account?{' '}
              <Link to="/login" className="font-semibold text-indigo-400 hover:text-indigo-300">
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
