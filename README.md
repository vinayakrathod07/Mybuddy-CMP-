<!--
  MyBuddy / AcademiaLife
  A clean, student-focused README for the project.
-->

<div align="center">

# 🎓 MyBuddy
### Your personal companion for a more organized student life.

**Plan smarter. Stay on track. Make every semester count.**

<p>
  <a href="https://github.com/vinayakrathod07/Mybuddy-CMP-">
    <img src="https://img.shields.io/badge/Project-MyBuddy-6C63FF?style=for-the-badge" alt="MyBuddy project" />
  </a>
  <img src="https://img.shields.io/badge/Status-In%20Development-FFB020?style=for-the-badge" alt="In development" />
  <img src="https://img.shields.io/badge/Platform-Web%20%7C%20Mobile--friendly-20B486?style=for-the-badge" alt="Web and mobile friendly" />
</p>

<p>
  <a href="#-overview">Overview</a> •
  <a href="#-key-features">Features</a> •
  <a href="#-tech-stack">Tech Stack</a> •
  <a href="#-getting-started">Get Started</a> •
  <a href="#-roadmap">Roadmap</a>
</p>

</div>

---

## ✨ Overview

**MyBuddy** (also referred to in the project as **AcademiaLife — Student Academic Assistant**) is a student-focused productivity project designed to bring everyday academic planning into one place.

From organizing timetables and assignments to reviewing attendance and academic performance, the goal is to help students understand what needs attention and plan their next steps with less stress. The project also explores AI-powered assistance and Firebase-backed data persistence.

> **The vision:** one helpful workspace for your classes, deadlines, study plans, and academic progress.

> **Project status:** This repository is under active development. The feature list describes the project scope; individual features and integrations may still be in progress and should be verified in the current build.

## 🚀 Key Features

| Module | Purpose |
| --- | --- |
| 📊 **Student Dashboard** | A central view of academic activities and priorities. |
| 🗓️ **Timetable & Schedule** | Organize classes and keep your weekly routine visible. |
| ✅ **Tasks & Assignments** | Track academic work and upcoming deadlines. |
| 📈 **Attendance & Performance** | Review attendance information and academic progress. |
| 🧠 **Study Planning** | Plan study sessions and organize learning goals. |
| 📚 **Course Documents** | Keep course-related resources easier to access. |
| 🤖 **AI Assistance** | Explore AI-powered support for student questions and planning. |
| ☁️ **Cloud Data** | Firebase integration for application data and persistence. |
| 📱 **Responsive Experience** | Designed with browser and mobile-sized screens in mind. |

*Feature availability depends on the current implementation and configuration.*

## 🛠️ Tech Stack

<div align="center">

![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=20232A)
![TypeScript](https://img.shields.io/badge/TypeScript-TS-3178C6?style=flat-square&logo=typescript&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-Build%20Tool-646CFF?style=flat-square&logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-Styling-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Cloud%20Services-FFCA28?style=flat-square&logo=firebase&logoColor=20232A)
![Gemini](https://img.shields.io/badge/Google%20Gemini-AI%20Integration-8E75B2?style=flat-square&logo=googlegemini&logoColor=white)

</div>

- **Frontend:** React, TypeScript, React Router
- **Development & build:** Vite
- **Styling & UI:** Tailwind CSS, Motion, Lucide React
- **Charts:** Recharts
- **Cloud services:** Firebase
- **AI integration:** Google Gemini API
- **Server tooling:** Express
- **Package manager:** npm or Bun

## 🧑‍💻 Getting Started

### Prerequisites

Make sure you have installed:

- [Node.js](https://nodejs.org/) (a version compatible with the project's Vite dependencies)
- npm, included with Node.js, or [Bun](https://bun.sh/)
- A Firebase project and Gemini API key if you want to run the corresponding integrations

### 1. Clone the repository

```bash
git clone https://github.com/vinayakrathod07/Mybuddy-CMP-.git
cd Mybuddy-CMP-
```

### 2. Install dependencies

Using npm:

```bash
npm install
```

Or using Bun:

```bash
bun install
```

### 3. Configure environment variables

Create a local `.env` file based on `.env.example`:

```bash
# Windows PowerShell
Copy-Item .env.example .env
```

Set the required values for your local setup. The example file includes `GEMINI_API_KEY` and `APP_URL`; the exact configuration depends on how you run the app and its server integrations.

**Security note:** Never commit real API keys, passwords, private service-account files, or other secrets to GitHub. Keep sensitive values in environment variables or your deployment platform's secret manager. Do not expose a private Gemini API key in client-side code.

### 4. Start the development server

```bash
npm run dev
```

Open the local URL printed by Vite in your terminal (the project script uses port `3000`).

### 5. Create a production build

```bash
npm run build
```

To preview the production build locally:

```bash
npm run preview
```

To run the TypeScript check:

```bash
npm run lint
```

## 📂 Project Structure

The repository currently includes these main areas:

```text
Mybuddy-CMP-/
├── android-app/          # Mobile-related project files
├── public/               # Static public assets
├── src/
│   ├── components/       # Reusable UI components
│   ├── context/          # Shared application state/context
│   ├── hooks/            # Reusable React hooks
│   ├── pages/            # Application pages
│   ├── services/         # Service and integration logic
│   ├── App.tsx           # Main application component
│   ├── main.tsx          # Application entry point
│   └── index.css         # Global styles
├── .env.example          # Example environment configuration
├── package.json          # Scripts and dependencies
├── vite.config.ts        # Vite configuration
└── README.md
```

*Folder contents may evolve as development continues.*

## 🗺️ Roadmap

- [x] Establish the React + TypeScript + Vite project foundation
- [x] Add Firebase and Gemini integration dependencies
- [ ] Verify and refine each student-planning workflow
- [ ] Complete and test dashboard, timetable, and assignment flows
- [ ] Validate authentication, database rules, and persistence
- [ ] Improve responsive layouts and accessibility
- [ ] Add reliable loading, empty, and error states
- [ ] Document deployment and production configuration
- [ ] Test the complete experience on desktop and mobile

## 🔐 Privacy & Security

- Keep credentials and API keys out of source control.
- Review Firebase Authentication and Firestore security rules before deploying.
- Use least-privilege access for data and server credentials.
- Avoid storing sensitive student information unless it is necessary and appropriately protected.

## 🤝 Contributing

This is a learning-focused project, and suggestions are welcome.

1. Fork the repository.
2. Create a branch: `git checkout -b feature/your-feature`
3. Make your changes and test them.
4. Commit your work: `git commit -m "Add: your feature"`
5. Push the branch and open a pull request.

Please describe the change clearly and include screenshots for UI improvements where useful.

## 👨‍💻 Developed and Managed by

**Vinay Rathod**  
BCA Student | Aspiring Software Developer | Interested in Android Development, Web Technologies, AI, and Data Analysis

- GitHub: [@vinayakrathod07](https://github.com/vinayakrathod07)
- LinkedIn: [Connect with me](https://www.linkedin.com/in/vinayakrathod07/)

---

<div align="center">

**Built with curiosity, code, and the goal of making student life a little easier.** 💜

If you find this project interesting, consider giving the repository a ⭐

</div>
