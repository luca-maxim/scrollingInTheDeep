package com.interventions.infinite_scrolling

import android.app.*
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

class TimerService : Service() {
    var startstudy= 0L
    var sevdaytimer = false
    var finalquest_started=false
    var notificationId = 1210
    val CHANNEL_ID = "thanks"
    var sched=true
    companion object {
        private const val NOTIFICATION_ID = 1210
    }
   /* override fun onCreate() {
        super.onCreate()


        // Check if it's time to show the notification and create the notification if needed
        if (scheduleNotification()) {
            Log.e("destroy", "true schedulenotification")

            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
        }
    }*/

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

       /* if (scheduleNotification()) {
            Log.e("destroy", "true schedulenotification")

            val notification = createNotification()
            startForeground(notificationId, notification)
        }*/

        scheduleNotification()

        // Return START_NOT_STICKY to indicate that the service should not be restarted if it's killed
        return START_NOT_STICKY
    }

   private fun scheduleNotification() {
        val sharedPreferences = getSharedPreferences( "InfiniteScroll", 0)
      //   startstudy = sharedPreferences.edit().putLong(startstudy.Calendar.getInstance())
        startstudy= Calendar.getInstance().timeInMillis


        Log.e("STUDYTIMER","TIMERSERVICE")

        val sevenDaystimer = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
//        val sevenDaystimer = 20*1000L //only 30 seconds for debugging
//          val sevenDaystimer = 1 * 24 * 60 * 60 * 1000L // 1 days in milliseconds
//        val sevenDaystimer = 2 * 60 * 60 * 1000L //2hours
        val triggerTimeMillis = sevenDaystimer + startstudy
        val notificationIntent = Intent(this, NotificationReceiver::class.java)


       val pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        Log.e("STUDYTIMER", "TIMERSERVICE AFTER")
        sevdaytimer= true
        sharedPreferences.edit().putBoolean("startstudy", sevdaytimer).apply()
        if(sevdaytimer){
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
          //   finalquest_started=true
            //val sharedPref = getSharedPreferences("InfiniteScroll", 0)
            //sharedPref.edit().putBoolean("finalquest_started", finalquest_started).apply()

        }

    }
  /*  fun scheduleNotification():Boolean{
       val sharedPreferences = getSharedPreferences( "InfiniteScroll", 0)

       Log.e("destroy", "in schedulenotification")

       startstudy= Calendar.getInstance().timeInMillis
     // val currentTime = System.currentTimeMillis()
     // val elapsedTime = currentTime - startstudy
      // Check if the required time (7 days in this case) has passed
    //  val sevenDayMillis = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
        val sevenDayMillis = 30 * 1000L // 7 days in milliseconds
       val temp = 0L
       val trigger = startstudy +temp

       sevdaytimer = true
       sharedPreferences.edit().putBoolean("startstudy", sevdaytimer).apply()
     /*  if(sevdaytimer){

       }*/
      if(trigger==startstudy+sevenDayMillis){
          sched= true
      }else{
          sched=false
      }

       return sched
    }*/
    fun createNotification(): Notification {

        //  var channeld_id ="id_smi01"
        val name = "Last Questionnaire"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val descriptionText = "Click to open the last questionnaire"
        //  AppCheckerService.shouldStopOldNotification = true
//        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //  notificationManager.cancel(1)
        val questionnaireIntent = Intent(applicationContext, FinalQuestionnaire::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, questionnaireIntent, 0 or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Study completed")
            .setContentText("Click to confirm that you have completed the study")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)



        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        with(NotificationManagerCompat.from(applicationContext)) {

            notify(notificationId, builder.build())
        }

        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)


        Log.e("STUDYTIMER", "AFTER"+notificationId.toString()+ CHANNEL_ID.toString())

        finalquest_started=true
        val sharedPref = applicationContext.getSharedPreferences("InfiniteScroll", 0)
        sharedPref.edit().putBoolean("finalquest_started", finalquest_started).apply()

        return builder.build()
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}


