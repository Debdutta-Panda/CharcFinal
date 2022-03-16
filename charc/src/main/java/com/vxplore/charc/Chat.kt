package com.vxplore.charc

import com.google.gson.Gson
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import io.objectbox.annotation.Unique
import kotlinx.serialization.*

@Entity
@Serializable
data class Chat(
    @Id
    @EncodeDefault var id: Long = 0,


    @EncodeDefault
    @Index
    @Unique
    var chatId: String = "",
    @EncodeDefault var sender: String = "",
    @EncodeDefault var receiver: String = "",
    @EncodeDefault var data: String = "",
    @EncodeDefault var createdAt: Long = 0,
    @EncodeDefault var arrivedServerAt: Long = 0,
    @EncodeDefault var receivedAt: Long = 0,
    @EncodeDefault var seenAt: Long = 0,
    @EncodeDefault var updatedAt: Long = 0
){
    fun clone(): Chat{
        return Chat(
            id,
            chatId,
            sender,
            receiver,
            data,
            createdAt,
            arrivedServerAt,
            receivedAt,
            seenAt,
            updatedAt,
        )
    }
}

@Serializable
data class ChatPackets(
    val items: List<Chat>
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }

    fun clone(): ChatPackets {
        return ChatPackets(
            items.map {
                it.clone()
            }
        )
    }

    companion object {
        fun fromString(json: String): ChatPackets? {
            try {
                return Gson().fromJson(json, ChatPackets::class.java)
            } catch (e: Exception) {
            }
            return null
        }
    }
}

@Serializable
data class ChatPacketAttachment(
    val url: String? = null,
    val thumbnail: String? = null,
    val type: String,
    val name: String? = null,
    val json: String? = null
){
    fun clone(): ChatPacketAttachment{
        return ChatPacketAttachment(
            url,
            thumbnail,
            type,
            name,
            json
        )
    }
}

@Serializable
data class ChatPacketData(
    val text: String? = null,
    val attachments: List<ChatPacketAttachment>? = null
) {
    fun clone(): ChatPacketData{
        return ChatPacketData(
            text,
            attachments?.map {
                it.clone()
            }
        )
    }
    fun jsonString(): String {
        return Gson().toJson(this)
    }


    companion object {
        fun fromString(json: String): ChatPacketData? {
            try {
                return Gson().fromJson(json, ChatPacketData::class.java)
            } catch (e: Exception) {
            }
            return null
        }
    }
}

@Serializable
data class GetChatResponse(
    val success: Boolean,
    val message: String,
    val chat: Chat? = null
)

@Serializable
data class GetChatsResponse(
    val success: Boolean,
    val message: String,
    val chats: List<Chat>? = null
)

@Serializable
data class PutChatResponse(
    val success: Boolean,
    val message: String,
    val chats: List<Chat>? = null
)
@Serializable
data class FcmTokenUpdateResponse(
    val success: Boolean,
    val message: String
)

@Serializable
data class TestRestResponse(
    val success: Boolean,
    val message: String
)