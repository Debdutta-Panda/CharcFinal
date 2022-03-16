package com.vxplore.charc

import android.app.Application
import com.vxplore.charc.tokener.rtm.RtmTokenBuilder
import io.agora.rtm.*
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatAgora(
    private val application: Application,
    private val appId: String,
    private val appCertificate: String
) {
    var loginDone = AtomicBoolean(false)
    private set
    var loginOnGoing = AtomicBoolean(false)
    private var userId = ""
    private var rtmClient: RtmClient? = null
    private val mListenerList: MutableList<RtmClientListener> = ArrayList()
    private val onMessageReceivedListeners = mutableListOf<(ChatPackets?, String)->Unit>()
    init{
        try {
            rtmClient = RtmClient.createInstance(application, appId, object : RtmClientListener {
                override fun onConnectionStateChanged(state: Int, reason: Int) {

                }

                override fun onMessageReceived(rtmMessage: RtmMessage, peerId: String) {
                    mListenerList.forEach {
                        it.onMessageReceived(rtmMessage,peerId)
                    }
                    onMessageReceivedListeners.forEach {
                        val text = rtmMessage.text
                        it(ChatPackets.fromString(text),peerId)
                    }
                }

                override fun onImageMessageReceivedFromPeer(
                    rtmImageMessage: RtmImageMessage,
                    peerId: String
                ) {

                }

                override fun onFileMessageReceivedFromPeer(
                    rtmFileMessage: RtmFileMessage,
                    s: String
                ) {
                }

                override fun onMediaUploadingProgress(
                    rtmMediaOperationProgress: RtmMediaOperationProgress,
                    l: Long
                ) {

                }

                override fun onMediaDownloadingProgress(
                    rtmMediaOperationProgress: RtmMediaOperationProgress,
                    l: Long
                ) {
                }

                override fun onTokenExpired() {}
                override fun onPeersOnlineStatusChanged(status: Map<String, Int>) {

                }
            })
        } catch (e: Exception) {
            
        }
    }

    fun registerListener(listener: RtmClientListener) {
        mListenerList.add(listener)
    }

    fun unregisterListener(listener: RtmClientListener) {
        mListenerList.remove(listener)
    }

    fun loginAsync(userId: String,callback: (Boolean)->Unit){
        this.userId = userId
        val tokener = RtmTokenBuilder()
        val token: String = tokener.buildToken(
            appId,
            appCertificate,
            userId,
            RtmTokenBuilder.Role.Rtm_User,
            0
        )
        rtmClient?.login(token, userId, object: io.agora.rtm.ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                loginDone.set(true)
                callback(true)
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                callback(false)
            }
        })
    }

    suspend fun login(userId: String): Boolean =
        suspendCoroutine {cont->
            if(loginOnGoing.get()){
                cont.resume(false)
                return@suspendCoroutine
            }
            this.userId = userId
            val tokener = RtmTokenBuilder()
            val token: String = tokener.buildToken(
                appId,
                appCertificate,
                userId,
                RtmTokenBuilder.Role.Rtm_User,
                0
            )
            loginOnGoing.set(true)
            loginDone.set(false)
            rtmClient?.login(token, userId, object : io.agora.rtm.ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {
                    loginOnGoing.set(false)
                    loginDone.set(true)
                    cont.resume(true)
                }

                override fun onFailure(errorInfo: ErrorInfo) {
                    loginOnGoing.set(false)
                    loginDone.set(false)
                    cont.resume(false)
                }
            })
        }
    suspend fun logout(): Boolean =
    suspendCoroutine{cont->
        if(loginOnGoing.get()){
            cont.resume(false)
            return@suspendCoroutine
        }
        rtmClient?.logout(object : io.agora.rtm.ResultCallback<Void?> {
            override fun onSuccess(responseInfo: Void?) {
                loginDone.set(false)
                cont.resume(true)
            }

            override fun onFailure(errorInfo: ErrorInfo) {
                cont.resume(true)
            }
        })
    }

    suspend fun sendToPeerWithLoginAssurance(senderId: String, peerId: String, chatPackets: ChatPackets): Boolean =
        suspendCoroutine {cont->
            if(!loginDone.get()){
                loginAsync(senderId){
                    sendToPeerAsync(peerId,chatPackets){
                        cont.resume(it)
                    }
                }
            }else{
                sendToPeerAsync(peerId,chatPackets){
                    cont.resume(it)
                }
            }
        }

    fun sendToPeerAsync(peerId: String, chatPackets: ChatPackets, callback: (Boolean) -> Unit){
        val message = rtmClient?.createMessage()
        message?.text = chatPackets.toString()
        rtmClient?.sendMessageToPeer(
            peerId,
            message,
            SendMessageOptions(),
            object : io.agora.rtm.ResultCallback<Void?> {
                override fun onSuccess(responseInfo: Void?) {
                    callback(true)
                }

                override fun onFailure(errorInfo: ErrorInfo) {
                    callback(false)
                }
            }
        )
    }

    fun addOnMessageReceiveListener(listener: (ChatPackets?,String)->Unit){
        onMessageReceivedListeners.add(listener)
    }
    fun removeOnMessageReceiveListener(listener: (ChatPackets?,String)->Unit){
        onMessageReceivedListeners.remove(listener)
    }
}