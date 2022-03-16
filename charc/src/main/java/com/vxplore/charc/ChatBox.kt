package com.vxplore.charc

import android.app.Application
import com.vxplore.charc.objectBox.ObjectBox
import io.objectbox.Box

internal class ChatBox(app: Application){
    fun put(chat: Chat) {
        chatBox.put(chat)
    }

    private lateinit var chatBox: Box<Chat>
    init{
        ObjectBox.init(app)
        chatBox = ObjectBox.store.boxFor(Chat::class.java)
    }
}