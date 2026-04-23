package com.example.myapplication

import android.Manifest
import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.net.Uri
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
// ─────────────────────────────────────────────────────────────────────────────
//  CONSTANTS
// ─────────────────────────────────────────────────────────────────────────────

private const val CHANNEL_ID       = "habit_channel_v2"
private const val USERS_COLLECTION = "users6"
private val SDF get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private fun today() = SDF.format(Date())

private const val APP_LOGO_URL = "https://i.pinimg.com/564x/aa/89/1c/aa891ca5af0fa2ba520d2206de4a8c1e.jpg"

// ─────────────────────────────────────────────────────────────────────────────
//  THEME MODE ENUM
// ─────────────────────────────────────────────────────────────────────────────

enum class ThemeMode { DARK, LIGHT, CREAM }

fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.DARK  -> ThemeMode.LIGHT
    ThemeMode.LIGHT -> ThemeMode.CREAM
    ThemeMode.CREAM -> ThemeMode.DARK
}

fun ThemeMode.icon() = when (this) {
    ThemeMode.DARK  -> Icons.Default.NightsStay
    ThemeMode.LIGHT -> Icons.Default.WbSunny
    ThemeMode.CREAM -> Icons.Default.Coffee
}
fun ThemeMode.label() = when (this) {
    ThemeMode.DARK  -> "Dark"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.CREAM -> "Cream"
}

// ─────────────────────────────────────────────────────────────────────────────
//  MOTIVATIONAL QUOTES
// ─────────────────────────────────────────────────────────────────────────────

private val motivationalQuotes = listOf(
    "We are what we repeatedly do. Excellence is not an act, but a habit." to "— Aristotle",
    "Motivation gets you started. Habit keeps you going." to "— Jim Ryun",
    "The secret of your future is hidden in your daily routine." to "— Mike Murdock",
    "Small disciplines repeated with consistency lead to great achievements." to "— John Maxwell",
    "Your habits today will determine your success tomorrow." to "— Unknown",
    "Discipline is choosing between what you want now and what you want most." to "— Augusta F. Kantra",
    "A year from now you'll wish you had started today." to "— Karen Lamb",
    "Success is the sum of small efforts, repeated day in and day out." to "— Robert Collier",
    "The chains of habit are too light to be felt until they are too heavy to be broken." to "— Warren Buffett",
    "Every action you take is a vote for the type of person you wish to become." to "— James Clear",
    "You don't have to be extreme, just consistent… like brushing your teeth, but for your goals." to "— Unknown",
    "Your future self is watching you right now through memories. Don't embarrass them." to "— Unknown",
    "Discipline is basically choosing between Netflix and a better life." to "— Unknown",
    "We first make our habits, then our habits make us." to "— John Dryden",
    "Quality is not an act, it is a habit." to "— Aristotle",
    "Good habits formed at youth make all the difference." to "— Aristotle",
    "Success doesn't come from what you do occasionally, it comes from what you do consistently." to "— Marie Forleo",
    "Habits are the compound interest of self-improvement." to "— James Clear",
    "The difference between who you are and who you want to be is what you do." to "— Unknown",
    "Action is the foundational key to all success." to "— Pablo Picasso",
    "You do not rise to the level of your goals. You fall to the level of your systems." to "— James Clear",
    "Sow a thought, and you reap an action; sow an act, and you reap a habit." to "— Ralph Waldo Emerson",
    "Success is nothing more than a few simple disciplines, practiced every day." to "— Jim Rohn",
    "Do it even when you don't feel like it. Especially then. That's the whole trick." to "— Unknown",
    "Small progress is still progress… unless you're scrolling endlessly." to "— Unknown",
    "If it's important, you'll find a way. If not, you'll find Wi-Fi." to "— Unknown",
    "Consistency beats motivation… because motivation has commitment issues." to "— Unknown",
    "You said 'just 5 minutes' yesterday too. Maybe today let's surprise yourself." to "— Unknown",
    "The only bad workout is the one you didn't do… yes, even the awkward ones." to "— Unknown",
    "You don't need a new plan. You need to stop negotiating with yourself." to "— Unknown",
    "Your goals don't care how you feel today. Show up anyway." to "— Unknown",
    "Be stronger than your excuses. They're getting way too comfortable." to "— Unknown"
)

// ─────────────────────────────────────────────────────────────────────────────
//  DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class Habit(
    val id: String              = "",
    val name: String            = "",
    val checklist: List<String> = emptyList(),
    val streak: Int             = 0,
    val longestStreak: Int      = 0,
    val xp: Int                 = 0,
    val category: String        = "General",
    val priority: String        = "Medium",
    val notes: String           = "",
    val reminderHour: Int       = 8,
    val reminderMinute: Int     = 0,
    val streakFreezeCount: Int  = 0,
    val logs: List<String>      = emptyList()
)

data class UserProfile(
    val uid: String   = "",
    val name: String  = "",
    val email: String = "",
    val totalXp: Int  = 0,
    val level: Int    = 1
)

fun levelFromXp(xp: Int): Int = maxOf(1, (xp / 100) + 1)

// ─────────────────────────────────────────────────────────────────────────────
//  MAIN ACTIVITY
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        configureFirestore()
        requestNotificationPermission()
        setContent { HabitApp() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.deleteNotificationChannel("habit_channel")

            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Habit Tracker Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily habit reminders with sound"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 200, 300)
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, audioAttrs)
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    private fun configureFirestore() {
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  REMINDER RECEIVER
// ─────────────────────────────────────────────────────────────────────────────

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return

        val action    = intent?.getStringExtra("action")
        val habitName = intent?.getStringExtra("habit_name") ?: "Your habit"
        val hour      = intent?.getIntExtra("reminder_hour", 8) ?: 8
        val minute    = intent?.getIntExtra("reminder_minute", 0) ?: 0
        val reqCode   = intent?.getIntExtra("request_code", habitName.hashCode()) ?: habitName.hashCode()

        if (action == "mark_done") return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("$habitName ⏰")
            .setContentText("Time to work on: $habitName 💪")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(habitName.hashCode(), notification)
        } catch (_: SecurityException) { }

        scheduleHabitReminder(context, habitName, hour, minute, reqCode)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  BOOT RECEIVER
// ─────────────────────────────────────────────────────────────────────────────

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db  = FirebaseFirestore.getInstance()

        db.collection(USERS_COLLECTION).document(uid).collection("habits")
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    val name   = doc.getString("name") ?: return@forEach
                    val hour   = doc.getLong("reminderHour")?.toInt() ?: 8
                    val minute = doc.getLong("reminderMinute")?.toInt() ?: 0
                    scheduleHabitReminder(context, name, hour, minute, name.hashCode())
                }
            }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SCHEDULER
// ─────────────────────────────────────────────────────────────────────────────

fun scheduleHabitReminder(
    context: Context,
    habitName: String,
    hour: Int,
    minute: Int,
    requestCode: Int
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !alarmManager.canScheduleExactAlarms()) {
        return
    }

    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("habit_name",      habitName)
        putExtra("reminder_hour",   hour)
        putExtra("reminder_minute", minute)
        putExtra("request_code",    requestCode)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context, requestCode, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE,      minute)
        set(Calendar.SECOND,      0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }

    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pendingIntent
        )
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

fun cancelHabitReminder(context: Context, habitName: String, requestCode: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pending = PendingIntent.getBroadcast(
        context, requestCode,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
    ) ?: return
    alarmManager.cancel(pending)
    pending.cancel()
}

// ─────────────────────────────────────────────────────────────────────────────
//  FIRESTORE REPOSITORY
// ─────────────────────────────────────────────────────────────────────────────

object HabitRepository {
    private val db  get() = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private fun habitsRef(userId: String) =
        db.collection(USERS_COLLECTION).document(userId).collection("habits")
    private fun logsRef(userId: String, habitId: String) =
        habitsRef(userId).document(habitId).collection("logs")
    private fun userRef(userId: String) =
        db.collection(USERS_COLLECTION).document(userId)

    fun habitsFlow(userId: String): Flow<List<Habit>> = callbackFlow {
        val listener = habitsRef(userId).addSnapshotListener { snap, err ->
            if (err != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap.documents.mapNotNull { doc ->
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    Habit(
                        id                = doc.id,
                        name              = doc.getString("name") ?: "",
                        checklist         = doc.get("checklist") as? List<String> ?: emptyList(),
                        streak            = doc.getLong("streak")?.toInt() ?: 0,
                        longestStreak     = doc.getLong("longestStreak")?.toInt() ?: 0,
                        xp                = doc.getLong("xp")?.toInt() ?: 0,
                        category          = doc.getString("category") ?: "General",
                        priority          = doc.getString("priority") ?: "Medium",
                        notes             = doc.getString("notes") ?: "",
                        reminderHour      = doc.getLong("reminderHour")?.toInt() ?: 8,
                        reminderMinute    = doc.getLong("reminderMinute")?.toInt() ?: 0,
                        streakFreezeCount = doc.getLong("streakFreezeCount")?.toInt() ?: 0
                    )
                }.getOrNull()
            }
            trySend(list)
        }
        awaitClose { listener.remove() }
    }

    fun logsFlow(userId: String, habitId: String): Flow<List<String>> = callbackFlow {
        val listener = logsRef(userId, habitId).addSnapshotListener { snap, err ->
            if (err != null || snap == null) { trySend(emptyList()); return@addSnapshotListener }
            trySend(snap.documents.mapNotNull { it.getString("date") })
        }
        awaitClose { listener.remove() }
    }

    suspend fun addHabit(habit: Habit): Result<Unit> {
        val userId = uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return runCatching {
            habitsRef(userId).add(
                mapOf(
                    "name"              to habit.name,
                    "checklist"         to habit.checklist,
                    "streak"            to 0,
                    "longestStreak"     to 0,
                    "xp"                to 0,
                    "category"          to habit.category,
                    "priority"          to habit.priority,
                    "notes"             to habit.notes,
                    "reminderHour"      to habit.reminderHour,
                    "reminderMinute"    to habit.reminderMinute,
                    "streakFreezeCount" to 0
                )
            ).await(); Unit
        }
    }

    suspend fun deleteHabit(habitId: String): Result<Unit> {
        val userId = uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return runCatching {
            val habitRef  = habitsRef(userId).document(habitId)
            val habitSnap = habitRef.get().await()
            val habitXp   = habitSnap.getLong("xp")?.toInt() ?: 0
            val logs      = habitRef.collection("logs").get().await()
            for (doc in logs.documents) doc.reference.delete().await()
            habitRef.delete().await()
            if (habitXp > 0) {
                val userSnap     = userRef(userId).get().await()
                val currentTotal = userSnap.getLong("totalXp")?.toInt() ?: 0
                val newTotal     = maxOf(0, currentTotal - habitXp)
                userRef(userId).update(mapOf("totalXp" to newTotal, "level" to levelFromXp(newTotal))).await()
            }
        }
    }

    suspend fun updateHabit(habit: Habit): Result<Unit> {
        val userId = uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return runCatching {
            habitsRef(userId).document(habit.id).update(
                mapOf(
                    "name"           to habit.name,
                    "checklist"      to habit.checklist,
                    "category"       to habit.category,
                    "priority"       to habit.priority,
                    "notes"          to habit.notes,
                    "reminderHour"   to habit.reminderHour,
                    "reminderMinute" to habit.reminderMinute
                )
            ).await()
        }
    }

    suspend fun markDone(habitId: String): Result<Unit> {
        val userId = uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return runCatching {
            val logRef   = logsRef(userId, habitId)
            val habitRef = habitsRef(userId).document(habitId)
            val todayStr = today()
            val existing = logRef.whereEqualTo("date", todayStr).get().await()
            if (!existing.isEmpty) return@runCatching

            logRef.add(mapOf("date" to todayStr, "ts" to System.currentTimeMillis())).await()

            val allDates    = logRef.get().await().documents.mapNotNull { it.getString("date") }.sortedDescending()
            val streak      = calcStreak(allDates)
            val currentSnap = habitRef.get().await()
            val prevLongest = currentSnap.getLong("longestStreak")?.toInt() ?: 0
            val prevHabitXp = currentSnap.getLong("xp")?.toInt() ?: 0
            val xpGain      = 10 + (streak * 2)
            val newHabitXp  = prevHabitXp + xpGain

            habitRef.update(
                mapOf(
                    "streak"        to streak,
                    "longestStreak" to maxOf(streak, prevLongest),
                    "xp"            to newHabitXp
                )
            ).await()

            val userSnap  = userRef(userId).get().await()
            val prevTotal = userSnap.getLong("totalXp")?.toInt() ?: 0
            val newTotal  = prevTotal + xpGain
            userRef(userId).update(
                mapOf("totalXp" to newTotal, "level" to levelFromXp(newTotal))
            ).await()
        }
    }

    suspend fun useStreakFreeze(habitId: String): Result<Unit> {
        val userId = uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return runCatching {
            val ref     = habitsRef(userId).document(habitId)
            val snap    = ref.get().await()
            val freezes = snap.getLong("streakFreezeCount")?.toInt() ?: 0
            if (freezes > 0) {
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                logsRef(userId, habitId).add(mapOf("date" to SDF.format(yesterday.time), "freeze" to true)).await()
                ref.update("streakFreezeCount", freezes - 1).await()
            }
        }
    }

    suspend fun recalculateTotalXp(userId: String): Result<Unit> {
        return runCatching {
            val habits = habitsRef(userId).get().await()
            val total  = habits.documents.sumOf { it.getLong("xp")?.toInt() ?: 0 }
            userRef(userId).update(
                mapOf("totalXp" to total, "level" to levelFromXp(total))
            ).await()
        }
    }

    fun profileFlow(userId: String): Flow<UserProfile> = callbackFlow {
        val listener = userRef(userId).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            trySend(
                UserProfile(
                    uid     = userId,
                    name    = snap.getString("name") ?: "",
                    email   = snap.getString("email") ?: "",
                    totalXp = snap.getLong("totalXp")?.toInt() ?: 0,
                    level   = snap.getLong("level")?.toInt() ?: 1
                )
            )
        }
        awaitClose { listener.remove() }
    }

    private fun calcStreak(sortedDescDates: List<String>): Int {
        if (sortedDescDates.isEmpty()) return 0
        var streak = 0
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        for (dateStr in sortedDescDates) {
            if (dateStr == SDF.format(cal.time)) { streak++; cal.add(Calendar.DAY_OF_YEAR, -1) }
            else break
        }
        return streak
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  AUTH UI STATE
// ─────────────────────────────────────────────────────────────────────────────

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val msg: String) : AuthUiState()
    data class VerificationSent(val email: String) : AuthUiState()
    data class UnverifiedEmail(val email: String) : AuthUiState()
    data class PasswordResetSent(val email: String) : AuthUiState()
}

// ─────────────────────────────────────────────────────────────────────────────
//  AUTH VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    var uiState by mutableStateOf<AuthUiState>(AuthUiState.Idle)
        private set

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) { uiState = AuthUiState.Error("Fill all fields"); return }
        uiState = AuthUiState.Loading
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) { uiState = AuthUiState.Error("Unexpected error — user is null"); return@addOnSuccessListener }
                user.reload().addOnSuccessListener {
                    if (user.isEmailVerified) {
                        viewModelScope.launch { HabitRepository.recalculateTotalXp(user.uid) }
                        uiState = AuthUiState.Idle; onSuccess()
                    } else { auth.signOut(); uiState = AuthUiState.UnverifiedEmail(email.trim()) }
                }.addOnFailureListener { auth.signOut(); uiState = AuthUiState.Error("Could not verify email status.") }
            }
            .addOnFailureListener { uiState = AuthUiState.Error(it.message ?: "Login failed") }
    }

    fun signup(name: String, email: String, password: String, onSuccess: () -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) { uiState = AuthUiState.Error("Fill all fields"); return }
        uiState = AuthUiState.Loading
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { res ->
                val uid  = res.user?.uid
                val user = res.user
                if (uid == null || user == null) { uiState = AuthUiState.Error("Unknown error"); return@addOnSuccessListener }
                db.collection(USERS_COLLECTION).document(uid)
                    .set(mapOf("name" to name, "email" to email.trim(),
                        "createdAt" to System.currentTimeMillis(), "totalXp" to 0, "level" to 1))
                    .addOnSuccessListener {
                        user.sendEmailVerification()
                            .addOnSuccessListener { auth.signOut(); uiState = AuthUiState.VerificationSent(email.trim()) }
                            .addOnFailureListener { e -> auth.signOut(); uiState = AuthUiState.Error("Account created but verification email failed: ${e.message}") }
                    }
                    .addOnFailureListener { uiState = AuthUiState.Error(it.message ?: "Error saving profile") }
            }
            .addOnFailureListener { uiState = AuthUiState.Error(it.message ?: "Signup failed") }
    }

    fun resendVerificationEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { uiState = AuthUiState.Error("Enter your password to resend"); return }
        uiState = AuthUiState.Loading
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user ?: run { uiState = AuthUiState.Error("Could not sign in"); return@addOnSuccessListener }
                if (user.isEmailVerified) { uiState = AuthUiState.Idle; return@addOnSuccessListener }
                user.sendEmailVerification()
                    .addOnSuccessListener { auth.signOut(); uiState = AuthUiState.VerificationSent(email.trim()) }
                    .addOnFailureListener { e -> auth.signOut(); uiState = AuthUiState.Error(e.message ?: "Failed to resend") }
            }
            .addOnFailureListener { uiState = AuthUiState.Error(it.message ?: "Sign-in failed") }
    }

    fun resetPassword(email: String, onResult: (String) -> Unit) {
        if (email.isBlank()) { onResult("Enter your email first"); return }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { onResult("Enter a valid email"); return }
        uiState = AuthUiState.Loading
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener { uiState = AuthUiState.PasswordResetSent(email.trim()); onResult("Reset link sent to ${email.trim()} 📧") }
            .addOnFailureListener { uiState = AuthUiState.Idle; onResult(it.message ?: "Failed to send reset email") }
    }

    fun logout(onDone: () -> Unit) { auth.signOut(); onDone() }
    fun clearError() { if (uiState is AuthUiState.Error) uiState = AuthUiState.Idle }
}

// ─────────────────────────────────────────────────────────────────────────────
//  THEME — Royal Blue, three modes: Dark / Light / Cream
// ─────────────────────────────────────────────────────────────────────────────

private val RoyalBlue      = Color(0xFF2962FF)
private val RoyalBlueLight = Color(0xFF82B1FF)
private val RoyalBlueDark  = Color(0xFF0039CB)
private val SkyBlue        = Color(0xFF40C4FF)
private val NavyBlue       = Color(0xFF0D1B4B)

private val DarkColorScheme = darkColorScheme(
    primary        = RoyalBlue,
    onPrimary      = Color.White,
    secondary      = SkyBlue,
    background     = Color(0xFF060B18),
    surface        = Color(0xFF0D1526),
    onBackground   = Color(0xFFE8EEFF),
    onSurface      = Color(0xFFE8EEFF),
    error          = Color(0xFFFF5252),
    surfaceVariant = Color(0xFF111D36),
    outline        = Color(0xFF1A2A4A)
)

private val LightColorScheme = lightColorScheme(
    primary        = RoyalBlueDark,
    onPrimary      = Color.White,
    secondary      = RoyalBlue,
    background     = Color(0xFFF0F4FF),
    surface        = Color.White,
    onBackground   = Color(0xFF0A1A40),
    onSurface      = Color(0xFF0A1A40),
    error          = Color(0xFFD50000),
    surfaceVariant = Color(0xFFDDE5FF),
    outline        = Color(0xFFB3C3F0)
)

private val CreamColorScheme = lightColorScheme(
    primary        = Color(0xFF1A4ED8),
    onPrimary      = Color.White,
    secondary      = Color(0xFF3B82F6),
    background     = Color(0xFFFFF9F0),
    surface        = Color(0xFFFFF5E6),
    onBackground   = Color(0xFF1C1A10),
    onSurface      = Color(0xFF1C1A10),
    error          = Color(0xFFB71C1C),
    surfaceVariant = Color(0xFFF5EDDA),
    outline        = Color(0xFFD4C4A0)
)

@Composable
fun HabitTheme(themeMode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val scheme = when (themeMode) {
        ThemeMode.DARK  -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.CREAM -> CreamColorScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

// ─────────────────────────────────────────────────────────────────────────────
//  MADE WITH LOVE FOOTER  — whisper-loud animated branding
// ─────────────────────────────────────────────────────────────────────────────

// Tiny lerp helper — avoids kotlin.math.lerp import ambiguity
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

@Composable
fun MadeWithLoveFooter(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "footer")

    // Beating heart
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.30f, label = "heartScale",
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val heartAlpha by infiniteTransition.animateFloat(
        initialValue = 0.60f, targetValue = 1f, label = "heartAlpha",
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Shimmer sweep across names
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f, label = "shimmer",
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Restart)
    )

    val nameColor1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
    val nameColor2 = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        HorizontalDivider(
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
            thickness = 0.5.dp,
            modifier  = Modifier.padding(horizontal = 40.dp)
        )
        Spacer(Modifier.height(6.dp))

        // "crafted with ❤️ by" row
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text          = "crafted with ",
                fontSize      = 11.sp,
                letterSpacing = 1.8.sp,
                color         = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f),
                fontWeight    = FontWeight.Light
            )
            Text(
                text     = "❤️",
                fontSize = 13.sp,
                modifier = Modifier
                    .scale(heartScale)
                    .alpha(heartAlpha)
            )
            Text(
                text          = " by",
                fontSize      = 11.sp,
                letterSpacing = 1.8.sp,
                color         = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.30f),
                fontWeight    = FontWeight.Light
            )
        }

        // Shimmering names
        Text(
            text          = "Shashanka  ✦  Jijnashu",
            fontSize      = 12.sp,
            letterSpacing = 3.sp,
            fontWeight    = FontWeight.SemiBold,
            style         = MaterialTheme.typography.labelMedium.copy(
                brush = Brush.horizontalGradient(
                    colors = listOf(nameColor1, nameColor2, nameColor1),
                    startX = lerp(-400f, 200f, shimmer),
                    endX   = lerp(0f, 800f, shimmer)
                )
            )
        )

        Spacer(Modifier.height(4.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  APP ROOT + NAVIGATION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HabitApp() {
    var themeMode by remember { mutableStateOf(ThemeMode.DARK) }
    HabitTheme(themeMode = themeMode) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppNavigation(themeMode) { themeMode = themeMode.next() }
        }
    }
}

enum class MainTab { HABITS, STATS, PROFILE }

// ─────────────────────────────────────────────────────────────────────────────
//  APP NAVIGATION
//  FIX: sessionKey changes on every login → key(sessionKey) forces Compose to
//  tear down and recreate the entire MainScaffold subtree, which discards all
//  stale ViewModels (DashboardViewModel, HabitCardViewModel, etc.) that were
//  bound to the previous user's UID. No app restart needed.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppNavigation(themeMode: ThemeMode, onCycleTheme: () -> Unit) {
    // Initialise sessionKey from the current user (already logged in) or a placeholder.
    var sessionKey by remember {
        mutableStateOf(
            FirebaseAuth.getInstance().currentUser?.uid ?: "logged_out"
        )
    }
    var screen by remember {
        mutableStateOf(
            if (FirebaseAuth.getInstance().currentUser != null) "main" else "login"
        )
    }

    AnimatedContent(
        targetState = screen, label = "nav",
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }
    ) { s ->
        when (s) {
            "login" -> LoginScreen(
                onLogin = {
                    // Refresh sessionKey so key(sessionKey) below triggers a full
                    // recomposition of MainScaffold and all its child ViewModels.
                    sessionKey = FirebaseAuth.getInstance().currentUser?.uid
                        ?: UUID.randomUUID().toString()
                    screen = "main"
                },
                goSignup = { screen = "signup" }
            )
            "signup" -> SignupScreen(onDone = { screen = "login" })
            "main"   -> key(sessionKey) {
                // This key() call is the core of the logout fix.
                // When sessionKey changes (new login), Compose treats this as a
                // brand-new node in the tree → all viewModel() calls inside get
                // fresh instances with the correct uid.
                MainScaffold(
                    themeMode    = themeMode,
                    onCycleTheme = onCycleTheme,
                    onLogout     = { screen = "login" }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MAIN SCAFFOLD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScaffold(themeMode: ThemeMode, onCycleTheme: () -> Unit, onLogout: () -> Unit) {
    var currentTab by remember { mutableStateOf(MainTab.HABITS) }
    val vm: DashboardViewModel = viewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier       = Modifier.height(64.dp)
            ) {
                listOf(
                    Triple(MainTab.HABITS,  Icons.Default.Home,     "Habits"),
                    Triple(MainTab.STATS,   Icons.Default.BarChart, "Stats"),
                    Triple(MainTab.PROFILE, Icons.Default.Person,   "Profile")
                ).forEach { (tab, icon, label) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick  = { currentTab = tab },
                        icon     = { Icon(icon, label, modifier = Modifier.size(22.dp)) },
                        label    = { Text(label, fontSize = 11.sp) },
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            indicatorColor      = MaterialTheme.colorScheme.primary.copy(0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(targetState = currentTab, label = "tab",
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }
            ) { tab ->
                when (tab) {
                    MainTab.HABITS  -> DashboardScreen(vm, themeMode, onCycleTheme, onLogout)
                    MainTab.STATS   -> StatsScreen(vm)
                    MainTab.PROFILE -> ProfileScreen(vm, themeMode, onCycleTheme, onLogout)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DASHBOARD VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class DashboardViewModel : ViewModel() {
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    val habits: StateFlow<List<Habit>> = run {
        val userId = uid ?: return@run MutableStateFlow(emptyList())
        HabitRepository.habitsFlow(userId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val profile: StateFlow<UserProfile?> = run {
        val userId = uid ?: return@run MutableStateFlow(null)
        HabitRepository.profileFlow(userId).map { it as UserProfile? }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    var errorMsg by mutableStateOf<String?>(null)

    fun addHabit(habit: Habit)    { viewModelScope.launch { HabitRepository.addHabit(habit).onFailure { errorMsg = it.message } } }
    fun deleteHabit(id: String)   { viewModelScope.launch { HabitRepository.deleteHabit(id).onFailure { errorMsg = it.message } } }
    fun updateHabit(habit: Habit) { viewModelScope.launch { HabitRepository.updateHabit(habit).onFailure { errorMsg = it.message } } }

    // FIX: Clear local state before signing out so nothing leaks to the UI
    // during the brief window before key(sessionKey) tears this VM down.
    fun logout(onDone: () -> Unit) {
        errorMsg = null
        FirebaseAuth.getInstance().signOut()
        onDone()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HABIT CARD VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class HabitCardViewModel(private val habitId: String) : ViewModel() {
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    val logs: StateFlow<List<String>> = run {
        val userId = uid ?: return@run MutableStateFlow(emptyList())
        HabitRepository.logsFlow(userId, habitId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    var isLoading by mutableStateOf(false)
    var errorMsg  by mutableStateOf<String?>(null)

    fun markDone() {
        viewModelScope.launch {
            isLoading = true
            HabitRepository.markDone(habitId).onFailure { errorMsg = it.message }
            isLoading = false
        }
    }

    fun useStreakFreeze() {
        viewModelScope.launch { HabitRepository.useStreakFreeze(habitId).onFailure { errorMsg = it.message } }
    }
}

class HabitCardVMFactory(private val id: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HabitCardViewModel(id) as T
}

// ─────────────────────────────────────────────────────────────────────────────
//  APP LOGO COMPOSABLE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppLogo(size: Dp = 90.dp) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.27f))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(APP_LOGO_URL)
                .crossfade(true)
                .build(),
            contentDescription = "Habit Tracker Logo",
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size * 0.27f))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  LOGIN SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(onLogin: () -> Unit, goSignup: () -> Unit) {
    val vm: AuthViewModel = viewModel()
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotPassword by remember { mutableStateOf(false) }
    var showResendPanel    by remember { mutableStateOf(false) }
    val state = vm.uiState

    var quoteIndex by remember { mutableStateOf((motivationalQuotes.indices).random()) }
    LaunchedEffect(Unit) {
        while (true) { delay(6000); quoteIndex = (quoteIndex + 1) % motivationalQuotes.size }
    }

    LaunchedEffect(state) { showResendPanel = state is AuthUiState.UnverifiedEmail }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppLogo(size = 90.dp)

            Text(
                "Habit Tracker",
                style      = MaterialTheme.typography.headlineLarge,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Build streaks. Level up.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            AnimatedContent(
                targetState = quoteIndex, label = "quote",
                transitionSpec = {
                    (fadeIn(tween(600)) + slideInVertically { it / 4 }) togetherWith
                            (fadeOut(tween(400)) + slideOutVertically { -it / 4 })
                }
            ) { idx ->
                val (qt, qa) = motivationalQuotes[idx]
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.10f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("💬", fontSize = 16.sp)
                            Text(
                                qt,
                                style      = MaterialTheme.typography.bodySmall,
                                color      = MaterialTheme.colorScheme.onBackground.copy(0.85f),
                                fontStyle  = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier   = Modifier.weight(1f)
                            )
                        }
                        Text(
                            qa,
                            style      = MaterialTheme.typography.labelSmall,
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it; vm.clearError() },
                label = { Text("Email/Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it; vm.clearError() },
                label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(), singleLine = true
            )

            when (state) {
                is AuthUiState.Error -> Text(
                    state.msg, color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp, modifier = Modifier.fillMaxWidth()
                )
                is AuthUiState.PasswordResetSent -> Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4332))
                ) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✅", fontSize = 16.sp)
                        Text("Reset link sent to ${state.email}", color = Color(0xFF52B788), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    }
                }
                is AuthUiState.UnverifiedEmail -> Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2B00))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📧", fontSize = 16.sp)
                            Text(
                                "Email not verified. Check your inbox for ${state.email}.",
                                color = Color(0xFFFFB347), fontSize = 13.sp, modifier = Modifier.weight(1f)
                            )
                        }
                        TextButton(onClick = { showResendPanel = true }, modifier = Modifier.align(Alignment.End)) {
                            Text("Resend Email", fontSize = 12.sp, color = Color(0xFFFFB347))
                        }
                    }
                }
                else -> {}
            }

            Button(
                onClick  = { vm.login(email, password, onLogin) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = state !is AuthUiState.Loading,
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (state is AuthUiState.Loading)
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { showForgotPassword = true }) {
                    Text("Forgot Password?", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = goSignup) {
                    Text("Sign up", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            // ── ANIMATED "MADE WITH LOVE" FOOTER ─────────────────────────────
            MadeWithLoveFooter()
        }
    }

    if (showForgotPassword)
        ForgotPasswordSheet(prefillEmail = email, onDismiss = { showForgotPassword = false }, vm = vm)
    if (showResendPanel) {
        val unverifiedEmail = (state as? AuthUiState.UnverifiedEmail)?.email
            ?: (state as? AuthUiState.VerificationSent)?.email ?: email
        ResendVerificationSheet(email = unverifiedEmail, onDismiss = { showResendPanel = false }, vm = vm)
    }
}

@Composable
fun ForgotPasswordSheet(prefillEmail: String, onDismiss: () -> Unit, vm: AuthViewModel) {
    var resetEmail   by remember { mutableStateOf(prefillEmail) }
    var localMessage by remember { mutableStateOf<String?>(null) }
    val state = vm.uiState
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("🔑 Forgot Password", fontWeight = FontWeight.Bold)
                Text("We'll send a reset link to your inbox.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = resetEmail, onValueChange = { resetEmail = it; localMessage = null },
                    label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    )
                )
                localMessage?.let { msg ->
                    val isSuccess = msg.contains("sent", ignoreCase = true)
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) Color(0xFF1B4332) else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text     = msg, modifier = Modifier.padding(10.dp), fontSize = 13.sp,
                            color    = if (isSuccess) Color(0xFF52B788) else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { vm.resetPassword(resetEmail) { result -> localMessage = result } }, enabled = state !is AuthUiState.Loading) {
                if (state is AuthUiState.Loading)
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Send Reset Link")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ResendVerificationSheet(email: String, onDismiss: () -> Unit, vm: AuthViewModel) {
    var password by remember { mutableStateOf("") }
    val state = vm.uiState
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("📧 Resend Verification Email", fontWeight = FontWeight.Bold)
                Text("Enter your password to send a new link to $email", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(), singleLine = true
                )
                when (state) {
                    is AuthUiState.Error -> Text(state.msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    is AuthUiState.VerificationSent -> Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4332))
                    ) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✅", fontSize = 14.sp)
                            Text("Verification email sent! Check your inbox.", color = Color(0xFF52B788), fontSize = 13.sp)
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { vm.resendVerificationEmail(email, password) },
                enabled  = state !is AuthUiState.Loading && state !is AuthUiState.VerificationSent
            ) {
                if (state is AuthUiState.Loading)
                    CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Resend Email")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (state is AuthUiState.VerificationSent) "Close" else "Cancel")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  SIGNUP SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignupScreen(onDone: () -> Unit) {
    val vm: AuthViewModel = viewModel()
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val state = vm.uiState

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppLogo(size = 80.dp)

            Text("Create Account", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)

            if (state is AuthUiState.VerificationSent) {
                VerificationSentCard(email = state.email, onGoLogin = onDone)
            } else {
                OutlinedTextField(name, { name = it; vm.clearError() }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(email, { email = it; vm.clearError() }, label = { Text("Email — used as your username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    password, { password = it; vm.clearError() }, label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true
                )
                if (state is AuthUiState.Error)
                    Text(state.msg, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Button(
                    onClick  = { vm.signup(name, email, password) {} },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled  = state !is AuthUiState.Loading
                ) {
                    if (state is AuthUiState.Loading)
                        CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text("Sign Up", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDone) { Text("Back to Login") }
            }

            // ── ANIMATED "MADE WITH LOVE" FOOTER ─────────────────────────────
            MadeWithLoveFooter()
        }
    }
}

@Composable
fun VerificationSentCard(email: String, onGoLogin: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("📬", fontSize = 48.sp)
            Text("Verify your email", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "We sent a verification link to:\n$email\n\nOpen the email and tap the link, then come back here to log in.",
                style       = MaterialTheme.typography.bodyMedium,
                color       = MaterialTheme.colorScheme.onSurface.copy(0.7f),
                textAlign   = TextAlign.Center
            )
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💡", fontSize = 14.sp)
                    Text("Don't see it? Check your spam folder.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                }
            }
            Button(onClick = onGoLogin, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Go to Login", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DASHBOARD SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(vm: DashboardViewModel, themeMode: ThemeMode, onCycleTheme: () -> Unit, onLogout: () -> Unit) {
    val habits  by vm.habits.collectAsState()
    val profile by vm.profile.collectAsState()
    val context = LocalContext.current

    var showAddDialog  by remember { mutableStateOf(false) }
    var editingHabit   by remember { mutableStateOf<Habit?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    val categories = listOf("All", "General", "Health", "Study", "Fitness", "Mindfulness", "Other")
    val filtered   = if (selectedFilter == "All") habits else habits.filter { it.category == selectedFilter }

    val totalXp   = profile?.totalXp ?: habits.sumOf { it.xp }
    val level     = levelFromXp(totalXp)
    val xpInLevel = totalXp % 100

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        "Hey ${profile?.name?.takeIf { it.isNotBlank() } ?: "there"} 👋",
                        style      = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text("Level $level · $totalXp XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onCycleTheme) {
                    Icon(themeMode.icon(), "Theme: ${themeMode.label()}", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Level $level", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                Text("$xpInLevel / 100 XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            LinearProgressIndicator(
                progress   = { xpInLevel / 100f },
                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        if (habits.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickStatChip("Total", "${habits.size}",                             "📋", Modifier.weight(1f))
                QuickStatChip("Best",  "${habits.maxOfOrNull { it.streak } ?: 0}🔥", "🏆", Modifier.weight(1f))
                QuickStatChip("XP",    "$totalXp⚡",                                  "💰", Modifier.weight(1f))
            }
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { cat ->
                FilterChip(selected = selectedFilter == cat, onClick = { selectedFilter = cat }, label = { Text(cat, fontSize = 12.sp) })
            }
        }

        vm.errorMsg?.let { err ->
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(err, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.errorMsg = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (habits.isEmpty()) "👀" else "🔍", fontSize = 48.sp)
                    Text(
                        if (habits.isEmpty()) "No habits yet!" else "No $selectedFilter habits",
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (habits.isEmpty()) "Tap + to add your first habit" else "Try a different category",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { habit ->
                    SwipeToDeleteHabitCard(habit, onDelete = { vm.deleteHabit(habit.id) }, onEdit = { editingHabit = habit }, context)
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.CenterEnd) {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add Habit", tint = Color.White)
            }
        }
    }

    if (showAddDialog)
        HabitFormDialog(null, context, onSave = { vm.addHabit(it); showAddDialog = false }, onDismiss = { showAddDialog = false })
    editingHabit?.let { h ->
        HabitFormDialog(h, context, onSave = { vm.updateHabit(it); editingHabit = null }, onDismiss = { editingHabit = null })
    }
}

@Composable
fun QuickStatChip(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STATS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsScreen(vm: DashboardViewModel) {
    val habits  by vm.habits.collectAsState()
    val profile by vm.profile.collectAsState()
    val totalXp = profile?.totalXp ?: habits.sumOf { it.xp }
    val level   = levelFromXp(totalXp)

    if (habits.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📊", fontSize = 48.sp)
                Text("No stats yet", style = MaterialTheme.typography.titleMedium)
                Text("Add habits and complete them to see stats", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(0.5f), textAlign = TextAlign.Center)
            }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        item {
            val topStreak  = habits.maxOfOrNull { it.streak } ?: 0
            val topLongest = habits.maxOfOrNull { it.longestStreak } ?: 0
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), modifier = Modifier.height(200.dp),
                userScrollEnabled     = false,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                item { StatCard("Total Habits", "${habits.size}", "📋", MaterialTheme.colorScheme.primary) }
                item { StatCard("Total XP",     "${totalXp}⚡",   "💰", Color(0xFFFFD700)) }
                item { StatCard("Best Streak",  "$topStreak 🔥",  "🏅", MaterialTheme.colorScheme.secondary) }
                item { StatCard("Longest Ever", "$topLongest 🔥", "🏆", Color(0xFF44BB44)) }
            }
        }

        item {
            val xpByCategory = habits.groupBy { it.category }
                .mapValues { (_, v) -> v.sumOf { it.xp } }
                .entries.sortedByDescending { it.value }
            if (xpByCategory.isNotEmpty()) {
                val maxXp = xpByCategory.maxOf { it.value }.coerceAtLeast(1)
                SectionCard("XP by Category") {
                    xpByCategory.forEach { (cat, xp) ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, fontSize = 13.sp)
                                Text("$xp XP", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress   = { xp / maxXp.toFloat() },
                                modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color      = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard("Habits by Streak 🔥") {
                habits.sortedByDescending { it.streak }.forEachIndexed { idx, habit ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx + 1}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.width(20.dp), textAlign = TextAlign.End)
                            Text(habit.name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔥 ${habit.streak}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                            Text("⚡ ${habit.xp}",     fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    if (idx < habits.size - 1)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f), thickness = 0.5.dp)
                }
            }
        }

        item {
            val topStreak  = habits.maxOfOrNull { it.streak } ?: 0
            val topLongest = habits.maxOfOrNull { it.longestStreak } ?: 0
            val allBadges = buildList {
                if (habits.isNotEmpty())  add("🌱" to "Started tracking")
                if (habits.size >= 3)     add("📚" to "3+ habits")
                if (habits.size >= 5)     add("🎯" to "5+ habits")
                if (topStreak >= 3)       add("🔥" to "3-day streak")
                if (topStreak >= 7)       add("💪" to "Week warrior")
                if (topStreak >= 14)      add("🌟" to "2-week legend")
                if (topStreak >= 30)      add("👑" to "30-day king")
                if (topLongest >= 60)     add("🏆" to "60-day champ")
                if (totalXp >= 500)       add("⚡" to "500 XP club")
                if (totalXp >= 1000)      add("💎" to "1000 XP elite")
                if (level >= 5)           add("🚀" to "Level 5+")
                if (level >= 10)          add("🌌" to "Level 10+")
            }
            if (allBadges.isNotEmpty()) {
                SectionCard("Achievements") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(((allBadges.size / 3 + 1) * 80).dp.coerceAtMost(400.dp)),
                        userScrollEnabled     = false,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allBadges) { (emoji, label) ->
                            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emoji, fontSize = 24.sp)
                                    Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: String, accentColor: Color) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(icon, fontSize = 22.sp)
            Column {
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = accentColor, maxLines = 1)
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  PROFILE SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(vm: DashboardViewModel, themeMode: ThemeMode, onCycleTheme: () -> Unit, onLogout: () -> Unit) {
    val profile by vm.profile.collectAsState()
    val habits  by vm.habits.collectAsState()
    val context = LocalContext.current

    val totalXp   = profile?.totalXp ?: habits.sumOf { it.xp }
    val level     = levelFromXp(totalXp)
    val xpInLevel = totalXp % 100
    val name      = profile?.name?.takeIf { it.isNotBlank() } ?: "User"
    val email     = profile?.email ?: ""

    var snackMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snackMsg) { if (snackMsg != null) { delay(2500); snackMsg = null } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.fillMaxWidth().height(80.dp)
                                .background(brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(0.55f))))
                        )
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .offset(y = (-32).dp)
                                    .size(64.dp)
                                    .background(brush = Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, RoyalBlueDark)), shape = CircleShape)
                                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.first().uppercaseChar().toString(), fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-20).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            if (email.isNotBlank())
                                Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(0.15f)) {
                                    Text("Level $level", modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFD700).copy(0.15f)) {
                                    Text("$totalXp XP ⚡", modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), color = Color(0xFFFFAA00), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Column(Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Lv $level",         fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                                    Text("$xpInLevel / 100 XP", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    Text("Lv ${level + 1}",   fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress   = { xpInLevel / 100f },
                                    modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color      = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Text("${100 - xpInLevel} XP to Level ${level + 1}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            if (habits.isNotEmpty()) {
                item {
                    Text("Your Numbers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2), modifier = Modifier.height(240.dp),
                        userScrollEnabled     = false,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        item { StatCard("Habits",        "${habits.size}",                                      "📋", MaterialTheme.colorScheme.primary) }
                        item { StatCard("Best Streak",   "${habits.maxOfOrNull { it.streak } ?: 0} 🔥",        "🏅", MaterialTheme.colorScheme.secondary) }
                        item { StatCard("Longest Ever",  "${habits.maxOfOrNull { it.longestStreak } ?: 0} 🔥", "🏆", Color(0xFF44BB44)) }
                        item { StatCard("High Priority", "${habits.count { it.priority == "High" }}",           "🚨", Color(0xFFFF5252)) }
                    }
                }
            }

            item {
                Text("⏰ Scheduled Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (habits.isEmpty()) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsNone, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f), modifier = Modifier.size(24.dp))
                                Text("No habits yet — add some to schedule reminders.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                        } else {
                            habits.forEachIndexed { idx, habit ->
                                val timeStr = "${habit.reminderHour.toString().padStart(2, '0')}:${habit.reminderMinute.toString().padStart(2, '0')}"
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                        Column {
                                            Text(habit.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Daily at $timeStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scheduleHabitReminder(context, habit.name, habit.reminderHour, habit.reminderMinute, habit.name.hashCode())
                                            snackMsg = "✅ Reminder set for \"${habit.name}\" at $timeStr"
                                        },
                                        modifier       = Modifier.height(34.dp),
                                        shape          = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        border         = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.5f))
                                    ) {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Set", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (idx < habits.size - 1)
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.25f), thickness = 0.5.dp)
                            }

                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    habits.forEach { h -> scheduleHabitReminder(context, h.name, h.reminderHour, h.reminderMinute, h.name.hashCode()) }
                                    snackMsg = "✅ All ${habits.size} reminders rescheduled!"
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape    = RoundedCornerShape(12.dp),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(0.12f),
                                    contentColor   = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.NotificationAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Reschedule All Reminders", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onCycleTheme() }.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Icon(themeMode.icon(), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Appearance", fontSize = 15.sp)
                                Text("${themeMode.label()} mode · tap to cycle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            }
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(0.12f)) {
                            Text(themeMode.label(), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Reminders", fontSize = 15.sp)
                            Text("Configured per habit above • ${habits.size} active", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        vm.logout {
                            onLogout()

                            Toast.makeText(context, "See you soon 👋 Logout successful", Toast.LENGTH_SHORT).show()

                            (context as? Activity)?.finishAffinity()

                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, 1200) // give user time to SEE the message
                        }

                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(0.12f),
                        contentColor   = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // ── ANIMATED "MADE WITH LOVE" FOOTER ─────────────────────────────
            item { MadeWithLoveFooter() }
        }

        snackMsg?.let { msg ->
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp, start = 16.dp, end = 16.dp)) {
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HABIT FORM DIALOG
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HabitFormDialog(initial: Habit?, context: Context, onSave: (Habit) -> Unit, onDismiss: () -> Unit) {
    val categories = listOf("General", "Health", "Study", "Fitness", "Mindfulness", "Other")
    val priorities = listOf("High", "Medium", "Low")

    var name          by remember { mutableStateOf(initial?.name ?: "") }
    var checklistText by remember { mutableStateOf(initial?.checklist?.joinToString(", ") ?: "") }
    var category      by remember { mutableStateOf(initial?.category ?: "General") }
    var priority      by remember { mutableStateOf(initial?.priority ?: "Medium") }
    var notes         by remember { mutableStateOf(initial?.notes ?: "") }
    var reminderHour  by remember { mutableStateOf(initial?.reminderHour ?: 8) }
    var reminderMin   by remember { mutableStateOf(initial?.reminderMinute ?: 0) }
    var nameError     by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Habit ✨" else "Edit Habit ✏️", fontWeight = FontWeight.Bold) },
        text  = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label = { Text("Habit Name *") }, modifier = Modifier.fillMaxWidth(),
                    singleLine    = true, isError = nameError,
                    supportingText = if (nameError) {{ Text("Name is required") }} else null
                )
                OutlinedTextField(
                    checklistText, { checklistText = it },
                    label = { Text("Checklist (comma separated)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), maxLines = 3
                )

                Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat, fontSize = 11.sp) })
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    priorities.forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p, fontSize = 11.sp) })
                    }
                }

                Text(
                    "Reminder: ${reminderHour.toString().padStart(2, '0')}:${reminderMin.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("H", fontSize = 12.sp)
                    Slider(value = reminderHour.toFloat(), onValueChange = { reminderHour = it.toInt() }, valueRange = 0f..23f, steps = 22, modifier = Modifier.weight(1f))
                    Text("M", fontSize = 12.sp)
                    Slider(value = reminderMin.toFloat(), onValueChange = { reminderMin = it.toInt() }, valueRange = 0f..55f, steps = 10, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) { nameError = true; return@Button }
                val checklist = checklistText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val habit = Habit(
                    id            = initial?.id ?: "", name = name.trim(), checklist = checklist,
                    category      = category, priority = priority, notes = notes,
                    reminderHour  = reminderHour, reminderMinute = reminderMin,
                    streak        = initial?.streak ?: 0, longestStreak = initial?.longestStreak ?: 0,
                    xp            = initial?.xp ?: 0
                )
                scheduleHabitReminder(context, name, reminderHour, reminderMin, name.hashCode())
                onSave(habit)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  SWIPE TO DELETE + HABIT CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SwipeToDeleteHabitCard(habit: Habit, onDelete: () -> Unit, onEdit: () -> Unit, context: Context) {
    var offsetX   by remember { mutableStateOf(0f) }
    val threshold = 150f
    Box(Modifier.fillMaxWidth().pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragEnd        = { if (offsetX <= -threshold) onDelete(); offsetX = 0f },
            onHorizontalDrag = { _, delta -> offsetX = (offsetX + delta).coerceIn(-threshold, 0f) }
        )
    }) {
        if (offsetX < -20f) {
            Box(
                Modifier.matchParentSize().background(MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.White)
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(Modifier.offset(x = offsetX.dp)) {
            HabitCard(habit, onEdit, context)
        }
    }
}

@Composable
fun HabitCard(habit: Habit, onEdit: () -> Unit, context: Context) {
    val vm: HabitCardViewModel = viewModel(key = habit.id, factory = HabitCardVMFactory(habit.id))
    val logs     by vm.logs.collectAsState()
    var checked  by remember(habit.id) { mutableStateOf(setOf<String>()) }
    var expanded by remember { mutableStateOf(false) }

    val todayDone = logs.contains(today())
    val priorityColor = when (habit.priority) {
        "High"   -> Color(0xFFFF5252)
        "Medium" -> Color(0xFFFFAA00)
        else     -> Color(0xFF44BB44)
    }
    val allChecklistDone = habit.checklist.isEmpty() || checked.size == habit.checklist.size

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(8.dp).background(priorityColor, CircleShape))
                    Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (todayDone) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF44BB44), modifier = Modifier.size(18.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expand", tint = MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(0.15f)) {
                    Text(habit.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Text("🔥 ${habit.streak}",   fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                Text("⚡ ${habit.xp} XP",    fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                if (habit.streakFreezeCount > 0)
                    Text("🧊 ${habit.streakFreezeCount}", fontSize = 12.sp, color = Color(0xFF5EC8E5))
            }

            if (habit.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(habit.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    if (habit.checklist.isNotEmpty()) {
                        Text("Tasks", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        habit.checklist.forEach { task ->
                            val isChecked = checked.contains(task)
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { checked = if (isChecked) checked - task else checked + task }
                                    .background(if (isChecked) MaterialTheme.colorScheme.primary.copy(0.08f) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isChecked, onCheckedChange = { v -> checked = if (v) checked + task else checked - task })
                                Text(
                                    task, fontSize = 14.sp,
                                    textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                                    color          = MaterialTheme.colorScheme.onSurface.copy(if (isChecked) 0.35f else 1f)
                                )
                            }
                        }
                        if (!allChecklistDone)
                            Text("${checked.size}/${habit.checklist.size} tasks done", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    }

                    WeeklyStats(logs)
                    StreakGraph(logs)
                    CalendarView(logs)
                    BadgeView(habit.streak, habit.longestStreak)

                    vm.errorMsg?.let { err -> Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick  = { vm.markDone() },
                            modifier = Modifier.weight(1f),
                            enabled  = !todayDone && !vm.isLoading && allChecklistDone,
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor         = if (todayDone) Color(0xFF44BB44) else MaterialTheme.colorScheme.primary,
                                disabledContainerColor = if (todayDone) Color(0xFF44BB44).copy(0.5f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            if (vm.isLoading)
                                CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text(
                                when {
                                    todayDone                                          -> "✅ Done!"
                                    !allChecklistDone && habit.checklist.isNotEmpty() -> "Finish tasks first"
                                    else                                               -> "Mark Complete"
                                }, fontWeight = FontWeight.Bold, fontSize = 13.sp
                            )
                        }
                        if (habit.streakFreezeCount > 0) {
                            OutlinedButton(onClick = { vm.useStreakFreeze() }, shape = RoundedCornerShape(12.dp)) {
                                Text("🧊 ${habit.streakFreezeCount}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  STATS COMPONENTS (per-habit inside expanded card)
// ─────────────────────────────────────────────────────────────────────────────

fun getLast7Days(logs: List<String>): List<Boolean> {
    val result = mutableListOf<Boolean>()
    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
        result.add(logs.contains(SDF.format(cal.time)))
    }
    return result
}

@Composable
fun StreakGraph(logs: List<String>) {
    val data      = getLast7Days(logs)
    val days      = listOf("M", "T", "W", "T", "F", "S", "S")
    val dayOffset = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    Column {
        Text("Last 7 Days", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            data.forEachIndexed { i, done ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier.width(28.dp).height(if (done) 48.dp else 16.dp)
                            .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                    Text(days[(dayOffset - 6 + i + 7) % 7], fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.35f))
                }
            }
        }
    }
}

@Composable
fun WeeklyStats(logs: List<String>) {
    var success = 0
    val cal = Calendar.getInstance()
    repeat(7) { if (logs.contains(SDF.format(cal.time))) success++; cal.add(Calendar.DAY_OF_YEAR, -1) }
    val pct = if (success == 0) 0f else success / 7f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Weekly completion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
            Text("$success/7", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress   = { pct },
            modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color      = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun CalendarView(logs: List<String>) {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    Column {
        Text("This Month", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.heightIn(max = 200.dp), userScrollEnabled = false) {
            items(startDayOfWeek) { Box(Modifier.size(28.dp)) }
            items(daysInMonth) { dayIdx ->
                val dayCal  = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, dayIdx + 1) }
                val dateStr = SDF.format(dayCal.time)
                val done    = logs.contains(dateStr)
                val isToday = dayIdx + 1 == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                Box(
                    Modifier.padding(2.dp).size(26.dp).background(
                        when {
                            done    -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primary.copy(0.25f)
                            else    -> MaterialTheme.colorScheme.surfaceVariant
                        }, RoundedCornerShape(4.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${dayIdx + 1}", fontSize = 9.sp, color = if (done) Color.White else MaterialTheme.colorScheme.onSurface.copy(0.55f))
                }
            }
        }
    }
}

@Composable
fun BadgeView(streak: Int, longestStreak: Int) {
    val badges = buildList {
        if (streak >= 3)         add("🔥" to "3-day streak")
        if (streak >= 7)         add("💪" to "Week warrior")
        if (streak >= 14)        add("🌟" to "2-week legend")
        if (streak >= 30)        add("👑" to "30-day king")
        if (longestStreak >= 60) add("🏆" to "60-day champ")
    }
    if (badges.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Badges", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.forEach { (emoji, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center)
                        }
                        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f), textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// =============================================================================
//  FEATURE LIST
// =============================================================================
//
//  AUTH
//    • Email + password sign-up with email-verification gate
//    • Login with email-verified enforcement
//    • Forgot-password flow (Firebase reset email)
//    • Resend-verification-email bottom sheet
//    • Clean logout — session key invalidation via key(sessionKey) forces full
//      ViewModel disposal; no app restart required
//
//  HABIT MANAGEMENT
//    • Create / edit / delete habits (Firestore-backed)
//    • Swipe-left-to-delete gesture with animated red reveal
//    • Habit fields: name, checklist tasks, category, priority, notes
//    • Category filter chips: All / General / Health / Study / Fitness /
//      Mindfulness / Other
//    • Priority colour dot indicator (High=red, Medium=amber, Low=green)
//    • Expandable habit card revealing full detail + analytics
//
//  STREAKS & XP GAMIFICATION
//    • Daily check-in via "Mark Complete" button
//    • Checklist gate — all tasks must be ticked before marking done
//    • Streak counter auto-calculated from sorted Firestore log dates
//    • Longest-ever streak tracking
//    • XP formula: 10 + (streak × 2) per completion
//    • Global level system: level = (totalXP / 100) + 1
//    • Streak-freeze consumable (🧊) to protect streaks
//
//  REMINDERS
//    • Per-habit daily alarm via AlarmManager.setExactAndAllowWhileIdle
//    • Alarms survive Doze mode
//    • Auto-reschedule after device reboot / app update (BootReceiver)
//    • "Set" and "Reschedule All" controls in Profile tab
//    • Android 12+ exact-alarm permission guard
//
//  STATS SCREEN
//    • Overview grid: total habits, total XP, best streak, longest-ever streak
//    • XP-by-category animated bar breakdown
//    • Habits ranked by current streak
//    • Achievement badge gallery (12 milestones)
//
//  PER-HABIT ANALYTICS (inside expanded card)
//    • Last-7-days bar graph
//    • Weekly completion % progress bar
//    • Monthly calendar heatmap
//    • Per-habit streak badge shelf
//
//  PROFILE SCREEN
//    • Avatar initial + gradient banner header
//    • Level badge + XP badge
//    • XP progress bar toward next level
//    • Personal numbers grid
//    • Scheduled-reminders management list
//    • Appearance setting row (theme cycle)
//
//  THEMES
//    • Dark  — deep navy / royal-blue
//    • Light — white / royal-blue
//    • Cream — warm off-white / blue
//    • One-tap cycle button (Dashboard header + Profile settings row)
//
//  UX POLISH
//    • Animated motivational-quote carousel on login (32 quotes, 6 s rotation)
//    • Animated "Made with ❤️" footer — beating heart + shimmering gradient
//      names (MadeWithLoveFooter composable, shown on Login / Signup / Profile)
//    • Fade animated tab & screen transitions
//    • Quick-stat chips (Total / Best / XP) at top of Dashboard
//    • Snackbar confirmations for reminder scheduling
//    • Offline support via Firestore persistence
//
// =============================================================================
//  TECH STACK
// =============================================================================
//
//  Language          Kotlin
//  UI toolkit        Jetpack Compose  (Material 3)
//  Architecture      MVVM — ViewModel + StateFlow + Compose state
//  Auth              Firebase Authentication  (email / password)
//  Database          Cloud Firestore  (real-time listeners via callbackFlow)
//  Local persistence Firestore offline persistence  (setPersistenceEnabled)
//  Image loading     Coil  (AsyncImage)
//  Notifications     AlarmManager (setExactAndAllowWhileIdle) + NotificationCompat
//  Background        BroadcastReceiver  (ReminderReceiver, BootReceiver)
//  Coroutines        kotlinx.coroutines  (viewModelScope, Flow, callbackFlow)
//  Navigation        Custom Compose state machine  (no Jetpack Navigation lib)
//  Build             Gradle  (Kotlin DSL)
//  Min SDK           21   |   Target SDK  34
//
// =============================================================================
//  MADE WITH ❤️  BY  SHASHANKA  &  JIJNASHU
// =============================================================================