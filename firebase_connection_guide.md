# AcademiaLife — Connecting to Your Own Firebase Project

This guide provides the exact steps and credentials to link your **AcademiaLife** Android app (`com.academialife.app`) to your personal **Firebase Account & Project**.

---

## 1. App Identity & Signing Credentials

When adding an Android app to your Firebase project in the [Firebase Console](https://console.firebase.google.com/), enter these exact parameters:

- **Android Package Name**: `com.academialife.app`
- **App Nickname**: `AcademiaLife`
- **Debug Signing Certificate SHA-1**:
  ```text
  8B:BF:70:FF:EA:AE:A2:53:19:20:7D:02:F9:35:9E:16:9F:21:05:A3
  ```
- **SHA-256 Fingerprint**:
  ```text
  83:1D:EB:7B:56:24:B1:73:CC:7F:D8:30:DB:74:89:7A:B0:81:FD:1E:C3:6A:45:08:D9:04:29:82:60:80:72:27
  ```

---

## 2. Step-by-Step Firebase Console Setup

### Step A: Create or Select Your Firebase Project
1. Open [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or select your existing project).
3. Name it (e.g. `AcademiaLife`) and complete project creation.

### Step B: Register the Android App
1. In your Project Overview, click the **Android** icon (or go to **Project Settings > General > Your apps > Add app > Android**).
2. Enter the Package Name: `com.academialife.app`.
3. Paste the **SHA-1** fingerprint from Section 1 above.
4. Click **Register app**.

### Step C: Download `google-services.json`
1. Download the generated `google-services.json` file.
2. Place / overwrite the file at this exact location:
   ```text
   C:\Users\ACER\Mybuddy-CMP-\android-app\app\google-services.json
   ```

### Step D: Enable Firebase Authentication
1. In Firebase Console left menu, navigate to **Build > Authentication**.
2. Click **Get Started**.
3. Under the **Sign-in method** tab:
   - Click **Email/Password** -> Toggle **Enable** -> Click **Save**.
   - (Optional) Click **Anonymous** -> Toggle **Enable** -> Click **Save** (allows guest browsing before login).

### Step E: Enable Cloud Firestore Database
1. Navigate to **Build > Firestore Database**.
2. Click **Create database**.
3. Choose your preferred server location (e.g. `asia-south1` or `us-central`).
4. Select **Start in production mode**.
5. Click **Create**.
6. Switch to the **Rules** tab and paste the security rules below:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      match /{allSubcollections=**} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```
7. Click **Publish**.

---

## 3. How Authentication & Data Sync Work in the App

The app is now fully wired with:
1. **Email / Password Authentication**:
   - **Sign In**: Validates credentials against your Firebase Auth database.
   - **Create Account**: Registers new users with name, email, and password. Automatically creates their student profile in Firestore under `/users/{uid}`.
   - **Forgot Password**: Sends a verified Firebase password reset email.
2. **User Data Isolation**:
   - All student records (subjects, timetable, tasks, attendance, marks, study sessions, and vault documents) are automatically scoped under `/users/{currentUserId}/...`.
   - Logging in on any Android phone running this app restores the student's personal data.
3. **Guest / Anonymous Fallback**:
   - If a student hasn't signed in yet, data is stored in the local session until they register or sign in, which links their records to their Firebase account.

---

## 4. Rebuild & Run

Once you copy your `google-services.json` into `android-app/app/google-services.json`, run:
```powershell
cmd /c "cd android-app && gradlew.bat assembleDebug"
& 'C:\Users\ACER\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r android-app\app\build\outputs\apk\debug\app-debug.apk
```
Or run `run_on_mobile.bat` in the root folder.

