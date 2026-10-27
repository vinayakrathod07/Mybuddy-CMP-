import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Sparkles,
  CalendarDays,
  Clock,
  CheckCircle2,
  AlertTriangle,
  BookOpen,
  TrendingUp,
  BarChart2,
  Plus,
  ArrowRight,
  GraduationCap,
  ChevronRight,
  Layers,
  Award,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { getDashboardSummary, updateTaskStatus } from '../services/firebaseDb';
import { DashboardSummary, Task } from '../types';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const { success, error } = useToast();

  const loadDashboard = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const summary = await getDashboardSummary(user.uid);
      setData(summary);
    } catch (err: any) {
      error('Failed to load dashboard data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, [user]);

  const handleToggleTask = async (task: Task) => {
    if (!user) return;
    try {
      const newStatus = task.status === 'completed' ? 'pending' : 'completed';
      await updateTaskStatus(user.uid, task.id, newStatus);
      success(newStatus === 'completed' ? 'Task marked as completed!' : 'Task re-opened.');
      loadDashboard();
    } catch (err) {
      error('Failed to update task status.');
    }
  };

  if (loading) {
    return (
      <LoadingSpinner
        size="lg"
        label="Preparing your personalized academic briefing..."
        className="py-24"
      />
    );
  }

  if (!data) {
    return (
      <EmptyState
        icon={BookOpen}
        title="Could not load dashboard"
        description="Unable to connect to the Firebase database. Please check your network."
        actionText="Retry"
        onAction={loadDashboard}
      />
    );
  }

  return (
    <div className="space-y-6">
      {/* 1. Header Greeting & Date */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">
            Welcome, {data.student_name} 👋
          </h1>
          <p className="text-xs text-slate-500 font-medium mt-0.5">
            {data.current_day}, {data.current_date} • {data.college} • {data.semester}
          </p>
        </div>

        {/* Quick Actions Bar */}
        <div className="flex items-center flex-wrap gap-2">
          <Link
            to="/tasks"
            className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-xl shadow-xs transition-all"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>New Task</span>
          </Link>
          <Link
            to="/attendance"
            className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 text-xs font-semibold rounded-xl shadow-xs transition-all"
          >
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
            <span>Mark Attendance</span>
          </Link>
          <Link
            to="/study-planner"
            className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 text-white text-xs font-semibold rounded-xl shadow-xs transition-all"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>Smart Plan</span>
          </Link>
        </div>
      </div>

      {/* 2. Daily Briefing Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white rounded-3xl p-5 sm:p-6 shadow-xl border border-indigo-900/40 relative overflow-hidden">
        <div className="absolute right-0 top-0 w-80 h-80 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="flex items-start justify-between mb-3 relative z-10">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-xl bg-indigo-500/20 border border-indigo-400/30 flex items-center justify-center text-cyan-300">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-cyan-300">
                Daily Academic Briefing
              </span>
              <span className="text-[10px] text-slate-400 block">
                Live Grounded Intelligence
              </span>
            </div>
          </div>
          <Link
            to="/ai-assistant"
            className="text-[11px] text-cyan-300 hover:text-white flex items-center space-x-1 font-semibold transition-colors"
          >
            <span>Ask AI Assistant</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <p className="text-sm font-medium text-slate-200 leading-relaxed relative z-10 mb-4">
          {data.daily_briefing.summary}
        </p>

        {/* Highlight Bullets */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 relative z-10">
          {data.daily_briefing.highlights.map((item, idx) => (
            <div
              key={idx}
              className="bg-white/5 border border-white/10 rounded-xl p-2.5 flex items-start space-x-2"
            >
              <div className="w-1.5 h-1.5 rounded-full bg-cyan-400 mt-1.5 shrink-0" />
              <span className="text-xs text-slate-300 leading-snug">{item}</span>
            </div>
          ))}
        </div>
      </div>

      {/* 3. Top Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Attendance Card */}
        <div className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold">Attendance</span>
            <CheckCircle2
              className={`w-4 h-4 ${
                data.is_attendance_warning ? 'text-amber-500' : 'text-emerald-500'
              }`}
            />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900 tracking-tight">
              {data.overall_attendance_percentage}%
            </div>
            <p
              className={`text-[11px] font-medium mt-0.5 ${
                data.is_attendance_warning ? 'text-amber-600' : 'text-emerald-600'
              }`}
            >
              {data.is_attendance_warning ? 'Below 75% target' : 'Safely compliant'}
            </p>
          </div>
          <Link
            to="/attendance"
            className="mt-3 text-[11px] text-indigo-600 font-semibold hover:underline flex items-center justify-between"
          >
            <span>View records</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Pending Tasks */}
        <div className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold">Active Tasks</span>
            <Clock className="w-4 h-4 text-indigo-500" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900 tracking-tight">
              {data.pending_tasks_count}
            </div>
            <p className="text-[11px] font-medium text-slate-500 mt-0.5">
              {data.overdue_tasks_count > 0 ? (
                <span className="text-rose-600 font-semibold">
                  {data.overdue_tasks_count} overdue
                </span>
              ) : (
                '0 overdue'
              )}
            </p>
          </div>
          <Link
            to="/tasks"
            className="mt-3 text-[11px] text-indigo-600 font-semibold hover:underline flex items-center justify-between"
          >
            <span>Manage coursework</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Performance Marks Average */}
        <div className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold">Marks Average</span>
            <BarChart2 className="w-4 h-4 text-cyan-600" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900 tracking-tight">
              {data.overall_marks_average > 0 ? `${data.overall_marks_average}%` : 'N/A'}
            </div>
            <p className="text-[11px] font-medium text-slate-500 mt-0.5">
              {data.weak_subjects.length > 0
                ? `${data.weak_subjects.length} weak subjects`
                : 'All courses healthy'}
            </p>
          </div>
          <Link
            to="/marks"
            className="mt-3 text-[11px] text-indigo-600 font-semibold hover:underline flex items-center justify-between"
          >
            <span>Performance analytics</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {/* Today's Lectures */}
        <div className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs flex flex-col justify-between">
          <div className="flex items-center justify-between text-slate-500 mb-2">
            <span className="text-xs font-semibold">Today's Classes</span>
            <CalendarDays className="w-4 h-4 text-violet-500" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-900 tracking-tight">
              {data.todays_classes.length}
            </div>
            <p className="text-[11px] font-medium text-slate-500 mt-0.5">
              {data.todays_classes.length > 0 ? `Next: ${data.todays_classes[0].start_time}` : 'Free day'}
            </p>
          </div>
          <Link
            to="/timetable"
            className="mt-3 text-[11px] text-indigo-600 font-semibold hover:underline flex items-center justify-between"
          >
            <span>Full schedule</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>

      {/* 4. Main Two Column Grid: Today's Schedule & Urgent Tasks */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Today's Timetable */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <CalendarDays className="w-4 h-4 text-indigo-600" />
              <h2 className="text-sm font-bold text-slate-900">Today's Schedule</h2>
            </div>
            <Link
              to="/timetable"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-700"
            >
              View Week
            </Link>
          </div>

          {data.todays_classes.length === 0 ? (
            <EmptyState
              icon={CalendarDays}
              title="No classes scheduled today"
              description="You have no lectures or practicals registered for today in your weekly timetable."
              actionText="Manage Timetable"
              onAction={() => window.location.assign('/timetable')}
            />
          ) : (
            <div className="space-y-3">
              {data.todays_classes.map((c) => (
                <div
                  key={c.id}
                  className="flex items-start justify-between p-3.5 rounded-2xl border border-slate-100 hover:border-indigo-200 hover:bg-slate-50/50 transition-all"
                >
                  <div className="flex items-start space-x-3">
                    <div
                      className="w-2.5 h-10 rounded-full mt-0.5 shrink-0"
                      style={{ backgroundColor: c.subject_color || '#4f46e5' }}
                    />
                    <div>
                      <h3 className="text-xs font-bold text-slate-900">
                        {c.subject_name || c.subject_code}
                      </h3>
                      <p className="text-[11px] text-slate-500">
                        {c.classroom} {c.faculty_name ? `• ${c.faculty_name}` : ''}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <span className="text-xs font-bold text-slate-900">
                      {c.start_time} - {c.end_time}
                    </span>
                    <span className="text-[10px] text-slate-400 block font-medium">Class session</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Urgent Deadlines & Tasks */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <Clock className="w-4 h-4 text-indigo-600" />
              <h2 className="text-sm font-bold text-slate-900">Urgent Coursework</h2>
            </div>
            <Link
              to="/tasks"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-700"
            >
              All Tasks ({data.pending_tasks_count})
            </Link>
          </div>

          {data.upcoming_tasks.length === 0 ? (
            <EmptyState
              icon={CheckCircle2}
              title="No pending tasks!"
              description="You have no active coursework or exam prep pending. Relax or create a new assignment."
              actionText="Create New Task"
              onAction={() => window.location.assign('/tasks')}
            />
          ) : (
            <div className="space-y-3">
              {data.upcoming_tasks.map((task) => (
                <div
                  key={task.id}
                  className="flex items-center justify-between p-3.5 rounded-2xl border border-slate-100 hover:border-slate-200 transition-all group"
                >
                  <div className="flex items-center space-x-3 min-w-0">
                    <button
                      onClick={() => handleToggleTask(task)}
                      className={`w-5 h-5 rounded-lg border flex items-center justify-center transition-colors shrink-0 ${
                        task.status === 'completed'
                          ? 'bg-emerald-600 border-emerald-600 text-white'
                          : 'border-slate-300 hover:border-indigo-600'
                      }`}
                    >
                      {task.status === 'completed' && <CheckCircle2 className="w-3.5 h-3.5" />}
                    </button>
                    <div className="min-w-0">
                      <p
                        className={`text-xs font-bold truncate ${
                          task.status === 'completed'
                            ? 'line-through text-slate-400'
                            : 'text-slate-900'
                        }`}
                      >
                        {task.title}
                      </p>
                      <p className="text-[10px] text-slate-500 truncate">
                        {task.subject_name || 'Coursework'} • Due{' '}
                        {new Date(task.due_date).toLocaleDateString([], {
                          month: 'short',
                          day: 'numeric',
                        })}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center space-x-2 shrink-0 ml-3">
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-md ${
                        task.priority_score >= 70
                          ? 'bg-rose-100 text-rose-700'
                          : task.priority_score >= 40
                          ? 'bg-amber-100 text-amber-700'
                          : 'bg-slate-100 text-slate-600'
                      }`}
                    >
                      {task.priority_score} pts
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 5. Attendance & Performance Summary Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Attendance Compliance */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-bold text-slate-900">Attendance Compliance Standing</h2>
            <Link
              to="/attendance"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-700"
            >
              Open Tracker
            </Link>
          </div>

          {data.subject_attendance.length === 0 ? (
            <p className="text-xs text-slate-400 text-center py-6">
              No subjects registered yet. Add subjects to start tracking attendance compliance.
            </p>
          ) : (
            <div className="space-y-3.5">
              {data.subject_attendance.map((sub) => (
                <div key={sub.subject_id} className="space-y-1.5">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-slate-800">{sub.subject_name}</span>
                    <span
                      className={`font-bold ${
                        sub.status === 'safe'
                          ? 'text-emerald-600'
                          : sub.status === 'warning'
                          ? 'text-amber-600'
                          : 'text-rose-600'
                      }`}
                    >
                      {sub.percentage}%
                    </span>
                  </div>
                  <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
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
                  <p className="text-[10px] text-slate-400">{sub.explanation}</p>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Study Planner Today */}
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-bold text-slate-900">Scheduled Study Blocks</h2>
            <Link
              to="/study-planner"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-700"
            >
              Planner Calendar
            </Link>
          </div>

          {data.todays_study_sessions.length === 0 ? (
            <div className="text-center py-8">
              <Sparkles className="w-8 h-8 text-indigo-400 mx-auto mb-2 opacity-60" />
              <p className="text-xs font-semibold text-slate-700">No study sessions for today</p>
              <p className="text-[11px] text-slate-400 max-w-xs mx-auto mt-1 mb-4">
                Use our Smart Planner to auto-schedule study blocks around your timetable.
              </p>
              <Link
                to="/study-planner"
                className="inline-flex items-center space-x-1.5 px-3 py-1.5 bg-indigo-50 text-indigo-600 hover:bg-indigo-100 text-xs font-semibold rounded-xl transition-colors"
              >
                <span>Generate Smart Schedule</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          ) : (
            <div className="space-y-3">
              {data.todays_study_sessions.map((ses) => (
                <div
                  key={ses.id}
                  className="p-3.5 rounded-2xl border border-slate-100 bg-slate-50/50 flex items-center justify-between"
                >
                  <div>
                    <h3 className="text-xs font-bold text-slate-900">{ses.title}</h3>
                    <p className="text-[10px] text-slate-500">
                      {ses.start_time} - {ses.end_time} ({ses.duration_minutes}m)
                    </p>
                  </div>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-indigo-100 text-indigo-700 uppercase">
                    {ses.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
