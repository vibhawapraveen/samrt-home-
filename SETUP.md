# Smart Home Monitor - New Device Setup Guide

Follow this guide in order after cloning the repository on a new machine.

---

## Prerequisites - Install These First

| Tool | Minimum Version | Download |
|---|---|---|
| Android Studio | Hedgehog (2023.1) or newer | https://developer.android.com/studio |
| JDK | 11 (bundled with Android Studio) | - |
| Node.js | 18.x or newer | https://nodejs.org |
| npm | 9.x or newer (comes with Node.js) | - |
| Firebase CLI | Latest | npm install -g firebase-tools |
| Git | Any recent version | https://git-scm.com |

Check versions after installing:

    node --version      # should print v18.x.x or higher
    npm --version
    firebase --version
    java --version

---

## 1. Clone the Repository

    git clone https://github.com/<your-username>/<your-repo>.git
    cd "Smart Home Monitor"

---

## 2. Firebase Setup (Required for ALL parts)

NOTE: google-services.json is NOT committed to GitHub (it contains secret keys).
You must download it manually from the Firebase Console.

### Step 1 - Download google-services.json
1. Go to https://console.firebase.google.com
2. Open your project -> Project Settings (gear icon)
3. Scroll to "Your Apps" -> select the Android app
4. Click "Download google-services.json"
5. Place the file in TWO locations:

    Smart Home Monitor\google-services.json        <- root copy
    Smart Home Monitor\app\google-services.json    <- Android app copy

### Step 2 - Login to Firebase CLI

    firebase login

Follow the browser prompt to authenticate with your Google account.

---

## 3. Android App Setup

### Step 1 - Open in Android Studio
1. Open Android Studio
2. Click Open -> select the "Smart Home Monitor" root folder
3. Wait for Gradle sync to finish (may take a few minutes on first run)

### Step 2 - Configure local.properties
Android Studio usually creates this automatically. If missing, create it at the root:

    # local.properties  (root of the project)
    sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

Replace YourName with your actual Windows username.

### Step 3 - Run the App
- Connect an Android device (min API 26) OR start an emulator
- Click the Run button in Android Studio

---

## 4. Web Simulator Setup (smart-home-system/)

    # Navigate into the web simulator folder
    cd smart-home-system

    # Install all dependencies
    npm install

    # Start the development server
    npm run dev

The simulator will open at http://localhost:5173

Other useful commands:
    npm run build    # Build for production
    npm run preview  # Preview the production build locally
    npm run lint     # Run the linter

---

## 5. Firebase Functions Setup (functions/)

    # Navigate into the functions folder
    cd functions

    # Install all dependencies
    npm install

### Deploy functions to Firebase

    npm run deploy
    # or from the root:
    firebase deploy --only functions

### Run functions locally (emulator)

    npm run serve
    # or:
    firebase emulators:start --only functions

---

## 6. Deploy Firestore Rules & Indexes

From the project root:

    # Deploy security rules
    firebase deploy --only firestore:rules

    # Deploy indexes
    firebase deploy --only firestore:indexes

---

## Quick Checklist

After completing all steps, verify:

- [ ] google-services.json placed in both root and app/ folder
- [ ] firebase login completed successfully
- [ ] Android Studio Gradle sync succeeded (no red errors)
- [ ] npm install ran inside smart-home-system/
- [ ] npm install ran inside functions/
- [ ] Web simulator loads at http://localhost:5173
- [ ] Android app builds and runs on device/emulator
- [ ] Firebase Functions deployed (or emulator running)

---

## Common Errors & Fixes

| Error | Fix |
|---|---|
| google-services.json not found | Download from Firebase Console and place in app/ folder |
| Gradle sync failed | File -> Invalidate Caches -> Restart in Android Studio |
| npm: command not found | Install Node.js from https://nodejs.org and restart terminal |
| firebase: command not found | Run: npm install -g firebase-tools |
| Port 5173 already in use | Kill the process or run: npm run dev -- --port 5174 |
| Firebase login error | Run: firebase logout then firebase login again |
| SDK location not found | Create/fix local.properties with your Android SDK path |

---

## Project Structure Overview

    Smart Home Monitor/
    ├── app/                        # Android app (Kotlin + Jetpack Compose)
    │   ├── src/
    │   └── build.gradle.kts
    ├── smart-home-system/          # Web Simulator (React + Vite)
    │   ├── src/
    │   ├── package.json
    │   └── vite.config.js
    ├── functions/                  # Firebase Cloud Functions (Node.js 18)
    │   ├── index.js
    │   └── package.json
    ├── firestore.rules             # Firestore security rules
    ├── firestore.indexes.json      # Firestore indexes
    ├── firebase.json               # Firebase project config
    ├── google-services.json        # NOT in Git - download manually
    └── SETUP.md                    # This file
