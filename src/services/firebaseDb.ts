import {
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy,
  serverTimestamp,
  Timestamp,
} from 'firebase/firestore';
import { db, handleFirestoreError, OperationType } from './firebase';
import {
  StudentProfile,
  UserPreference,
  Subject,
  TimetableEntry,
  Task,
  AttendanceRecord,
  SubjectAttendanceSummary,
  OverallAttendanceSummary,
  Assessment,
  SubjectPerformanceSummary,
  PerformanceAnalytics,
  StudySession,
  VaultDocument,
  ChatConversation,
  ChatMessage,
  NotificationItem,
  DashboardSummary,
  GlobalSearchResponse,
  SearchResultItem,
} from '../types';

/* =========================================================================
 * 1. USER PROFILE & PREFERENCES
 * ========================================================================= */

export async function getUserProfile(userId: string): Promise<StudentProfile | null> {
  const p = `users/${userId}`;
  try {
    const snap = await getDoc(doc(db, 'users', userId));
    if (!snap.exists()) return null;
    const d = snap.data();
    return {
      id: snap.id,
      user_id: userId,
      email: d.email || '',
      full_name: d.fullName || 'Student',
      college: d.college || '',
      course: d.course || '',
      semester: d.semester || '',
      academic_year: d.academicYear || '2026-2027',
      avatar_url: d.avatarUrl || '',
      preferred_study_hours_per_day: d.preferredStudyHoursPerDay ?? 4.0,
      preferred_break_duration_minutes: d.preferredBreakDurationMinutes ?? 15,
      minimum_attendance_target: d.minimumAttendanceTarget ?? 75.0,
      created_at: d.createdAt || new Date().toISOString(),
      updated_at: d.updatedAt || new Date().toISOString(),
    };
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function initUserProfile(
  userId: string,
  email: string,
  fullName: string,
  extra?: Partial<StudentProfile>
): Promise<StudentProfile> {
  const p = `users/${userId}`;
  try {
    const profile: StudentProfile = {
      id: userId,
      user_id: userId,
      email,
      full_name: fullName,
      college: extra?.college || 'National Institute of Technology',
      course: extra?.course || 'B.Tech Computer Science & Engineering',
      semester: extra?.semester || 'Semester 6',
      academic_year: extra?.academic_year || '2026-2027',
      avatar_url: extra?.avatar_url || '',
      preferred_study_hours_per_day: extra?.preferred_study_hours_per_day ?? 4.0,
      preferred_break_duration_minutes: extra?.preferred_break_duration_minutes ?? 15,
      minimum_attendance_target: extra?.minimum_attendance_target ?? 75.0,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };

    await setDoc(doc(db, 'users', userId), {
      userId,
      email,
      fullName,
      college: profile.college,
      course: profile.course,
      semester: profile.semester,
      academicYear: profile.academic_year,
      avatarUrl: profile.avatar_url,
      preferredStudyHoursPerDay: profile.preferred_study_hours_per_day,
      preferredBreakDurationMinutes: profile.preferred_break_duration_minutes,
      minimumAttendanceTarget: profile.minimum_attendance_target,
      theme: 'dark',
      attendanceWarningThreshold: 75.0,
      soundEnabled: true,
      emailNotifications: true,
      reminderHoursBeforeDeadline: 24,
      autoGenerateStudyPlans: true,
      createdAt: profile.created_at,
      updatedAt: profile.updated_at,
    });

    return profile;
  } catch (error) {
    handleFirestoreError(error, OperationType.WRITE, p);
  }
}

export async function updateUserProfile(
  userId: string,
  updates: Partial<StudentProfile>
): Promise<void> {
  const p = `users/${userId}`;
  try {
    const payload: any = {
      updatedAt: new Date().toISOString(),
    };
    if (updates.full_name !== undefined) payload.fullName = updates.full_name;
    if (updates.college !== undefined) payload.college = updates.college;
    if (updates.course !== undefined) payload.course = updates.course;
    if (updates.semester !== undefined) payload.semester = updates.semester;
    if (updates.academic_year !== undefined) payload.academicYear = updates.academic_year;
    if (updates.avatar_url !== undefined) payload.avatarUrl = updates.avatar_url;
    if (updates.preferred_study_hours_per_day !== undefined)
      payload.preferredStudyHoursPerDay = Number(updates.preferred_study_hours_per_day);
    if (updates.preferred_break_duration_minutes !== undefined)
      payload.preferredBreakDurationMinutes = Number(updates.preferred_break_duration_minutes);
    if (updates.minimum_attendance_target !== undefined)
      payload.minimumAttendanceTarget = Number(updates.minimum_attendance_target);

    await updateDoc(doc(db, 'users', userId), payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

export async function getUserPreferences(userId: string): Promise<UserPreference> {
  const p = `users/${userId}`;
  try {
    const snap = await getDoc(doc(db, 'users', userId));
    if (!snap.exists()) {
      return {
        theme: 'dark',
        attendance_warning_threshold: 75.0,
        email_notifications: true,
        sound_enabled: true,
        reminder_hours_before_deadline: 24,
        auto_generate_study_plans: true,
      };
    }
    const d = snap.data();
    return {
      theme: d.theme || 'dark',
      attendance_warning_threshold: d.attendanceWarningThreshold ?? 75.0,
      email_notifications: d.emailNotifications ?? true,
      sound_enabled: d.soundEnabled ?? true,
      reminder_hours_before_deadline: d.reminderHoursBeforeDeadline ?? 24,
      auto_generate_study_plans: d.autoGenerateStudyPlans ?? true,
    };
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function updateUserPreferences(
  userId: string,
  prefs: Partial<UserPreference>
): Promise<void> {
  const p = `users/${userId}`;
  try {
    const payload: any = {
      updatedAt: new Date().toISOString(),
    };
    if (prefs.theme !== undefined) payload.theme = prefs.theme;
    if (prefs.attendance_warning_threshold !== undefined)
      payload.attendanceWarningThreshold = Number(prefs.attendance_warning_threshold);
    if (prefs.email_notifications !== undefined) payload.emailNotifications = prefs.email_notifications;
    if (prefs.sound_enabled !== undefined) payload.soundEnabled = prefs.sound_enabled;
    if (prefs.reminder_hours_before_deadline !== undefined)
      payload.reminderHoursBeforeDeadline = Number(prefs.reminder_hours_before_deadline);
    if (prefs.auto_generate_study_plans !== undefined)
      payload.autoGenerateStudyPlans = prefs.auto_generate_study_plans;

    await updateDoc(doc(db, 'users', userId), payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

/* =========================================================================
 * 2. SUBJECTS
 * ========================================================================= */

export async function getSubjects(userId: string): Promise<Subject[]> {
  const p = `users/${userId}/subjects`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'subjects'));
    const list: Subject[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      list.push({
        id: docSnap.id,
        user_id: userId,
        name: d.name,
        code: d.code,
        faculty: d.faculty || '',
        semester: d.semester || '',
        color: d.color || '#4f46e5',
        target_attendance: d.targetAttendance ?? 75.0,
        created_at: d.createdAt || new Date().toISOString(),
      });
    });
    return list.sort((a, b) => a.name.localeCompare(b.name));
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function createSubject(
  userId: string,
  data: Omit<Subject, 'id' | 'user_id' | 'created_at'>
): Promise<Subject> {
  const subjectId = 'sub_' + Math.random().toString(36).substring(2, 10);
  const p = `users/${userId}/subjects/${subjectId}`;
  try {
    const now = new Date().toISOString();
    const newDoc: Subject = {
      id: subjectId,
      user_id: userId,
      name: data.name,
      code: data.code.toUpperCase(),
      faculty: data.faculty || '',
      semester: data.semester || '',
      color: data.color || '#4f46e5',
      target_attendance: Number(data.target_attendance) || 75.0,
      created_at: now,
    };
    await setDoc(doc(db, 'users', userId, 'subjects', subjectId), {
      userId,
      name: newDoc.name,
      code: newDoc.code,
      faculty: newDoc.faculty,
      semester: newDoc.semester,
      color: newDoc.color,
      targetAttendance: newDoc.target_attendance,
      createdAt: now,
      updatedAt: now,
    });
    return newDoc;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, p);
  }
}

export async function updateSubject(
  userId: string,
  id: string,
  data: Partial<Subject>
): Promise<void> {
  const p = `users/${userId}/subjects/${id}`;
  try {
    const payload: any = { updatedAt: new Date().toISOString() };
    if (data.name !== undefined) payload.name = data.name;
    if (data.code !== undefined) payload.code = data.code.toUpperCase();
    if (data.faculty !== undefined) payload.faculty = data.faculty;
    if (data.semester !== undefined) payload.semester = data.semester;
    if (data.color !== undefined) payload.color = data.color;
    if (data.target_attendance !== undefined) payload.targetAttendance = Number(data.target_attendance);

    await updateDoc(doc(db, 'users', userId, 'subjects', id), payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

export async function deleteSubject(userId: string, id: string): Promise<void> {
  const p = `users/${userId}/subjects/${id}`;
  try {
    await deleteDoc(doc(db, 'users', userId, 'subjects', id));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, p);
  }
}

/* =========================================================================
 * 3. TIMETABLE
 * ========================================================================= */

export async function getTimetable(
  userId: string,
  dayFilter?: string
): Promise<TimetableEntry[]> {
  const p = `users/${userId}/timetable`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'timetable'));
    const list: TimetableEntry[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      list.push({
        id: docSnap.id,
        user_id: userId,
        subject_id: d.subjectId,
        subject_name: d.subjectName || '',
        subject_code: d.subjectCode || '',
        subject_color: d.subjectColor || '#4f46e5',
        day_of_week: d.dayOfWeek,
        start_time: d.startTime,
        end_time: d.endTime,
        classroom: d.classroom || '',
        faculty_name: d.facultyName || '',
        notes: d.notes || '',
        created_at: d.createdAt || new Date().toISOString(),
      });
    });

    let filtered = list;
    if (dayFilter && dayFilter !== 'All') {
      filtered = filtered.filter(
        (e) => e.day_of_week.toLowerCase() === dayFilter.toLowerCase()
      );
    }
    // Sort by day then by start_time
    const dayOrder = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
    return filtered.sort((a, b) => {
      const d1 = dayOrder.indexOf(a.day_of_week);
      const d2 = dayOrder.indexOf(b.day_of_week);
      if (d1 !== d2) return d1 - d2;
      return a.start_time.localeCompare(b.start_time);
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export function checkTimetableConflict(
  existingEntries: TimetableEntry[],
  newEntry: { day_of_week: string; start_time: string; end_time: string; id?: string }
): { hasConflict: boolean; conflictingEntry?: TimetableEntry } {
  const sameDay = existingEntries.filter(
    (e) => e.day_of_week.toLowerCase() === newEntry.day_of_week.toLowerCase() && e.id !== newEntry.id
  );
  for (const item of sameDay) {
    // Collision check: start1 < end2 && end1 > start2
    if (item.start_time < newEntry.end_time && item.end_time > newEntry.start_time) {
      return { hasConflict: true, conflictingEntry: item };
    }
  }
  return { hasConflict: false };
}

export async function createTimetableEntry(
  userId: string,
  entry: Omit<TimetableEntry, 'id' | 'user_id' | 'created_at'>
): Promise<TimetableEntry> {
  const entryId = 'tt_' + Math.random().toString(36).substring(2, 10);
  const p = `users/${userId}/timetable/${entryId}`;
  try {
    const now = new Date().toISOString();
    const item: TimetableEntry = {
      ...entry,
      id: entryId,
      user_id: userId,
      created_at: now,
    };
    await setDoc(doc(db, 'users', userId, 'timetable', entryId), {
      userId,
      subjectId: entry.subject_id,
      subjectName: entry.subject_name || '',
      subjectCode: entry.subject_code || '',
      subjectColor: entry.subject_color || '#4f46e5',
      dayOfWeek: entry.day_of_week,
      startTime: entry.start_time,
      endTime: entry.end_time,
      classroom: entry.classroom || '',
      facultyName: entry.faculty_name || '',
      notes: entry.notes || '',
      createdAt: now,
      updatedAt: now,
    });
    return item;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, p);
  }
}

export async function updateTimetableEntry(
  userId: string,
  id: string,
  data: Partial<TimetableEntry>
): Promise<void> {
  const p = `users/${userId}/timetable/${id}`;
  try {
    const payload: any = { updatedAt: new Date().toISOString() };
    if (data.subject_id !== undefined) payload.subjectId = data.subject_id;
    if (data.subject_name !== undefined) payload.subjectName = data.subject_name;
    if (data.subject_code !== undefined) payload.subjectCode = data.subject_code;
    if (data.subject_color !== undefined) payload.subjectColor = data.subject_color;
    if (data.day_of_week !== undefined) payload.dayOfWeek = data.day_of_week;
    if (data.start_time !== undefined) payload.startTime = data.start_time;
    if (data.end_time !== undefined) payload.endTime = data.end_time;
    if (data.classroom !== undefined) payload.classroom = data.classroom;
    if (data.faculty_name !== undefined) payload.facultyName = data.faculty_name;
    if (data.notes !== undefined) payload.notes = data.notes;

    await updateDoc(doc(db, 'users', userId, 'timetable', id), payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

export async function deleteTimetableEntry(userId: string, id: string): Promise<void> {
  const p = `users/${userId}/timetable/${id}`;
  try {
    await deleteDoc(doc(db, 'users', userId, 'timetable', id));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, p);
  }
}

/* =========================================================================
 * 4. TASKS & EXAMS (WITH DETERMINISTIC PRIORITY SCORING)
 * ========================================================================= */

export function calculatePriorityScore(
  taskType: string,
  priority: string,
  dueDateStr: string,
  effortMinutes: number,
  isCompleted: boolean = false
): { score: number; reason: string; isOverdue: boolean } {
  if (isCompleted) {
    return { score: 0.0, reason: 'Task completed', isOverdue: false };
  }

  const pWeights: Record<string, number> = {
    urgent: 40.0,
    high: 30.0,
    medium: 20.0,
    low: 10.0,
  };
  const pScore = pWeights[priority.toLowerCase()] ?? 20.0;

  const typeWeights: Record<string, number> = {
    examination: 35.0,
    assignment: 25.0,
    project: 20.0,
    revision: 15.0,
    personal: 10.0,
  };
  const tScore = typeWeights[taskType.toLowerCase()] ?? 15.0;

  const due = new Date(dueDateStr);
  const now = new Date();
  const diffHours = (due.getTime() - now.getTime()) / (1000 * 3600);
  const isOverdue = diffHours < 0;

  let uScore = 3.0;
  let urgencyText = '';
  if (diffHours < 0) {
    uScore = 35.0;
    urgencyText = `OVERDUE by ${Math.abs(Math.floor(diffHours))}h`;
  } else if (diffHours <= 24) {
    uScore = 30.0;
    urgencyText = `due in ${Math.max(1, Math.floor(diffHours))}h`;
  } else if (diffHours <= 72) {
    uScore = 22.0;
    urgencyText = `due in ${Math.floor(diffHours / 24)}d`;
  } else if (diffHours <= 168) {
    uScore = 15.0;
    urgencyText = `due in ${Math.floor(diffHours / 24)}d`;
  } else if (diffHours <= 336) {
    uScore = 8.0;
    urgencyText = `due in ${Math.floor(diffHours / 24)}d`;
  } else {
    uScore = 3.0;
    urgencyText = `due in ${Math.floor(diffHours / 24)}d`;
  }

  let eScore = 2.0;
  let effortText = `${effortMinutes}m effort`;
  if (effortMinutes >= 180) {
    eScore = 10.0;
    effortText = `${Math.floor(effortMinutes / 60)}h effort`;
  } else if (effortMinutes >= 120) {
    eScore = 7.0;
    effortText = '2h effort';
  } else if (effortMinutes >= 60) {
    eScore = 4.0;
    effortText = '1h effort';
  }

  const rawTotal = pScore + tScore + uScore + eScore;
  const normalized = Math.min(100.0, Math.round((rawTotal / 120.0) * 1000) / 10);
  const reason = `${taskType.charAt(0).toUpperCase() + taskType.slice(1)} (${priority} priority, ${urgencyText}, ${effortText})`;

  return { score: normalized, reason, isOverdue };
}

export async function getTasks(
  userId: string,
  params?: {
    status?: string;
    priority?: string;
    task_type?: string;
    sort_by?: string;
    search?: string;
  }
): Promise<Task[]> {
  const p = `users/${userId}/tasks`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'tasks'));
    const list: Task[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      const effort = d.estimatedEffortMinutes ?? 60;
      const isCompleted = d.status === 'completed';
      const scoring = calculatePriorityScore(
        d.taskType || 'assignment',
        d.priority || 'medium',
        d.dueDate,
        effort,
        isCompleted
      );

      list.push({
        id: docSnap.id,
        user_id: userId,
        subject_id: d.subjectId,
        subject_name: d.subjectName || '',
        subject_code: d.subjectCode || '',
        subject_color: d.subjectColor || '#4f46e5',
        title: d.title,
        description: d.description || '',
        task_type: d.taskType || 'assignment',
        due_date: d.dueDate,
        estimated_effort_minutes: effort,
        priority: d.priority || 'medium',
        priority_score: scoring.score,
        priority_reason: scoring.reason,
        status: d.status || 'pending',
        completed_at: d.completedAt,
        is_overdue: scoring.isOverdue,
        created_at: d.createdAt || new Date().toISOString(),
        updated_at: d.updatedAt || new Date().toISOString(),
      });
    });

    let filtered = list;
    if (params?.status && params.status !== 'all') {
      filtered = filtered.filter((t) => t.status === params.status);
    }
    if (params?.priority && params.priority !== 'all') {
      filtered = filtered.filter((t) => t.priority === params.priority);
    }
    if (params?.task_type && params.task_type !== 'all') {
      filtered = filtered.filter((t) => t.task_type === params.task_type);
    }
    if (params?.search && params.search.trim()) {
      const q = params.search.toLowerCase().trim();
      filtered = filtered.filter(
        (t) =>
          t.title.toLowerCase().includes(q) ||
          (t.description && t.description.toLowerCase().includes(q)) ||
          (t.subject_name && t.subject_name.toLowerCase().includes(q))
      );
    }

    // Sort
    const sortBy = params?.sort_by || 'urgency';
    if (sortBy === 'urgency') {
      filtered.sort((a, b) => b.priority_score - a.priority_score);
    } else if (sortBy === 'due_date') {
      filtered.sort((a, b) => new Date(a.due_date).getTime() - new Date(b.due_date).getTime());
    } else if (sortBy === 'created') {
      filtered.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());
    }

    return filtered;
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function createTask(
  userId: string,
  task: Omit<Task, 'id' | 'user_id' | 'created_at' | 'updated_at' | 'priority_score' | 'priority_reason' | 'is_overdue'>
): Promise<Task> {
  const taskId = 'tsk_' + Math.random().toString(36).substring(2, 10);
  const p = `users/${userId}/tasks/${taskId}`;
  try {
    const now = new Date().toISOString();
    const scoring = calculatePriorityScore(
      task.task_type,
      task.priority,
      task.due_date,
      task.estimated_effort_minutes,
      task.status === 'completed'
    );

    const fullTask: Task = {
      ...task,
      id: taskId,
      user_id: userId,
      priority_score: scoring.score,
      priority_reason: scoring.reason,
      is_overdue: scoring.isOverdue,
      created_at: now,
      updated_at: now,
    };

    await setDoc(doc(db, 'users', userId, 'tasks', taskId), {
      userId,
      subjectId: task.subject_id || '',
      subjectName: task.subject_name || '',
      subjectCode: task.subject_code || '',
      subjectColor: task.subject_color || '#4f46e5',
      title: task.title,
      description: task.description || '',
      taskType: task.task_type,
      dueDate: task.due_date,
      estimatedEffortMinutes: Number(task.estimated_effort_minutes),
      priority: task.priority,
      priorityScore: scoring.score,
      priorityReason: scoring.reason,
      status: task.status,
      completedAt: task.status === 'completed' ? now : null,
      createdAt: now,
      updatedAt: now,
    });

    return fullTask;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, p);
  }
}

export async function updateTask(
  userId: string,
  id: string,
  updates: Partial<Task>
): Promise<void> {
  const p = `users/${userId}/tasks/${id}`;
  try {
    const now = new Date().toISOString();
    const payload: any = { updatedAt: now };

    if (updates.title !== undefined) payload.title = updates.title;
    if (updates.description !== undefined) payload.description = updates.description;
    if (updates.subject_id !== undefined) payload.subjectId = updates.subject_id;
    if (updates.subject_name !== undefined) payload.subjectName = updates.subject_name;
    if (updates.subject_code !== undefined) payload.subjectCode = updates.subject_code;
    if (updates.subject_color !== undefined) payload.subjectColor = updates.subject_color;
    if (updates.task_type !== undefined) payload.taskType = updates.task_type;
    if (updates.due_date !== undefined) payload.dueDate = updates.due_date;
    if (updates.estimated_effort_minutes !== undefined)
      payload.estimatedEffortMinutes = Number(updates.estimated_effort_minutes);
    if (updates.priority !== undefined) payload.priority = updates.priority;
    if (updates.status !== undefined) {
      payload.status = updates.status;
      if (updates.status === 'completed') {
        payload.completedAt = now;
      } else {
        payload.completedAt = null;
      }
    }

    await updateDoc(doc(db, 'users', userId, 'tasks', id), payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

export async function updateTaskStatus(
  userId: string,
  id: string,
  status: 'pending' | 'in_progress' | 'completed'
): Promise<void> {
  const p = `users/${userId}/tasks/${id}`;
  try {
    const now = new Date().toISOString();
    await updateDoc(doc(db, 'users', userId, 'tasks', id), {
      status,
      completedAt: status === 'completed' ? now : null,
      updatedAt: now,
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

export async function deleteTask(userId: string, id: string): Promise<void> {
  const p = `users/${userId}/tasks/${id}`;
  try {
    await deleteDoc(doc(db, 'users', userId, 'tasks', id));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, p);
  }
}

/* =========================================================================
 * 5. ATTENDANCE (WITH COMPLIANCE & RECOVERY CALCULATOR)
 * ========================================================================= */

export function calculateClassesNeeded(present: number, total: number, targetPct: number): number {
  if (total === 0) return 0;
  const currentPct = (present / total) * 100.0;
  if (currentPct >= targetPct) return 0;

  const tRatio = targetPct / 100.0;
  if (tRatio >= 1.0) return 0;

  const numerator = tRatio * total - present;
  const denominator = 1.0 - tRatio;
  return Math.max(0, Math.ceil(numerator / denominator));
}

export function computeAttendanceSummary(
  subjects: Subject[],
  records: AttendanceRecord[],
  minThreshold: number = 75.0
): OverallAttendanceSummary {
  const subjectSummaries: SubjectAttendanceSummary[] = subjects.map((sub) => {
    const subRecords = records.filter((r) => r.subject_id === sub.id);
    const total = subRecords.length;
    const present = subRecords.filter((r) => r.status === 'present').length;
    const absent = subRecords.filter((r) => r.status === 'absent').length;
    const leave = subRecords.filter((r) => r.status === 'leave').length;
    const pct = total > 0 ? Math.round((present / total) * 1000) / 10 : 100.0;
    const target = sub.target_attendance || minThreshold;

    let statusLabel: 'safe' | 'warning' | 'critical' = 'safe';
    let classesNeeded = 0;
    let explanation = '';

    if (total === 0) {
      statusLabel = 'safe';
      classesNeeded = 0;
      explanation = 'No classes logged yet for this subject.';
    } else if (pct >= target) {
      statusLabel = 'safe';
      classesNeeded = 0;
      // Calculate how many can be missed
      const canMiss = Math.floor((100 * present - target * total) / target);
      explanation = canMiss > 0
        ? `Attendance is healthy (${pct}%). You can miss up to ${canMiss} class${canMiss > 1 ? 'es' : ''} safely.`
        : `Attendance meets the target (${pct}%). Don't miss the next class.`;
    } else if (pct >= target - 10.0) {
      statusLabel = 'warning';
      classesNeeded = calculateClassesNeeded(present, total, target);
      explanation = `Attendance warning (${pct}%). Attend next ${classesNeeded} consecutive class${classesNeeded > 1 ? 'es' : ''} to reach ${target}%.`;
    } else {
      statusLabel = 'critical';
      classesNeeded = calculateClassesNeeded(present, total, target);
      explanation = `Critical attendance deficit (${pct}%). Must attend next ${classesNeeded} consecutive class${classesNeeded > 1 ? 'es' : ''} to avoid debarment.`;
    }

    return {
      subject_id: sub.id,
      subject_name: sub.name,
      subject_code: sub.code,
      color: sub.color,
      total_classes: total,
      present_count: present,
      absent_count: absent,
      leave_count: leave,
      percentage: pct,
      target_percentage: target,
      status: statusLabel,
      classes_needed_for_target: classesNeeded,
      explanation,
    };
  });

  const totalConducted = records.length;
  const totalPresent = records.filter((r) => r.status === 'present').length;
  const totalAbsent = records.filter((r) => r.status === 'absent').length;
  const totalLeave = records.filter((r) => r.status === 'leave').length;
  const overallPct = totalConducted > 0 ? Math.round((totalPresent / totalConducted) * 1000) / 10 : 100.0;
  const isWarning = totalConducted > 0 && overallPct < minThreshold;

  return {
    overall_percentage: overallPct,
    total_conducted: totalConducted,
    total_present: totalPresent,
    total_absent: totalAbsent,
    total_leave: totalLeave,
    target_percentage: minThreshold,
    is_warning: isWarning,
    subject_summaries: subjectSummaries,
  };
}

export async function getAttendanceRecords(
  userId: string,
  params?: { subject_id?: string; status_filter?: string }
): Promise<AttendanceRecord[]> {
  const p = `users/${userId}/attendance`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'attendance'));
    const list: AttendanceRecord[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      list.push({
        id: docSnap.id,
        user_id: userId,
        subject_id: d.subjectId,
        subject_name: d.subjectName || '',
        subject_code: d.subjectCode || '',
        date: d.date,
        status: d.status,
        notes: d.notes || '',
        created_at: d.createdAt || new Date().toISOString(),
      });
    });

    let filtered = list;
    if (params?.subject_id) {
      filtered = filtered.filter((r) => r.subject_id === params.subject_id);
    }
    if (params?.status_filter && params.status_filter !== 'all') {
      filtered = filtered.filter((r) => r.status === params.status_filter);
    }

    return filtered.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function recordAttendance(
  userId: string,
  rec: Omit<AttendanceRecord, 'id' | 'user_id' | 'created_at'>
): Promise<AttendanceRecord> {
  const recordId = 'att_' + Math.random().toString(36).substring(2, 10);
  const p = `users/${userId}/attendance/${recordId}`;
  try {
    const now = new Date().toISOString();
    const item: AttendanceRecord = {
      ...rec,
      id: recordId,
      user_id: userId,
      created_at: now,
    };
    await setDoc(doc(db, 'users', userId, 'attendance', recordId), {
      userId,
      subjectId: rec.subject_id,
      subjectName: rec.subject_name || '',
      subjectCode: rec.subject_code || '',
      date: rec.date,
      status: rec.status,
      notes: rec.notes || '',
      createdAt: now,
      updatedAt: now,
    });
    return item;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, p);
  }
}

export async function deleteAttendanceRecord(userId: string, id: string): Promise<void> {
  const p = `users/${userId}/attendance/${id}`;
  try {
    await deleteDoc(doc(db, 'users', userId, 'attendance', id));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, p);
  }
}

/* =========================================================================
 * 6. ASSESSMENTS & MARKS ANALYTICS
 * ========================================================================= */

export function computeMarksAnalytics(
  subjects: Subject[],
  assessments: Assessment[]
): PerformanceAnalytics {
  const subjectPerformance: SubjectPerformanceSummary[] = subjects.map((sub) => {
    const subAss = assessments.filter((a) => a.subject_id === sub.id);
    const count = subAss.length;
    let avg = 0;
    let maxPct = 0;
    let minPct = 0;

    if (count > 0) {
      const pcts = subAss.map((a) => a.percentage);
      avg = Math.round((pcts.reduce((sum, v) => sum + v, 0) / count) * 10) / 10;
      maxPct = Math.max(...pcts);
      minPct = Math.min(...pcts);
    }

    return {
      subject_id: sub.id,
      subject_name: sub.name,
      subject_code: sub.code,
      color: sub.color,
      assessment_count: count,
      average_percentage: avg,
      highest_percentage: maxPct,
      lowest_percentage: minPct,
      is_weak_subject: count > 0 && avg < 65.0,
    };
  });

  const totalAss = assessments.length;
  let overallAvg = 0;
  if (totalAss > 0) {
    overallAvg =
      Math.round(
        (assessments.reduce((sum, a) => sum + a.percentage, 0) / totalAss) * 10
      ) / 10;
  }

  const weakSubjects = subjectPerformance
    .filter((sp) => sp.is_weak_subject)
    .map((sp) => sp.subject_name);

  const insights: string[] = [];
  if (totalAss === 0) {
    insights.push('Add your recent tests or quiz scores to unlock academic analytics.');
  } else {
    if (overallAvg >= 80) {
      insights.push(`Strong academic performance! Your overall average is ${overallAvg}%. Keep up the consistency.`);
    } else if (overallAvg >= 65) {
      insights.push(`Good standing (${overallAvg}%). Dedicated study sessions in weaker modules can push your grade to Distinction.`);
    } else {
      insights.push(`Academic attention needed: Overall average is ${overallAvg}%. Priority study sessions recommended.`);
    }

    if (weakSubjects.length > 0) {
      insights.push(
        `Focus areas (<65% average): ${weakSubjects.join(', ')}. Use Smart Study Planner to reinforce these topics.`
      );
    } else if (totalAss > 2) {
      insights.push('All subjects are currently performing above the 65% threshold.');
    }
  }

  return {
    overall_average: overallAvg,
    total_assessments: totalAss,
    subject_performance: subjectPerformance,
    weak_subjects: weakSubjects,
    insights,
    recent_assessments: assessments.slice(0, 10),
  };
}

export async function getAssessments(
  userId: string,
  subjectId?: string
): Promise<Assessment[]> {
  const p = `users/${userId}/assessments`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'assessments'));
    const list: Assessment[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      const obtained = Number(d.marksObtained);
      const maximum = Number(d.maximumMarks);
      const pct = maximum > 0 ? Math.round((obtained / maximum) * 1000) / 10 : 0;
      list.push({
        id: docSnap.id,
        user_id: userId,
        subject_id: d.subjectId,
        subject_name: d.subjectName || '',
        subject_code: d.subjectCode || '',
        subject_color: d.subjectColor || '#4f46e5',
        assessment_name: d.assessmentName,
        assessment_type: d.assessmentType || 'Quiz',
        marks_obtained: obtained,
        maximum_marks: maximum,
        percentage: pct,
        assessment_date: d.assessmentDate,
        notes: d.notes || '',
        created_at: d.createdAt || new Date().toISOString(),
      });
    });

    let filtered = list;
    if (subjectId) {
      filtered = filtered.filter((a) => a.subject_id === subjectId);
    }

    return filtered.sort(
      (a, b) => new Date(b.assessment_date).getTime() - new Date(a.assessment_date).getTime()
    );
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function createAssessment(
  userId: string,
  data: Omit<Assessment, 'id' | 'user_id' | 'percentage' | 'created_at'>
): Promise<Assessment> {
  const assessmentId = 'ass_' + Math.random().toString(36).substring(2, 10);
  const p = `users/${userId}/assessments/${assessmentId}`;
  try {
    const now = new Date().toISOString();
    const obtained = Number(data.marks_obtained);
    const maximum = Number(data.maximum_marks);
    const pct = maximum > 0 ? Math.round((obtained / maximum) * 1000) / 10 : 0;

    const item: Assessment = {
      ...data,
      id: assessmentId,
      user_id: userId,
      marks_obtained: obtained,
      maximum_marks: maximum,
      percentage: pct,
      created_at: now,
    };

    await setDoc(doc(db, 'users', userId, 'assessments', assessmentId), {
      userId,
      subjectId: data.subject_id,
      subjectName: data.subject_name || '',
      subjectCode: data.subject_code || '',
      subjectColor: data.subject_color || '#4f46e5',
      assessmentName: data.assessment_name,
      assessmentType: data.assessment_type,
      marksObtained: obtained,
      maximumMarks: maximum,
      percentage: pct,
      assessmentDate: data.assessment_date,
      notes: data.notes || '',
      createdAt: now,
      updatedAt: now,
    });

    return item;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, p);
  }
}

export async function updateAssessment(
  userId: string,
  id: string,
  data: Partial<Assessment>
): Promise<void> {
  const p = `users/${userId}/assessments/${id}`;
  try {
    const payload: any = { updatedAt: new Date().toISOString() };
    if (data.assessment_name !== undefined) payload.assessmentName = data.assessment_name;
    if (data.assessment_type !== undefined) payload.assessmentType = data.assessment_type;
    if (data.subject_id !== undefined) payload.subjectId = data.subject_id;
    if (data.subject_name !== undefined) payload.subjectName = data.subject_name;
    if (data.subject_code !== undefined) payload.subjectCode = data.subject_code;
    if (data.subject_color !== undefined) payload.subjectColor = data.subject_color;
    if (data.assessment_date !== undefined) payload.assessmentDate = data.assessment_date;
    if (data.notes !== undefined) payload.notes = data.notes;
    if (data.marks_obtained !== undefined) payload.marksObtained = Number(data.marks_obtained);
    if (data.maximum_marks !== undefined) payload.maximumMarks = Number(data.maximum_marks);
    if (data.marks_obtained !== undefined && data.maximum_marks !== undefined) {
      payload.percentage =
        Number(data.maximum_marks) > 0
          ? Math.round((Number(data.marks_obtained) / Number(data.maximum_marks)) * 1000) / 10
          : 0;
    }

    await updateDoc(doc(db, 'users', userId, 'assessments', id), payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

export async function deleteAssessment(userId: string, id: string): Promise<void> {
  const p = `users/${userId}/assessments/${id}`;
  try {
    await deleteDoc(doc(db, 'users', userId, 'assessments', id));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, p);
  }
}

/* =========================================================================
 * 7. STUDY SESSIONS & AI STUDY PLANNER
 * ========================================================================= */

export async function getStudySessions(
  userId: string,
  dateFilter?: string
): Promise<StudySession[]> {
  const p = `users/${userId}/study_sessions`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'study_sessions'));
    const list: StudySession[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      list.push({
        id: docSnap.id,
        user_id: userId,
        subject_id: d.subjectId,
        subject_name: d.subjectName || '',
        subject_color: d.subjectColor || '#6366f1',
        taskId: d.taskId,
        taskTitle: d.taskTitle,
        title: d.title,
        session_date: d.sessionDate,
        start_time: d.startTime,
        end_time: d.endTime,
        duration_minutes: d.durationMinutes ?? 60,
        status: d.status || 'scheduled',
        recommendation_reason: d.recommendationReason || '',
        is_accepted: d.isAccepted ?? true,
        created_at: d.createdAt || new Date().toISOString(),
      });
    });

    let filtered = list;
    if (dateFilter) {
      filtered = filtered.filter((s) => s.session_date === dateFilter);
    }

    return filtered.sort((a, b) => {
      const cmp = a.session_date.localeCompare(b.session_date);
      if (cmp !== 0) return cmp;
      return a.start_time.localeCompare(b.start_time);
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function createStudySession(
  userId: string,
  session: Omit<StudySession, 'id' | 'user_id' | 'created_at'>
): Promise<StudySession> {
  const sessionId = 'ses_' + Math.random().toString(36).substring(2, 10);
  const p = `users/${userId}/study_sessions/${sessionId}`;
  try {
    const now = new Date().toISOString();
    const item: StudySession = {
      ...session,
      id: sessionId,
      user_id: userId,
      created_at: now,
    };
    await setDoc(doc(db, 'users', userId, 'study_sessions', sessionId), {
      userId,
      subjectId: session.subject_id || '',
      subjectName: session.subject_name || '',
      subjectColor: session.subject_color || '#6366f1',
      taskId: session.taskId || '',
      taskTitle: session.taskTitle || '',
      title: session.title,
      sessionDate: session.session_date,
      startTime: session.start_time,
      endTime: session.end_time,
      durationMinutes: Number(session.duration_minutes),
      status: session.status || 'scheduled',
      recommendationReason: session.recommendation_reason || '',
      isAccepted: session.is_accepted ?? true,
      createdAt: now,
      updatedAt: now,
    });
    return item;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, p);
  }
}

export async function updateStudySession(
  userId: string,
  id: string,
  data: Partial<StudySession>
): Promise<void> {
  const p = `users/${userId}/study_sessions/${id}`;
  try {
    const payload: any = { updatedAt: new Date().toISOString() };
    if (data.title !== undefined) payload.title = data.title;
    if (data.status !== undefined) payload.status = data.status;
    if (data.session_date !== undefined) payload.sessionDate = data.session_date;
    if (data.start_time !== undefined) payload.startTime = data.start_time;
    if (data.end_time !== undefined) payload.endTime = data.end_time;
    if (data.duration_minutes !== undefined) payload.durationMinutes = Number(data.duration_minutes);
    if (data.is_accepted !== undefined) payload.isAccepted = data.is_accepted;

    await updateDoc(doc(db, 'users', userId, 'study_sessions', id), payload);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

export async function deleteStudySession(userId: string, id: string): Promise<void> {
  const p = `users/${userId}/study_sessions/${id}`;
  try {
    await deleteDoc(doc(db, 'users', userId, 'study_sessions', id));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, p);
  }
}

export async function autoGenerateStudyPlan(
  userId: string,
  options: {
    days_ahead?: number;
    daily_available_hours?: number;
    break_duration_minutes?: number;
  }
): Promise<StudySession[]> {
  const [subjects, timetable, tasks, assessments] = await Promise.all([
    getSubjects(userId),
    getTimetable(userId),
    getTasks(userId, { status: 'pending' }),
    getAssessments(userId),
  ]);

  if (subjects.length === 0 && tasks.length === 0) {
    return [];
  }

  const daysAhead = options.days_ahead || 7;
  const breakMins = options.break_duration_minutes || 15;
  const createdSessions: StudySession[] = [];
  const daysOfWeek = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  // Identify weak subjects
  const analytics = computeMarksAnalytics(subjects, assessments);
  const weakSubNames = new Set(analytics.weak_subjects);

  // Urgent pending tasks
  const urgentTasks = [...tasks].sort((a, b) => b.priority_score - a.priority_score);

  for (let d = 0; d < daysAhead; d++) {
    const targetDate = new Date();
    targetDate.setDate(targetDate.getDate() + d);
    const dateStr = targetDate.toISOString().slice(0, 10);
    const dayName = daysOfWeek[targetDate.getDay()];

    // Look at timetable classes for this day
    const dayClasses = timetable.filter(
      (c) => c.day_of_week.toLowerCase() === dayName.toLowerCase()
    );

    // Evening windows: 17:00 - 18:30 (slot 1), 19:00 - 20:30 (slot 2)
    const slotTimes = [
      { start: '17:00', end: '18:30', duration: 90 },
      { start: '19:30', end: '21:00', duration: 90 },
    ];

    for (let sIdx = 0; sIdx < slotTimes.length; sIdx++) {
      const slot = slotTimes[sIdx];
      // Check collision with classes (rare in evening)
      const hasClassCollision = dayClasses.some(
        (c) => c.start_time < slot.end && c.end_time > slot.start
      );
      if (hasClassCollision) continue;

      let chosenSubject: Subject | undefined;
      let associatedTask: Task | undefined;
      let title = 'Study Session';
      let reason = 'Daily scheduled revision block';

      // 1. Check if an urgent task due within 3 days
      const pendingTaskForDay = urgentTasks.find(
        (t) =>
          !createdSessions.some((cs) => cs.taskId === t.id && cs.session_date === dateStr)
      );

      if (pendingTaskForDay) {
        associatedTask = pendingTaskForDay;
        chosenSubject = subjects.find((s) => s.id === pendingTaskForDay.subject_id);
        title = `Focus: ${pendingTaskForDay.title}`;
        reason = `Urgent deadline: ${pendingTaskForDay.priority_reason || 'Pending coursework'}`;
      } else {
        // 2. Pick a weak subject or rotate subject
        const weakSub = subjects.find((s) => weakSubNames.has(s.name));
        if (weakSub) {
          chosenSubject = weakSub;
          title = `Revision & Practice: ${weakSub.name}`;
          reason = `Reinforce weak subject (<65% average performance)`;
        } else if (subjects.length > 0) {
          const rotSub = subjects[(d * 2 + sIdx) % subjects.length];
          chosenSubject = rotSub;
          title = `Coursework Review: ${rotSub.name}`;
          reason = `Regular syllabus pacing for ${rotSub.code}`;
        }
      }

      if (!chosenSubject && !associatedTask) continue;

      const session = await createStudySession(userId, {
        subject_id: chosenSubject?.id,
        subject_name: chosenSubject?.name,
        subject_color: chosenSubject?.color || '#6366f1',
        taskId: associatedTask?.id,
        taskTitle: associatedTask?.title,
        title,
        session_date: dateStr,
        start_time: slot.start,
        end_time: slot.end,
        duration_minutes: slot.duration,
        status: 'scheduled',
        recommendation_reason: reason,
        is_accepted: true,
      });

      createdSessions.push(session);
    }
  }

  return createdSessions;
}

/* =========================================================================
 * 8. DOCUMENT VAULT
 * ========================================================================= */

export async function getDocuments(
  userId: string,
  categoryFilter?: string,
  subjectId?: string
): Promise<VaultDocument[]> {
  const p = `users/${userId}/documents`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'documents'));
    const list: VaultDocument[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      list.push({
        id: docSnap.id,
        user_id: userId,
        subject_id: d.subjectId,
        subject_name: d.subjectName || '',
        title: d.title,
        category: d.category || 'study_material',
        original_filename: d.originalFilename || d.title,
        file_type: d.fileType || 'txt',
        file_size: d.fileSize || 1024,
        has_extracted_text: !!d.extractedText,
        extracted_text: d.extractedText || '',
        created_at: d.createdAt || new Date().toISOString(),
      });
    });

    let filtered = list;
    if (categoryFilter && categoryFilter !== 'all') {
      filtered = filtered.filter((doc) => doc.category === categoryFilter);
    }
    if (subjectId) {
      filtered = filtered.filter((doc) => doc.subject_id === subjectId);
    }

    return filtered.sort(
      (a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
    );
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function createDocument(
  userId: string,
  docData: {
    title: string;
    category: string;
    subject_id?: string;
    subject_name?: string;
    file_type?: string;
    file_size?: number;
    extracted_text?: string;
    original_filename?: string;
  }
): Promise<VaultDocument> {
  const docId = 'doc_' + Math.random().toString(36).substring(2, 10);
  const p = `users/${userId}/documents/${docId}`;
  try {
    const now = new Date().toISOString();
    const item: VaultDocument = {
      id: docId,
      user_id: userId,
      subject_id: docData.subject_id,
      subject_name: docData.subject_name || '',
      title: docData.title,
      category: docData.category,
      original_filename: docData.original_filename || docData.title,
      file_type: docData.file_type || 'txt',
      file_size: docData.file_size || 1024,
      has_extracted_text: !!docData.extracted_text,
      extracted_text: docData.extracted_text || '',
      created_at: now,
    };

    await setDoc(doc(db, 'users', userId, 'documents', docId), {
      userId,
      subjectId: docData.subject_id || '',
      subjectName: docData.subject_name || '',
      title: docData.title,
      category: docData.category,
      originalFilename: item.original_filename,
      fileType: item.file_type,
      fileSize: item.file_size,
      extractedText: item.extracted_text,
      createdAt: now,
      updatedAt: now,
    });

    return item;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, p);
  }
}

export async function deleteDocument(userId: string, id: string): Promise<void> {
  const p = `users/${userId}/documents/${id}`;
  try {
    await deleteDoc(doc(db, 'users', userId, 'documents', id));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, p);
  }
}

/* =========================================================================
 * 9. AI CHAT & ACADEMIC ASSISTANT
 * ========================================================================= */

export async function getConversations(userId: string): Promise<ChatConversation[]> {
  const p = `users/${userId}/conversations`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'conversations'));
    const list: ChatConversation[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      list.push({
        id: docSnap.id,
        title: d.title || 'Academic Chat',
        created_at: d.createdAt || new Date().toISOString(),
        updated_at: d.updatedAt || new Date().toISOString(),
        message_count: d.messageCount || 0,
      });
    });
    return list.sort((a, b) => new Date(b.updated_at).getTime() - new Date(a.updated_at).getTime());
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function createConversation(
  userId: string,
  title: string = 'New Conversation'
): Promise<ChatConversation> {
  const convId = 'conv_' + Math.random().toString(36).substring(2, 10);
  const p = `users/${userId}/conversations/${convId}`;
  try {
    const now = new Date().toISOString();
    const item: ChatConversation = {
      id: convId,
      title,
      created_at: now,
      updated_at: now,
      message_count: 0,
    };
    await setDoc(doc(db, 'users', userId, 'conversations', convId), {
      userId,
      title,
      messageCount: 0,
      createdAt: now,
      updatedAt: now,
    });
    return item;
  } catch (error) {
    handleFirestoreError(error, OperationType.CREATE, p);
  }
}

export async function deleteConversation(userId: string, convId: string): Promise<void> {
  const p = `users/${userId}/conversations/${convId}`;
  try {
    await deleteDoc(doc(db, 'users', userId, 'conversations', convId));
  } catch (error) {
    handleFirestoreError(error, OperationType.DELETE, p);
  }
}

export async function getChatMessages(
  userId: string,
  convId: string
): Promise<ChatMessage[]> {
  const p = `users/${userId}/conversations/${convId}/messages`;
  try {
    const snap = await getDocs(
      collection(db, 'users', userId, 'conversations', convId, 'messages')
    );
    const list: ChatMessage[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      let sources = undefined;
      if (d.sources) {
        try {
          sources = typeof d.sources === 'string' ? JSON.parse(d.sources) : d.sources;
        } catch {
          sources = undefined;
        }
      }
      list.push({
        id: docSnap.id,
        conversation_id: convId,
        role: d.role,
        content: d.content,
        sources,
        is_demo: false,
        created_at: d.createdAt || new Date().toISOString(),
      });
    });
    return list.sort((a, b) => new Date(a.created_at).getTime() - new Date(b.created_at).getTime());
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function sendChatMessage(
  userId: string,
  convId: string,
  userText: string
): Promise<{ userMessage: ChatMessage; assistantMessage: ChatMessage }> {
  const now = new Date().toISOString();
  const userMsgId = 'msg_' + Math.random().toString(36).substring(2, 10);
  const pUser = `users/${userId}/conversations/${convId}/messages/${userMsgId}`;

  // 1. Save user message
  const userMessage: ChatMessage = {
    id: userMsgId,
    conversation_id: convId,
    role: 'user',
    content: userText,
    is_demo: false,
    created_at: now,
  };

  try {
    await setDoc(
      doc(db, 'users', userId, 'conversations', convId, 'messages', userMsgId),
      {
        conversationId: convId,
        userId,
        role: 'user',
        content: userText,
        createdAt: now,
      }
    );
  } catch (err) {
    handleFirestoreError(err, OperationType.CREATE, pUser);
  }

  // 2. Fetch student context for RAG grounding
  const [profile, subjects, timetable, tasks, attendance, assessments, docs] =
    await Promise.all([
      getUserProfile(userId),
      getSubjects(userId),
      getTimetable(userId),
      getTasks(userId),
      getAttendanceRecords(userId),
      getAssessments(userId),
      getDocuments(userId),
    ]);

  const attSummary = computeAttendanceSummary(
    subjects || [],
    attendance || [],
    profile?.minimum_attendance_target || 75.0
  );
  const perfAnalytics = computeMarksAnalytics(subjects || [], assessments || []);

  // Determine Grounded Response & Citations
  const queryLower = userText.toLowerCase();
  const sources: { source_type: string; title: string; reference_detail: string }[] = [];
  let answerText = '';

  // Class / Timetable query
  if (
    queryLower.includes('class') ||
    queryLower.includes('timetable') ||
    queryLower.includes('schedule') ||
    queryLower.includes('today')
  ) {
    const todayName = [
      'Sunday',
      'Monday',
      'Tuesday',
      'Wednesday',
      'Thursday',
      'Friday',
      'Saturday',
    ][new Date().getDay()];
    const todays = (timetable || []).filter(
      (c) => c.day_of_week.toLowerCase() === todayName.toLowerCase()
    );

    if (todays.length > 0) {
      answerText = `You have ${todays.length} class${todays.length > 1 ? 'es' : ''} scheduled for today (${todayName}):\n` +
        todays
          .map(
            (c) =>
              `• **${c.subject_name || c.subject_code}** from ${c.start_time} to ${c.end_time} in ${c.classroom || 'Lecture Hall'}${c.faculty_name ? ` (Faculty: ${c.faculty_name})` : ''}`
          )
          .join('\n');
      todays.forEach((c) => {
        sources.push({
          source_type: 'Timetable',
          title: `${c.day_of_week} Class Schedule`,
          reference_detail: `${c.subject_name || c.subject_code} (${c.start_time} - ${c.end_time}, ${c.classroom})`,
        });
      });
    } else {
      answerText = `You have no registered classes for today (${todayName}). Enjoy your study window or check other days in your Timetable!`;
    }
  }
  // Attendance query
  else if (
    queryLower.includes('attendance') ||
    queryLower.includes('present') ||
    queryLower.includes('absent') ||
    queryLower.includes('percentage')
  ) {
    if (attSummary.subject_summaries.length === 0) {
      answerText = `You haven't logged attendance for any subjects yet. Once you record your classes in the Attendance tab, I will calculate your compliance and recovery targets!`;
    } else {
      answerText =
        `Here is your live attendance summary:\n` +
        `• **Overall Attendance**: ${attSummary.overall_percentage}% across ${attSummary.total_conducted} logged sessions.\n` +
        attSummary.subject_summaries
          .map(
            (s) =>
              `• **${s.subject_name}** (${s.subject_code}): ${s.percentage}% (${s.present_count}/${s.total_classes} present). ${s.explanation}`
          )
          .join('\n');

      attSummary.subject_summaries.forEach((s) => {
        sources.push({
          source_type: 'Attendance Ledger',
          title: `${s.subject_name} Attendance Status`,
          reference_detail: `${s.percentage}% (${s.status.toUpperCase()}) - ${s.explanation}`,
        });
      });
    }
  }
  // Tasks, Assignments, Exams query
  else if (
    queryLower.includes('task') ||
    queryLower.includes('assignment') ||
    queryLower.includes('exam') ||
    queryLower.includes('due') ||
    queryLower.includes('deadline') ||
    queryLower.includes('project')
  ) {
    const pendingTasks = (tasks || []).filter((t) => t.status !== 'completed');
    if (pendingTasks.length === 0) {
      answerText = `Great news! You currently have 0 pending tasks or assignments. You're completely up to date!`;
    } else {
      answerText =
        `You have ${pendingTasks.length} active coursework task${pendingTasks.length > 1 ? 's' : ''}:\n` +
        pendingTasks
          .slice(0, 5)
          .map(
            (t) =>
              `• **${t.title}** (${t.task_type.toUpperCase()}) — Priority Score: ${t.priority_score}/100 [${t.priority_reason}]\n  Due: ${new Date(t.due_date).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}`
          )
          .join('\n');

      pendingTasks.slice(0, 3).forEach((t) => {
        sources.push({
          source_type: 'Tasks & Exam Registry',
          title: t.title,
          reference_detail: `Score: ${t.priority_score}/100, Due: ${t.due_date}`,
        });
      });
    }
  }
  // Marks / Grades / Performance query
  else if (
    queryLower.includes('mark') ||
    queryLower.includes('score') ||
    queryLower.includes('grade') ||
    queryLower.includes('assessment') ||
    queryLower.includes('weak') ||
    queryLower.includes('average')
  ) {
    if (perfAnalytics.total_assessments === 0) {
      answerText = `No assessments or test marks have been logged yet. Go to Marks & Analytics to add your test scores!`;
    } else {
      answerText =
        `Here is your academic performance overview:\n` +
        `• **Cumulative Average**: ${perfAnalytics.overall_average}% over ${perfAnalytics.total_assessments} assessments.\n` +
        `• **Subject Breakdown**:\n` +
        perfAnalytics.subject_performance
          .filter((sp) => sp.assessment_count > 0)
          .map(
            (sp) =>
              `  - ${sp.subject_name}: ${sp.average_percentage}% avg (Highest: ${sp.highest_percentage}%, Lowest: ${sp.lowest_percentage}%)`
          )
          .join('\n') +
        (perfAnalytics.weak_subjects.length > 0
          ? `\n\n⚠️ **Attention Areas (<65%)**: ${perfAnalytics.weak_subjects.join(', ')}. Recommend dedicated review.`
          : '\n\n✨ All subjects currently meet or exceed the 65% benchmark!');

      perfAnalytics.subject_performance.forEach((sp) => {
        if (sp.assessment_count > 0) {
          sources.push({
            source_type: 'Marks Analytics',
            title: `${sp.subject_name} Performance`,
            reference_detail: `Avg: ${sp.average_percentage}%, Tests: ${sp.assessment_count}`,
          });
        }
      });
    }
  }
  // Document Vault query
  else if (
    queryLower.includes('note') ||
    queryLower.includes('document') ||
    queryLower.includes('vault') ||
    queryLower.includes('syllabus') ||
    (docs && docs.some((d) => queryLower.includes(d.title.toLowerCase())))
  ) {
    const matchingDocs = (docs || []).filter(
      (d) =>
        queryLower.includes(d.title.toLowerCase()) ||
        (d.extracted_text && queryLower.includes(d.extracted_text.toLowerCase().slice(0, 20)))
    );

    if (matchingDocs.length > 0) {
      const best = matchingDocs[0];
      answerText =
        `Found matching material in your Document Vault for **${best.title}** (${best.category}):\n\n` +
        (best.extracted_text
          ? `"${best.extracted_text.slice(0, 450)}${best.extracted_text.length > 450 ? '...' : ''}"`
          : `Document is stored with size ${(best.file_size / 1024).toFixed(1)} KB.`);
      sources.push({
        source_type: 'Document Vault',
        title: best.title,
        reference_detail: `Category: ${best.category}, ${best.file_type.toUpperCase()}`,
      });
    } else if (docs && docs.length > 0) {
      answerText =
        `You have ${docs.length} document${docs.length > 1 ? 's' : ''} stored in your vault:\n` +
        docs.map((d) => `• **${d.title}** (${d.category})`).join('\n') +
        `\n\nAsk me specific questions about any of these subjects or topics!`;
    } else {
      answerText = `You haven't uploaded course documents or notes to your Vault yet. Upload syllabi or study materials to enable full retrieval here!`;
    }
  }
  // General student academic advice
  else {
    answerText =
      `Hello ${profile?.full_name || 'Student'}! I am your AI Academic Assistant for ${profile?.academic_year || '2026-2027'}.\n\n` +
      `I have live access to your registered subjects, timetable, tasks, attendance records, test marks, and document notes. You can ask me:\n` +
      `• *"What classes do I have today?"*\n` +
      `• *"What is my attendance status and how many classes do I need to attend?"*\n` +
      `• *"Which assignments are due this week?"*\n` +
      `• *"What are my weakest subjects according to test marks?"*\n` +
      `• *"Summarize my lecture notes or study materials"*`;
  }

  // Save assistant message
  const asstMsgId = 'msg_' + Math.random().toString(36).substring(2, 10);
  const pAsst = `users/${userId}/conversations/${convId}/messages/${asstMsgId}`;
  const assistantMessage: ChatMessage = {
    id: asstMsgId,
    conversation_id: convId,
    role: 'assistant',
    content: answerText,
    sources: sources.length > 0 ? sources : undefined,
    is_demo: false,
    created_at: new Date().toISOString(),
  };

  try {
    await setDoc(
      doc(db, 'users', userId, 'conversations', convId, 'messages', asstMsgId),
      {
        conversationId: convId,
        userId,
        role: 'assistant',
        content: answerText,
        sources: sources.length > 0 ? JSON.stringify(sources) : null,
        createdAt: assistantMessage.created_at,
      }
    );

    // Update conversation updated_at and message_count
    await updateDoc(doc(db, 'users', userId, 'conversations', convId), {
      updatedAt: assistantMessage.created_at,
      messageCount: 2, // incremented
    });
  } catch (err) {
    handleFirestoreError(err, OperationType.CREATE, pAsst);
  }

  return { userMessage, assistantMessage };
}

/* =========================================================================
 * 10. NOTIFICATIONS
 * ========================================================================= */

export async function getNotifications(userId: string): Promise<NotificationItem[]> {
  const p = `users/${userId}/notifications`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'notifications'));
    const list: NotificationItem[] = [];
    snap.forEach((docSnap) => {
      const d = docSnap.data();
      list.push({
        id: docSnap.id,
        user_id: userId,
        title: d.title,
        message: d.message,
        notification_type: d.notificationType || 'general',
        link_url: d.linkUrl || '',
        is_read: d.isRead ?? false,
        created_at: d.createdAt || new Date().toISOString(),
      });
    });

    // Check if dynamic notifications should be generated from live tasks/attendance
    if (list.length === 0) {
      const [tasks, subjects, records] = await Promise.all([
        getTasks(userId, { status: 'pending' }),
        getSubjects(userId),
        getAttendanceRecords(userId),
      ]);
      const att = computeAttendanceSummary(subjects || [], records || []);

      // If overdue or urgent task
      const now = new Date();
      for (const t of (tasks || []).slice(0, 3)) {
        const due = new Date(t.due_date);
        const hoursLeft = (due.getTime() - now.getTime()) / (1000 * 3600);
        if (hoursLeft <= 48) {
          const notifId = 'notif_' + Math.random().toString(36).substring(2, 9);
          const notif: NotificationItem = {
            id: notifId,
            user_id: userId,
            title: `Urgent Coursework: ${t.title}`,
            message: `${t.task_type.toUpperCase()} is due ${hoursLeft < 0 ? 'now (OVERDUE)' : `in ${Math.floor(hoursLeft)} hours`}. Priority Score: ${t.priority_score}/100.`,
            notification_type: t.task_type === 'examination' ? 'exam' : 'deadline',
            link_url: '/tasks',
            is_read: false,
            created_at: now.toISOString(),
          };
          try {
            await setDoc(doc(db, 'users', userId, 'notifications', notifId), {
              userId,
              title: notif.title,
              message: notif.message,
              notificationType: notif.notification_type,
              linkUrl: notif.link_url,
              isRead: false,
              createdAt: notif.created_at,
              updatedAt: notif.created_at,
            });
            list.push(notif);
          } catch {}
        }
      }

      // If low attendance warning
      for (const s of att.subject_summaries) {
        if (s.status === 'warning' || s.status === 'critical') {
          const notifId = 'notif_' + Math.random().toString(36).substring(2, 9);
          const notif: NotificationItem = {
            id: notifId,
            user_id: userId,
            title: `Attendance Alert: ${s.subject_name}`,
            message: `${s.explanation} Currently at ${s.percentage}% (Target: ${s.target_percentage}%).`,
            notification_type: 'attendance_warning',
            link_url: '/attendance',
            is_read: false,
            created_at: now.toISOString(),
          };
          try {
            await setDoc(doc(db, 'users', userId, 'notifications', notifId), {
              userId,
              title: notif.title,
              message: notif.message,
              notificationType: notif.notification_type,
              linkUrl: notif.link_url,
              isRead: false,
              createdAt: notif.created_at,
              updatedAt: notif.created_at,
            });
            list.push(notif);
          } catch {}
        }
      }
    }

    return list.sort(
      (a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
    );
  } catch (error) {
    handleFirestoreError(error, OperationType.GET, p);
  }
}

export async function markNotificationRead(userId: string, id: string): Promise<void> {
  const p = `users/${userId}/notifications/${id}`;
  try {
    await updateDoc(doc(db, 'users', userId, 'notifications', id), {
      isRead: true,
      updatedAt: new Date().toISOString(),
    });
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

export async function markAllNotificationsRead(userId: string): Promise<void> {
  const p = `users/${userId}/notifications`;
  try {
    const snap = await getDocs(collection(db, 'users', userId, 'notifications'));
    const updates = snap.docs.map((d) =>
      updateDoc(doc(db, 'users', userId, 'notifications', d.id), {
        isRead: true,
        updatedAt: new Date().toISOString(),
      })
    );
    await Promise.all(updates);
  } catch (error) {
    handleFirestoreError(error, OperationType.UPDATE, p);
  }
}

/* =========================================================================
 * 11. GLOBAL SEARCH
 * ========================================================================= */

export async function executeGlobalSearch(
  userId: string,
  searchQuery: string
): Promise<GlobalSearchResponse> {
  const q = searchQuery.toLowerCase().trim();
  if (!q) {
    return { query: searchQuery, total_results: 0, results: [] };
  }

  const [subjects, timetable, tasks, assessments, documents] = await Promise.all([
    getSubjects(userId),
    getTimetable(userId),
    getTasks(userId),
    getAssessments(userId),
    getDocuments(userId),
  ]);

  const results: SearchResultItem[] = [];

  // 1. Subjects
  (subjects || []).forEach((s) => {
    if (s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q)) {
      results.push({
        category: 'subject',
        id: s.id,
        title: s.name,
        subtitle: `${s.code} • Target: ${s.target_attendance}%`,
        link_url: '/attendance',
        details: s.faculty ? `Faculty: ${s.faculty}` : undefined,
      });
    }
  });

  // 2. Timetable
  (timetable || []).forEach((t) => {
    if (
      t.subject_name?.toLowerCase().includes(q) ||
      t.day_of_week.toLowerCase().includes(q) ||
      t.classroom?.toLowerCase().includes(q)
    ) {
      results.push({
        category: 'timetable',
        id: t.id,
        title: `${t.subject_name || 'Class'} (${t.day_of_week})`,
        subtitle: `${t.start_time} - ${t.end_time} in ${t.classroom}`,
        link_url: '/timetable',
        details: t.faculty_name ? `Faculty: ${t.faculty_name}` : undefined,
      });
    }
  });

  // 3. Tasks
  (tasks || []).forEach((t) => {
    if (
      t.title.toLowerCase().includes(q) ||
      t.description?.toLowerCase().includes(q) ||
      t.task_type.toLowerCase().includes(q)
    ) {
      results.push({
        category: 'task',
        id: t.id,
        title: t.title,
        subtitle: `${t.task_type.toUpperCase()} • Priority ${t.priority_score}/100 [${t.status}]`,
        link_url: '/tasks',
        details: `Due: ${new Date(t.due_date).toLocaleDateString()}`,
      });
    }
  });

  // 4. Assessments
  (assessments || []).forEach((a) => {
    if (
      a.assessment_name.toLowerCase().includes(q) ||
      a.subject_name?.toLowerCase().includes(q)
    ) {
      results.push({
        category: 'assessment',
        id: a.id,
        title: `${a.assessment_name} (${a.subject_name || 'Subject'})`,
        subtitle: `Scored ${a.marks_obtained}/${a.maximum_marks} (${a.percentage}%)`,
        link_url: '/marks',
        details: `Date: ${a.assessment_date}`,
      });
    }
  });

  // 5. Documents
  (documents || []).forEach((d) => {
    if (
      d.title.toLowerCase().includes(q) ||
      d.category.toLowerCase().includes(q) ||
      d.extracted_text?.toLowerCase().includes(q)
    ) {
      results.push({
        category: 'document',
        id: d.id,
        title: d.title,
        subtitle: `${d.category.toUpperCase()} • ${(d.file_size / 1024).toFixed(1)} KB`,
        link_url: '/documents',
        details: d.extracted_text ? d.extracted_text.slice(0, 100) + '...' : undefined,
      });
    }
  });

  return {
    query: searchQuery,
    total_results: results.length,
    results,
  };
}

/* =========================================================================
 * 12. DASHBOARD AGGREGATE SUMMARY
 * ========================================================================= */

export async function getDashboardSummary(userId: string): Promise<DashboardSummary> {
  const [profile, subjects, timetable, tasks, attendance, assessments, sessions, notifs] =
    await Promise.all([
      getUserProfile(userId),
      getSubjects(userId),
      getTimetable(userId),
      getTasks(userId),
      getAttendanceRecords(userId),
      getAssessments(userId),
      getStudySessions(userId),
      getNotifications(userId),
    ]);

  const daysOfWeek = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  const today = new Date();
  const todayDayName = daysOfWeek[today.getDay()];
  const todayDateStr = today.toISOString().slice(0, 10);

  // Today's classes
  const todaysClasses = (timetable || []).filter(
    (c) => c.day_of_week.toLowerCase() === todayDayName.toLowerCase()
  );

  // Task stats
  const pendingTasks = (tasks || []).filter((t) => t.status === 'pending' || t.status === 'in_progress');
  const completedTasks = (tasks || []).filter((t) => t.status === 'completed');
  const overdueTasks = (tasks || []).filter((t) => t.is_overdue && t.status !== 'completed');
  const upcomingTasks = pendingTasks.slice(0, 5);

  // Attendance
  const attSummary = computeAttendanceSummary(
    subjects || [],
    attendance || [],
    profile?.minimum_attendance_target || 75.0
  );

  // Marks analytics
  const perf = computeMarksAnalytics(subjects || [], assessments || []);

  // Today's study sessions
  const todaysStudy = (sessions || []).filter((s) => s.session_date === todayDateStr);

  // Unread notifs
  const unreadNotifs = (notifs || []).filter((n) => !n.is_read).length;

  // Generate Daily Briefing
  let briefingSummary = '';
  const highlights: string[] = [];

  if (todaysClasses.length > 0) {
    highlights.push(
      `You have ${todaysClasses.length} lecture${todaysClasses.length > 1 ? 's' : ''} today starting at ${todaysClasses[0].start_time}.`
    );
  } else {
    highlights.push('No timetable classes scheduled for today. Great day for deep focus!');
  }

  if (pendingTasks.length > 0) {
    highlights.push(
      `${pendingTasks.length} pending academic assignment${pendingTasks.length > 1 ? 's' : ''} to complete.`
    );
  } else {
    highlights.push('All coursework assignments and tasks are up to date!');
  }

  if (attSummary.is_warning) {
    highlights.push(`Attendance alert: Overall attendance is at ${attSummary.overall_percentage}% (Target: ${attSummary.target_percentage}%).`);
  } else {
    highlights.push(`Attendance compliance is safely above your ${attSummary.target_percentage}% goal.`);
  }

  if (perf.weak_subjects.length > 0) {
    highlights.push(`Reinforce weak modules: ${perf.weak_subjects.join(', ')}.`);
  }

  briefingSummary = `Good day, ${profile?.full_name || 'Student'}. You have ${todaysClasses.length} class${todaysClasses.length !== 1 ? 'es' : ''} scheduled today and ${pendingTasks.length} active coursework task${pendingTasks.length !== 1 ? 's' : ''}. Academic attendance standing is at ${attSummary.overall_percentage}%.`;

  return {
    student_name: profile?.full_name || 'Student',
    college: profile?.college || 'National Institute of Technology',
    course: profile?.course || 'B.Tech Computer Science & Engineering',
    semester: profile?.semester || 'Semester 6',
    current_date: today.toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric' }),
    current_day: todayDayName,
    daily_briefing: {
      summary: briefingSummary,
      highlights: highlights.slice(0, 3),
      is_ai_generated: true,
      generated_at: new Date().toISOString(),
    },
    todays_classes: todaysClasses,
    upcoming_tasks: upcomingTasks,
    pending_tasks_count: pendingTasks.length,
    completed_tasks_count: completedTasks.length,
    overdue_tasks_count: overdueTasks.length,
    overall_attendance_percentage: attSummary.overall_percentage,
    is_attendance_warning: attSummary.is_warning,
    subject_attendance: attSummary.subject_summaries,
    overall_marks_average: perf.overall_average,
    subject_marks: perf.subject_performance,
    weak_subjects: perf.weak_subjects,
    todays_study_sessions: todaysStudy,
    unread_notifications_count: unreadNotifs,
  };
}
