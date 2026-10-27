import React, { useState, useEffect } from 'react';
import {
  User,
  Mail,
  GraduationCap,
  Building,
  Calendar,
  Lock,
  Save,
  Clock,
  Target,
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

export const Profile: React.FC = () => {
  const { profile, updateLocalProfile } = useAuth();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    full_name: '',
    college: '',
    course: '',
    semester: '',
    academic_year: '',
    preferred_study_hours_per_day: 4.0,
    preferred_break_duration_minutes: 15,
    minimum_attendance_target: 75.0,
  });

  const { success, error } = useToast();

  useEffect(() => {
    if (profile) {
      setFormData({
        full_name: profile.full_name || '',
        college: profile.college || '',
        course: profile.course || '',
        semester: profile.semester || '',
        academic_year: profile.academic_year || '2026-2027',
        preferred_study_hours_per_day: profile.preferred_study_hours_per_day ?? 4.0,
        preferred_break_duration_minutes: profile.preferred_break_duration_minutes ?? 15,
        minimum_attendance_target: profile.minimum_attendance_target ?? 75.0,
      });
    }
  }, [profile]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!profile) return;
    setLoading(true);
    try {
      await updateLocalProfile({
        full_name: formData.full_name,
        college: formData.college,
        course: formData.course,
        semester: formData.semester,
        academic_year: formData.academic_year,
        preferred_study_hours_per_day: Number(formData.preferred_study_hours_per_day),
        preferred_break_duration_minutes: Number(formData.preferred_break_duration_minutes),
        minimum_attendance_target: Number(formData.minimum_attendance_target),
      });
      success('Student academic profile updated in Firebase!');
    } catch {
      error('Failed to update profile.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h1 className="text-xl font-black text-slate-900 tracking-tight flex items-center space-x-2">
          <User className="w-5 h-5 text-indigo-600" />
          <span>Student Academic Profile</span>
        </h1>
        <p className="text-xs text-slate-500 font-medium mt-0.5">
          Configure university enrollment credentials, semester details, and academic compliance thresholds
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Basic Info Card */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center space-x-3 pb-3 border-b border-slate-100">
            <div className="w-12 h-12 rounded-2xl bg-indigo-50 border border-indigo-100 text-indigo-700 flex items-center justify-center font-black text-lg">
              {formData.full_name ? formData.full_name[0] : 'S'}
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-900 leading-tight">
                {formData.full_name || 'Student Profile'}
              </h2>
              <p className="text-xs text-slate-500">{profile?.email}</p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Student Full Name *
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={formData.full_name}
                  onChange={(e) => setFormData({ ...formData, full_name: e.target.value })}
                  required
                  className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                University Email Address (Read-Only)
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  disabled
                  value={profile?.email || ''}
                  className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-slate-200 bg-slate-50 text-slate-500 cursor-not-allowed"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                College / University
              </label>
              <div className="relative">
                <Building className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={formData.college}
                  onChange={(e) => setFormData({ ...formData, college: e.target.value })}
                  placeholder="e.g. National Institute of Technology"
                  className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Degree / Program
              </label>
              <div className="relative">
                <GraduationCap className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={formData.course}
                  onChange={(e) => setFormData({ ...formData, course: e.target.value })}
                  placeholder="e.g. B.Tech Computer Science"
                  className="w-full pl-9 pr-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                />
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Current Semester</label>
              <select
                value={formData.semester}
                onChange={(e) => setFormData({ ...formData, semester: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
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
              <label className="block text-xs font-semibold text-slate-700 mb-1">Academic Year</label>
              <input
                type="text"
                value={formData.academic_year}
                onChange={(e) => setFormData({ ...formData, academic_year: e.target.value })}
                placeholder="2026-2027"
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>
        </div>

        {/* Academic Targets & Habits Card */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center space-x-2 pb-3 border-b border-slate-100">
            <Target className="w-4 h-4 text-indigo-600" />
            <h2 className="text-sm font-bold text-slate-900">Academic Goals & Habits</h2>
          </div>

          <div>
            <div className="flex items-center justify-between text-xs mb-1">
              <span className="font-semibold text-slate-700">Minimum Attendance Target %</span>
              <span className="font-black text-indigo-600 text-sm">
                {formData.minimum_attendance_target}%
              </span>
            </div>
            <input
              type="range"
              min="60"
              max="95"
              step="1"
              value={formData.minimum_attendance_target}
              onChange={(e) =>
                setFormData({ ...formData, minimum_attendance_target: parseFloat(e.target.value) })
              }
              className="w-full accent-indigo-600 cursor-pointer"
            />
            <p className="text-[11px] text-slate-400 mt-1">
              The compliance calculator flags any courses falling below this percentage threshold.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Target Daily Study Hours
              </label>
              <input
                type="number"
                step="0.5"
                min="1"
                max="8"
                value={formData.preferred_study_hours_per_day}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    preferred_study_hours_per_day: parseFloat(e.target.value),
                  })
                }
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Break Duration Between Study Blocks (mins)
              </label>
              <input
                type="number"
                step="5"
                min="5"
                max="60"
                value={formData.preferred_break_duration_minutes}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    preferred_break_duration_minutes: parseInt(e.target.value),
                  })
                }
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>
        </div>

        {/* Submit */}
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-2.5 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-xs flex items-center space-x-1.5 disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>{loading ? 'Saving Profile...' : 'Save Profile Changes'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};
