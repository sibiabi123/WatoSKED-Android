# 📱 WatoSKED — Native Android WhatsApp Auto-Scheduler (SKEDit Alternative)

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Build APK](https://img.shields.io/badge/Build-Automated%20CI%2FCD-blue?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/sibiabi123/WatoSKED-Android/actions)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

A standalone **Native Android Application** that schedules and automatically dispatches WhatsApp messages directly from your phone — **without WhatsApp Web, without external servers, and without scanning QR codes**.

---

## ⚡ How It Works (SKEDit Architecture)

```
  ┌─────────────────────────────────┐
  │   User Schedules Message in     │
  │            WatoSKED             │
  └────────────────┬────────────────┘
                   │
                   ▼ Exact RTC Alarm
  ┌─────────────────────────────────┐
  │   Android AlarmManager Triggers  │
  │     (Even in Doze/Deep Sleep)   │
  └────────────────┬────────────────┘
                   │
                   ▼ Launches URI Intent
  ┌─────────────────────────────────┐
  │  Native WhatsApp App Opens      │
  │  (api.whatsapp.com/send?phone)  │
  └────────────────┬────────────────┘
                   │
                   ▼ Accessibility Node Event
  ┌─────────────────────────────────┐
  │  WhatsAppAccessibilityService   │
  │  Finds & Auto-Clicks "Send" 🚀  │
  └─────────────────────────────────┘
```

---

## 🌟 Key Features

- 🤖 **On-Device Automation**: Uses Android's **`AccessibilityService`** to simulate clicking the WhatsApp Send button.
- 📱 **Native WhatsApp Support**: Works with standard **WhatsApp** and **WhatsApp Business**.
- ⏰ **Exact Alarm Scheduling**: Employs `AlarmManager.setExactAndAllowWhileIdle()` to guarantee millisecond-level precision even in battery saver / doze modes.
- 🔋 **Battery Optimization Helper**: Direct bypass prompt to prevent background task killing.
- 🔄 **Reboot Resilience**: Listens to `BOOT_COMPLETED` and automatically restores pending alarms when your phone restarts.
- 🔒 **100% Private**: Your messages and contacts stay strictly on your device SQLite database — nothing is uploaded to any server.

---

## 📲 Download & Installation

1. Go to the [**Releases**](https://github.com/sibiabi123/WatoSKED-Android/releases) tab.
2. Download the latest `WatoSKED-v1.0.0-debug.apk` directly to your Android device.
3. Tap the file to install (enable *Install Unknown Apps* if prompted).
4. Launch **WatoSKED** and enable the **Accessibility Service** in your phone's Settings.
5. Schedule your message and enjoy autonomous sending!
