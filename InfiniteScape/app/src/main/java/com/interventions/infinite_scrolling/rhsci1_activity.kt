package com.interventions.infinite_scrolling

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import com.google.firebase.firestore.FirebaseFirestore
import com.interventions.infinite_scrolling.*
import kotlinx.android.synthetic.main.activity_after_rshci.*
import kotlinx.android.synthetic.main.activity_after_rshci1.*
import kotlinx.android.synthetic.main.activity_after_rshci1.view.*
import kotlinx.android.synthetic.main.activity_sess_feeling_after.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.random.Random
import android.widget.TextView

class rhsci1_activity : AppCompatActivity() {
    var timestamp= ""
    var appName= ""
    val question = "How do you feel after the session?"
    private lateinit var cardView1: CardView
    private lateinit var cardView2: CardView
    var check0 =false
    var check1 =false
    var check2 =false
    var check3 = false
    var check4 =false
    var check5=false
    var check6=false
    var check11=false
    var checkKSS = false
    var checkAtHome = false
    var checkSam = false
    var answer6= "false"
    var answer0= "false"
        var pID= ""
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_after_rshci1)
        cardView1 = findViewById(R.id.cardack1)
        cardView2 = findViewById(R.id.cardack2)

        val randomNumber = Random.nextInt(3)
        Log.e("QUESTIONNAIRE", "$randomNumber")
    /*    val delayTimeInSeconds = intent.getLongExtra("delayTimeInSeconds", 0)
        val delayTimeFormatted = intent.getLongExtra("delayTimeFormatted", 0)
          Log.e("VARIABLE ARRIVED in 1", delayTimeInSeconds.toString())*/
        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        Log.e("SharedPref", sharedPref.all.toString())
        val sideActivityText = intent.getStringExtra("sideActivityText")
        val textView = findViewById<TextView>(R.id.sideactivity)
        textView.text = sideActivityText ?: "Did you do anything else besides being on the phone?"


        /* val sit1 = sharedPref.getString("sit1", "")
         val sit2 = sharedPref.getString("sit2", "")

         Log.e("Situation", sit1+sit2)
         rs1.text=sit1
         rs2.text=sit2*/
       // rs3.text=sit3

       // startBtn.isEnabled=false
      //  Log.e("QUESTIONNAIRE", check0.toString() + check1.toString() +check3.toString()+ check4.toString()+ check5.toString() + check6.toString() + check11.toString() )

        startBtn.setOnClickListener {
          checkGroup1()
        //sendData()
        }
        when (randomNumber) {
            0 -> {
                cardView1.visibility = View.VISIBLE
                cardView2.visibility = View.GONE

            }1 -> {
                cardView1.visibility = View.GONE
                cardView2.visibility = View.VISIBLE
            }
            2 -> {
                cardView1.visibility = View.GONE
                cardView2.visibility = View.GONE
            }
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onStart() {
        super.onStart()
     //   val sharedPreferences = getSharedPreferences("InfiniteScroll", 0)
       // val sit1 = sharedPreferences.getString("sit1", "")
        //val sit2 = sharedPreferences.getString("sit2", "")

      //  Log.e("Situation", sit1+sit2)
//        refreshsit(sit1.toString(), sit2.toString())


    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

  /*  fun refreshsit(sit1: String, sit2:String){
        rs1.visibility= View.VISIBLE
        rs2.visibility= View.VISIBLE
      //  rs3.visibility= View.VISIBLE
        rs1.text=sit1
        if(rs1.text==""){
            rs1.visibility= View.GONE
        }
        rs2.text=sit2
        if(rs2.text==""){
            rs2.visibility= View.GONE
        }

    }
*/
    fun gon(){
        val intent = Intent(this, ThankActivity::class.java)
        /*Toast.makeText(
            this,
            "Thank you!", Toast.LENGTH_SHORT
        ).show()*/
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    fun sendQMWarning(){
        Toast.makeText(
            this,
            "At least one question is missing", Toast.LENGTH_SHORT).show()
    }

    fun checkGroup0(){
         answer0 = "false"
         check0 = false
        Log.e("QUESTIONNAIRE", "group0")
        if(cardView1.isVisible==true) {
            Log.e("QUESTIONNAIRE", "cardView1nvisibile-  checkgroup 0")
            if (this.rad54.isChecked) {
                answer0 = "false"
                saveres_ack(answer0, 0)
                check0 = true
            } else if (rad53.isChecked) {
                answer0 = "false"
                saveres_ack(answer0, 0)
                check0 = true

            } else if (rad52.isChecked) {
                answer0 = "false"
                saveres_ack(answer0, 0)
                check0 = true
            } else if (rad51.isChecked) {
                answer0 = "true"
                saveres_ack(answer0, 0)
                check0 = true
            } else if (rad50.isChecked) {
                answer0 = "true"
                saveres_ack(answer0, 0)
                check0 = true
            } else {
                sendQMWarning()
            }
            if(check0){
                if(check0 || check6 || (check6==false && check0==false) ){
                    Log.e("QUESTIONNAIRE", "card1 visible $check11, $check0 , $check6")
                    sendData()
                }
            }
            /*
        }else if(cardView1.isVisible==false){
            Log.e("QUESTIONNAIRE", "cardView1notvisibile- checkgroup 0")
            saveres(answer0, 0)
            check0 = false
            checkGroup6()
        } else if(cardView1.isVisible==false&& cardView2.isVisible==false){
            saveres(answer0, 0)
            saveres(answer6,0)
            Log.e("QUESTIONNAIRE", "both not visible- checkgroup 0")
            check0 = true
            checkGroup10()
        }*/
        }else{


            check0=false
            if(check0 || check6 || (check6==false && check0==false) ){
                Log.e("QUESTIONNAIRE", "both cards not there $check11, $check0 , $check6")
                answer0 = "true"
                saveres_ack(answer0, 0)
                sendData()

            }
        }



    }
    fun checkGroup1(){
        var answer1 = 42
         check1 = false
        if(this.rad01.isChecked){
            answer1 = 5
            saveres(answer1,1 )
            check1=true
        }else if (rad02.isChecked){
            answer1 = 4
            saveres(answer1,1 )
            check1=true

        }else if (rad03.isChecked){
            answer1 = 3
            saveres(answer1,1 )
            check1=true
        }else if (rad04.isChecked){
            answer1 = 2
            saveres(answer1,1 )
            check1=true
        }else if (rad05.isChecked){
            answer1 = 1
            saveres(answer1,1 )
            check1=true
        }else{
           sendQMWarning()
        }

        if (check1){
            checkGroup2()
        }
    }

    fun checkGroup2(){
        var answer2 = 42
         check2 = false
        if(this.rad11.isChecked){
            answer2 = 5
            saveres(answer2,2)
            check2=true
        }else if (rad12.isChecked){
            answer2 = 4
            saveres(answer2,2)
            check2=true

        }else if (rad13.isChecked){
            answer2 = 3
            saveres(answer2,2)
            check2=true
        }else if (rad14.isChecked){
            answer2 = 2
            saveres(answer2,2)
            check2=true
        }else if (rad15.isChecked){
            answer2 = 1
            saveres(answer2,2)
            check2=true
        }else{
            sendQMWarning()
        }

        if (check2){
            checkGroupKSS()
        }
    }

    fun checkGroupKSS(){
        var answerKSS = 100
        checkKSS = false
        if(this.kss1.isChecked){
            answerKSS = 1
            saveres(answerKSS,230)
            checkKSS=true
        }else if (kss2.isChecked){
            answerKSS = 2
            saveres(answerKSS,230)
            checkKSS=true
        }else if (kss3.isChecked){
            answerKSS = 3
            saveres(answerKSS,230)
            checkKSS=true
        }else if (kss4.isChecked){
            answerKSS = 4
            saveres(answerKSS,230)
            checkKSS=true
        }else if (kss5.isChecked){
            answerKSS = 5
            saveres(answerKSS,230)
            checkKSS=true
        }else if (kss6.isChecked){
            answerKSS = 6
            saveres(answerKSS,230)
            checkKSS=true
        }else if (kss7.isChecked){
            answerKSS = 7
            saveres(answerKSS,230)
            checkKSS=true
        }else if (kss8.isChecked){
            answerKSS = 8
            saveres(answerKSS,230)
            checkKSS=true
        }else if (kss9.isChecked){
            answerKSS = 9
            saveres(answerKSS,230)
            checkKSS=true
        }else{
            sendQMWarning()
        }
        if (checkKSS){
            checkGroupAtHome()
        }
    }

    fun checkGroupAtHome(){
        var answerAtHome = ""
        checkAtHome = false
        if(this.atHome_no.isChecked){
            answerAtHome = "false"
            saveres(answerAtHome,233)
            checkAtHome=true
        }else if (atHome_yes.isChecked){
            answerAtHome = "true"
            saveres(answerAtHome,233)
            checkAtHome=true
        }else{
            sendQMWarning()
        }
        if (checkAtHome){
            checkGroupSam()
        }
    }

    fun checkGroupSam(){
        var answerSam = 404
        checkSam = false
        if(this.sam1.isChecked){
            answerSam = 1
            saveres(answerSam,333)
            checkSam=true
        }else if (sam2.isChecked){
            answerSam = 2
            saveres(answerSam,333)
            checkSam = true
        }else if (sam3.isChecked){
            answerSam = 3
            saveres(answerSam,333)
            checkSam = true
        }else if (sam4.isChecked) {
            answerSam = 4
            saveres(answerSam, 333)
            checkSam = true
        }else if (sam5.isChecked) {
            answerSam = 5
            saveres(answerSam, 333)
            checkSam = true
        }else{
            sendQMWarning()
        }
        if (checkSam){
            checkGroup3()
        }
    }

    fun checkGroup3(){
        var answer3 = 42
         check3 = false
        if(this.rad21.isChecked){
            answer3 = 5
            saveres(answer3,3)
            check3=true
        }else if (rad22.isChecked){
            answer3 = 4
            saveres(answer3,3)
            check3=true

        }else if (rad23.isChecked){
            answer3 = 3
            saveres(answer3,3)
            check3=true
        }else if (rad24.isChecked){
            answer3 = 2
            saveres(answer3,3)
            check3=true
        }else if (rad25.isChecked){
            answer3 = 1
            saveres(answer3,3)
            check3=true
        }else{
            sendQMWarning()
        }

        if (check3){
            checkGroup4()
        }
    }

    fun checkGroup4(){
        var answer4 = 42
         check4 = false
        if(this.rad31.isChecked){
            answer4 = 5
            saveres(answer4,4)
            check4=true
        }else if (rad32.isChecked){
            answer4 = 4
            saveres(answer4,4)
            check4=true

        }else if (rad33.isChecked){
            answer4 = 3
            saveres(answer4,4)
            check4=true
        }else if (rad34.isChecked){
            answer4 = 2
            saveres(answer4,4)
            check4=true
        }else if (rad35.isChecked){
            answer4 = 1
            saveres(answer4,4)
            check4=true
        }else{
            sendQMWarning()
        }

        if (check4){
            checkGroup5()
        }
    }

    fun checkGroup5(){
        var answer5 = 42
         check5 = false
        if(this.rad41.isChecked){
            answer5 = 5
            saveres(answer5,5)
            check5 = true
        }else if (rad42.isChecked){
            answer5 = 4
            saveres(answer5,5)
            check5=true

        }else if (rad43.isChecked){
            answer5 = 3
            saveres(answer5,5)
            check5=true
        }else if (rad44.isChecked){
            answer5 = 2
            saveres(answer5,5)
            check5=true
        }else if (rad45.isChecked){
            answer5 = 1
            saveres(answer5,5)
            check5=true
        }else{
            sendQMWarning()
        }
/*
        if (check5){
            if(cardView1.isVisible==true) {
                Log.e("QUESTIONNAIRE", "cardView1visibile-  checkgroup 5")
                checkGroup0()
            }else if(cardView2.isVisible==true){
                Log.e("QUESTIONNAIRE", "cardView1notvisibile-  checkgroup 5")
                checkGroup6()
            }
        }*/
        if(check5){
            checkGroup9()
        }
    }

    fun checkGroup6(){
         answer0 = "false"
         check6 = false
        if(cardView2.isVisible==true) {
            Log.e("QUESTIONNAIRE", "cardView2visibile- checkgroup 6")

            if (this.radack2_1.isChecked) {
                answer0 = "false"
               saveres_ack(answer0, 6)

                check6 = true
            } else if (radack2_2.isChecked) {

                answer0 = "false"
                saveres_ack(answer0, 6)
                check6 = true

            } else if (radack2_3.isChecked) {
                answer0 = "false"
                saveres_ack(answer0, 6)
                check6 = true
            } else if (radack2_4.isChecked) {
                answer0 = "true"
                saveres_ack(answer0, 6)

                check6 = true
            } else if (radack2_5.isChecked) {
                answer0 = "true"
                saveres_ack(answer0, 6)
                check6 = true
            } else {
                sendQMWarning()
            }
            if(check6){
                if(check0 || check6 || (check6==false && check0==false) ){
                    Log.e("QUESTIONNAIRE", "card2 visible $check11, $check0 , $check6")
                    sendData()
                }
            }
        }else{
            check6=false
            checkGroup0()
        }



        }


    fun checkGroup9(){
        var answer9 = ""
        var check9 = false




        if(rs1.isChecked){
            answer9 = "-3"
            saveres(answer9,9)
            check9=true
        }else if (rs2.isChecked){
            answer9 = "-2"
            saveres(answer9,9)
            check9=true
        }else if (rs3.isChecked){
            answer9 = "-1"
            saveres(answer9,9)
            check9=true
        }else if (rs4.isChecked){
            answer9 = "0"
            saveres(answer9,9)
            check9=true
        }else if (rs5.isChecked){
            answer9 = "1"
            saveres(answer9,9)
            check9=true
        }else if (rs6.isChecked){
            answer9 = "2"
            saveres(answer9,9)
            check9=true
        }else if (rs7.isChecked){
            answer9 = "3"
            saveres(answer9,9)
            check9=true
        }else{
            sendQMWarning()
        }

        if (check9){
            checkGroup10()
        }
    }

    fun checkGroup10(){
        var answer10 = ""
        var check10 = false


        if(this.sideactivity_no.isChecked){
            answer10 = sideactivity_no.text.toString()
            saveres(answer10,10)
            check10=true
        }else if (sideactivity_yes.isChecked){
            answer10 = sideactivity_yes.text.toString()
            saveres(answer10,10)
            check10=true

        }else{
            sendQMWarning()
        }

        if (check10){
            checkGroup11()
        }
    }


    fun checkGroup11(){
        var answer11 = ""
         check11 = false
        if(alone.isChecked){
            answer11 = "alone"
            saveres(answer11,11)
            check11=true
        }else if (friends.isChecked){
            answer11 = "friends"
            saveres(answer11,11)
            check11=true
        }else if (strangers.isChecked){
            answer11 = "strangers"
            saveres(answer11,11)
            check11=true
        }else{
            sendQMWarning()
        }
        if (check11){
            checkGroup6()
        }

    }
    fun saveres(answer1:Int, spot:Int) {
        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        if (spot == 1) {
            editor.putInt("RSHCI1", answer1)
            editor.apply()
        } else if (spot == 2) {
            editor.putInt("RSHCI2", answer1)
            editor.apply()
        } else if (spot == 3) {
            editor.putInt("RSHCI3", answer1)
            editor.apply()
        } else if (spot == 4) {
            editor.putInt("RSHCI4", answer1)
            editor.apply()
        } else if (spot == 5) {
            editor.putInt("RSHCI5", answer1)
            editor.apply()
        }else if (spot==230){
            editor.putInt("kss", answer1)
            editor.apply()
        }else if (spot==333){
            editor.putInt("sam", answer1)
            editor.apply()
        }else{
            Toast.makeText(this,"Problem saving your Answer",Toast.LENGTH_SHORT)
        }

    }

    fun saveres(answer1:String, spot:Int){
        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        if (spot==11){
            editor.putString("situation", answer1)
            editor.apply()
        }else if (spot==10){
            editor.putString("sideActivity", answer1)
            editor.apply()
        }else if (spot==9){
            editor.putString("currentActivity", answer1)
            editor.apply()
        }else if (spot==233){
            editor.putString("atHome", answer1)
            editor.apply()
        }
        else{
            Toast.makeText(this,"Problem saving your Answer",Toast.LENGTH_SHORT)
        }

    }
    fun saveres_ack(answer1:String, spot:Int){
        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        if (spot==0 || spot==6){
            editor.putString("ack", answer1)
            editor.apply()
        }else{
            Toast.makeText(this,"Problem saving your Answer",Toast.LENGTH_SHORT)
        }

    }

    data class InterventionAnswers(
        val appName : String,
        val situation: String,
        val sideActivity: String,
        val currAct: String,
        val rshci1: Int,
        val rshci2: Int,
        val rshci3: Int,
        val rshci4: Int,
        val rshci5: Int,
        val ack: String,
        val kss: Int,
        val atHome: String,
        val sam: Int,
        val AndroidID: String,
        val delayTimeInSeconds: Long,
        val pid: String,
        val timestamp : String


    )
    fun sendData(){
      Log.e("QUESTIONNAIRE", " AT SENDATA : $check11 , $check6, $check0")


     //   val delayTimeInSeconds = intent.getLongExtra("delayTimeInSeconds", 0)
       // val delayTimeFormatted = intent.getLongExtra("delayTimeFormatted", 0)
        val sharedPref = getSharedPreferences("InfiniteScroll", 0)
     //    var pid = sharedPref.getString("pID", "EMPTY")
        Log.e("PROLIFIC ID ARRIVAL", pID.toString())
        var app_name = sharedPref.getString("App_Name", "EMPTY")
        var intv_name = sharedPref.getString("Intv_Name", "EMPTY")
        var t1 = sharedPref.getString("t1", "EMPTY")
        var t2 = sharedPref.getString("t2", "EMPTY")
        //var delta = sharedPref.getString("Delta", "EMPTY")
        var rshci1 = sharedPref.getInt("RSHCI1", 404)
        var rshci2 = sharedPref.getInt("RSHCI2", 404)
        var rshci3 = sharedPref.getInt("RSHCI3", 404)
        var rshci4 = sharedPref.getInt("RSHCI4", 404)
        var rshci5 = sharedPref.getInt("RSHCI5", 404)
        var ack = sharedPref.getString("ack", "")
        var kss = sharedPref.getInt("kss", 404)
        var atHome = sharedPref.getString("atHome", "")
        var sam = sharedPref.getInt("sam", 404)

        var situation = sharedPref.getString("situation", "Default")
//        var location = sharedPref.getString("location", "Default")
        var sideActivity = sharedPref.getString("sideActivity", "Default") ?: "Default"
       var currAct = sharedPref.getString("currentActivity", "Default")
     //   var delayTimeInSec = sharedPref.getLong("delayTimeinSeconds", delayTimeInSeconds)

        val dID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        timestamp = sharedPref.getString("timestamp", "").toString()

        Log.e("Shared Pref", sharedPref.all.toString())

        val db = FirebaseFirestore.getInstance()
        Log.e("SharedPref", sharedPref.toString())
        appName = sharedPref.getString("appName", "").toString()
        pID = sharedPref.getString("pID", "").toString()

        var delayTimeInSeconds = sharedPref.getLong("delayTime", 404L)

        val interventionAnswers = rhsci1_activity.InterventionAnswers(appName,situation.toString(), sideActivity.toString(), currAct.toString(), rshci1, rshci2, rshci3, rshci4, rshci5, ack.toString(), kss, atHome.toString(), sam, dID, delayTimeInSeconds, pID, timestamp)
        db.collection("intervention_answers")
            .add(interventionAnswers)
            .addOnSuccessListener { documentReference -> Log.d("Firestore", "DocumentSnapshot added with ID: ${documentReference.id}")}
            .addOnFailureListener { e ->Log.w("Firestore", "Error adding document", e)}
        gon()
    }


    fun postJsonToServer(url: String, jsonObject: JSONObject, credentials: String, callback: (response: String?) -> Unit) {
        val client = OkHttpClient()
        //val intent = Intent(this, WelcomeActivity::class.java)
        var intent = Intent(this, AppCheckerService::class.java)
        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .addHeader("Authorization", credentials)
            .addHeader("Content-Type", "text/csv")
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                callback(responseBody)
            }

            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }
        })
    }


}