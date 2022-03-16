package com.vxplore.charcfinal

import android.app.Application
import android.content.ContextWrapper
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.vxplore.charc.Charc
import com.vxplore.charc.CharcApp
import com.vxplore.charc.ConnectivityListener

class App: Application(), CharcApp {
    lateinit var charc: Charc
    private set
    override fun onCreate() {
        super.onCreate()
        instnace = this
        charc = Charc.initialize(this)

        ConnectivityListener(this).net.observeForever {
            net.on = it.on
            net.metered = it.metered
            onNetChange()
        }
    }

    private fun onNetChange() {
        charc.onNetChange(net)
    }

    companion object{
        lateinit var instnace: App
        var net: ConnectivityListener.Net = ConnectivityListener.Net()
        private set
    }

    fun queryFcmToken(callback: (String?)->Unit){
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d("fcm_token", "Fetching FCM registration token failed", task.exception)
                callback(null)
                return@OnCompleteListener
            }
            val token = task.result
            Log.d("fcm_token", token?:"not found")
            callback(token)
        })
    }

    override val chatEndUrl: String
    get(){
        return "https://hellomydoc.com/common/chat"
    }
}