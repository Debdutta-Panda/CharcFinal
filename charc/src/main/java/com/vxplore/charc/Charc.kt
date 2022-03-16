package com.vxplore.charc

import android.app.Application
import android.content.ContextWrapper
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface CharcApp{
    val chatEndUrl: String
}
class Charc private constructor(val app: Application) {
    fun onNetChange(net: ConnectivityListener.Net) {
        agoraLogin()
    }

    fun put(chat: Chat) {
        chatBox.put(chat)
    }

    suspend fun testRest(): ChatServer.Response<TestRestResponse> {
        return chatServer.testRest()
    }

    private val chatBox: ChatBox = ChatBox(app)
    private val chatServer = ChatServer((app as CharcApp).chatEndUrl)

    private val chatAgora = ChatAgora(
        app,
        "a47b50dab04949b7b9ee95b05902b014",
        "4b2f8c4f885c40a7b4f25793231c35da"
    )

    init {
        Prefs.Builder()
            .setContext(app)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName("com.vxplore.charc")
            .setUseDefaultSharedPreference(true)
            .build()
        agoraLogin()
    }

    private fun agoraLogin() {
        val sender = getSenderId()
        if(sender.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                chatAgora.login(sender)
            }
        }
    }

    fun getSenderId(): String{
        return Prefs.getString("sender")
    }

    fun setSenderId(sender: String){
        Prefs.putString("sender",sender)
        agoraLogin()
    }

    companion object{
        var instance: Charc? = null
        private set
        fun initialize(app: Application): Charc{
            return instance?:Charc(app).apply {
                instance = this
            }
        }
    }
}