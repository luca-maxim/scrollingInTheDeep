package com.interventions.infinite_scrolling

//import com.txusballesteros.bubbles.BubbleLayout
//import com.txusballesteros.bubbles.BubblesManager

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.*
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.*
import android.content.ContentValues.TAG
import android.graphics.PixelFormat
import android.os.*
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.FirebaseFirestore
import com.rvalerio.fgchecker.AppChecker
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*


public class AppCheckerService : AccessibilityService() {
//here
    private val CHANNEL_ID = "id_smi01"
    companion object {
        var shouldStopOldNotification = false
    }
    private val notificationIdStudyEnd = 102
    private var notificationIdWarning = 103

    var contentJSON = JSONObject()
    var screenOn = true;
    var isRelevantApp = false;
    var currentRelevantApp = ""
    var appJSON = JSONObject()
    var iteration: Int = 0;
    var i: Int = 1

    private var timer: Timer? = null
    private var timerCount: Int = 0
    val cooldown = 600
    val maxdur = cooldown + 120

    //content var
    var isRelevantContent = false;
    var currentRelevantContent = ""
    var lastRelevantContent = ""
    var questionnaireDisplayed= false;
    var intervention = 42
    private var reelsSectionEnterTime: Long = 0
    val reelsTimer = 30000


    private var floatingView: View? = null
    private var timerTextView: TextView? = null
    var context = this

    lateinit var mainHandler: Handler

    var d_timer_value=0
    var dtimerHandler: Handler? = null
    private var d_timer: Runnable? = null
    var startDelayTime: Long = 0
    var dTimerStarted=false;
    var delayTimeInSeconds=0L
    var pID= ""
    var package_name= ""
    var startdelaytime_started= false


    var infinte= true
    var timerStarted = false
    var isRelevantAppOpen = false
    var previousValue: String? = "NO IS"
    var timeStampScrollTimerStarted = LocalDateTime.now()
    var scrollingTimer = 900L // change to 10min are secounds now change later
    var OverlayShowed = false
    var isNotificationShown = false
    val packageNameQueue: ArrayDeque<String> = ArrayDeque(10)
    var lastForegroundApp: String? = null
    var OverlayClicked = false
    var AppNameToBeShownInOverlay = ""
    var lastRelevantApp: String? = null
    val myPackageName = "com.interventions.infinite_scrolling"  // Replace with your app's package name
    val relevantApps = setOf("com.twitter.android", "com.google.android.youtube")  // Add relevant package names
    var lastEventTime = 0L
    val debounceTime = 5000  // 1 second
    var isOverlayBeingDismissed = false
    val mainHandlerTimer = Handler(Looper.getMainLooper())

    // close delayTimer
    private var isAppClosing = false
    private val closeDelayMillis = 2000 // 2 seconds
    private var closeTimer: CountDownTimer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var delayJob: Job? = null


    var isRelevantcontentOpen = false
    // This represents the task which will run every 5 sec to check the current app
    private val getAppTask = object : Runnable {
        @RequiresApi(VERSION_CODES.O)
        override fun run() {
            // Send a ping to the server, that the app is still working/user has not closed it
            var delayMillis = 1000;
            if (checkSendAliveTimer()) {
                sendAliveTag()
            }
            Log.e("runfunction", "run")
            getActiveApp(delayMillis)
            mainHandler.postDelayed(this, delayMillis.toLong())
        }
    }



    lateinit var screenReceiver: BroadcastReceiver
    val screenFilter = IntentFilter()


    private fun registerBroadcastReceivers() {
        // Register a broadcast receiver to listen for screen off/on events to stop and start the timer
        // as it does not need to listen when the screen is off. Also stops a running session if the screen
        // is locked
        screenReceiver = object : BroadcastReceiver() {
            @RequiresApi(VERSION_CODES.O)
            override fun onReceive(context: Context?, intent: Intent) {
                when (Objects.requireNonNull(intent.action)) {
                    Intent.ACTION_USER_PRESENT -> {
                        Log.e("SCREEN", Intent.ACTION_SCREEN_ON)
                        screenOn = true;
                        startChecker()
                    }

                    Intent.ACTION_SCREEN_OFF -> {
                        Log.e("SCREEN", Intent.ACTION_SCREEN_OFF)
                        if(OverlayShowed){
                            OverlayShowed = false
                        }
                        screenOn = false
                        isRelevantAppOpen = false
                        stopOverlay()
                         stopDelayTimer()
                       // startQuest_screenoff()
                        if (appJSON.has("appName")) {
                        }
                        mainHandler.removeCallbacks(getAppTask)
                    }


                }
            }
        }
        // User present instead of screen on because this way the timer is only started once the phone is unlocked
        screenFilter.addAction(Intent.ACTION_USER_PRESENT)
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, screenFilter)
    }

    @RequiresApi(VERSION_CODES.O)


    /**
     * Starts the loop which checks the foreground app
     */
    fun startChecker() {
        // todo change timer to observable
        mainHandler.post(getAppTask)
    }

    @RequiresApi(VERSION_CODES.O)
    override fun onDestroy() {
        // As we don't want the service to be destroyed, check the source which wants to destroy the
        // service and if its not from us restart the service again


        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        val quitService = sharedPref.getString("QUIT", "")
        if (quitService == "true") {
            Log.e("DESTROY", "true " + quitService.toString())
            val editor: SharedPreferences.Editor = sharedPref.edit()
            editor.putString("QUIT", "false")
            editor.apply()
            mainHandler.removeCallbacks(getAppTask)
            stopForeground(true);
            unregisterReceiver(screenReceiver)
            sendBroadcast(Intent("YouWillNeverKillMe"))
            stopSelf();
            super.onDestroy()
        } else {
            Log.e("DESTROY", "other " + quitService.toString())
            val intent = Intent(this, AppCheckerService::class.java)
            startForegroundService(intent);
            mainHandler.removeCallbacks(getAppTask)
            stopForeground(true);
            unregisterReceiver(screenReceiver)
            stopSelf();
            super.onDestroy()
        }
    }

    /**
     * Gets the active app and handles it.
     */
    @RequiresApi(VERSION_CODES.O)
    fun getActiveApp(delayMillis: Int) {
        val appChecker = AppChecker()
        val timeRunning: Long
        if (appChecker.getForegroundApp(this) == null) {
            return
        }
        val packageName: String = appChecker.getForegroundApp(this)
        var app_name = getAppName(packageName)

        Log.e("appname", "hallo")
        // Current foreground app is important
        if (packageIsRelevantApp(packageName)) {


            // Not yet a opened app active -> Create app JSON
            if (!isRelevantApp) {
                startAppJSON(packageName);
                iteration = 1
            } else {
                val sharedPref = getSharedPreferences("InfiniteScroll", 0)
                val editor: SharedPreferences.Editor = sharedPref.edit()
                editor.putString("App_Name", app_name)
                editor.apply()

            }
            when (getAppName(packageName)) {
                "Youtube" -> {
                    if (currentRelevantApp == "com.google.android.youtube") {
                        // Same app all good
                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++

                        Log.d("SAME", "YouTube Time Running: $timeRunning Seconds")
                    } else {

                        startQuestionnaire()
                    }
                }
                "com.facebook.android" -> {
                    if (currentRelevantApp == "com.facebook.android" && iteration < maxdur) {
                        // Same app all good
                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }
                        Log.d("SAME", "Facebook Time Running: $timeRunning Seconds")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {

                    }
                }

                "com.facebook.katana" -> {
                    if (currentRelevantApp == "com.facebook.katana" && iteration < maxdur) {
                        // Same app all good
                        /*timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration % 5==0){
                            startVibration()
                        }*/
                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }


                        Log.d("SAME", "Facebook Time Running: $timeRunning Seconds")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {
                        Log.d("Double", "Same app again")
                    }
                }

                "com.reddit.frontpage" -> {
                    if (currentRelevantApp == "com.reddit.frontpage" && iteration < maxdur) {
                        // Same app all good
                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }
                        Log.d("SAME", "Reddit Time Running: $timeRunning Seconds\")")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {

                    }
                }

                "free.reddit.news" -> {
                    if (currentRelevantApp == "free.reddit.news" && iteration < maxdur) {
                        // Same app all good
                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }
                        Log.d("SAME", "Reddit Time Running: $timeRunning Seconds")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {

                    }
                }

                //
                "com.rubenmayayo.reddit" -> {
                    if (currentRelevantApp == "com.rubenmayayo.reddit" && iteration < maxdur) {
                        // Same app all good
                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }
                        Log.d("SAME", "Reddit Time Running: $timeRunning Seconds")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {

                    }
                }

                "com.andrewshu.android.reddit" -> {
                    if (currentRelevantApp == "com.andrewshu.android.reddit" && iteration < maxdur) {
                        // Same app all good
                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }
                        Log.d("SAME", "Reddit Time Running: $timeRunning Seconds")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {

                    }
                }

                "com.instagram.android" -> {
                    if (currentRelevantApp == "com.instagram.android" && iteration < maxdur) {
                        // Same app all good

                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }
                        Log.d("SAME", "Instagram Time Running: $timeRunning Seconds")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {
                        Log.d("Double", "Same app again")
                    }
                }

                "com.zhiliaoapp.musically" -> {
                    if (currentRelevantApp == "com.zhiliaoapp.musically" && iteration < maxdur) {
                        // Same app all good

                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }
                        Log.d("SAME", "TikTok Time Running: $timeRunning Seconds")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {

                    }
                }

                "com.twitter.android" -> {
                    if (currentRelevantApp == "com.twitter.android" && iteration < maxdur) {
                        // Same app all good
                        timeRunning = refreshTimeRunning(delayMillis, iteration)
                        iteration++
                        if (iteration >= cooldown && iteration % 5 == 0) {

                        }
                        Log.d("SAME", "Twitter Time Running: $timeRunning Seconds")
                    } else if (iteration >= cooldown) {
                        startQuestionnaire()
                    } else {

                    }
                }
            }
        } else {

            Log.d(
                "App",
                Date().hours.toString() + ":" + Date().minutes.toString() + ":" + Date().seconds.toString() + ": " + packageName
            )


            // Relevant app was open before
            if (isRelevantApp) {

                if (iteration >= cooldown) {
                    startQuestionnaire()
                } else {
                    reset()
                }
            }
            iteration = 1
        }
    }


    /**
     * Creates the app JSON using the package name and current Date.
     * Also sets the isRelevantApp and currentRelevantApp
     */
    fun startAppJSON(packageName: String) {
        val appName = getAppName(packageName)
        appJSON
            .put("appName", appName)
            .put("startDate", Date())

        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.putString("App_Name", appName)
        editor.apply()

        Log.d("START APP JSON", appJSON.toString())
      //  startFirstQuestionaire()
        isRelevantApp = true;
        currentRelevantApp = packageName;

        if (checkViewHierarchy(getRootInActiveWindow(), 0) != null) {

            var hierachycontent = checkViewHierarchy(getRootInActiveWindow(), 0).toString()
            var content = getContentFromViewHierarchy(hierachycontent)
            if (!isRelevantContent) {
                if (content != null) {
                    startContentJSON(content)
                }
            }
        }
    }





    fun refreshTimeRunning(delayMillis: Int, iteration: Int): Long {
        var timeRunning: Long = (iteration * delayMillis / 1000).toLong()
        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        //T2 & Delta
        var current2 = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MM HH:mm:ss")
        var formatted = current2.format(formatter)
        editor.putString("t2", formatted)
        //editor.putString("Delta", (iteration*(1.4)).toString())
        editor.apply()
        return timeRunning
    }


    fun reset() {
        intervention = 42
        isRelevantApp = false;
        currentRelevantApp = "";
        appJSON = JSONObject()
    }

    /**
     * finishes the app JSON and sends it to the server as a session.
     * Also resets app variables.
     */
    @RequiresApi(VERSION_CODES.O)
    fun startQuestionnaire() {
        Log.d("TAG", "Different app -> Stopped scrolling")
        //addEndDateToAppJSON(appJSON);
        Log.d("FINAL APP JSON", appJSON.toString())

        fun onFailure(response: JSONObject?) {
            // todo cache sessions and try later?
        }
        intervention = 42
        isRelevantApp = false;
        currentRelevantApp = "";
        appJSON = JSONObject()

        // openQuestionaire
        val intent = Intent(this, rhsci1_activity::class.java)
        intent.putExtra("delayTimeinSeconds", delayTimeInSeconds)
        intent.putExtra("sideActivityText", "Did you do anything else besides being on $AppNameToBeShownInOverlay?")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Log.e("ACCESSSERVICE", delayTimeInSeconds.toString())
        Log.e("ifloop", "startActivity")
        startActivity(intent)
        questionnaireDisplayed = true
        stopOverlay()
        stopDelayTimer()
//        Toast.makeText(
//            this,
//            "Questionnaire Opening", Toast.LENGTH_SHORT
//        ).show()

    }

    fun startQuest_screenoff(){
        val intent = Intent(this, rhsci1_activity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        startActivity(intent)

    }
    /**
     * Returns a simplified name using the package name.
     */
    fun getAppName(packageName: String): String {
        if (packageName == "com.instagram.android") {
            return "Instagram"
        } else if (packageName == "com.reddit.frontpage") {
            return "Reddit"
        } else if (packageName == "free.reddit.news") {
            return "Reddit"
        } else if (packageName == "com.andrewshu.android.reddit") {
            return "Reddit"
        } else if (packageName == "com.rubenmayayo.reddit") {
            return "Reddit"
        } else if (packageName == "com.reddit.frontpage") {
            return "Reddit"
        } else if (packageName == "com.facebook.android") {
            return "Facebook"
        } else if (packageName == "com.facebook.katana") {
            return "Facebook"
        } else if (packageName == "com.ninegag.android.app") {
            return "9gag"
        } else if (packageName == "com.zhiliaoapp.musically") {
            return "TikTok"
        } else if (packageName == "com.pinterest") {
            return "pinterest"
        } else if (packageName == "com.twitter.android") {
            return "Twitter"
        } else if (packageName == "com.google.android.youtube") {
            return "YouTube"
        } else if (packageName == "com.interventions.infinite_scrolling") {
            return "InfiniteScape"
        } else {
            return ""
        }
    }

    /**
     * Returns a boolean whether the current app is a relevant app.
     */
    fun packageIsRelevantApp(packageName: String): Boolean {
        return when (packageName) {
            "com.facebook.android" -> {
                true
            }
            "com.google.android.youtube" -> {
                true
            }
            "com.facebook.katana" -> {
                true
            }

            "com.instagram.android" -> {
                true
            }

            "com.reddit.frontpage" -> {
                true
            }

            "free.reddit.news" -> {
                true
            }

            "com.andrewshu.android.reddit" -> {
                true
            }

            "com.rubenmayayo.reddit" -> {
                true
            }

            "com.zhiliaoapp.musically" -> {
                true
            }
            /*"com.ninegag.android.app" -> {
                true
            }*/
            /*"com.pinterest" -> {
                true
            }*/
            "com.google.android.youtube" -> {
                true
            }
            "com.twitter.android" -> {
                true
            }

            else -> {
                false
            }
        }
    }


    /**
     * Sets thesend alive timer every 24 hours.
     */
    @RequiresApi(VERSION_CODES.O)
    fun setSendAliveTimer() {
        val sharedPref: SharedPreferences = this.getSharedPreferences("InfiniteScroll", 0)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        val date = Date()
        date.time = (date.time + 1 * 60 * 60 * 1000)
        editor.putString("SEND_ALIVE_TIMER", date.toString())
        editor.apply()
    }






    /**
     * Checks the current status of the send alive timer. If over send ping to server
     */
    @RequiresApi(VERSION_CODES.O)
    fun checkSendAliveTimer(): Boolean {
        val sharedPref: SharedPreferences = this.getSharedPreferences("InfiniteScroll", 0)
        val sendAliveTimer = sharedPref.getString("SEND_ALIVE_TIMER", "true")
        if (sendAliveTimer == null || sendAliveTimer == "true") {
            return true
        }
        val now = Date()
        val date = Date(sendAliveTimer)
//        return true
        return date < now
    }




    fun scheduleNotification(context: Context){

    }
    /**
     * Checks the end timer of the study. If 7 days are over handle the finish of the study
     */
    @RequiresApi(VERSION_CODES.O)
    fun checkStudyEndTimer(): Boolean {
        val sharedPref: SharedPreferences = this.getSharedPreferences("InfiniteScroll", 0)
        val notificationTimer = sharedPref.getString("STUDY_END_TIMER", "true")
        if (notificationTimer == null || notificationTimer == "true") {
            return false
        }

        val now = Date()
        val date = Date(notificationTimer)
        if (date < now) {
            Log.e("STUDY_OVER", "OVER")
            // todo finish study - end service - some popup - clickworker vs free
            val helper = Helper()
            val completionHistory = JSONObject()
                .put("completionHistory", getUsageStats())
            /*helper.sendRequest(
                this,
                object : VolleyCallBack {
                    override fun onSuccess(response: JSONObject?) {

                    }

                    override fun onFailure(errorResponse: JSONObject?) {
                    }
                },
                "/user/finish",
                "POST",
                completionHistory.toString()
            )*/
      /*      sendEndNotification(
                "The study for infinite scroll is over",
                "Please consider giving us feedback by clicking this notification."
            )*/
            val editor: SharedPreferences.Editor = sharedPref.edit()
            editor.putString("QUIT", "true")
            editor.apply()
            val intent = Intent(this, AppCheckerService::class.java)
            stopService(intent)
        }
        return date < now
    }

    /**
     * Gets and add the usage stats of the android device.
     */
    fun getUsageStats(): String {
        val usageStatsManager =
            this.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

        val cal: Calendar = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -7)
        val queryUsageStats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            System.currentTimeMillis()
        )
        var statsData: String = ""
        for (i in 0..queryUsageStats.size - 1) {
            if (queryUsageStats.get(i).totalTimeInForeground > 0) {
                if (packageIsRelevantApp(queryUsageStats.get(i).packageName)) {
                    statsData =
                        statsData + "Package Name : " + queryUsageStats.get(i).packageName + "\n" +
                                "Last Time Used : " + convertTime(queryUsageStats.get(i).lastTimeUsed) + "\n" +
                                "Total time in foreground : " + (queryUsageStats.get(i).totalTimeInForeground / 1000) + "seconds" + "\n" + "\n"
                }
            }
        }
        return statsData
    }

    /**
     * Converts the time stamp to a usable time object.
     */
    private fun convertTime(lastTimeUsed: Long): String {
        val date = Date(lastTimeUsed)
        val format = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH)
        return format.format(date)
    }



    /**
     * Sends the end notification
     */
 /*   private fun sendEndNotification(title: String, message: String) {
//        setNotificationTimer()

        val intent = Intent(applicationContext, EndNoteActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                1,
                intent,
                PendingIntent.FLAG_ONE_SHOT
            )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationIdStudyEnd, builder.build())
        }
    }*/


    @RequiresApi(VERSION_CODES.O)
    fun sendAliveTag() {
        setSendAliveTimer()
    }

    /**
     * Creates the Channel used for notifications.
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            val name = "Social Media Interventions"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.enableVibration(false) // Disable vibration for this notification channel
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /*
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
*/

    //to check if the permission overlay is still running
    fun Context.drawOverOtherAppsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT < VERSION_CODES.M) {
            true
        } else {
            Settings.canDrawOverlays(this)
        }
    }











    fun startFirstQuestionaire() {

    }

    private fun startContentJSON(content: String) {
        if(appJSON.has("appName")) {
            if (contentJSON.has("contentName")) {
                return
            }
            contentJSON
                .put("contentName", content)
                .put("startDate", Date())

            val sharedPref = getSharedPreferences("InfiniteScroll", 0)
            val editor: SharedPreferences.Editor = sharedPref.edit()
            editor.putString("contentName", content)
            editor.apply()
            Log.e("START CONTENT JSON", contentJSON.toString())
          //  startOverlay()
            isRelevantContent = true;
          //  currentRelevantContent = getContent(content);
        }
    }
    /**
     * Adds the enddate to the appJSON
     */
    fun addEndDateToAppJSON(appJSON: JSONObject) {
        appJSON
            .put("endDate", Date())
    }
    fun finishContentJSON() {
        if( contentJSON.has("contentName")) {
            contentJSON
                .put("endDate", Date())
            addContentToAppJSON()
            isRelevantContent = false;
            lastRelevantContent = contentJSON.getString("contentName")
        //    currentRelevantContent = "";
            Log.e("FINAL Content JSON", contentJSON.toString())
            contentJSON = JSONObject()

        }
        }

    /**
     * Adds the contentJSON to the AppJSON
     */
    fun addContentToAppJSON() {
        var newJSON= JSONArray()

        if(!appJSON.has("contentArray")) {
            newJSON.put(contentJSON)
            appJSON.put("contentArray", newJSON)
        }else                   {
            var oldJSON = appJSON.getJSONArray("contentArray")
            oldJSON.put(contentJSON)
            appJSON.put("contentArray", oldJSON)
        }
    }


    fun isRelevantInstagramContent(content: String): String {

        if (content.contains("Home")) {
            return "IS"
        }
        if (content.contains("Reel")) {
            return "IS"
        }
        if (content.contains("Search and explore")) {
            return "IS"
        }
        if (content.contains("Profile")) {
            return "NO IS"
        }
        if (content.contains("SearchandExploreReels")) {
            return "NO IS"
        }
        if (content.contains("SearchandExplorePosts")) {
            return "NO IS"
        }
        if (content.contains("Camera")) {
            return "NO IS"
        }
        if (content.contains("message")) {
            return "NO IS"
        }
        if (content.contains("@")) {
            return "IS"
        }
        if (content.contains("Messages")) {
            return "NO IS"
        }
        if (content.contains("Active")) {
            return "NO IS"
        }
        return ""
    }

    fun isRelevantTwitterContent(content: String): String {

        if (content.contains("Home")) {
            return "IS"
        }
        if (content.contains("Search and Explore")) {
            return "NO IS"
        }
        if (content.contains("Spaces")) {
            return "NO IS"
        }
        if (content.contains("Trends")) {
            return "NO IS"
        }
        if (content.contains("Tweet")) {
            return "NO IS"
        }
        if (content.contains("Notifications")) {
            return "NO IS"
        }
        if (content.contains("Messages")) {
            return "NO IS"
        }
        if (content.contains("New post")) {
            return "NO IS"
        }
        if (content.contains("timeline")) {
            return "NO IS"
        }
        if (content.contains("Communities")) {
            return "NO IS"
        }
        return ""
    }

    fun isRelevantFacebookContent(content: String): String {

        if (content.contains("Feed, tab 1 of 6")) {
            return "IS"
        }
        if (content.contains("Video, tab 3 of 6")) {
            return "IS"
        }
        if (content.contains("Friends, tab 2 of 6")) {
            return "NO IS"
        }
        if (content.contains("Marketplace, tab 4 of 6")) {
            return "NO IS"
        }
        if (content.contains("Menu, tab 6 of 6")) {
            return "NO IS"
        }
        if (content.contains("Notifications, tab 5 of 6")) {
            return "NO IS"
        }
        return ""
    }

    fun isRelevantYoutubeContent(content: String): String {

        if (content.contains("Home")) {
            return "NO IS"
        }
        if (content.contains("Shorts")) {
             return "IS"
        }
        if (content.contains("Subscriptions")) {
            return "NO IS"
        }
        if (content.contains("Library")) {
            return "NO IS"
        }
        if (content.contains("Create")) {
            return "NO IS"
        }
        return ""
    }


    fun isRelevantTiktokContent(content: String): String {

        if (content.contains("com.zhiliaoapp.musically")) {
            return "IS"
        }

        return "IS"
    }

    fun isRelevantRedditContent(content: String): String {

        if (content.contains("Home")) {
            return "IS"
        }
        if (content.contains("Communities")) {
            return "NO IS"
        }
        if (content.contains("Chat")) {
            return "NO IS"
        }
        if (content.contains("Inbox")) {
            return "NO IS"
        }
        if (content.contains("Create")) {
            return "NO IS"
        }
        return ""
    }
    @RequiresApi(VERSION_CODES.JELLY_BEAN_MR2)
    fun checkViewHierarchy(nodeInfo: AccessibilityNodeInfo, depth: Int): String? {

        if (nodeInfo == null) return null;
        var content = ""


       /* if (currentRelevantApp == "com.facebook.katana") {
            if (nodeInfo.className != null) {
                content += nodeInfo.className.toString()
            }
            if (nodeInfo.contentDescription != null) {

                content += nodeInfo.contentDescription.toString()
            }
        }else {
            if (nodeInfo.viewIdResourceName != null) {
                content += nodeInfo.viewIdResourceName.toString()
            }
            if (nodeInfo.contentDescription != null) {
                content += nodeInfo.contentDescription.toString()
            }
        }*/


        if (nodeInfo.viewIdResourceName != null) {
            content += nodeInfo.viewIdResourceName.toString()
        }
        if (nodeInfo.contentDescription != null) {
            content += nodeInfo.contentDescription.toString()
        }

        try {
            for (i in 0..nodeInfo.getChildCount() - 1) {
                if (nodeInfo.getChild(i) != null) {
                    content += " " + checkViewHierarchy(nodeInfo.getChild(i), depth + 1);
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    //  Log.e("ViewHierarchycontent",content)
        return content
    }

    fun getContentFromViewHierarchy(content: String): String? {

        if (currentRelevantApp == "com.instagram.android") {
            if (content.contains("left_product_tile", false)) {
                return "Shop";
            }
            if (content.contains("action_bar_titleExplore", false)) {
                return "SearchandExplorePosts";
            }
            if (content.contains("action_bar_search_edit_text", false)) {
                return "Search and explore";
            }
            if (content.contains("action_bar_titleReels", false)) {
                return "SearchandExploreReels";
            }
            if (content.contains("action_bar_large_titleReels", false)) {
                return "Reels";
            }
            if (content.contains("row_profile_header_imageview", false)) {
                return "Profile";
            }
            return "Home"
        }

//todo Twitter hinzufügen überlegen wie man unterscheidet

        if (currentRelevantApp == "com.twitter.android") {
            if (content.contains(
                    "com.twitter.android:id/toolbar_settings_notifNotification Settings",
                    false
                )
            ) {
                return "Notifications"
            }
            if (content.contains(
                    "com.twitter.android:id/toolbar_timeline_switchTop Tweets",
                    false
                )
            ) {
                return "Home"
            }
            if (content.contains(
                    "com.twitter.android:id/trends_menu_settingsTrends settings",
                    false
                )
            ) {
                return "Search and Explore"
            }
            if (content.contains(
                    "com.twitter.android:id/search_activity_tabs Top  Latest  People  Photos  Videos",
                    false
                )
            ) {
                return "Trend";
            }
            if (content.contains("com.twitter.android:id/outer_layout_row_view_tweet", false)) {
                return "Tweet";

            }

        }


   /*     if (currentRelevantApp == "com.facebook.katana") {
            if (content.contains(
                    "android.view.ViewFeed",
                    false
                )
            ) {
                return "Feed"
            }
            if (content.contains(
                    "android.view.ViewVideo",
                    false
                )
            ) {
                return "Video"
            }


        }*/

        if(currentRelevantApp == "com.reddit.frontpage"){
            if(content.contains("com.reddit.frontpage:id/home_screen",false)){
                return "Home"
            }
        }


      /*  if(currentRelevantApp =="com.google.android.youtube"){
            if(content.contains("com.google.android.youtube:id/home",false)){
                return "Home"
            }
            if(content.contains("com.google.android.youtube:id/shorts",false)){
                return "Shorts"
            }
            if(content.contains("com.google.android.youtube:id/library",false)){
                return "Library"
            }
        }*/

        Log.e("Viewcontent", content)
        return null
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e(TAG,"Try to connect")
        val info = AccessibilityServiceInfo()
        info.apply {
            // Set the type of events that this service wants to listen to. Others
            // won't be passed to this service.
            //eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_FOCUSED
            eventTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION or AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_SCROLLED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC



            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;


       //     createNotificationChannel()
            registerBroadcastReceivers()

            val notificationIntent = Intent(applicationContext, MainActivity::class.java)
           /* val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0, notificationIntent, 0
            )*/
           /* val notification: Notification = Notification.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("InfinteScape")
                .setContentText("Thank you for Participating in this Study. You can quit anytime by deleting the app")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentIntent(pendingIntent)
                .build()
            // Start the foreground notification to allow constant tracking
            startForeground(1, notification)*/
            if (shouldStopOldNotification) {
            //    stopForeground()
                //cancel here
                shouldStopOldNotification = false
            }
            val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        }

        this.setServiceInfo(info)
        Log.e(TAG,"Service connected")

    }

    fun findMostFrequentPackageName(packageNames: ArrayDeque<String>): String? {
        if (packageNames.isEmpty()) return null

        val packageNameCount = mutableMapOf<String, Int>()

        // Count occurrences of each package name
        for (packageName in packageNames) {
            packageNameCount[packageName] = packageNameCount.getOrDefault(packageName, 0) + 1
        }

        // Find the package name with the highest count
        var mostFrequentPackageName: String? = null
        var maxCount = 0
        for ((name, count) in packageNameCount) {
            if (count > maxCount) {
                maxCount = count
                mostFrequentPackageName = name
            }
        }

        return mostFrequentPackageName
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        var content = event?.contentDescription.toString()
        var packageName = event?.packageName.toString()
        var eventType = event?.eventType
        val className = event?.className.toString()
        var getText = event?.text.toString()
        addPackageNameToArray(packageName)
        var currentAppClosed = getAppName(findMostFrequentPackageName(packageNameQueue).toString())

        mainHandler = Handler(Looper.getMainLooper())
        var eventTypes = AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION or AccessibilityEvent.TYPE_VIEW_CLICKED or AccessibilityEvent.TYPE_VIEW_SCROLLED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        Log.e("detect Close", eventTypes.toString())
        Log.e("PackageName", packageName)
        Log.e("content", content)
        Log.e("eventType", eventType.toString())
        Log.e("booleans", "")
        Log.e("booleans", "Infinite: $infinte")
        Log.e("booleans", "timerStarted: $timerStarted, $packageName")
        Log.e("booleans", "isRelevantAppOpen: $isRelevantAppOpen")
        Log.e("booleans", "OverlayShowed: $OverlayShowed")
        Log.e("booleans", "floatingView: $floatingView")
        Log.e("booleans", "lastForegroundApp: $lastForegroundApp")
        Log.e("booleans", "lastRelevantApp: $lastRelevantApp")
        Log.e("booleans", "packageName: " + getAppName(packageName).toString())
        Log.e("booleans", "currentAppname:$currentAppClosed")
        Log.e("booleans", "className:$className")
        Log.e("booleans", "eventType:$eventType")
        Log.e("booleans", "getText:$getText")
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val ignoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName())
        Log.e("booleans", ignoringBatteryOptimizations.toString())


        if(lastRelevantApp != packageName){
            lastRelevantApp = packageName
        }

        if (event != null) {
            val currentTime = System.currentTimeMillis()
            val currentPackageName = packageName
            // Check if an App was closed and debounce time has passed
            if (event.eventType == 32 && packageName != "com.interventions.infinite_scrolling" && packageName != "com.android.systemui" && packageName != "com.samsung.android.app.cocktailbarservice") {
                Log.e("notificationProblem", "packageName: $packageName eventType : $eventType")
                mainHandler.removeCallbacksAndMessages(null)
                mainHandler.postDelayed({

                    Log.e("ifloop", "in 32 if")
                    if (getAppName(packageName) == "") {
                        Log.e("ifloop", "in 1 if, $OverlayShowed, $OverlayClicked")
                        if (OverlayShowed && !OverlayClicked) {
                            Log.e("ifloop", "in 2 if")
                            isRelevantAppOpen = false
                            Log.e("Relevant App closed", "start Questionnaire")
                           startQuestionnaire()
                            //                        Log.e("delaytest", getAppName(packageName).toString())
                            OverlayShowed = false
                        }
                        if (!isOverlayBeingDismissed) {  // Add this condition
                            Log.e("ifloop", "in 3 if")
                            if (OverlayShowed && lastRelevantApp == packageName && !OverlayClicked) {
                                Log.e("ifloop", "in 4 if")
                                Log.e("Relevant App closed", "start Questionnaire")
                                startQuestionnaire()

                                OverlayShowed = false
                            }

                            // Reset the overlayDismissed flag only if the app being closed is the relevant app
                            if (lastRelevantApp == packageName) {
                                Log.e("ifloop", "in 5 if")
                                OverlayClicked = false

                            }
                        }
                        timerStarted = false
                        isRelevantAppOpen = false
//                        infinte = false
                    }
                }, 200)
            }
// CHeck if keyboard is opend
//            if (getText.toString() != "") {  // Generic check
//                infinte = false
//            }
        }


// Check if the scrollingTimer is expired
        if (timerStarted && isRelevantAppOpen && infinte) {
            Log.e("checkIS", "yes")
            if (isScrollTimerExpired(timeStampScrollTimerStarted) && !OverlayShowed) {
                startOverlay()
            //    Log.e("10minTimer", "StartOverlay")
            }
        }


        when (getAppName(packageName)) {
            "YouTube" -> {
                try {
                    if (!isRelevantAppOpen) {
                        isRelevantAppOpen = true
                        infinte = false
                    }
                    if(!timerStarted  && infinte){
                        Log.e("10minTimer", "Start")
                        timerStarted = true
                        startScrollTimer()
                    }
                    if (isRelevantYoutubeContent(content) != "" && isRelevantYoutubeContent(content) != previousValue) {
                        if ((isRelevantYoutubeContent(content) == "IS" && previousValue == "NO IS") || (isRelevantYoutubeContent(
                                content
                            ) == "NO IS" && previousValue == "IS")
                        ) {
                            if(previousValue == "NO IS") {
                                infinte = true
                            } else if (previousValue == "IS") {
                                infinte = false
                                timerStarted = false
                            }
                            if (infinte) {
                                if (!timerStarted && !isRelevantcontentOpen) {
                                    Log.e("10minTimer", "Start, $packageName")
                                    isRelevantcontentOpen = true;
                                    startScrollTimer()
                                    timerStarted = true
                                }
                            } else {
                                if (timerStarted) {
                                    Log.e("10minTimer", "Stop")
                                    isRelevantcontentOpen = false
                                    timerStarted = false
                                }
                            }
                        }
                        previousValue = isRelevantYoutubeContent(content)
                    }
                } catch (e:Exception){
                    Log.e("CONTENTEXCEPTION", "An exception occurred: ${e.message}")
                }
            }
            "Reddit" -> {

                try {
                    if (!isRelevantAppOpen) {
                        isRelevantAppOpen = true
                        infinte = true
                    }
                    if(!timerStarted  && infinte){
                        Log.e("10minTimer", "Start")
                        timerStarted = true
                        startScrollTimer()
                    }
                    if (isRelevantRedditContent(content) != "" && isRelevantRedditContent(content) != previousValue) {
                        if ((isRelevantRedditContent(content) == "IS" && previousValue == "NO IS") || (isRelevantRedditContent(
                                content
                            ) == "NO IS" && previousValue == "IS")
                        ) {
                            if(previousValue == "NO IS") {
                                infinte = true
                            } else if (previousValue == "IS") {
                                infinte = false
                                timerStarted = false
                            }
                            if (infinte) {
                                if (!timerStarted && !isRelevantcontentOpen) {
                                    isRelevantcontentOpen = true
                                    Log.e("10minTimer", "Start")
                                    startScrollTimer()
                                    timerStarted = true
                                }
                            } else {
                                if (timerStarted) {
                                    Log.e("10minTimer", "Stop")
                                    isRelevantcontentOpen = false
                                    timerStarted = false
                                }
                            }
                        }
                        previousValue = isRelevantRedditContent(content)
                    }
                } catch (e:Exception){
                    Log.e("CONTENTEXCEPTION", "An exception occurred: ${e.message}")
                }
            }

            "Facebook" -> {
                try {
                    if (!isRelevantAppOpen) {
                        isRelevantAppOpen = true
                        infinte = true
                    }
                    if(!timerStarted  && infinte){
                        Log.e("10minTimer", "Start")
                        startScrollTimer()
                        timerStarted = true
                        isRelevantAppOpen = true
                    }
                    if (isRelevantFacebookContent(content) != "" && isRelevantFacebookContent(content) != previousValue) {
                        if ((isRelevantFacebookContent(content) == "IS" && previousValue == "NO IS") || (isRelevantFacebookContent(
                                content
                            ) == "NO IS" && previousValue == "IS")
                        ) {

                            if(previousValue == "NO IS") {
                                infinte = true
                            } else if (previousValue == "IS") {
                                infinte = false
                                timerStarted = false
                            }
                            if (infinte) {
                                if (!timerStarted && !isRelevantcontentOpen) {
                                    isRelevantcontentOpen = true
                                    Log.e("10minTimer", "Start")
                                    startScrollTimer()
                                    timerStarted = true
                                }
                            } else {
                                if (timerStarted) {
                                    isRelevantcontentOpen = false
                                    Log.e("10minTimer", "Stop")
                                    timerStarted = false
                                }
                            }

                        }
                        previousValue = isRelevantFacebookContent(content)
                    }
                } catch (e:Exception){
                    Log.e("CONTENTEXCEPTION", "An exception occurred: ${e.message}")
                }
            }
            "Twitter" -> {
                try {
                    if (!isRelevantAppOpen) {
                        isRelevantAppOpen = true
                        infinte = true
                    }
                    if(!timerStarted  && infinte){
                        Log.e("10minTimer", "Start")
                        startScrollTimer()
                        timerStarted = true
                    }
                    if (isRelevantTwitterContent(content) != "" && isRelevantTwitterContent(content) != previousValue) {
                        if ((isRelevantTwitterContent(content) == "IS" && previousValue == "NO IS") || (isRelevantTwitterContent(
                                content
                            ) == "NO IS" && previousValue == "IS")
                        ) {
                            if(previousValue == "NO IS") {
                                infinte = true
                            } else if (previousValue == "IS") {
                                infinte = false
                                timerStarted = false
                            }
                            if (infinte) {
                                if (!timerStarted && !isRelevantcontentOpen) {
                                    isRelevantcontentOpen = true
                                    Log.e("10minTimer", "Start")
                                    startScrollTimer()
                                    timerStarted = true
                                }
                            } else {
                                if (timerStarted) {
                                    Log.e("10minTimer", "Stop")
                                    isRelevantcontentOpen = false
                                    timerStarted = false
                                }
                            }
                        }
                        previousValue = isRelevantTwitterContent(content)
                    }
                } catch (e:Exception){
                    Log.e("CONTENTEXCEPTION", "An exception occurred: ${e.message}")
                }
            }

            "Instagram" -> {
                try {
                    if(!isRelevantAppOpen){
                        isRelevantAppOpen = true
                        infinte = true
                    }
                    if(!timerStarted && infinte){
                        Log.e("10minTimer", "Start")
                        startScrollTimer()
                        timerStarted = true
                    }

                    if (isRelevantInstagramContent(content) != "" && isRelevantInstagramContent(content) != previousValue) {
                        if((isRelevantInstagramContent(content) == "IS" && previousValue == "NO IS") || (isRelevantInstagramContent(content) == "NO IS" && previousValue == "IS")) {
                            if(previousValue == "NO IS") {
                                infinte = true
                            } else if (previousValue == "IS") {
                                infinte = false
                                timerStarted = false
                            }
                            if(infinte) {
                                if(!timerStarted && !isRelevantcontentOpen) {
                                    isRelevantcontentOpen = true
                                    Log.e("10minTimer", "Start")
                                    startScrollTimer()
                                    timerStarted = true
                                }
                            } else {
                                if(timerStarted) {
                                    Log.e("10minTimer", "Stop")
                                    isRelevantcontentOpen = false
                                    timerStarted = false
                                }
                            }
                        }
                        previousValue = isRelevantInstagramContent(content)
                    }
                } catch (e:Exception){
                        Log.e("CONTENTEXCEPTION", "An exception occurred: ${e.message}")
                }
            }

            "TikTok" -> {

                try {
                    if (!isRelevantAppOpen) {
                        isRelevantAppOpen = true
                        infinte = true
                    }
                    if(!timerStarted  && infinte){
                        Log.e("10minTimer", "Start")
                        startScrollTimer()
                        timerStarted = true
                    }
                    if (isRelevantTiktokContent(content) != "" && isRelevantTiktokContent(
                            content
                        ) != previousValue
                    ) {
                        if ((isRelevantTiktokContent(content) == "IS" && previousValue == "NO IS") || (isRelevantTiktokContent(content) == "NO IS" && previousValue == "IS")) {
                            if(previousValue == "NO IS") {
                                infinte = true
                            } else if (previousValue == "IS") {
                                infinte = true
                            }
                            if (infinte) {
                                if (!timerStarted && !isRelevantcontentOpen) {
                                    isRelevantcontentOpen = true
                                    Log.e("10minTimer", "Start")
                                    startScrollTimer()
                                    timerStarted = true
                                }
                            } else {
                                if (timerStarted) {
                                    Log.e("10minTimer", "Stop")
                                    isRelevantcontentOpen = false
                                    timerStarted = false
                                }
                            }
                        }
                        previousValue = isRelevantTiktokContent(content)
                    }
                } catch (e:Exception){
                    Log.e("CONTENTEXCEPTION", "An exception occurred: ${e.message}")
                }
            }
//            do nothing when InfiniteScape is opened including the overlay
            "InfiniteScape" -> {
                isRelevantAppOpen = false
                timerStarted = false


            }

            else -> {
                isRelevantAppOpen = false
//                timerStarted = false
            }
        }
    }

    fun addPackageNameToArray(packageName: String) {
        // Check if the queue is already at max capacity
        if (packageNameQueue.size == 10) {
            // Remove the oldest package name
            packageNameQueue.removeFirst()
        }
        // Add the new package name to the end of the queue
        packageNameQueue.addLast(packageName)
    }

    private fun startScrollTimer() {
        timeStampScrollTimerStarted = LocalDateTime.now()
        Log.e("startScrollTimer", "started,  $packageName")
    }

    private fun isScrollTimerExpired(otherTime: LocalDateTime): Boolean {
        // Adding 10 minutes to the input LocalDateTime instance
        val tenMinutesLater = otherTime.plus(scrollingTimer, ChronoUnit.SECONDS)

        // Current LocalDateTime instance
        val currentDateTime = LocalDateTime.now()

        // Return true if the current time is after the 10 minutes added time, false otherwise
        return currentDateTime.isAfter(tenMinutesLater)
}


    private fun startTimer() {
       /*
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                timerCount++

                Log.d("Reels Timer", "Time : $timerCount seconds")
            }
        }, 1000, 1000) //  repeat every 1000ms (1 second)
       // startFloatingView()
        //updateTimerUI()
       /* val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_timer_layout, null)
        timerTextView = floatingView?.findViewById(R.id.timerTextView)
        timerTextView?.setText("Timer:" + timerCount)*/
         */
        mainHandler.postDelayed({

            // startFirstQuestionaire()
            startOverlay()
            Log.e("STARTOVERLAY", "OVERLAY INTERVENTION")
        }, 5000)
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                timerCount++

                Log.d("Reels Timer", " $timerCount")
             //   updateTimerUI()
            }
        }, 1000, 1000)

       // startFloatingView()

    }



    private fun updateTimerUI() {
        timerTextView?.text = " $timerCount"


    }

  fun startFloatingView() {

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_timer_layout, null)
        timerTextView = floatingView?.findViewById(R.id.timerTextView)
        updateTimerUI()

        // Add the floating view to the window manager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)
    }
    private fun stopTimer() {
        timer?.cancel()
        timer=null
        timerCount = 0

    }

    @SuppressLint("SuspiciousIndentation")
    fun startOverlay() {
    //    mainHandler.postDelayed({
            Log.e("OVERLAY","START")
        startdelaytime_started= true
        startDelayTimer()
        OverlayShowed = true
        isOverlayBeingDismissed = true
        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.intervention_overlay, null)
        timerTextView = floatingView?.findViewById(R.id.textHeadline)
        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        var appname = sharedPref.getString("App_Name", "App_Name")
        AppNameToBeShownInOverlay = getAppName(findMostFrequentPackageName(packageNameQueue).toString())
       // val appName = getAppName(packageName)

        //timerTextView?.text = " You spent $timerCount  on Instagram"
        timerTextView?.text="Time to close " + AppNameToBeShownInOverlay

            Log.d("OverlayTimerText",  getAppName(findMostFrequentPackageName(packageNameQueue).toString()))

            // Add the floating view to the window manager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
              WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE,
            PixelFormat.TRANSLUCENT

        )


        params.gravity = Gravity.CENTER

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView,params)


    /*    val viewTreeObserver = floatingView?.viewTreeObserver

        viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                // Remove focus from the overlay (set focus to 0)
                floatingView?.clearFocus()
            }
        }*/


        val closeButton = floatingView?.findViewById<Button>(R.id.buttonCloseOverlay)


            /*
        closeButton?.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                windowManager.removeView(floatingView)
                startDelayTimer()
                true
            } else {
                false
            }
        }
*/

            closeButton?.setOnClickListener {
     //   closeButton.isFocusable=true
                windowManager.removeView(floatingView)
                floatingView = null
                infinte = true
                isRelevantApp = true
                lastForegroundApp = ""
                OverlayClicked = true
                isOverlayBeingDismissed = false

            }
         //   closeButton?.isFocusable=false


     //   }, 10000)
    }
    fun startDelayTimer(){
        d_timer_value=0
        d_timer = Runnable {
            d_timer_value++
            println("DelayTime: $d_timer_value")
            dtimerHandler?.postDelayed(d_timer!!, 1000)
        }

        dtimerHandler = Handler()

        startDelayTime = System.currentTimeMillis()
    dtimerHandler?.postDelayed(d_timer!!, 1000)
//    dTimerStarted=true
       /* if(!isRelevantApp){
            stopDelayTimer()
        }*/

        Log.e("DelayTimer", "Started + $")
        startdelaytime_started= true
    }


    fun stopDelayTimer() {
        // startFirstQuestionaire()
        var elapsedTimeInMinutes = 0L
        if (startdelaytime_started == true) {

            val dID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            dtimerHandler?.removeCallbacks(d_timer!!)
            // d_timer_value=0

            Log.d("DelayTimer", "Stopped")

            val currentTime = System.currentTimeMillis()
            delayTimeInSeconds = (currentTime - startDelayTime) / 1000
            elapsedTimeInMinutes = (currentTime - startDelayTime) / (1000 * 60)
            /*else if(startdelaytime_started==false){
            delayTimeInSeconds=0
            elapsedTimeInMinutes=0
        }*/
            Log.d(
                "DelayTimer",
                "User closed app after : $elapsedTimeInMinutes.$delayTimeInSeconds minutes"
            )
            val sharedPref = getSharedPreferences("InfiniteScroll", 0)
            pID = sharedPref.getString("pID", "").toString()

            val formattedTime = String.format(
                "%02d:%02d minutes", elapsedTimeInMinutes / 60, delayTimeInSeconds
            )


            var sdf_1: Calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("MMM d HH:mm:ss", Locale.getDefault())
            var formattedTimestamp = sdf.format(sdf_1.time)


            val editor = sharedPref.edit()
            editor.putString("timestamp", formattedTimestamp)
            editor.putLong("delayTime", delayTimeInSeconds)
            Log.e("CheckEditorDelayTime", delayTimeInSeconds.toString())
            package_name = AppNameToBeShownInOverlay
            Log.e("AppNameToBeShownInOverlay", AppNameToBeShownInOverlay)
            editor.putString("appName", package_name)
            editor.apply()

            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "delayTime" to delayTimeInSeconds,
                "delayTimeFormatted" to formattedTime,
                "Android ID" to dID,
                "pID" to pID,
                "timestamp" to formattedTimestamp,
                "appName" to package_name
            )
            db.collection("delay_time").add(data)
            Log.d("DelayTimer", "DelayTime added $delayTimeInSeconds")
            startdelaytime_started == false

        } else {
            Log.d("DelayTimer", "DelayTime set to zero: $delayTimeInSeconds $elapsedTimeInMinutes")
            /*   val intent=Intent(this ,rhsci_activity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)*/
            /*   if(AccessibilityEvent.WINDOWS_CHANGE_FOCUSED){
                startFirstQuestionaire()
            }*/
            // startQuestionnaire()
        }

        startdelaytime_started == false
        delayTimeInSeconds = 0
        elapsedTimeInMinutes = 0
    }
    fun stopOverlay() {
        floatingView?.let {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeViewImmediate(it)
            floatingView = null
        }
        OverlayShowed = false
        Log.e("OVERLAY","STOP"+ isRelevantcontentOpen.toString())

        /* Handler().postDelayed({
                windowManager2.removeViewImmediate(blinkView)
                blink_initialized = false
            }, 1000)*/

    }

/*
    fun onResumeApp() {
        val prefs = getSharedPreferences("OverlayPrefs", Context.MODE_PRIVATE)
        val overlayActive = prefs.getBoolean("overlayActive", false)

        if (overlayActive) {
            val elapsedTimeMillis = prefs.getLong("elapsedTime", 0)
            // Resume overlay
            startOverlay()
            // Calculate timerCount based on elapsed time
            timerCount = (elapsedTimeMillis / 1000).toInt()
            startTimer()
        }
    }*/
    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

}



