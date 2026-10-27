import React, { useState, useEffect } from 'react';
import {
  FolderLock,
  Upload,
  FileText,
  Download,
  Trash2,
  Search,
  Eye,
  Plus,
  BookOpen,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  getDocuments,
  getSubjects,
  createDocument,
  deleteDocument,
} from '../services/firebaseDb';
import { VaultDocument, Subject } from '../types';
import { Modal } from '../components/common/Modal';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';

const CATEGORIES = [
  { id: 'all', label: 'All Documents' },
  { id: 'syllabus', label: 'Syllabus' },
  { id: 'lecture_notes', label: 'Lecture Notes' },
  { id: 'assignment', label: 'Assignment Spec' },
  { id: 'exam_prep', label: 'Exam Prep' },
  { id: 'reference', label: 'Reference' },
];

export const DocumentVault: React.FC = () => {
  const { user } = useAuth();
  const [documents, setDocuments] = useState<VaultDocument[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('all');

  // Upload/Create Modal
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadCategory, setUploadCategory] = useState('lecture_notes');
  const [uploadSubjectId, setUploadSubjectId] = useState('');
  const [textContent, setTextContent] = useState('');

  // Preview Modal
  const [previewModalOpen, setPreviewModalOpen] = useState(false);
  const [previewDoc, setPreviewDoc] = useState<VaultDocument | null>(null);

  const { success, error } = useToast();

  const loadData = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const [docs, subs] = await Promise.all([
        getDocuments(user.uid, categoryFilter),
        getSubjects(user.uid),
      ]);
      setDocuments(docs || []);
      setSubjects(subs || []);
      if (subs && subs.length > 0 && !uploadSubjectId) {
        setUploadSubjectId(subs[0].id);
      }
    } catch {
      error('Failed to load document vault.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [user, categoryFilter]);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadTitle(file.name.replace(/\.[^/.]+$/, ''));
    const reader = new FileReader();
    reader.onload = (event) => {
      const content = event.target?.result as string;
      setTextContent(content || '');
    };
    reader.readAsText(file);
  };

  const handleCreateDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!uploadTitle.trim()) {
      error('Document title is required.');
      return;
    }

    const sub = subjects.find((s) => s.id === uploadSubjectId);

    try {
      await createDocument(user.uid, {
        title: uploadTitle.trim(),
        category: uploadCategory,
        subject_id: uploadSubjectId || undefined,
        subject_name: sub?.name,
        extracted_text: textContent,
        file_size: textContent.length || 1024,
        file_type: 'txt',
        original_filename: `${uploadTitle.toLowerCase().replace(/\s+/g, '_')}.txt`,
      });
      success(`Saved "${uploadTitle}" to your Document Vault!`);
      setUploadModalOpen(false);
      setUploadTitle('');
      setTextContent('');
      loadData();
    } catch {
      error('Failed to save document.');
    }
  };

  const handleDelete = async (id: string) => {
    if (!user) return;
    if (!window.confirm('Delete this course document from vault?')) return;
    try {
      await deleteDocument(user.uid, id);
      success('Document deleted.');
      loadData();
    } catch {
      error('Failed to delete document.');
    }
  };

  const handleOpenPreview = (doc: VaultDocument) => {
    setPreviewDoc(doc);
    setPreviewModalOpen(true);
  };

  const filteredDocs = documents.filter((d) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      d.title.toLowerCase().includes(q) ||
      (d.subject_name && d.subject_name.toLowerCase().includes(q)) ||
      (d.extracted_text && d.extracted_text.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 tracking-tight flex items-center space-x-2">
            <FolderLock className="w-5 h-5 text-indigo-600" />
            <span>Smart Document & Course Notes Vault</span>
          </h1>
          <p className="text-xs text-slate-500 font-medium mt-0.5">
            Store course syllabi, lecture notes, and study materials with AI-grounded retrieval support
          </p>
        </div>

        <button
          onClick={() => setUploadModalOpen(true)}
          className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition-all shadow-xs flex items-center space-x-1.5 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add Course Document</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs space-y-3">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search documents by title, subject, or content text..."
            className="w-full pl-9 pr-3 py-1.5 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
          />
        </div>

        <div className="flex items-center space-x-1.5 overflow-x-auto pb-1">
          {CATEGORIES.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setCategoryFilter(cat.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all ${
                categoryFilter === cat.id
                  ? 'bg-slate-900 text-white shadow-xs'
                  : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200/80'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>
      </div>

      {/* Document Grid */}
      {loading ? (
        <LoadingSpinner size="md" label="Loading document vault..." className="py-12" />
      ) : filteredDocs.length === 0 ? (
        <EmptyState
          icon={FolderLock}
          title="No documents in your vault"
          description="Store syllabus outlines, cheat sheets, or lecture notes. The AI assistant can automatically reference these materials during study sessions!"
          actionText="Add Course Note or Document"
          onAction={() => setUploadModalOpen(true)}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredDocs.map((doc) => (
            <div
              key={doc.id}
              className="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-xs hover:border-indigo-300 transition-all flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between mb-2">
                  <div className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                    <FileText className="w-4 h-4" />
                  </div>
                  <div className="flex items-center space-x-1">
                    <button
                      onClick={() => handleOpenPreview(doc)}
                      className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-slate-100 transition-colors"
                      title="Preview content"
                    >
                      <Eye className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => handleDelete(doc.id)}
                      className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-slate-100 transition-colors"
                      title="Delete"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>

                <h3 className="text-sm font-bold text-slate-900 leading-tight mb-1">
                  {doc.title}
                </h3>

                <div className="flex items-center space-x-2 text-[11px] text-slate-500 mb-2">
                  <span className="uppercase font-bold tracking-wider px-1.5 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[10px]">
                    {doc.category.replace('_', ' ')}
                  </span>
                  {doc.subject_name && <span>• {doc.subject_name}</span>}
                </div>

                {doc.extracted_text && (
                  <p className="text-xs text-slate-500 line-clamp-3 bg-slate-50 p-2.5 rounded-xl border border-slate-100 leading-relaxed">
                    {doc.extracted_text}
                  </p>
                )}
              </div>

              <div className="flex items-center justify-between text-[10px] text-slate-400 pt-3 mt-3 border-t border-slate-100">
                <span>{(doc.file_size / 1024).toFixed(1)} KB</span>
                <span>
                  {new Date(doc.created_at).toLocaleDateString([], {
                    month: 'short',
                    day: 'numeric',
                  })}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Document Modal */}
      <Modal
        isOpen={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        title="Add Document or Course Notes"
      >
        <form onSubmit={handleCreateDocument} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Document Title *
            </label>
            <input
              type="text"
              value={uploadTitle}
              onChange={(e) => setUploadTitle(e.target.value)}
              placeholder="e.g. Operating Systems Chapter 3 Notes"
              required
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Category *</label>
              <select
                value={uploadCategory}
                onChange={(e) => setUploadCategory(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="syllabus">Syllabus</option>
                <option value="lecture_notes">Lecture Notes</option>
                <option value="assignment">Assignment Spec</option>
                <option value="exam_prep">Exam Prep / Cheat Sheet</option>
                <option value="reference">Reference Material</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Subject</label>
              <select
                value={uploadSubjectId}
                onChange={(e) => setUploadSubjectId(e.target.value)}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
              >
                <option value="">General</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Optional: Load from Text / Markdown File (.txt, .md)
            </label>
            <input
              type="file"
              accept=".txt,.md,.json"
              onChange={handleFileUpload}
              className="text-xs text-slate-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-xl file:border-0 file:text-xs file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Document Text / Notes Content
            </label>
            <textarea
              rows={5}
              value={textContent}
              onChange={(e) => setTextContent(e.target.value)}
              placeholder="Paste key notes, formulas, syllabus outlines, or lecture summaries here..."
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 font-mono"
            />
          </div>

          <div className="flex justify-end space-x-2 pt-2">
            <button
              type="button"
              onClick={() => setUploadModalOpen(false)}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs"
            >
              Store in Vault
            </button>
          </div>
        </form>
      </Modal>

      {/* Preview Modal */}
      <Modal
        isOpen={previewModalOpen}
        onClose={() => setPreviewModalOpen(false)}
        title={previewDoc ? previewDoc.title : 'Document Preview'}
        maxWidth="lg"
      >
        {previewDoc && (
          <div className="space-y-4">
            <div className="flex items-center space-x-2 text-xs text-slate-500 pb-3 border-b border-slate-100">
              <span className="uppercase font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded-md">
                {previewDoc.category}
              </span>
              {previewDoc.subject_name && <span>• {previewDoc.subject_name}</span>}
              <span>• {(previewDoc.file_size / 1024).toFixed(1)} KB</span>
            </div>

            <div className="max-h-96 overflow-y-auto bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs text-slate-800 font-mono whitespace-pre-wrap leading-relaxed">
              {previewDoc.extracted_text || 'No text extracted for this document.'}
            </div>

            <div className="flex justify-end pt-2">
              <button
                onClick={() => setPreviewModalOpen(false)}
                className="px-4 py-2 text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
              >
                Close Preview
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
