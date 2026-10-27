import React, { useState, useEffect } from 'react';
import {
  ClipboardCheck,
  Plus,
  AlertTriangle,
  CheckCircle2,
  Calendar,
  XCircle,
  Clock,
  Filter,
  Trash2,
  HelpCircle,
  TrendingUp,
  BookOpen,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  getAttendanceRecords,
  getSubjects,
  recordAttendance,
  deleteAttendanceRecord,
  computeAttendanceSummary,
  createSubject,
} from '../services/firebaseDb';
import { OverallAttendanceSummary, AttendanceRecord, Subject } from '../types';
import { Modal } from '../components/common/Modal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

export const Attendance: React.FC = () => {
  const { user, profile } = useAuth();
  const [summary, setSummary] = useState<OverallAttendanceSummary | null>(null);
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters
  const [filterSubjectId, setFilterSubjectId] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  // Modal
  const [logModalOpen, setLogModalOpen] = useState(false);
  const [subjectModalOpen, setSubjectModalOpen] = useState(false);
  const [formData, setFormData] = useState({
    subject_id: '',
    date: new Date().toISOString().slice(0, 10),
    status: 'present' as 'present' | 'absent' | 'leave',
    notes: '',
  });

  // Subject Modal
  const [newSubject, setNewSubject] = useState({
    name: '',
    code: '',
    color: '#4f46e5',
    target_attendance: 75.0,
  });

  const { success, error } = useToast();

  const loadData = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const [recData, subData] = await Promise.all([
        getAttendanceRecords(user.uid, {
          subject_id: filterSubjectId === 'all' ? undefined : filterSubjectId,
          status_filter: filterStatus,
        }),
        getSubjects(user.uid),
      ]);

      const subs = subData || [];
      const recs = recData || [];
      const sum = computeAttendanceSummary(
        subs,
        recs,
        profile?.minimum_attendance_target || 75.0
      );

      setSummary(sum);
      setRecords(recs);
      setSubjects(subs);
      if (subs.length > 0 && !formData.subject_id) {
        setFormData((prev) => ({ ...prev, subject_id: subs[0].id }));
      }
    } catch {
      error('Failed to load attendance records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user, filterSubjectId, filterStatus]);

  const handleQuickMark = async (
    subject: Subject,
    status: 'present' | 'absent' | 'leave'
  ) => {
    if (!user) return;
    try {
      await recordAttendance(user.uid, {
        subject_id: subject.id,
        subject_name: subject.name,
        subject_code: subject.code,
        date: new Date().toISOString().slice(0, 10),
        status,
      });
      success(`Logged "${status.toUpperCase()}" for ${subject.name}!`);
      loadData();
    } catch {
      error('Failed to log attendance.');
    }
  };

  const handleDeleteRecord = async (id: string) => {
    if (!user) return;
    if (!window.confirm('Delete this attendance entry?')) return;
    try {
      await deleteAttendanceRecord(user.uid, id);
      success('Attendance entry removed.');
      loadData();
    } catch {
      error('Failed to delete attendance record.');
    }
  };

  const handleLogSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!formData.subject_id) {
      error('Please select a subject.');
      return;
    }
    const sub = subjects.find((s) => s.id === formData.subject_id);
    try {
      await recordAttendance(user.uid, {
        subject_id: formData.subject_id,
        subject_name: sub?.name,
        subject_code: sub?.code,
        date: formData.date,
        status: formData.status,
        notes: formData.notes,
      });
      success('Attendance marked successfully.');
      setLogModalOpen(false);
      loadData();
    } catch {
      error('Failed to record attendance.');
    }
  };

  const handleCreateSubject = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!newSubject.name || !newSubject.code) {
      error('Subject name and code are required.');
      return;
    }
    try {
      const created = await createSubject(user.uid, {
        name: newSubject.name,
        code: newSubject.code,
        color: newSubject.color,
        target_attendance: Number(newSubject.target_attendance) || 75.0,
      });
      success(`Course "${created.name}" created!`);
      setNewSubject({
        name: '',
        code: '',
        color: '#4f46e5',
        target_attendance: 75.0,
      });
      setSubjectModalOpen(false);
      await loadData();
      setFormData((prev) => ({ ...prev, subject_id: created.id }));
    } catch {
      error('Failed to create subject.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 tracking-tight flex items-center space-x-2">
            <ClipboardCheck className="w-5 h-5 text-indigo-600" />
            <span>Attendance Compliance & Target Calculator</span>
          </h1>
          <p className="text-xs text-slate-500 font-medium mt-0.5">
            Strict deterministic formula showing exact consecutive classes needed to maintain ≥ 75% target
          </p>
        </div>

        <div className="flex items-center space-x-2">
          {subjects.length === 0 && (
            <button
              onClick={() => setSubjectModalOpen(true)}
              className="px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl transition-all shadow-xs flex items-center space-x-1.5"
            >
              <BookOpen className="w-3.5 h-3.5 text-indigo-600" />
              <span>Add Subject</span>
            </button>
          )}
          <button
            onClick={() => setLogModalOpen(true)}
            className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-xs flex items-center space-x-1.5 self-start sm:self-auto"
          >
            <Plus className="w-4 h-4" />
            <span>Log Attendance Session</span>
          </button>
        </div>
      </div>

      {/* Top Compliance Stats Banner */}
      {summary && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 items-center">
            {/* Gauge */}
            <div className="flex items-center space-x-4 md:border-r border-slate-100 pr-4">
              <div
                className={`w-16 h-16 rounded-2xl flex items-center justify-center font-black text-xl text-white shadow-md ${
                  summary.overall_percentage >= summary.target_percentage
                    ? 'bg-emerald-600 shadow-emerald-500/20'
                    : summary.overall_percentage >= summary.target_percentage - 10
                    ? 'bg-amber-500 shadow-amber-500/20'
                    : 'bg-rose-600 shadow-rose-500/20'
                }`}
              >
                {summary.overall_percentage}%
              </div>
              <div>
                <span className="text-xs font-bold text-slate-900 block">
                  Overall Compliance
                </span>
                <span
                  className={`text-[11px] font-semibold ${
                    summary.is_warning ? 'text-amber-600' : 'text-emerald-600'
                  }`}
                >
                  {summary.is_warning ? 'Shortage Alert' : 'Safely Compliant'}
                </span>
                <span className="text-[10px] text-slate-400 block mt-0.5">
                  Target: {summary.target_percentage}% minimum
                </span>
              </div>
            </div>

            {/* Breakdown numbers */}
            <div className="grid grid-cols-3 gap-3 md:col-span-3">
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-center">
                <span className="text-lg font-black text-slate-900 block">
                  {summary.total_conducted}
                </span>
                <span className="text-[10px] font-semibold text-slate-500 uppercase">
                  Conducted
                </span>
              </div>
              <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-100 text-center">
                <span className="text-lg font-black text-emerald-700 block">
                  {summary.total_present}
                </span>
                <span className="text-[10px] font-semibold text-emerald-600 uppercase">
                  Attended
                </span>
              </div>
              <div className="p-3 bg-rose-50 rounded-xl border border-rose-100 text-center">
                <span className="text-lg font-black text-rose-700 block">
                  {summary.total_absent}
                </span>
                <span className="text-[10px] font-semibold text-rose-600 uppercase">
                  Missed
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Subject-Wise Attendance Cards with Quick Actions */}
      <div>
        <h2 className="text-sm font-bold text-slate-900 mb-3">Course Attendance Standing</h2>
        {loading ? (
          <LoadingSpinner size="md" label="Calculating attendance compliance..." className="py-8" />
        ) : subjects.length === 0 ? (
          <EmptyState
            icon={BookOpen}
            title="No courses registered yet"
            description="Add your semester subjects to start logging class attendance and compute compliance standing."
            actionText="Add Course / Subject"
            onAction={() => setSubjectModalOpen(true)}
          />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {summary?.subject_summaries.map((sub) => {
              const fullSub = subjects.find((s) => s.id === sub.subject_id);
              return (
                <div
                  key={sub.subject_id}
                  className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs hover:border-slate-300 transition-all flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-start justify-between mb-2">
                      <div className="flex items-center space-x-2">
                        <span
                          className="w-3 h-3 rounded-full shrink-0"
                          style={{ backgroundColor: sub.color || '#4f46e5' }}
                        />
                        <span className="text-xs font-bold text-slate-900">{sub.subject_code}</span>
                      </div>
                      <span
                        className={`text-[10px] font-black uppercase px-2 py-0.5 rounded-md ${
                          sub.status === 'safe'
                            ? 'bg-emerald-100 text-emerald-800'
                            : sub.status === 'warning'
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-rose-100 text-rose-800'
                        }`}
                      >
                        {sub.status}
                      </span>
                    </div>

                    <h3 className="text-sm font-bold text-slate-900 leading-tight mb-2">
                      {sub.subject_name}
                    </h3>

                    <div className="flex items-baseline space-x-2 mb-2">
                      <span className="text-2xl font-black text-slate-900">{sub.percentage}%</span>
                      <span className="text-xs text-slate-400">
                        ({sub.present_count}/{sub.total_classes} attended)
                      </span>
                    </div>

                    {/* Progress Bar */}
                    <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden mb-3">
                      <div
                        className={`h-full rounded-full transition-all duration-500 ${
                          sub.status === 'safe'
                            ? 'bg-emerald-500'
                            : sub.status === 'warning'
                            ? 'bg-amber-500'
                            : 'bg-rose-500'
                        }`}
                        style={{ width: `${Math.min(100, sub.percentage)}%` }}
                      />
                    </div>

                    <p className="text-[11px] text-slate-600 bg-slate-50 p-2.5 rounded-xl border border-slate-100 leading-relaxed mb-4">
                      {sub.explanation}
                    </p>
                  </div>

                  {/* 1-Click Quick Action Buttons */}
                  <div className="grid grid-cols-3 gap-2 pt-3 border-t border-slate-100">
                    <button
                      onClick={() => fullSub && handleQuickMark(fullSub, 'present')}
                      className="py-1.5 px-2 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-bold rounded-xl transition-colors border border-emerald-200/60 flex items-center justify-center space-x-1"
                    >
                      <span>+ Present</span>
                    </button>
                    <button
                      onClick={() => fullSub && handleQuickMark(fullSub, 'absent')}
                      className="py-1.5 px-2 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-bold rounded-xl transition-colors border border-rose-200/60 flex items-center justify-center space-x-1"
                    >
                      <span>+ Absent</span>
                    </button>
                    <button
                      onClick={() => fullSub && handleQuickMark(fullSub, 'leave')}
                      className="py-1.5 px-2 bg-amber-50 hover:bg-amber-100 text-amber-700 text-xs font-bold rounded-xl transition-colors border border-amber-200/60 flex items-center justify-center space-x-1"
                    >
                      <span>+ Leave</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Attendance History Ledger */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <h2 className="text-sm font-bold text-slate-900">Logged Attendance Sessions</h2>

          <div className="flex items-center space-x-2">
            <select
              value={filterSubjectId}
              onChange={(e) => setFilterSubjectId(e.target.value)}
              className="text-xs px-3 py-1.5 rounded-xl border border-slate-200 bg-white"
            >
              <option value="all">All Subjects</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>

            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="text-xs px-3 py-1.5 rounded-xl border border-slate-200 bg-white"
            >
              <option value="all">All Statuses</option>
              <option value="present">Present</option>
              <option value="absent">Absent</option>
              <option value="leave">Leave</option>
            </select>
          </div>
        </div>

        {records.length === 0 ? (
          <p className="text-xs text-slate-400 text-center py-8">
            No attendance sessions logged matching the filter. Use the "+ Present" buttons above to quickly log today's classes.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 border-b border-slate-100">
                <tr>
                  <th className="py-2.5 px-3 font-semibold">Date</th>
                  <th className="py-2.5 px-3 font-semibold">Subject</th>
                  <th className="py-2.5 px-3 font-semibold">Status</th>
                  <th className="py-2.5 px-3 font-semibold">Notes</th>
                  <th className="py-2.5 px-3 font-semibold text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {records.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="py-2.5 px-3 font-medium text-slate-800">{r.date}</td>
                    <td className="py-2.5 px-3 font-bold text-slate-900">
                      {r.subject_name || r.subject_code}
                    </td>
                    <td className="py-2.5 px-3">
                      <span
                        className={`px-2 py-0.5 rounded-md font-bold uppercase text-[10px] ${
                          r.status === 'present'
                            ? 'bg-emerald-100 text-emerald-800'
                            : r.status === 'absent'
                            ? 'bg-rose-100 text-rose-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {r.status}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-slate-500 italic max-w-xs truncate">
                      {r.notes || '—'}
                    </td>
                    <td className="py-2.5 px-3 text-right">
                      <button
                        onClick={() => handleDeleteRecord(r.id)}
                        className="p-1 text-slate-400 hover:text-rose-600 rounded-md transition-colors"
                        title="Delete log"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Log Modal */}
      <Modal
        isOpen={logModalOpen}
        onClose={() => setLogModalOpen(false)}
        title="Log Course Attendance Session"
      >
        <form onSubmit={handleLogSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Subject *</label>
            {subjects.length === 0 ? (
              <p className="text-xs text-amber-700 bg-amber-50 p-2.5 rounded-xl border border-amber-200">
                Please add a course first.
              </p>
            ) : (
              <select
                value={formData.subject_id}
                onChange={(e) => setFormData({ ...formData, subject_id: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.code})
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Date *</label>
              <input
                type="date"
                value={formData.date}
                onChange={(e) => setFormData({ ...formData, date: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Status *</label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="present">Present</option>
                <option value="absent">Absent</option>
                <option value="leave">Approved Leave</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Notes (Optional)
            </label>
            <input
              type="text"
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              placeholder="e.g. Covered Chapter 4 lecture"
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <button
              type="button"
              onClick={() => setLogModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs"
            >
              Save Attendance Record
            </button>
          </div>
        </form>
      </Modal>

      {/* Subject Modal */}
      <Modal
        isOpen={subjectModalOpen}
        onClose={() => setSubjectModalOpen(false)}
        title="Add Academic Subject"
      >
        <form onSubmit={handleCreateSubject} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Subject Name *</label>
            <input
              type="text"
              value={newSubject.name}
              onChange={(e) => setNewSubject({ ...newSubject, name: e.target.value })}
              placeholder="e.g. Computer Networks"
              required
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Course Code *</label>
              <input
                type="text"
                value={newSubject.code}
                onChange={(e) => setNewSubject({ ...newSubject, code: e.target.value.toUpperCase() })}
                placeholder="e.g. CS302"
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Target Attendance %
              </label>
              <input
                type="number"
                min="50"
                max="100"
                value={newSubject.target_attendance}
                onChange={(e) =>
                  setNewSubject({ ...newSubject, target_attendance: parseFloat(e.target.value) })
                }
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <button
              type="button"
              onClick={() => setSubjectModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs"
            >
              Save Course
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
