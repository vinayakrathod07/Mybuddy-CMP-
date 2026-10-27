import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import {
  onAuthStateChanged,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  signInAnonymously,
  updateProfile,
  User as FirebaseUser,
} from 'firebase/auth';
import { auth, testConnection } from '../services/firebase';
import { getUserProfile, initUserProfile, updateUserProfile as updateProfileInDb } from '../services/firebaseDb';
import { StudentProfile } from '../types';

interface AuthContextType {
  user: FirebaseUser | null;
  profile: StudentProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  loginWithEmail: (email: string, pass: string) => Promise<void>;
  registerWithEmail: (
    email: string,
    pass: string,
    details: { full_name: string; college?: string; course?: string; semester?: string; academic_year?: string }
  ) => Promise<void>;
  loginWithGoogle: () => Promise<void>;
  loginAsGuest: () => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  updateLocalProfile: (updated: Partial<StudentProfile>) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<FirebaseUser | null>(null);
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Initialize and test connection on mount
  useEffect(() => {
    testConnection();
  }, []);

  const loadProfileForUser = useCallback(async (fbUser: FirebaseUser) => {
    try {
      let p = await getUserProfile(fbUser.uid);
      if (!p) {
        // Initialize profile with NO sample data
        p = await initUserProfile(
          fbUser.uid,
          fbUser.email || 'student@academic.edu',
          fbUser.displayName || 'Student',
          {
            college: 'National Institute of Technology',
            course: 'B.Tech Computer Science & Engineering',
            semester: 'Semester 6',
            academic_year: '2026-2027',
          }
        );
      }
      setProfile(p);
    } catch (err) {
      console.error('Failed to load student profile from Firestore:', err);
    }
  }, []);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (currentUser) => {
      setUser(currentUser);
      if (currentUser) {
        await loadProfileForUser(currentUser);
      } else {
        setProfile(null);
      }
      setIsLoading(false);
    });

    return () => unsubscribe();
  }, [loadProfileForUser]);

  const loginWithEmail = async (email: string, pass: string) => {
    setIsLoading(true);
    try {
      const cred = await signInWithEmailAndPassword(auth, email, pass);
      await loadProfileForUser(cred.user);
    } finally {
      setIsLoading(false);
    }
  };

  const registerWithEmail = async (
    email: string,
    pass: string,
    details: { full_name: string; college?: string; course?: string; semester?: string; academic_year?: string }
  ) => {
    setIsLoading(true);
    try {
      const cred = await createUserWithEmailAndPassword(auth, email, pass);
      if (details.full_name) {
        await updateProfile(cred.user, { displayName: details.full_name });
      }
      const newProfile = await initUserProfile(cred.user.uid, email, details.full_name, details);
      setProfile(newProfile);
    } finally {
      setIsLoading(false);
    }
  };

  const loginWithGoogle = async () => {
    setIsLoading(true);
    try {
      const provider = new GoogleAuthProvider();
      const cred = await signInWithPopup(auth, provider);
      await loadProfileForUser(cred.user);
    } finally {
      setIsLoading(false);
    }
  };

  const loginAsGuest = async () => {
    setIsLoading(true);
    try {
      const cred = await signInAnonymously(auth);
      await loadProfileForUser(cred.user);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      await signOut(auth);
      setUser(null);
      setProfile(null);
    } finally {
      setIsLoading(false);
    }
  };

  const refreshProfile = async () => {
    if (user) {
      await loadProfileForUser(user);
    }
  };

  const updateLocalProfile = async (updated: Partial<StudentProfile>) => {
    if (!user || !profile) return;
    await updateProfileInDb(user.uid, updated);
    setProfile((prev) => (prev ? { ...prev, ...updated } : null));
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        profile,
        isAuthenticated: !!user && !!profile,
        isLoading,
        loginWithEmail,
        registerWithEmail,
        loginWithGoogle,
        loginAsGuest,
        logout,
        refreshProfile,
        updateLocalProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
