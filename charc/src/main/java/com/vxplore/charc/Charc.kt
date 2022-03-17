package com.vxplore.charc

import android.app.Application
import android.content.ContextWrapper
import com.google.gson.Gson
import com.pixplicity.easyprefs.library.Prefs
import com.vxplore.charc.objectBox.ObjectBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface CharcApp{
    val chatEndUrl: String
}
typealias OnChangeCallback = ()->Unit
class Charc private constructor(val app: Application) {
    private val listeners = mutableListOf<OnChangeCallback>()
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
        chatBox.markAllNotRendered()
        ObjectBox.store.subscribe(Chat::class.java)
            .observer {
                onChange()
            }
    }
    private fun onMessageReceived(cps: ChatPackets?, peer: String) {
        if(cps==null){
            return
        }
        handleIncomingChats(cps.items)
    }

    private val onMessageReceivedListener: (ChatPackets?, String)->Unit = {cps,peer->
        onMessageReceived(cps,peer)
    }
    private fun agoraLogin() {
        val sender = getSenderId()
        if(sender.isNotEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                if(chatAgora.login(sender)){
                    chatAgora.removeOnMessageReceiveListener(onMessageReceivedListener)
                    chatAgora.addOnMessageReceiveListener(onMessageReceivedListener)
                }
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

    private fun onChange() {
        listeners.forEach {
            it()
        }
        processOnChange()
    }

    private fun processOnChange() {
        CoroutineScope(Dispatchers.IO).launch {
            var toPublish = chatBox.get(Purpose.SYNC)
            if(toPublish.isEmpty()){
                return@launch
            }
            val sender = getSenderId()
            val toSendToPeer = mutableMapOf<String,MutableList<Chat>>()
            toPublish.forEach {
                if(
                    (it.sender==sender&&it.receivedAt==0L)
                    ||(it.sender!=sender
                            &&(
                            (it.receivedAt>0L&&!it.receiveAcknowledged)
                                    ||(it.seenAt>0L&&!it.seenAcknowledged)
                            ))
                ){
                    val s = it.sender
                    val r = it.receiver
                    val peer = when{
                        s!=sender->s
                        r!=sender->r
                        else->""
                    }
                    if(peer.isNotEmpty()){
                        if(!toSendToPeer.containsKey(peer)){
                            toSendToPeer[peer] = mutableListOf()
                        }
                        toSendToPeer[peer]?.add(it)
                    }
                }
            }
            if(toSendToPeer.isEmpty()){
                val serverResponse = chatServer.puts(ChatPackets(toPublish))
                val fromServer = serverResponse.data?.chats?: emptyList()
                if(serverResponse.data!=null){
                    toPublish.forEach {
                        it.serverNotified = true
                    }
                }
                chatBox.put(toPublish)
                handleIncomingChats(fromServer)
            }
            else{
                val failed = mutableListOf<Chat>()
                val succeed = mutableListOf<Chat>()
                toSendToPeer.forEach { map->
                    val peer = map.key
                    val items = map.value
                    val chatPackets = ChatPackets(items)
                    val success = chatAgora.sendToPeerWithLoginAssurance(sender,peer,chatPackets)
                    if(!success){
                        failed.addAll(items)
                    }
                    else{
                        items.forEach {
                            if(it.sender==sender){
                                if(!it.receiveAcknowledged){
                                    it.receiveAcknowledged = true
                                }
                            }else{
                                it.receiveAcknowledged = true
                                if(it.seenAt>0L){
                                    it.seenAcknowledged = true
                                }
                            }
                        }
                        succeed.addAll(items)
                    }
                }
                val failedCount = failed.size
                val succeedCount = succeed.size
                val totalCount = failedCount + succeedCount
                if(totalCount>0){
                    val allToServer = mutableListOf<Chat>()
                    allToServer.addAll(failed)
                    allToServer.addAll(succeed)
                    val toSendToServer = ChatPackets(allToServer)
                    val serverResponse = chatServer.puts(toSendToServer)
                    val fromServer = serverResponse.data?.chats?: emptyList()
                    if(serverResponse.data!=null){
                        allToServer.forEach {
                            it.serverNotified = true
                        }
                    }
                    chatBox.put(allToServer)
                    handleIncomingChats(fromServer)
                }
            }
        }
    }

    data class PrevNew(
        var prev: Chat? = null,
        var new: Chat
    ) {
        val merged: Chat?
        get(){
            if(prev==null){
                return new.apply {
                    id = 0L
                    receivedAt = utcTimestamp
                }
            }
            return Chat().apply {
                id = prev.chatId
            }
        }
    }

    private fun handleIncomingChats(incomings: List<Chat>?) {
        if(incomings==null){
            return
        }
        var incomingIds = incomings.map {
            it.chatId
        }
        val existingItems = chatBox.allIn(incomingIds)
        val idChatMap = mutableMapOf<String,PrevNew>()
        incomings.forEach {
            val chatId = it.chatId
            idChatMap[chatId] = PrevNew(new = it)
        }
        existingItems.forEach {
            val chatId = it.chatId
            idChatMap[chatId]?.prev = it
        }
        val pairs = idChatMap.map {
            it.value
        }
        val merged = pairs.map {
            it.merged
        }
        chatBox.put(merged)
    }

    fun registerListener(listener: OnChangeCallback){
        listeners.add(listener)
    }

    fun unRegisterListener(listener: OnChangeCallback){
        listeners.remove(listener)
    }

    enum class Purpose{
        READ,
        RENDER,
        SYNC
    }
    fun allForPeer(peerId: String, purpose: Purpose): List<Chat> {
        return chatBox.allForPeer(peerId,purpose)
    }

    fun deleteAllFor(peerId: String) {
        chatBox.deleteAllForPeer(peerId)
    }

    fun send(chatPackets: ChatPackets) {
        val items = chatPackets.items
        preBornProcessing(items)
        chatBox.put(items)
    }

    private fun preBornProcessing(items: List<Chat>) {
        items.forEach {
            it.createdAt = utcTimestamp
            it.chatId = newUid
            it.published = false
            it.rendered = false
        }
    }

    fun markRendered(chats: List<Chat>) {
        chatBox.markRendered(chats)
    }

    fun handleChatsFromNotification(d: String?) {
        val chatPackets = try {
            Gson().fromJson(d, ChatPackets::class.java)
        } catch (e: Exception) {
            null
        }
        handleIncomingChats(chatPackets?.items)
    }
}