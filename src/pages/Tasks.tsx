import React, { useState, useEffect } from 'react';
import {
  CheckSquare,
  Plus,
  Search,
  Filter,
  Calendar,
  Clock,
  AlertTriangle,
  CheckCircle2,
  Trash2,
  Edit2,
  Sparkles,
  ArrowUpDown,
  BookOpen,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  getTasks,
  getSubjects,
  createTask,
  updateTask,
  updateTaskStatus,
  deleteTask,
} from '../services/firebaseDb';
import { Task, Subject } from '../types';
import { Modal } from '../components/common/Modal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

export const Tasks: React.FC = () => {
  const { user } = useAuth();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);

  // Filters & Search
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [priorityFilter, setPriorityFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [sortBy, setSortBy] = useState('urgency');

  // Modal State
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);

  // Form
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    subject_id: '',
    task_type: 'assignment' as 'assignment' | 'examination' | 'project' | 'revision' | 'personal',
    due_date: new Date(Date.now() + 86400000 * 2).toISOString().slice(0, 16),
    estimated_effort_minutes: 60,
    priority: 'medium' as 'low' | 'medium' | 'high' | 'urgent',
    status: 'pending' as 'pending' | 'in_progress' | 'completed',
  });

  const { success, error } = useToast();

  const loadData = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const [tData, sData] = await Promise.all([
        getTasks(user.uid, {
          status: statusFilter,
          priority: priorityFilter,
          task_type: typeFilter,
          sort_by: sortBy,
          search,
        }),
        getSubjects(user.uid),
      ]);
      setTasks(tData || []);
      setSubjects(sData || []);
    } catch {
      error('Failed to load tasks.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user, statusFilter, priorityFilter, typeFilter, sortBy, search]);

  const handleOpenAdd = () => {
    setEditingTask(null);
    setFormData({
      title: '',
      description: '',
      subject_id: subjects.length > 0 ? subjects[0].id : '',
      task_type: 'assignment',
      due_date: new Date(Date.now() + 86400000 * 2).toISOString().slice(0, 16),
      estimated_effort_minutes: 60,
      priority: 'medium',
      status: 'pending',
    });
    setModalOpen(true);
  };

  const handleOpenEdit = (t: Task) => {
    setEditingTask(t);
    setFormData({
      title: t.title,
      description: t.description || '',
      subject_id: t.subject_id || '',
      task_type: t.task_type,
      due_date: new Date(t.due_date).toISOString().slice(0, 16),
      estimated_effort_minutes: t.estimated_effort_minutes,
      priority: t.priority,
      status: t.status,
    });
    setModalOpen(true);
  };

  const handleToggleStatus = async (task: Task) => {
    if (!user) return;
    try {
      const nextStatus = task.status === 'completed' ? 'pending' : 'completed';
      await updateTaskStatus(user.uid, task.id, nextStatus);
      success(nextStatus === 'completed' ? 'Task completed! Good job!' : 'Task re-opened.');
      loadData();
    } catch {
      error('Failed to update status.');
    }
  };

  const handleDelete = async (id: string) => {
    if (!user) return;
    if (!window.confirm('Delete this task?')) return;
    try {
      await deleteTask(user.uid, id);
      success('Task removed.');
      loadData();
    } catch {
      error('Failed to delete task.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!formData.title) {
      error('Task title is required.');
      return;
    }

    const sub = subjects.find((s) => s.id === formData.subject_id);

    try {
      if (editingTask) {
        await updateTask(user.uid, editingTask.id, {
          title: formData.title,
          description: formData.description,
          subject_id: formData.subject_id || undefined,
          subject_name: sub?.name,
          subject_code: sub?.code,
          subject_color: sub?.color,
          task_type: formData.task_type,
          due_date: new Date(formData.due_date).toISOString(),
          estimated_effort_minutes: Number(formData.estimated_effort_minutes),
          priority: formData.priority,
          status: formData.status,
        });
        success('Task details updated.');
      } else {
        await createTask(user.uid, {
          title: formData.title,
          description: formData.description,
          subject_id: formData.subject_id || undefined,
          subject_name: sub?.name,
          subject_code: sub?.code,
          subject_color: sub?.color,
          task_type: formData.task_type,
          due_date: new Date(formData.due_date).toISOString(),
          estimated_effort_minutes: Number(formData.estimated_effort_minutes),
          priority: formData.priority,
          status: formData.status,
        });
        success('New task registered with deterministic priority scoring!');
      }
      setModalOpen(false);
      loadData();
    } catch {
      error('Failed to save task.');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 tracking-tight flex items-center space-x-2">
            <CheckSquare className="w-5 h-5 text-indigo-600" />
            <span>Tasks, Assignments & Examination Registry</span>
          </h1>
          <p className="text-xs text-slate-500 font-medium mt-0.5">
            Deterministic urgency scoring factoring priority, deadline proximity, and estimated effort
          </p>
        </div>

        <button
          onClick={handleOpenAdd}
          className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-xs flex items-center space-x-1.5 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>New Task / Exam</span>
        </button>
      </div>

      {/* Controls & Filters Bar */}
      <div className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search tasks, descriptions, or subjects..."
              className="w-full pl-9 pr-3 py-1.5 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="flex items-center space-x-2">
            <ArrowUpDown className="w-3.5 h-3.5 text-slate-400" />
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="px-3 py-1.5 text-xs rounded-xl border border-slate-200 bg-white text-slate-700 focus:outline-none"
            >
              <option value="urgency">Sort by Priority Score</option>
              <option value="due_date">Sort by Due Date</option>
              <option value="created">Sort by Created Date</option>
            </select>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2 pt-1 border-t border-slate-100 text-xs">
          <span className="text-slate-400 font-semibold text-[11px] mr-1">Status:</span>
          {['all', 'pending', 'in_progress', 'completed'].map((st) => (
            <button
              key={st}
              onClick={() => setStatusFilter(st)}
              className={`px-2.5 py-1 rounded-lg capitalize font-medium transition-colors ${
                statusFilter === st
                  ? 'bg-indigo-100 text-indigo-800 font-bold'
                  : 'text-slate-500 hover:bg-slate-100'
              }`}
            >
              {st.replace('_', ' ')}
            </button>
          ))}

          <span className="text-slate-400 font-semibold text-[11px] ml-3 mr-1">Type:</span>
          {['all', 'assignment', 'examination', 'project', 'revision'].map((tp) => (
            <button
              key={tp}
              onClick={() => setTypeFilter(tp)}
              className={`px-2.5 py-1 rounded-lg capitalize font-medium transition-colors ${
                typeFilter === tp
                  ? 'bg-slate-900 text-white font-bold'
                  : 'text-slate-500 hover:bg-slate-100'
              }`}
            >
              {tp}
            </button>
          ))}
        </div>
      </div>

      {/* Task List */}
      {loading ? (
        <LoadingSpinner size="md" label="Loading tasks & exams..." className="py-12" />
      ) : tasks.length === 0 ? (
        <EmptyState
          icon={CheckSquare}
          title="No tasks match your criteria"
          description="You currently have no tasks in this view. Click 'New Task' to register coursework or an examination."
          actionText="Create First Task"
          onAction={handleOpenAdd}
        />
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => (
            <div
              key={task.id}
              className={`p-4 rounded-2xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4 ${
                task.status === 'completed'
                  ? 'bg-slate-50/70 border-slate-200/60 opacity-75'
                  : task.is_overdue
                  ? 'bg-rose-50/30 border-rose-200 shadow-xs'
                  : 'bg-white border-slate-200/80 shadow-xs hover:border-indigo-300'
              }`}
            >
              <div className="flex items-start space-x-3.5 flex-1 min-w-0">
                <button
                  onClick={() => handleToggleStatus(task)}
                  className={`mt-0.5 w-5 h-5 rounded-lg border flex items-center justify-center transition-colors shrink-0 ${
                    task.status === 'completed'
                      ? 'bg-emerald-600 border-emerald-600 text-white'
                      : 'border-slate-300 hover:border-indigo-600'
                  }`}
                >
                  {task.status === 'completed' && <CheckCircle2 className="w-3.5 h-3.5" />}
                </button>

                <div className="min-w-0">
                  <div className="flex items-center flex-wrap gap-2 mb-1">
                    <h3
                      className={`text-sm font-bold leading-tight ${
                        task.status === 'completed'
                          ? 'line-through text-slate-400'
                          : 'text-slate-900'
                      }`}
                    >
                      {task.title}
                    </h3>
                    <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-md bg-slate-100 text-slate-700">
                      {task.task_type}
                    </span>
                    {task.subject_name && (
                      <span
                        className="text-[10px] font-bold px-2 py-0.5 rounded-md text-white"
                        style={{ backgroundColor: task.subject_color || '#4f46e5' }}
                      >
                        {task.subject_code || task.subject_name}
                      </span>
                    )}
                  </div>

                  {task.description && (
                    <p className="text-xs text-slate-500 mb-2 line-clamp-2">
                      {task.description}
                    </p>
                  )}

                  <div className="flex items-center flex-wrap gap-3 text-[11px] text-slate-500">
                    <div className="flex items-center space-x-1">
                      <Calendar className="w-3.5 h-3.5 text-slate-400" />
                      <span className={task.is_overdue ? 'text-rose-600 font-bold' : ''}>
                        {new Date(task.due_date).toLocaleString([], {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>

                    <div className="flex items-center space-x-1">
                      <Clock className="w-3.5 h-3.5 text-slate-400" />
                      <span>{task.estimated_effort_minutes} min effort</span>
                    </div>

                    <span className="text-slate-400 italic">
                      {task.priority_reason}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between sm:justify-end space-x-3 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                <div className="text-right">
                  <span
                    className={`inline-block text-xs font-black px-2.5 py-1 rounded-xl shadow-xs ${
                      task.priority_score >= 70
                        ? 'bg-rose-100 text-rose-700 border border-rose-200'
                        : task.priority_score >= 40
                        ? 'bg-amber-100 text-amber-800 border border-amber-200'
                        : 'bg-slate-100 text-slate-700'
                    }`}
                  >
                    {task.priority_score} pts
                  </span>
                </div>

                <div className="flex items-center space-x-1">
                  <button
                    onClick={() => handleOpenEdit(task)}
                    className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-slate-100 transition-colors"
                  >
                    <Edit2 className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={() => handleDelete(task.id)}
                    className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-slate-100 transition-colors"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Task Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingTask ? 'Edit Task / Exam' : 'Register New Task / Exam'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Title *</label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder="e.g. Distributed Systems Lab Assignment 2"
              required
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Description (Optional)
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={2}
              placeholder="Key instructions, rubric, submission portal link..."
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Course / Subject</label>
              <select
                value={formData.subject_id}
                onChange={(e) => setFormData({ ...formData, subject_id: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="">General / None</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.code})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Task Type *</label>
              <select
                value={formData.task_type}
                onChange={(e) => setFormData({ ...formData, task_type: e.target.value as any })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="assignment">Assignment</option>
                <option value="examination">Examination</option>
                <option value="project">Project Work</option>
                <option value="revision">Revision</option>
                <option value="personal">Personal Study</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Deadline Date & Time *</label>
              <input
                type="datetime-local"
                value={formData.due_date}
                onChange={(e) => setFormData({ ...formData, due_date: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Estimated Effort</label>
              <select
                value={formData.estimated_effort_minutes}
                onChange={(e) =>
                  setFormData({ ...formData, estimated_effort_minutes: parseInt(e.target.value) })
                }
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value={30}>30 minutes</option>
                <option value={60}>1 hour</option>
                <option value={120}>2 hours</option>
                <option value={180}>3 hours</option>
                <option value={240}>4 hours</option>
                <option value={360}>6 hours</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Base Priority *</label>
              <select
                value={formData.priority}
                onChange={(e) => setFormData({ ...formData, priority: e.target.value as any })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="low">Low</option>
                <option value="medium">Medium</option>
                <option value="high">High</option>
                <option value="urgent">Urgent</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Status</label>
              <select
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value as any })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="pending">Pending</option>
                <option value="in_progress">In Progress</option>
                <option value="completed">Completed</option>
              </select>
            </div>
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
              {editingTask ? 'Save Changes' : 'Calculate & Register'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
