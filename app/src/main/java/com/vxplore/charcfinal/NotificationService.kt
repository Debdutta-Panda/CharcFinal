package com.vxplore.charcfinal
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vxplore.charc.ChatPackets
import org.json.JSONObject

class NotificationService : FirebaseMessagingService() {

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
    }


    override fun onMessageReceived(p0: RemoteMessage) {
        Log.d("new_message",p0.toString())
        val data = p0.data
        val values = data.values
        if(values.isNotEmpty()){
            val d = values.first()
            val j = try {
                JSONObject(d)
            } catch (e: Exception) {
                null
            }
            if(j==null){
                return
            }
            val type = j.getString("notification_type")
            if(type in listOf(
                    "chats",
                    "chatAvailable"
            ))
            {
                App.instnace.charc.handleChatsFromNotification(d)
            }
        }
    }
}