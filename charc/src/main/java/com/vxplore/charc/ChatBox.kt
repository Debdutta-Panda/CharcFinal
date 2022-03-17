package com.vxplore.charc

import android.app.Application
import com.vxplore.charc.objectBox.ObjectBox
import io.objectbox.Box
import io.objectbox.query.QueryBuilder

internal class ChatBox(app: Application){
    fun put(chat: Chat) {
        chatBox.put(chat)
    }
    fun put(chats: List<Chat>) {
        chatBox.put(chats)
    }

    fun allForPeer(peerId: String, purpose: Charc.Purpose): List<Chat> {
        return chatBox
            .query(
                when(purpose){
                    Charc.Purpose.READ -> {
                        (Chat_.sender.equal(peerId)
                            .or(Chat_.receiver.equal(peerId)))
                    }
                    Charc.Purpose.RENDER -> {
                        (Chat_.sender.equal(peerId)
                            .or(Chat_.receiver.equal(peerId)))
                            .and(
                                Chat_.rendered.equal(false)
                            )
                    }
                    Charc.Purpose.SYNC -> {
                        (Chat_.sender.equal(peerId)
                            .or(Chat_.receiver.equal(peerId)))
                            .and(
                                Chat_.published.equal(false)
                            )
                    }
                }



            )
            .build()
            .find()
    }

    fun deleteAllForPeer(peerId: String) {
        chatBox.put(allForPeer(peerId,Charc.Purpose.READ).map {
            it.rendered = false
            it.published = false
            it
        })
    }

    fun markAllNotRendered() {
        chatBox.put(chatBox.all.map {
            it.rendered = false
            it
        })
    }

    fun markRendered(chats: List<Chat>) {
        chatBox.put(chats.map {
            it.rendered = true
            it
        })
    }

    fun get(purpose: Charc.Purpose): List<Chat> {
        return when(purpose){
            Charc.Purpose.READ -> {
                chatBox.all
            }
            Charc.Purpose.RENDER -> {
                chatBox
                    .query(
                        Chat_.rendered.equal(false)
                    )
                    .build()
                    .find()
            }
            Charc.Purpose.SYNC -> {
                chatBox
                    .query(
                        Chat_.published.equal(false)
                    )
                    .build()
                    .find()
            }
        }
    }

    fun allIn(incomingIds: List<String>): List<Chat> {
        val output =  chatBox
            .query()
            ?.`in`(Chat_.chatId,incomingIds.toTypedArray(), QueryBuilder.StringOrder.CASE_SENSITIVE)
            ?.build()
            ?.find()
        return output?.filter {
            it!=null
        }?: emptyList()
    }

    private lateinit var chatBox: Box<Chat>
    init{
        ObjectBox.init(app)
        chatBox = ObjectBox.store.boxFor(Chat::class.java)
    }
}