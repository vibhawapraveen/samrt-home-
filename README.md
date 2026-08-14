# 🏠 Smart Home Monitor & Control System

A full-stack IoT Smart Home monitoring and control system built as part of SCS 3311 Mobile Application Design & Development.

---

## 📦 Project Structure

```
Smart Home Monitor/
├── SmartHomeApp/          # Android mobile app (Kotlin + Jetpack Compose)
├── functions/             # Firebase Cloud Functions (Node.js)
├── simulator/             # Web Hardware Simulator (React + Vite)
├── firebase.json          # Firebase project config
├── firestore.rules        # Firestore security rules
└── .firebaserc            # Firebase project alias
```

---

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Node.js 18+
- Firebase CLI (`npm install -g firebase-tools`)
- A Firebase project (free Spark tier works)

---

### Step 1: Create Firebase Project

1. Go to [console.firebase.google.com](https://console.firebase.google.com)
2. Create a new project (e.g., `smart-home-monitor`)
3. Enable:
   - **Authentication** → Email/Password + Anonymous
   - **Firestore Database** → Start in production mode
   - **Realtime Database** → Create database (pick region)
   - **Cloud Messaging** (for push notifications)
4. Add an **Android app**: package name = `com.smarthome.monitor`
5. Download `google-services.json` → place in `SmartHomeApp/app/`
6. Add a **Web app** → copy the config object

---

### Step 2: Configure the Android App

1. Replace `SmartHomeApp/app/google-services.json` with your real file from Firebase Console.
2. Open `SmartHomeApp/` in Android Studio.
3. Sync Gradle and build the project.
4. Run on emulator or device.

> **Note:** On first launch with an empty database, the app auto-seeds 15 sample devices across 3 floors.

---

### Step 3: Configure the Web Simulator

1. Open `simulator/src/firebase.js`
2. Replace the placeholder config object with your real Firebase web config:
```js
const firebaseConfig = {
  apiKey: "your-api-key",
  authDomain: "your-project-id.firebaseapp.com",
  databaseURL: "https://your-project-id-default-rtdb.firebaseio.com",
  projectId: "your-project-id",
  storageBucket: "your-project-id.appspot.com",
  messagingSenderId: "your-messaging-sender-id",
  appId: "your-app-id"
};
```
3. Run the simulator:
```bash
cd simulator
npm install
npm run dev
```
4. Open http://localhost:5173

---

### Step 4: Deploy Cloud Functions

```bash
# Login to Firebase
firebase login

# Update .firebaserc with your project ID
# Then deploy
firebase deploy --only functions
firebase deploy --only firestore:rules
```

---

## 📱 Android App Features

| Feature | Description |
|---|---|
| Multi-floor dashboard | Horizontal pager with floor tabs, add/remove floors |
| Floor plan view | Grid overlay with device markers at precise positions |
| Device list view | Grouped by type with summary stats |
| Device detail sheet | Type-specific controls (see below) |
| Usage reports | Vico bar charts + usage history log |
| Safety alerts | Real-time banner from Firebase RTDB |
| FCM notifications | Push alerts for safety cutoffs |

### Device Types & Controls
| Type | Control |
|---|---|
| 🔌 Outlet | Simple ON/OFF toggle |
| 🎛️ Multi-Switch | Individual per-switch toggles |
| ♨️ Iron | ON/OFF + max duration slider (5–120 min) |
| 💡 Light | ON/OFF + schedule picker (ON time / OFF time) |
| 📷 Camera | Live snapshot view + stream URL |

---

## 🖥️ Web Simulator Features

- **Real-time Firestore sync** — reflects all app changes instantly
- **Bidirectional control** — toggle devices from the simulator UI
- **Floor & type filtering** — filter by floor or device type
- **Search** — search by device name or room
- **Safety alert feed** — live alerts panel from RTDB
- **Camera snapshot view** — click to expand mock camera feed
- **Connection status** — shows Firebase connectivity state

---

## ☁️ Cloud Functions

| Function | Trigger | Purpose |
|---|---|---|
| `onDeviceStateWrite` | Firestore onDocumentWritten | Stamps `onSince`, logs usage |
| `checkSafetyCutoffs` | Cron every 1 min | Forces IRON devices OFF at max duration, sends FCM |
| `processLightSchedules` | Cron every 1 min | Flips LIGHT state per configured schedule |

---

## 🔄 Synchronization Architecture

```
Mobile App  ←──onSnapshot──→  Firestore  ←──Trigger──→  Cloud Functions
    │                              │                           │
    └──────────────────────────────┼───────────────────────────┘
                                   │
                         Realtime Database
                                   │
                         Web Simulator ←──onValue──→ Safety Alerts Feed
```

- **Latency:** < 2 seconds for state propagation (Firestore persistent connection)
- **Offline:** Android app uses Firestore offline persistence — syncs on reconnect
- **Safety:** Cloud Functions are server-side and run regardless of client state

---

## 🔐 Security

- All Firestore reads/writes require Firebase Authentication
- Device writes validated by security rules (type + state enums enforced)
- Usage logs are write-protected (Cloud Functions only)
- Anonymous auth supported for demo/testing

---

## 📊 Reporting

The Reports screen in the Android app shows:
- Overview: total devices, active count, error count
- Per device-type breakdown with total accumulated ON time
- Bar chart (Vico) of device distribution by type
- Usage log history with auto-cutoff events highlighted

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Mobile | Kotlin, Jetpack Compose, Material 3 |
| State | ViewModel, StateFlow, Kotlin Coroutines |
| Database | Firebase Firestore |
| Real-time Sync | Firebase Realtime Database |
| Safety Backend | Firebase Cloud Functions (Node.js 18) |
| Push Alerts | Firebase Cloud Messaging |
| Charts | Vico (Compose-native) |
| Image Loading | Coil |
| Web Simulator | React 18, Vite 5, Firebase Web SDK v9 |

---

## 📹 Video Presentation Guidelines

- Introduction of all 3 members + individual contributions
- Demo: login → floor navigation → device toggle → simulator reflects change
- Demo: iron auto-cutoff → push notification
- Demo: light schedule setting
- Demo: camera view
- Maximum 25 minutes
