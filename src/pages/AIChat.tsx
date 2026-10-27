import React, { useState, useEffect, useRef } from 'react';
import {
  Bot,
  Send,
  Plus,
  Trash2,
  Sparkles,
  Volume2,
  VolumeX,
  Mic,
  MicOff,
  BookOpen,
  Info,
  CheckCircle2,
  FileText,
  Clock,
  Layers,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import {
  getConversations,
  createConversation,
  deleteConversation,
  getChatMessages,
  sendChatMessage,
} from '../services/firebaseDb';
import { ChatConversation, ChatMessage } from '../types';
import { LoadingSpinner } from '../components/common/LoadingSpinner';

const SUGGESTIONS = [
  'What classes do I have today?',
  'What is my attendance percentage and compliance?',
  'Which tasks and exams are due this week?',
  'What are my weakest subjects based on test marks?',
  'Can you search my Document Vault for syllabus notes?',
];

export const AIChat: React.FC = () => {
  const { user, profile } = useAuth();
  const [conversations, setConversations] = useState<ChatConversation[]>([]);
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  // Web Speech API Voice Recognition
  const [isListening, setIsListening] = useState(false);
  const [speechSupported, setSpeechSupported] = useState(false);
  const recognitionRef = useRef<any>(null);

  // Text to Speech
  const [speakingId, setSpeakingId] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { success, error, warning, info } = useToast();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Check speech recognition
  useEffect(() => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (SpeechRecognition) {
      setSpeechSupported(true);
      const rec = new SpeechRecognition();
      rec.continuous = false;
      rec.interimResults = false;
      rec.lang = 'en-US';
      rec.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        setInputMessage((prev) => (prev ? `${prev} ${transcript}` : transcript));
        setIsListening(false);
        info(`Transcribed: "${transcript}"`);
      };
      rec.onerror = () => {
        setIsListening(false);
      };
      rec.onend = () => {
        setIsListening(false);
      };
      recognitionRef.current = rec;
    }
  }, []);

  const loadConversations = async () => {
    if (!user) return;
    try {
      setLoading(true);
      const list = await getConversations(user.uid);
      setConversations(list || []);
      if (list && list.length > 0) {
        setActiveConvId(list[0].id);
        const msgs = await getChatMessages(user.uid, list[0].id);
        setMessages(msgs || []);
      } else {
        // Create initial conversation
        const created = await createConversation(user.uid, 'Academic Assistant Chat');
        setConversations([created]);
        setActiveConvId(created.id);
        setMessages([]);
      }
    } catch {
      error('Failed to load chat history.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadConversations();
  }, [user]);

  const handleSelectConv = async (convId: string) => {
    if (!user) return;
    setActiveConvId(convId);
    try {
      const msgs = await getChatMessages(user.uid, convId);
      setMessages(msgs || []);
    } catch {
      error('Failed to load messages.');
    }
  };

  const handleNewConversation = async () => {
    if (!user) return;
    try {
      const created = await createConversation(user.uid, `Chat ${conversations.length + 1}`);
      setConversations((prev) => [created, ...prev]);
      setActiveConvId(created.id);
      setMessages([]);
    } catch {
      error('Failed to start new conversation.');
    }
  };

  const handleDeleteConversation = async (convId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!user) return;
    try {
      await deleteConversation(user.uid, convId);
      const remaining = conversations.filter((c) => c.id !== convId);
      setConversations(remaining);
      if (activeConvId === convId) {
        if (remaining.length > 0) {
          handleSelectConv(remaining[0].id);
        } else {
          setActiveConvId(null);
          setMessages([]);
        }
      }
      success('Conversation removed.');
    } catch {
      error('Failed to delete conversation.');
    }
  };

  const handleSendMessage = async (textToSend?: string) => {
    const text = textToSend || inputMessage;
    if (!text.trim() || !user || !activeConvId || sending) return;

    setInputMessage('');
    setSending(true);

    try {
      const { userMessage, assistantMessage } = await sendChatMessage(
        user.uid,
        activeConvId,
        text.trim()
      );
      setMessages((prev) => [...prev, userMessage, assistantMessage]);
    } catch (err: any) {
      error('Failed to send message: ' + (err.message || 'Unknown error'));
    } finally {
      setSending(false);
    }
  };

  // Toggle voice listening
  const toggleListening = () => {
    if (!recognitionRef.current) {
      warning('Speech recognition is not supported in this browser.');
      return;
    }
    if (isListening) {
      recognitionRef.current.stop();
      setIsListening(false);
    } else {
      try {
        recognitionRef.current.start();
        setIsListening(true);
        info('Listening for your voice question...');
      } catch {
        setIsListening(false);
      }
    }
  };

  // Text to Speech Read Aloud
  const handleSpeak = (msgId: string, text: string) => {
    if (!('speechSynthesis' in window)) {
      warning('Text to Speech is not supported in this browser.');
      return;
    }

    if (speakingId === msgId) {
      window.speechSynthesis.cancel();
      setSpeakingId(null);
      return;
    }

    window.speechSynthesis.cancel();
    const cleanText = text.replace(/[*#•_]/g, '');
    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.rate = 1.0;
    utterance.onend = () => setSpeakingId(null);
    utterance.onerror = () => setSpeakingId(null);
    setSpeakingId(msgId);
    window.speechSynthesis.speak(utterance);
  };

  return (
    <div className="h-[calc(100vh-8.5rem)] flex flex-col md:flex-row gap-4">
      {/* Sidebar: Conversation List */}
      <div className="w-full md:w-64 bg-white rounded-3xl p-4 border border-slate-200/80 shadow-xs flex flex-col shrink-0">
        <div className="flex items-center justify-between mb-3">
          <span className="text-xs font-bold text-slate-900">Conversations</span>
          <button
            onClick={handleNewConversation}
            className="p-1.5 text-indigo-600 hover:bg-indigo-50 rounded-xl transition-colors flex items-center space-x-1"
            title="New Chat"
          >
            <Plus className="w-4 h-4" />
            <span className="text-xs font-bold">New</span>
          </button>
        </div>

        <div className="flex-1 overflow-y-auto space-y-1">
          {conversations.map((conv) => (
            <div
              key={conv.id}
              onClick={() => handleSelectConv(conv.id)}
              className={`flex items-center justify-between p-2.5 rounded-xl text-xs font-semibold cursor-pointer transition-all ${
                activeConvId === conv.id
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <span className="truncate pr-2">{conv.title}</span>
              <button
                onClick={(e) => handleDeleteConversation(conv.id, e)}
                className={`p-1 rounded-md transition-colors ${
                  activeConvId === conv.id
                    ? 'text-indigo-200 hover:text-white'
                    : 'text-slate-400 hover:text-rose-600'
                }`}
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          ))}
        </div>

        <div className="pt-3 border-t border-slate-100 text-[10px] text-slate-400 text-center">
          Grounding: Timetable, Tasks, Ledger & Vault
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 bg-white rounded-3xl border border-slate-200/80 shadow-xs flex flex-col overflow-hidden">
        {/* Chat Header */}
        <div className="p-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center space-x-2.5">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-indigo-600 to-cyan-500 flex items-center justify-center text-white shadow-xs">
              <Bot className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h2 className="text-xs font-bold text-slate-900">Academic Tutor & Life Assistant</h2>
                <span className="text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded-md bg-cyan-100 text-cyan-800">
                  RAG Grounded
                </span>
              </div>
              <p className="text-[10px] text-slate-400">
                Connected to your courses, marks, assignments & syllabus
              </p>
            </div>
          </div>
        </div>

        {/* Message Thread */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
          {messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center max-w-md mx-auto py-8">
              <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mb-3">
                <Sparkles className="w-6 h-6" />
              </div>
              <h3 className="text-sm font-bold text-slate-900 mb-1">
                How can I assist your studies today?
              </h3>
              <p className="text-xs text-slate-400 mb-6 leading-relaxed">
                I can give you today's timetable, analyze attendance shortfall, list high-priority
                assignments, or retrieve concepts from your vault notes.
              </p>

              {/* Suggestions */}
              <div className="w-full space-y-2">
                {SUGGESTIONS.map((s, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleSendMessage(s)}
                    className="w-full p-2.5 text-xs font-semibold text-slate-700 bg-slate-50 hover:bg-indigo-50 hover:text-indigo-700 border border-slate-200/80 rounded-xl text-left transition-colors flex items-center justify-between group"
                  >
                    <span>{s}</span>
                    <Send className="w-3 h-3 text-slate-300 group-hover:text-indigo-600 transition-colors" />
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((m) => (
              <div
                key={m.id}
                className={`flex flex-col ${m.role === 'user' ? 'items-end' : 'items-start'}`}
              >
                <div
                  className={`max-w-[85%] sm:max-w-xl p-4 rounded-2xl text-xs leading-relaxed ${
                    m.role === 'user'
                      ? 'bg-indigo-600 text-white rounded-br-xs shadow-xs'
                      : 'bg-slate-50 text-slate-900 border border-slate-200/80 rounded-bl-xs'
                  }`}
                >
                  <div className="whitespace-pre-wrap">{m.content}</div>

                  {/* Sources Grounding */}
                  {m.sources && m.sources.length > 0 && (
                    <div className="mt-3 pt-2.5 border-t border-slate-200/80 space-y-1.5">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500 block">
                        Grounded Sources Cited:
                      </span>
                      {m.sources.map((src, sIdx) => (
                        <div
                          key={sIdx}
                          className="bg-white p-2 rounded-xl border border-slate-100 flex items-start space-x-2 text-[10px]"
                        >
                          <BookOpen className="w-3.5 h-3.5 text-indigo-600 mt-0.5 shrink-0" />
                          <div>
                            <span className="font-bold text-slate-800">
                              [{src.source_type}] {src.title}
                            </span>
                            <span className="text-slate-500 block">{src.reference_detail}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* TTS Read Aloud Button */}
                  {m.role === 'assistant' && (
                    <div className="mt-2.5 pt-2 border-t border-slate-200/60 flex items-center justify-between text-[10px] text-slate-400">
                      <span>AcademiaLife AI</span>
                      <button
                        onClick={() => handleSpeak(m.id, m.content)}
                        className="inline-flex items-center space-x-1 hover:text-indigo-600 font-semibold transition-colors"
                      >
                        {speakingId === m.id ? (
                          <>
                            <VolumeX className="w-3.5 h-3.5 text-rose-500" />
                            <span>Stop voice</span>
                          </>
                        ) : (
                          <>
                            <Volume2 className="w-3.5 h-3.5" />
                            <span>Read aloud</span>
                          </>
                        )}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))
          )}

          {sending && (
            <div className="flex items-center space-x-2 text-xs text-slate-400 p-3 bg-slate-50 rounded-2xl max-w-xs border border-slate-100">
              <Sparkles className="w-4 h-4 text-indigo-600 animate-spin" />
              <span>Analyzing student courses & records...</span>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <div className="p-3.5 border-t border-slate-100 bg-white">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            className="flex items-center space-x-2"
          >
            {speechSupported && (
              <button
                type="button"
                onClick={toggleListening}
                className={`p-2.5 rounded-xl transition-all shrink-0 ${
                  isListening
                    ? 'bg-rose-500 text-white animate-pulse'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
                title={isListening ? 'Listening... click to stop' : 'Click to ask with voice'}
              >
                {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
              </button>
            )}

            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              placeholder="Ask anything about classes, attendance targets, coursework deadlines..."
              disabled={sending}
              className="flex-1 px-4 py-2.5 text-xs rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
            />

            <button
              type="submit"
              disabled={sending || !inputMessage.trim()}
              className="p-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 text-white rounded-xl transition-all shadow-xs shrink-0"
            >
              <Send className="w-4 h-4" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
