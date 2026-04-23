# 📱 Habit Tracker App

🚀 A feature-rich Android habit tracking app built with modern Android architecture, designed to help users build consistency through gamification, analytics, and smart reminders.

---

## ✨ Features

### 🔐 Authentication
- Email & password sign-up with verification
- Secure login enforcement
- Password reset via email
- Resend verification support
- Clean logout with session reset

### 🧠 Habit Management
- Create, edit, delete habits
- Categories: General, Health, Study, Fitness, Mindfulness, Other
- Priority levels with visual indicators
- Checklist-based completion system
- Swipe-to-delete with animations

### 🔥 Gamification System
- Daily streak tracking
- Longest streak records
- XP reward system  
- Level progression system  
- 🧊 Streak freeze feature (prevents streak loss)

### ⏰ Smart Reminders
- Exact alarms using AlarmManager
- Works in Doze mode
- Auto-reschedules after reboot/update
- Reminder controls in profile

### 📊 Analytics & Insights
- Total habits, XP, streak stats
- XP distribution by category
- Habit ranking by streak
- Weekly progress tracking
- Monthly heatmap visualization

### 👤 Profile & Personalization
- Level & XP display
- Progress bar to next level
- Theme switching (Dark / Light / Cream)
- Reminder management UI

### 🎨 UX Enhancements
- Animated UI transitions
- Motivational quote carousel
- Expandable habit cards with analytics
- Snackbar feedback messages
- Offline support (Firestore persistence)

---

## 🛠️ Tech Stack

- **Language:** Kotlin  
- **UI:** Jetpack Compose (Material 3)  
- **Architecture:** MVVM  
- **Backend:** Firebase Authentication + Firestore  
- **Async:** Kotlin Coroutines & Flow  
- **Notifications:** AlarmManager + NotificationCompat  
- **Image Loading:** Coil  
- **Navigation:** Custom state-based navigation  

---

## 🧱 Architecture Overview

The app follows MVVM architecture:

- **View:** Jetpack Compose UI  
- **ViewModel:** State management using StateFlow  
- **Model:** Firebase + local persistence  

Real-time updates are handled using `Flow` and `callbackFlow`.

---

## Contribution
- **Developed by**: Shashanka
- **Testing, Feedback & Feature Recommendations**: Jijnashu

## connect us at
- **Shashanka** :shashankashekharnayak62@gmail.com

