import React, { useState, useEffect } from 'react';
import {
  Settings as SettingsIcon,
  Bell,
  Database,
  Key,
  ShieldAlert,
  Save,
  CheckCircle2,
  Cpu,
  RefreshCw,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { getUserPreferences, updateUserPreferences } from '../services/firebaseDb';
import { UserPreference } from '../types';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import firebaseConfig from '../../firebase-applet-config.json';

export const Settings: React.FC = () => {
  const { user } = useAuth();
  const [prefs, setPrefs] = useState<UserPreference | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const { success, error } = useToast();

  const loadPreferences = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const data = await getUserPreferences(user.uid);
      setPrefs(data);
    } catch {
      error('Failed to load user preferences.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPreferences();
  }, [user]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !prefs) return;
    setSaving(true);
    try {
      await updateUserPreferences(user.uid, prefs);
      success('Academic settings saved successfully!');
    } catch {
      error('Failed to save preferences.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <LoadingSpinner size="md" label="Loading preferences..." className="py-12" />;
  }

  if (!prefs) {
    return null;
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h1 className="text-xl font-black text-slate-900 tracking-tight flex items-center space-x-2">
          <SettingsIcon className="w-5 h-5 text-indigo-600" />
          <span>Application Settings & Preferences</span>
        </h1>
        <p className="text-xs text-slate-500 font-medium mt-0.5">
          Configure notifications, study plan automation, and examine active Firebase backend credentials
        </p>
      </div>

      <form onSubmit={handleSave} className="space-y-6">
        {/* Academic Alert Preferences */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center space-x-2 pb-3 border-b border-slate-100">
            <Bell className="w-4 h-4 text-indigo-600" />
            <h2 className="text-sm font-bold text-slate-900">Notification & Alert Rules</h2>
          </div>

          <div className="space-y-3.5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-bold text-slate-900">Audio Feedback & Chimes</p>
                <p className="text-[11px] text-slate-500">
                  Play notification sounds on task completions and timetable warnings
                </p>
              </div>
              <input
                type="checkbox"
                checked={prefs.sound_enabled}
                onChange={(e) => setPrefs({ ...prefs, sound_enabled: e.target.checked })}
                className="w-4 h-4 accent-indigo-600 cursor-pointer"
              />
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-bold text-slate-900">Deadline Warning Alert</p>
                <p className="text-[11px] text-slate-500">
                  Generate in-app warning banners for upcoming tasks and exams
                </p>
              </div>
              <select
                value={prefs.reminder_hours_before_deadline}
                onChange={(e) =>
                  setPrefs({
                    ...prefs,
                    reminder_hours_before_deadline: parseInt(e.target.value),
                  })
                }
                className="text-xs px-3 py-1.5 rounded-xl border border-slate-200 bg-white"
              >
                <option value={12}>12 Hours Before</option>
                <option value={24}>24 Hours Before (1 Day)</option>
                <option value={48}>48 Hours Before (2 Days)</option>
                <option value={72}>72 Hours Before (3 Days)</option>
              </select>
            </div>

            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-bold text-slate-900">Smart Study Plan Automation</p>
                <p className="text-[11px] text-slate-500">
                  Allow assistant to automatically recommend review sessions for weak subjects (&lt;65%)
                </p>
              </div>
              <input
                type="checkbox"
                checked={prefs.auto_generate_study_plans}
                onChange={(e) =>
                  setPrefs({ ...prefs, auto_generate_study_plans: e.target.checked })
                }
                className="w-4 h-4 accent-indigo-600 cursor-pointer"
              />
            </div>
          </div>
        </div>

        {/* Backend Infrastructure Info */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center space-x-2 pb-3 border-b border-slate-100">
            <Database className="w-4 h-4 text-emerald-600" />
            <h2 className="text-sm font-bold text-slate-900">Active Firebase Backend Status</h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
            <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Firestore Project ID
              </span>
              <span className="font-mono font-bold text-slate-900 mt-0.5 block truncate">
                {firebaseConfig.projectId}
              </span>
            </div>

            <div className="p-3.5 bg-slate-50 rounded-2xl border border-slate-100">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Database Instance
              </span>
              <span className="font-mono font-bold text-slate-900 mt-0.5 block truncate">
                {firebaseConfig.firestoreDatabaseId}
              </span>
            </div>
          </div>

          <div className="p-3.5 bg-emerald-50 rounded-2xl border border-emerald-100 flex items-start space-x-2 text-xs text-emerald-900">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 mt-0.5 shrink-0" />
            <div>
              <span className="font-bold">Persistent Cloud Storage Active: </span>
              Your account operates with zero sample data. All courses, timetable slots, attendance records, test marks, and vault documents are stored and synced in your private Firestore collection!
            </div>
          </div>
        </div>

        {/* Save Button */}
        <div className="flex justify-end">
          <button
            type="submit"
            disabled={saving}
            className="px-6 py-2.5 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-xs flex items-center space-x-1.5 disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            <span>{saving ? 'Saving...' : 'Save Preferences'}</span>
          </button>
        </div>
      </form>
    </div>
  );
};
