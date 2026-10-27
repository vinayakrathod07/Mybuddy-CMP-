import React, { useState, useEffect } from 'react';
import {
  Sparkles,
  Calendar,
  Clock,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Plus,
  BookOpen,
  Sliders,
  Trash2,
  Check,
  X,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  getStudySessions,
  getSubjects,
  createStudySession,
  updateStudySession,
  deleteStudySession,
  autoGenerateStudyPlan,
} from '../services/firebaseDb';
import { StudySession, Subject } from '../types';
import { Modal } from '../components/common/Modal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

export const StudyPlanner: React.FC = () => {
  const { user } = useAuth();
  const [sessions, setSessions] = useState<StudySession[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);

  // Modals
  const [generateModalOpen, setGenerateModalOpen] = useState(false);
  const [manualModalOpen, setManualModalOpen] = useState(false);

  // Generator Settings
  const [generateForm, setGenerateForm] = useState({
    days_ahead: 7,
    daily_available_hours: 4.0,
    break_duration_minutes: 15,
  });

  // Manual Session Form
  const [manualForm, setManualForm] = useState({
    title: '',
    subject_id: '',
    session_date: new Date().toISOString().slice(0, 10),
    start_time: '18:00',
    end_time: '19:30',
    duration_minutes: 90,
    recommendation_reason: 'Self-scheduled study block',
  });

  const { success, error, warning } = useToast();

  const loadData = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const [sData, subData] = await Promise.all([
        getStudySessions(user.uid),
        getSubjects(user.uid),
      ]);
      setSessions(sData || []);
      setSubjects(subData || []);
      if (subData && subData.length > 0 && !manualForm.subject_id) {
        setManualForm((prev) => ({ ...prev, subject_id: subData[0].id }));
      }
    } catch {
      error('Failed to load study sessions.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const handleGeneratePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setGenerating(true);
    try {
      const created = await autoGenerateStudyPlan(user.uid, generateForm);
      if (created.length === 0) {
        warning(
          'Please add courses or tasks first so the AI planner can identify study priorities and schedule revision blocks.'
        );
      } else {
        success(`Generated ${created.length} intelligent study sessions balancing your timetable!`);
        setGenerateModalOpen(false);
        loadData();
      }
    } catch {
      error('Failed to generate study plan.');
    } finally {
      setGenerating(false);
    }
  };

  const handleCreateManual = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!manualForm.title) {
      error('Session title is required.');
      return;
    }

    const sub = subjects.find((s) => s.id === manualForm.subject_id);

    try {
      await createStudySession(user.uid, {
        title: manualForm.title,
        subject_id: manualForm.subject_id || undefined,
        subject_name: sub?.name,
        subject_color: sub?.color,
        session_date: manualForm.session_date,
        start_time: manualForm.start_time,
        end_time: manualForm.end_time,
        duration_minutes: Number(manualForm.duration_minutes),
        status: 'scheduled',
        recommendation_reason: manualForm.recommendation_reason,
        is_accepted: true,
      });
      success('Study session scheduled!');
      setManualModalOpen(false);
      loadData();
    } catch {
      error('Failed to create study session.');
    }
  };

  const handleStatusUpdate = async (
    id: string,
    status: 'scheduled' | 'completed' | 'skipped'
  ) => {
    if (!user) return;
    try {
      await updateStudySession(user.uid, id, { status });
      success(`Session marked as ${status}.`);
      loadData();
    } catch {
      error('Failed to update session.');
    }
  };

  const handleDelete = async (id: string) => {
    if (!user) return;
    try {
      await deleteStudySession(user.uid, id);
      success('Session removed.');
      loadData();
    } catch {
      error('Failed to delete session.');
    }
  };

  // Group sessions by date
  const groupedSessions = sessions.reduce((acc, ses) => {
    const d = ses.session_date;
    if (!acc[d]) acc[d] = [];
    acc[d].push(ses);
    return acc;
  }, {} as Record<string, StudySession[]>);

  const sortedDates = Object.keys(groupedSessions).sort();

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 tracking-tight flex items-center space-x-2">
            <Sparkles className="w-5 h-5 text-indigo-600" />
            <span>AI Study Planner & Workload Balancer</span>
          </h1>
          <p className="text-xs text-slate-500 font-medium mt-0.5">
            Auto-schedules study blocks around your lecture timetable, prioritizing weak modules and urgent exams
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setManualModalOpen(true)}
            className="px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl transition-all shadow-xs flex items-center space-x-1.5"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Schedule Session</span>
          </button>
          <button
            onClick={() => setGenerateModalOpen(true)}
            className="px-4 py-2 text-xs font-bold text-white bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 rounded-xl transition-all shadow-xs flex items-center space-x-1.5"
          >
            <Sparkles className="w-4 h-4" />
            <span>Auto-Generate Plan</span>
          </button>
        </div>
      </div>

      {/* Content */}
      {loading ? (
        <LoadingSpinner size="md" label="Loading study plan..." className="py-12" />
      ) : sessions.length === 0 ? (
        <EmptyState
          icon={Sparkles}
          title="No study sessions scheduled yet"
          description="Click 'Auto-Generate Plan' to let the assistant inspect your registered timetable and schedule revision sessions for your urgent coursework."
          actionText="Auto-Generate Smart Plan"
          onAction={() => setGenerateModalOpen(true)}
        />
      ) : (
        <div className="space-y-6">
          {sortedDates.map((dateStr) => {
            const dateObj = new Date(dateStr);
            const isToday =
              dateStr === new Date().toISOString().slice(0, 10);
            const dateLabel = isToday
              ? `Today (${dateObj.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })})`
              : dateObj.toLocaleDateString(undefined, {
                  weekday: 'long',
                  month: 'short',
                  day: 'numeric',
                });

            return (
              <div key={dateStr} className="space-y-3">
                <div className="flex items-center space-x-2">
                  <Calendar className="w-4 h-4 text-indigo-600" />
                  <h2
                    className={`text-xs font-black uppercase tracking-wider ${
                      isToday ? 'text-indigo-600' : 'text-slate-700'
                    }`}
                  >
                    {dateLabel}
                  </h2>
                  {isToday && (
                    <span className="text-[9px] font-bold px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700">
                      Active Day
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
                  {groupedSessions[dateStr].map((ses) => (
                    <div
                      key={ses.id}
                      className={`p-4 rounded-2xl border transition-all flex flex-col justify-between ${
                        ses.status === 'completed'
                          ? 'bg-slate-50/70 border-slate-200/60 opacity-80'
                          : ses.status === 'skipped'
                          ? 'bg-rose-50/20 border-rose-100 opacity-60'
                          : 'bg-white border-slate-200/80 shadow-xs hover:border-indigo-300'
                      }`}
                    >
                      <div>
                        <div className="flex items-start justify-between mb-2">
                          <span
                            className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-md ${
                              ses.status === 'completed'
                                ? 'bg-emerald-100 text-emerald-800'
                                : ses.status === 'skipped'
                                ? 'bg-rose-100 text-rose-800'
                                : 'bg-indigo-100 text-indigo-800'
                            }`}
                          >
                            {ses.status}
                          </span>

                          <button
                            onClick={() => handleDelete(ses.id)}
                            className="text-slate-300 hover:text-rose-600 p-1 rounded-md transition-colors"
                            title="Delete session"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>

                        <h3 className="text-sm font-bold text-slate-900 leading-tight mb-1">
                          {ses.title}
                        </h3>

                        {ses.subject_name && (
                          <div className="flex items-center space-x-1.5 mb-2">
                            <span
                              className="w-2 h-2 rounded-full"
                              style={{ backgroundColor: ses.subject_color || '#4f46e5' }}
                            />
                            <span className="text-xs font-semibold text-slate-500">
                              {ses.subject_name}
                            </span>
                          </div>
                        )}

                        <div className="flex items-center space-x-2 text-xs text-slate-600 mb-3">
                          <Clock className="w-3.5 h-3.5 text-slate-400" />
                          <span>
                            {ses.start_time} - {ses.end_time} ({ses.duration_minutes}m)
                          </span>
                        </div>

                        {ses.recommendation_reason && (
                          <p className="text-[11px] text-slate-500 bg-slate-50 p-2 rounded-xl border border-slate-100 italic leading-snug">
                            "{ses.recommendation_reason}"
                          </p>
                        )}
                      </div>

                      {/* Actions */}
                      <div className="flex items-center justify-end space-x-2 pt-3 mt-3 border-t border-slate-100">
                        {ses.status !== 'completed' && (
                          <button
                            onClick={() => handleStatusUpdate(ses.id, 'completed')}
                            className="inline-flex items-center space-x-1 px-2.5 py-1 text-[11px] font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200/60 rounded-lg transition-colors"
                          >
                            <Check className="w-3 h-3" />
                            <span>Complete</span>
                          </button>
                        )}
                        {ses.status === 'scheduled' && (
                          <button
                            onClick={() => handleStatusUpdate(ses.id, 'skipped')}
                            className="inline-flex items-center space-x-1 px-2.5 py-1 text-[11px] font-bold text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                          >
                            <X className="w-3 h-3" />
                            <span>Skip</span>
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Auto-Generate Plan Modal */}
      <Modal
        isOpen={generateModalOpen}
        onClose={() => setGenerateModalOpen(false)}
        title="Auto-Generate AI Study Plan"
      >
        <form onSubmit={handleGeneratePlan} className="space-y-4">
          <div className="p-3 bg-indigo-50 border border-indigo-100 rounded-xl text-xs text-indigo-900 leading-relaxed">
            The planner checks your weekly lecture timetable to identify open evening study windows.
            It prioritizes urgent tasks and weak subjects (&lt;65% marks) without overlapping your classes.
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Days to Schedule Ahead
            </label>
            <select
              value={generateForm.days_ahead}
              onChange={(e) =>
                setGenerateForm({ ...generateForm, days_ahead: parseInt(e.target.value) })
              }
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            >
              <option value={3}>Next 3 Days</option>
              <option value={7}>Next 7 Days (1 Week)</option>
              <option value={14}>Next 14 Days (2 Weeks)</option>
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Target Daily Study Hours
              </label>
              <input
                type="number"
                min="1"
                max="8"
                step="0.5"
                value={generateForm.daily_available_hours}
                onChange={(e) =>
                  setGenerateForm({
                    ...generateForm,
                    daily_available_hours: parseFloat(e.target.value),
                  })
                }
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Break Duration (mins)
              </label>
              <input
                type="number"
                min="5"
                max="60"
                value={generateForm.break_duration_minutes}
                onChange={(e) =>
                  setGenerateForm({
                    ...generateForm,
                    break_duration_minutes: parseInt(e.target.value),
                  })
                }
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <button
              type="button"
              onClick={() => setGenerateModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={generating}
              className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs disabled:opacity-50 flex items-center space-x-1.5"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>{generating ? 'Scheduling...' : 'Generate Plan'}</span>
            </button>
          </div>
        </form>
      </Modal>

      {/* Manual Add Modal */}
      <Modal
        isOpen={manualModalOpen}
        onClose={() => setManualModalOpen(false)}
        title="Schedule Study Session"
      >
        <form onSubmit={handleCreateManual} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Session Title *</label>
            <input
              type="text"
              value={manualForm.title}
              onChange={(e) => setManualForm({ ...manualForm, title: e.target.value })}
              placeholder="e.g. Chapter 4 Practice Problems"
              required
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Subject</label>
              <select
                value={manualForm.subject_id}
                onChange={(e) => setManualForm({ ...manualForm, subject_id: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="">General Review</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Date *</label>
              <input
                type="date"
                value={manualForm.session_date}
                onChange={(e) => setManualForm({ ...manualForm, session_date: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Start Time *</label>
              <input
                type="time"
                value={manualForm.start_time}
                onChange={(e) => setManualForm({ ...manualForm, start_time: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">End Time *</label>
              <input
                type="time"
                value={manualForm.end_time}
                onChange={(e) => setManualForm({ ...manualForm, end_time: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Reason / Goals (Optional)
            </label>
            <input
              type="text"
              value={manualForm.recommendation_reason}
              onChange={(e) =>
                setManualForm({ ...manualForm, recommendation_reason: e.target.value })
              }
              placeholder="e.g. Prep for Quiz 2"
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <button
              type="button"
              onClick={() => setManualModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs"
            >
              Add to Schedule
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
