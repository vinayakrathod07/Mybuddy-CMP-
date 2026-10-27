import React, { useState, useEffect } from 'react';
import {
  BarChart3,
  Plus,
  TrendingUp,
  AlertTriangle,
  Award,
  Trash2,
  Edit2,
  BookOpen,
  Info,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  LineChart,
  Line,
} from 'recharts';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  getAssessments,
  getSubjects,
  createAssessment,
  updateAssessment,
  deleteAssessment,
  computeMarksAnalytics,
} from '../services/firebaseDb';
import { Assessment, PerformanceAnalytics, Subject } from '../types';
import { Modal } from '../components/common/Modal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

export const MarksAnalytics: React.FC = () => {
  const { user } = useAuth();
  const [assessments, setAssessments] = useState<Assessment[]>([]);
  const [analytics, setAnalytics] = useState<PerformanceAnalytics | null>(null);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);

  // Modal State
  const [modalOpen, setModalOpen] = useState(false);
  const [editingAssessment, setEditingAssessment] = useState<Assessment | null>(null);
  const [formData, setFormData] = useState({
    subject_id: '',
    assessment_name: '',
    assessment_type: 'Quiz',
    marks_obtained: 18.0,
    maximum_marks: 20.0,
    assessment_date: new Date().toISOString().slice(0, 10),
    notes: '',
  });

  const { success, error } = useToast();

  const loadData = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const [allAss, subData] = await Promise.all([
        getAssessments(user.uid),
        getSubjects(user.uid),
      ]);
      const assList = allAss || [];
      const subList = subData || [];
      const ana = computeMarksAnalytics(subList, assList);

      setAssessments(assList);
      setAnalytics(ana);
      setSubjects(subList);

      if (subList.length > 0 && !formData.subject_id) {
        setFormData((prev) => ({ ...prev, subject_id: subList[0].id }));
      }
    } catch {
      error('Failed to load assessments.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  const handleOpenAdd = () => {
    setEditingAssessment(null);
    setFormData({
      subject_id: subjects.length > 0 ? subjects[0].id : '',
      assessment_name: '',
      assessment_type: 'Quiz',
      marks_obtained: 18.0,
      maximum_marks: 20.0,
      assessment_date: new Date().toISOString().slice(0, 10),
      notes: '',
    });
    setModalOpen(true);
  };

  const handleOpenEdit = (a: Assessment) => {
    setEditingAssessment(a);
    setFormData({
      subject_id: a.subject_id,
      assessment_name: a.assessment_name,
      assessment_type: a.assessment_type,
      marks_obtained: a.marks_obtained,
      maximum_marks: a.maximum_marks,
      assessment_date: a.assessment_date,
      notes: a.notes || '',
    });
    setModalOpen(true);
  };

  const handleDelete = async (id: string) => {
    if (!user) return;
    if (!window.confirm('Delete this assessment result?')) return;
    try {
      await deleteAssessment(user.uid, id);
      success('Assessment deleted.');
      loadData();
    } catch {
      error('Failed to delete assessment.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!formData.subject_id) {
      error('Please select a course / subject.');
      return;
    }
    if (Number(formData.maximum_marks) <= 0) {
      error('Maximum marks must be greater than zero.');
      return;
    }
    if (Number(formData.marks_obtained) > Number(formData.maximum_marks)) {
      error('Marks obtained cannot exceed maximum marks.');
      return;
    }

    const sub = subjects.find((s) => s.id === formData.subject_id);

    try {
      if (editingAssessment) {
        await updateAssessment(user.uid, editingAssessment.id, {
          subject_id: formData.subject_id,
          subject_name: sub?.name,
          subject_code: sub?.code,
          subject_color: sub?.color,
          assessment_name: formData.assessment_name,
          assessment_type: formData.assessment_type,
          marks_obtained: Number(formData.marks_obtained),
          maximum_marks: Number(formData.maximum_marks),
          assessment_date: formData.assessment_date,
          notes: formData.notes,
        });
        success('Assessment marks updated.');
      } else {
        await createAssessment(user.uid, {
          subject_id: formData.subject_id,
          subject_name: sub?.name,
          subject_code: sub?.code,
          subject_color: sub?.color,
          assessment_name: formData.assessment_name,
          assessment_type: formData.assessment_type,
          marks_obtained: Number(formData.marks_obtained),
          maximum_marks: Number(formData.maximum_marks),
          assessment_date: formData.assessment_date,
          notes: formData.notes,
        });
        success('New assessment score recorded!');
      }
      setModalOpen(false);
      loadData();
    } catch {
      error('Failed to save assessment.');
    }
  };

  // Prepare chart data
  const chartData = (analytics?.subject_performance || [])
    .filter((sp) => sp.assessment_count > 0)
    .map((sp) => ({
      name: sp.subject_code || sp.subject_name.slice(0, 8),
      fullName: sp.subject_name,
      average: sp.average_percentage,
      highest: sp.highest_percentage,
      lowest: sp.lowest_percentage,
      count: sp.assessment_count,
    }));

  const trajectoryData = [...assessments]
    .sort((a, b) => new Date(a.assessment_date).getTime() - new Date(b.assessment_date).getTime())
    .map((a) => ({
      date: a.assessment_date,
      percentage: a.percentage,
      title: a.assessment_name,
    }));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 tracking-tight flex items-center space-x-2">
            <BarChart3 className="w-5 h-5 text-indigo-600" />
            <span>Marks & Academic Performance Analytics</span>
          </h1>
          <p className="text-xs text-slate-500 font-medium mt-0.5">
            Real-time score trajectories, subject comparison benchmarks, and weak module identification
          </p>
        </div>

        <button
          onClick={handleOpenAdd}
          className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-xs flex items-center space-x-1.5 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add Assessment Score</span>
        </button>
      </div>

      {/* Top Analytics KPI Row */}
      {analytics && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs flex items-center space-x-4">
            <div className="w-12 h-12 rounded-xl bg-indigo-50 flex items-center justify-center text-indigo-600">
              <Award className="w-6 h-6" />
            </div>
            <div>
              <span className="text-2xl font-black text-slate-900 block">
                {analytics.overall_average > 0 ? `${analytics.overall_average}%` : 'N/A'}
              </span>
              <span className="text-xs font-semibold text-slate-500">Cumulative Average</span>
            </div>
          </div>

          <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs flex items-center space-x-4">
            <div className="w-12 h-12 rounded-xl bg-cyan-50 flex items-center justify-center text-cyan-600">
              <TrendingUp className="w-6 h-6" />
            </div>
            <div>
              <span className="text-2xl font-black text-slate-900 block">
                {analytics.total_assessments}
              </span>
              <span className="text-xs font-semibold text-slate-500">Assessments Graded</span>
            </div>
          </div>

          <div className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs flex items-center space-x-4">
            <div
              className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                analytics.weak_subjects.length > 0
                  ? 'bg-rose-50 text-rose-600'
                  : 'bg-emerald-50 text-emerald-600'
              }`}
            >
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div>
              <span className="text-2xl font-black text-slate-900 block">
                {analytics.weak_subjects.length}
              </span>
              <span className="text-xs font-semibold text-slate-500">
                {analytics.weak_subjects.length > 0
                  ? 'Focus Areas (<65%)'
                  : 'All Subjects Passing'}
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Insights Box */}
      {analytics && analytics.insights.length > 0 && (
        <div className="bg-gradient-to-r from-indigo-900 to-slate-900 text-white rounded-2xl p-4 shadow-sm">
          <div className="flex items-center space-x-2 text-xs font-bold text-cyan-300 uppercase tracking-wider mb-2">
            <Info className="w-4 h-4" />
            <span>Diagnostic Academic Insights</span>
          </div>
          <div className="space-y-1 text-xs text-slate-200">
            {analytics.insights.map((ins, idx) => (
              <p key={idx} className="leading-relaxed">
                • {ins}
              </p>
            ))}
          </div>
        </div>
      )}

      {/* Visual Charts */}
      {loading ? (
        <LoadingSpinner size="md" label="Generating performance analytics charts..." className="py-8" />
      ) : assessments.length === 0 ? (
        <EmptyState
          icon={BarChart3}
          title="No assessment scores recorded yet"
          description="Record your quizzes, mid-terms, project marks, or lab test scores to see visual performance charts."
          actionText="Add First Assessment Score"
          onAction={handleOpenAdd}
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Bar Chart: Subject Comparison */}
          <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
            <h2 className="text-sm font-bold text-slate-900 mb-1">Subject Average Comparison (%)</h2>
            <p className="text-[11px] text-slate-400 mb-4">
              Average score per module with 65% minimum performance benchmark
            </p>
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                  <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                  <Tooltip
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const d = payload[0].payload;
                        return (
                          <div className="bg-slate-900 text-white p-2.5 rounded-xl text-xs shadow-lg">
                            <p className="font-bold">{d.fullName}</p>
                            <p className="text-cyan-300">Average: {d.average}%</p>
                            <p className="text-slate-400 text-[10px]">
                              Tests: {d.count} • High: {d.highest}% • Low: {d.lowest}%
                            </p>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Bar dataKey="average" fill="#4f46e5" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Line Chart: Trajectory */}
          <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
            <h2 className="text-sm font-bold text-slate-900 mb-1">Score Trajectory Over Time</h2>
            <p className="text-[11px] text-slate-400 mb-4">
              Individual assessment percentages chronologically
            </p>
            <div className="h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trajectoryData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="date" tick={{ fontSize: 10 }} />
                  <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} />
                  <Tooltip
                    content={({ active, payload }) => {
                      if (active && payload && payload.length) {
                        const d = payload[0].payload;
                        return (
                          <div className="bg-slate-900 text-white p-2.5 rounded-xl text-xs shadow-lg">
                            <p className="font-bold">{d.title}</p>
                            <p className="text-emerald-400">Score: {d.percentage}%</p>
                            <p className="text-slate-400 text-[10px]">Date: {d.date}</p>
                          </div>
                        );
                      }
                      return null;
                    }}
                  />
                  <Line
                    type="monotone"
                    dataKey="percentage"
                    stroke="#06b6d4"
                    strokeWidth={2.5}
                    dot={{ fill: '#06b6d4', r: 4 }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      {/* Assessment Table */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
        <h2 className="text-sm font-bold text-slate-900 mb-3">All Assessment Records</h2>
        {assessments.length === 0 ? (
          <p className="text-xs text-slate-400 text-center py-6">
            No assessments added yet. Click 'Add Assessment Score' above to record grades.
          </p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 border-b border-slate-100">
                <tr>
                  <th className="py-2.5 px-3 font-semibold">Assessment</th>
                  <th className="py-2.5 px-3 font-semibold">Subject</th>
                  <th className="py-2.5 px-3 font-semibold">Type</th>
                  <th className="py-2.5 px-3 font-semibold">Marks</th>
                  <th className="py-2.5 px-3 font-semibold">Percentage</th>
                  <th className="py-2.5 px-3 font-semibold">Date</th>
                  <th className="py-2.5 px-3 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {assessments.map((a) => (
                  <tr key={a.id} className="hover:bg-slate-50/60 transition-colors">
                    <td className="py-2.5 px-3 font-bold text-slate-900">{a.assessment_name}</td>
                    <td className="py-2.5 px-3 text-slate-700">
                      <span className="font-semibold">{a.subject_name || a.subject_code}</span>
                    </td>
                    <td className="py-2.5 px-3">
                      <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">
                        {a.assessment_type}
                      </span>
                    </td>
                    <td className="py-2.5 px-3 font-semibold text-slate-900">
                      {a.marks_obtained} / {a.maximum_marks}
                    </td>
                    <td className="py-2.5 px-3">
                      <span
                        className={`text-xs font-black ${
                          a.percentage >= 75
                            ? 'text-emerald-600'
                            : a.percentage >= 60
                            ? 'text-amber-600'
                            : 'text-rose-600'
                        }`}
                      >
                        {a.percentage}%
                      </span>
                    </td>
                    <td className="py-2.5 px-3 text-slate-500">{a.assessment_date}</td>
                    <td className="py-2.5 px-3 text-right">
                      <button
                        onClick={() => handleOpenEdit(a)}
                        className="p-1 text-slate-400 hover:text-indigo-600 rounded-md transition-colors mr-1"
                        title="Edit score"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() => handleDelete(a.id)}
                        className="p-1 text-slate-400 hover:text-rose-600 rounded-md transition-colors"
                        title="Delete score"
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

      {/* Assessment Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingAssessment ? 'Edit Assessment' : 'Record Assessment Marks'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Course / Subject *</label>
            {subjects.length === 0 ? (
              <p className="text-xs text-amber-700 bg-amber-50 p-2.5 rounded-xl border border-amber-200">
                Please add a course first in Timetable or Attendance.
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

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Assessment Name *
            </label>
            <input
              type="text"
              value={formData.assessment_name}
              onChange={(e) => setFormData({ ...formData, assessment_name: e.target.value })}
              placeholder="e.g. Mid-Term Exam 1 / Quiz 2"
              required
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Assessment Type</label>
              <select
                value={formData.assessment_type}
                onChange={(e) => setFormData({ ...formData, assessment_type: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="Quiz">Quiz</option>
                <option value="Mid-Term">Mid-Term Exam</option>
                <option value="End-Term">End-Term / Final</option>
                <option value="Assignment">Assignment</option>
                <option value="Project">Project / Lab</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Date</label>
              <input
                type="date"
                value={formData.assessment_date}
                onChange={(e) => setFormData({ ...formData, assessment_date: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Marks Obtained *
              </label>
              <input
                type="number"
                step="0.5"
                min="0"
                value={formData.marks_obtained}
                onChange={(e) =>
                  setFormData({ ...formData, marks_obtained: parseFloat(e.target.value) || 0 })
                }
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Maximum Marks *
              </label>
              <input
                type="number"
                step="0.5"
                min="1"
                value={formData.maximum_marks}
                onChange={(e) =>
                  setFormData({ ...formData, maximum_marks: parseFloat(e.target.value) || 1 })
                }
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Feedback / Notes (Optional)
            </label>
            <input
              type="text"
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              placeholder="e.g. Scored 10/10 in Section A, lost marks in Question 4"
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <button
              type="button"
              onClick={() => setModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs"
            >
              {editingAssessment ? 'Update Score' : 'Save Score'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
