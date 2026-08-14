/**
 * Smart Home Monitor — Firebase Cloud Functions
 *
 * Function 1: onDeviceStateWrite
 *   Firestore trigger — fires when any device document is updated.
 *   - Records onSince when an IRON device is turned ON.
 *   - Logs usage and clears onSince when turned OFF.
 *
 * Function 2: checkSafetyCutoffs
 *   Scheduled every 1 minute — scans IRON devices that have exceeded
 *   their maxOnDurationMinutes and forces them OFF, then sends FCM alert.
 *
 * Function 3: processLightSchedules
 *   Scheduled every 1 minute — checks LIGHT devices with active schedules
 *   and flips their state based on current time.
 */

const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const db = getFirestore();
const rtdb = getDatabase();
const HOME_ID = "home_001";

// ─────────────────────────────────────────────────────────
// FUNCTION 1: onDeviceStateWrite
// Firestore trigger — runs when any device document changes.
// ─────────────────────────────────────────────────────────
exports.onDeviceStateWrite = onDocumentWritten(
  `homes/${HOME_ID}/devices/{deviceId}`,
  async (event) => {
    const deviceId = event.params.deviceId;
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();

    if (!after) return; // Document deleted — nothing to do

    const prevState = before?.state;
    const newState = after.state;
    const deviceType = after.type;

    // ── IRON: record onSince when turned ON ──
    if (deviceType === "IRON") {
      if (newState === "ON" && prevState !== "ON") {
        // Turning ON: stamp onSince
        await db
          .doc(`homes/${HOME_ID}/devices/${deviceId}`)
          .update({ onSince: Timestamp.now() });
        console.log(`[Iron] ${after.name} turned ON — onSince stamped`);
      }

      if (newState === "OFF" && prevState === "ON" && before?.onSince) {
        // Turning OFF: log usage duration
        const onSince = before.onSince.toDate();
        const now = new Date();
        const durationMinutes = Math.round((now - onSince) / 60000);

        const logRef = db.collection(`homes/${HOME_ID}/usageLogs`).doc();
        await logRef.set({
          deviceId,
          deviceName: after.name,
          deviceType: "IRON",
          startTime: before.onSince,
          endTime: Timestamp.now(),
          durationMinutes,
          autoCutoff: false,
        });

        await db
          .doc(`homes/${HOME_ID}/devices/${deviceId}`)
          .update({
            onSince: FieldValue.delete(),
            totalOnTimeMinutes: FieldValue.increment(durationMinutes),
          });

        console.log(`[Iron] ${after.name} turned OFF — logged ${durationMinutes} minutes`);
      }
    }

    // ── LIGHT: log usage ──
    if (deviceType === "LIGHT" && newState === "OFF" && prevState === "ON" && before?.onSince) {
      const onSince = before.onSince.toDate();
      const durationMinutes = Math.round((new Date() - onSince) / 60000);
      const logRef = db.collection(`homes/${HOME_ID}/usageLogs`).doc();
      await logRef.set({
        deviceId,
        deviceName: after.name,
        deviceType: "LIGHT",
        startTime: before.onSince,
        endTime: Timestamp.now(),
        durationMinutes,
        autoCutoff: false,
      });
      await db
        .doc(`homes/${HOME_ID}/devices/${deviceId}`)
        .update({ totalOnTimeMinutes: FieldValue.increment(durationMinutes) });
    }

    // ── LIGHT: stamp onSince when turned ON ──
    if (deviceType === "LIGHT" && newState === "ON" && prevState !== "ON") {
      await db
        .doc(`homes/${HOME_ID}/devices/${deviceId}`)
        .update({ onSince: Timestamp.now() });
    }
  }
);

// ─────────────────────────────────────────────────────────
// FUNCTION 2: checkSafetyCutoffs
// Scheduled every minute — checks iron devices for duration breach.
// ─────────────────────────────────────────────────────────
exports.checkSafetyCutoffs = onSchedule("every 1 minutes", async (event) => {
  console.log("[Safety] Running safety cutoff check...");

  const snapshot = await db
    .collection(`homes/${HOME_ID}/devices`)
    .where("type", "==", "IRON")
    .where("state", "==", "ON")
    .get();

  const now = new Date();
  const batch = db.batch();
  const alerts = [];

  for (const doc of snapshot.docs) {
    const device = doc.data();
    if (!device.onSince) continue;

    const onSince = device.onSince.toDate();
    const elapsedMinutes = (now - onSince) / 60000;
    const maxMinutes = device.maxOnDurationMinutes || 30;

    if (elapsedMinutes >= maxMinutes) {
      console.log(`[Safety] CUTOFF: ${device.name} — on for ${elapsedMinutes.toFixed(1)} min (max: ${maxMinutes})`);

      // Force device OFF
      batch.update(doc.ref, {
        state: "OFF",
        onSince: FieldValue.delete(),
        lastUpdated: Timestamp.now(),
      });

      // Log the auto-cutoff event
      const logRef = db.collection(`homes/${HOME_ID}/usageLogs`).doc();
      batch.set(logRef, {
        deviceId: doc.id,
        deviceName: device.name,
        deviceType: "IRON",
        startTime: device.onSince,
        endTime: Timestamp.now(),
        durationMinutes: Math.round(elapsedMinutes),
        autoCutoff: true,
      });

      alerts.push({
        deviceId: doc.id,
        deviceName: device.name,
        message: `⚠️ ${device.name} was auto-OFF after ${Math.round(elapsedMinutes)} minutes (max: ${maxMinutes} min)`,
        timestamp: now.getTime(),
        type: "SAFETY_CUTOFF",
      });
    }
  }

  await batch.commit();

  // Push alerts to RTDB and send FCM notifications
  for (const alert of alerts) {
    const alertRef = rtdb.ref(`homes/${HOME_ID}/alerts`).push();
    await alertRef.set(alert);

    // Send FCM topic notification
    try {
      await getMessaging().sendToTopic("smart_home_safety", {
        notification: {
          title: "🔴 Safety Alert — Iron Auto Off",
          body: alert.message,
        },
        data: {
          type: "SAFETY_CUTOFF",
          deviceId: alert.deviceId,
          deviceName: alert.deviceName,
        },
      });
    } catch (err) {
      console.warn("[Safety] FCM send failed:", err.message);
    }
  }

  console.log(`[Safety] Check complete — ${alerts.length} cutoffs triggered`);
});

// ─────────────────────────────────────────────────────────
// FUNCTION 3: processLightSchedules
// Scheduled every minute — flips light state per schedule.
// ─────────────────────────────────────────────────────────
exports.processLightSchedules = onSchedule("every 1 minutes", async (event) => {
  console.log("[Scheduler] Processing light schedules...");

  const snapshot = await db
    .collection(`homes/${HOME_ID}/devices`)
    .where("type", "==", "LIGHT")
    .where("scheduleEnabled", "==", true)
    .get();

  const now = new Date();
  const currentHour = now.getHours();
  const currentMinute = now.getMinutes();
  const currentTime = `${String(currentHour).padStart(2, "0")}:${String(currentMinute).padStart(2, "0")}`;

  const batch = db.batch();
  let flips = 0;

  for (const doc of snapshot.docs) {
    const device = doc.data();
    const { scheduleOnTime, scheduleOffTime, state } = device;

    if (!scheduleOnTime || !scheduleOffTime) continue;

    // Turn ON at scheduleOnTime
    if (currentTime === scheduleOnTime && state !== "ON") {
      batch.update(doc.ref, {
        state: "ON",
        onSince: Timestamp.now(),
        lastUpdated: Timestamp.now(),
      });
      console.log(`[Scheduler] Turning ON: ${device.name} at ${currentTime}`);
      flips++;
    }

    // Turn OFF at scheduleOffTime
    if (currentTime === scheduleOffTime && state !== "OFF") {
      batch.update(doc.ref, {
        state: "OFF",
        onSince: FieldValue.delete(),
        lastUpdated: Timestamp.now(),
      });
      console.log(`[Scheduler] Turning OFF: ${device.name} at ${currentTime}`);
      flips++;
    }
  }

  await batch.commit();
  console.log(`[Scheduler] Done — ${flips} state flip(s)`);
});
