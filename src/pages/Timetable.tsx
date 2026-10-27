import React, { useState, useEffect } from 'react';
import {
  CalendarDays,
  Plus,
  Edit2,
  Trash2,
  Clock,
  MapPin,
  User,
  AlertCircle,
  BookOpen,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  getTimetable,
  getSubjects,
  createTimetableEntry,
  updateTimetableEntry,
  deleteTimetableEntry,
  createSubject,
  checkTimetableConflict,
} from '../services/firebaseDb';
import { TimetableEntry, Subject } from '../types';
import { Modal } from '../components/common/Modal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

export const Timetable: React.FC = () => {
  const { user } = useAuth();
  const [entries, setEntries] = useState<TimetableEntry[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedDay, setSelectedDay] = useState<string>('All');
  const [loading, setLoading] = useState(true);

  // Modals
  const [modalOpen, setModalOpen] = useState(false);
  const [subjectModalOpen, setSubjectModalOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<TimetableEntry | null>(null);

  // Class Form State
  const [formData, setFormData] = useState({
    subject_id: '',
    day_of_week: 'Monday',
    start_time: '09:00',
    end_time: '10:30',
    classroom: 'Hall 101',
    faculty_name: '',
    notes: '',
  });

  // Subject Form State
  const [newSubject, setNewSubject] = useState({
    name: '',
    code: '',
    faculty: '',
    color: '#4f46e5',
    target_attendance: 75.0,
  });

  const { success, error, warning } = useToast();

  const loadData = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const [ttData, subData] = await Promise.all([
        getTimetable(user.uid),
        getSubjects(user.uid),
      ]);
      setEntries(ttData || []);
      setSubjects(subData || []);
      if (subData && subData.length > 0 && !formData.subject_id) {
        setFormData((prev) => ({ ...prev, subject_id: subData[0].id }));
      }
    } catch {
      error('Failed to load timetable.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user]);

  // Real-time conflict preview
  const conflictCheck = checkTimetableConflict(entries, {
    day_of_week: formData.day_of_week,
    start_time: formData.start_time,
    end_time: formData.end_time,
    id: editingEntry?.id,
  });

  const handleOpenAdd = () => {
    setEditingEntry(null);
    setFormData({
      subject_id: subjects.length > 0 ? subjects[0].id : '',
      day_of_week: selectedDay !== 'All' ? selectedDay : 'Monday',
      start_time: '09:00',
      end_time: '10:30',
      classroom: 'Hall 101',
      faculty_name: '',
      notes: '',
    });
    setModalOpen(true);
  };

  const handleOpenEdit = (entry: TimetableEntry) => {
    setEditingEntry(entry);
    setFormData({
      subject_id: entry.subject_id,
      day_of_week: entry.day_of_week,
      start_time: entry.start_time,
      end_time: entry.end_time,
      classroom: entry.classroom,
      faculty_name: entry.faculty_name || '',
      notes: entry.notes || '',
    });
    setModalOpen(true);
  };

  const handleDelete = async (id: string) => {
    if (!user) return;
    if (!window.confirm('Are you sure you want to delete this class slot?')) return;
    try {
      await deleteTimetableEntry(user.uid, id);
      success('Class entry removed.');
      loadData();
    } catch {
      error('Failed to delete entry.');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!formData.subject_id) {
      error('Please select or create a subject first.');
      return;
    }
    if (formData.start_time >= formData.end_time) {
      error('Start time must be before end time.');
      return;
    }

    if (conflictCheck.hasConflict) {
      warning(
        `Collision warning: Overlaps with ${conflictCheck.conflictingEntry?.subject_name} (${conflictCheck.conflictingEntry?.start_time} - ${conflictCheck.conflictingEntry?.end_time})`
      );
    }

    const sub = subjects.find((s) => s.id === formData.subject_id);

    try {
      if (editingEntry) {
        await updateTimetableEntry(user.uid, editingEntry.id, {
          subject_id: formData.subject_id,
          subject_name: sub?.name,
          subject_code: sub?.code,
          subject_color: sub?.color,
          day_of_week: formData.day_of_week,
          start_time: formData.start_time,
          end_time: formData.end_time,
          classroom: formData.classroom,
          faculty_name: formData.faculty_name,
          notes: formData.notes,
        });
        success('Class slot updated.');
      } else {
        await createTimetableEntry(user.uid, {
          subject_id: formData.subject_id,
          subject_name: sub?.name,
          subject_code: sub?.code,
          subject_color: sub?.color,
          day_of_week: formData.day_of_week,
          start_time: formData.start_time,
          end_time: formData.end_time,
          classroom: formData.classroom,
          faculty_name: formData.faculty_name,
          notes: formData.notes,
        });
        success('New class scheduled!');
      }
      setModalOpen(false);
      loadData();
    } catch {
      error('Failed to save timetable entry.');
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
        faculty: newSubject.faculty,
        color: newSubject.color,
        target_attendance: Number(newSubject.target_attendance) || 75.0,
      });
      success(`Course "${created.name}" created!`);
      setNewSubject({
        name: '',
        code: '',
        faculty: '',
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

  const filteredEntries =
    selectedDay === 'All'
      ? entries
      : entries.filter((e) => e.day_of_week.toLowerCase() === selectedDay.toLowerCase());

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 tracking-tight flex items-center space-x-2">
            <CalendarDays className="w-5 h-5 text-indigo-600" />
            <span>Weekly Timetable & Schedule</span>
          </h1>
          <p className="text-xs text-slate-500 font-medium mt-0.5">
            Manage your daily lecture timetable with real-time overlap collision detection
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setSubjectModalOpen(true)}
            className="px-3.5 py-2 text-xs font-semibold text-slate-700 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl transition-all shadow-xs flex items-center space-x-1.5"
          >
            <BookOpen className="w-3.5 h-3.5 text-indigo-600" />
            <span>Manage Subjects</span>
          </button>
          <button
            onClick={handleOpenAdd}
            className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-xs flex items-center space-x-1.5"
          >
            <Plus className="w-4 h-4" />
            <span>Add Class</span>
          </button>
        </div>
      </div>

      {/* Day Filter Pills */}
      <div className="flex items-center space-x-1.5 overflow-x-auto pb-1">
        {['All', ...DAYS].map((day) => (
          <button
            key={day}
            onClick={() => setSelectedDay(day)}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all ${
              selectedDay === day
                ? 'bg-slate-900 text-white shadow-xs'
                : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200/80'
            }`}
          >
            {day}
          </button>
        ))}
      </div>

      {/* Content */}
      {loading ? (
        <LoadingSpinner size="md" label="Loading weekly timetable..." className="py-12" />
      ) : filteredEntries.length === 0 ? (
        <EmptyState
          icon={CalendarDays}
          title={
            selectedDay === 'All'
              ? 'No classes scheduled yet'
              : `No classes scheduled for ${selectedDay}`
          }
          description="Your timetable is currently empty. Click 'Add Class' to set up your schedule."
          actionText="Add First Class"
          onAction={handleOpenAdd}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredEntries.map((entry) => (
            <div
              key={entry.id}
              className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs hover:border-indigo-300 transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between mb-2">
                  <div className="flex items-center space-x-2">
                    <span
                      className="w-3 h-3 rounded-full"
                      style={{ backgroundColor: entry.subject_color || '#4f46e5' }}
                    />
                    <span className="text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded-md bg-slate-100 text-slate-600">
                      {entry.day_of_week}
                    </span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <button
                      onClick={() => handleOpenEdit(entry)}
                      className="p-1 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-slate-100 transition-colors"
                      title="Edit"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => handleDelete(entry.id)}
                      className="p-1 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-slate-100 transition-colors"
                      title="Delete"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                <h3 className="text-sm font-bold text-slate-900 leading-tight">
                  {entry.subject_name || entry.subject_code}
                </h3>
                {entry.subject_code && (
                  <span className="text-[11px] font-semibold text-slate-400 block mb-2">
                    {entry.subject_code}
                  </span>
                )}

                <div className="space-y-1.5 mt-3 text-xs text-slate-600">
                  <div className="flex items-center space-x-2">
                    <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                    <span>
                      {entry.start_time} - {entry.end_time}
                    </span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                    <span>{entry.classroom}</span>
                  </div>
                  {entry.faculty_name && (
                    <div className="flex items-center space-x-2">
                      <User className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <span>{entry.faculty_name}</span>
                    </div>
                  )}
                </div>
              </div>

              {entry.notes && (
                <div className="mt-3 pt-2.5 border-t border-slate-100 text-[11px] text-slate-400 italic">
                  "{entry.notes}"
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Add / Edit Class Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingEntry ? 'Edit Class Slot' : 'Add Class to Timetable'}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="text-xs font-semibold text-slate-700">Subject *</label>
              <button
                type="button"
                onClick={() => setSubjectModalOpen(true)}
                className="text-[11px] font-semibold text-indigo-600 hover:underline"
              >
                + New Subject
              </button>
            </div>
            {subjects.length === 0 ? (
              <div className="p-3 bg-amber-50 rounded-xl border border-amber-200 text-xs text-amber-800">
                You haven't added any subjects yet.{' '}
                <button
                  type="button"
                  onClick={() => setSubjectModalOpen(true)}
                  className="font-bold underline"
                >
                  Create your first subject
                </button>{' '}
                to schedule a class.
              </div>
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
              <label className="block text-xs font-semibold text-slate-700 mb-1">Day *</label>
              <select
                value={formData.day_of_week}
                onChange={(e) => setFormData({ ...formData, day_of_week: e.target.value })}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                {DAYS.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Classroom</label>
              <input
                type="text"
                value={formData.classroom}
                onChange={(e) => setFormData({ ...formData, classroom: e.target.value })}
                placeholder="Hall 101 / Lab 3"
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Start Time *</label>
              <input
                type="time"
                value={formData.start_time}
                onChange={(e) => setFormData({ ...formData, start_time: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">End Time *</label>
              <input
                type="time"
                value={formData.end_time}
                onChange={(e) => setFormData({ ...formData, end_time: e.target.value })}
                required
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              />
            </div>
          </div>

          {/* Real-time Conflict Alert */}
          {conflictCheck.hasConflict && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-start space-x-2 text-xs text-amber-800">
              <AlertCircle className="w-4 h-4 text-amber-600 mt-0.5 shrink-0" />
              <div>
                <span className="font-bold">Collision Warning: </span>
                This slot overlaps with{' '}
                <span className="font-semibold">
                  {conflictCheck.conflictingEntry?.subject_name}
                </span>{' '}
                ({conflictCheck.conflictingEntry?.start_time} -{' '}
                {conflictCheck.conflictingEntry?.end_time}) in{' '}
                {conflictCheck.conflictingEntry?.classroom}.
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Faculty / Instructor Name
            </label>
            <input
              type="text"
              value={formData.faculty_name}
              onChange={(e) => setFormData({ ...formData, faculty_name: e.target.value })}
              placeholder="e.g. Dr. A. Sharma"
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Notes (Optional)</label>
            <input
              type="text"
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              placeholder="e.g. Bring lab coat and manual"
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
              {editingEntry ? 'Update Slot' : 'Add to Schedule'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Add Subject Modal */}
      <Modal
        isOpen={subjectModalOpen}
        onClose={() => setSubjectModalOpen(false)}
        title="Add Academic Course / Subject"
      >
        <form onSubmit={handleCreateSubject} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Subject Name *</label>
            <input
              type="text"
              value={newSubject.name}
              onChange={(e) => setNewSubject({ ...newSubject, name: e.target.value })}
              placeholder="e.g. Operating Systems"
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
                placeholder="e.g. CS301"
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

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Faculty / Professor
            </label>
            <input
              type="text"
              value={newSubject.faculty}
              onChange={(e) => setNewSubject({ ...newSubject, faculty: e.target.value })}
              placeholder="e.g. Prof. David Miller"
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Color Tag</label>
            <div className="flex items-center space-x-2">
              {[
                '#4f46e5',
                '#06b6d4',
                '#10b981',
                '#f59e0b',
                '#ef4444',
                '#8b5cf6',
                '#ec4899',
                '#0ea5e9',
              ].map((c) => (
                <button
                  type="button"
                  key={c}
                  onClick={() => setNewSubject({ ...newSubject, color: c })}
                  className={`w-6 h-6 rounded-full border-2 transition-transform ${
                    newSubject.color === c ? 'scale-125 border-slate-900 ring-2 ring-indigo-300' : 'border-transparent'
                  }`}
                  style={{ backgroundColor: c }}
                />
              ))}
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
              Save Subject
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
