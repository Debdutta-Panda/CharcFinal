package com.vxplore.charc

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.net.UnknownHostException

class ChatServer(private val baseUrl: String) {
    data class Response<T>(
        val data: T? = null,
        val exception: RestException? = null
    ){
        override fun toString(): String{
            return "data: ${data.toString()}\nexception: ${exception.toString()}"
        }
    }
    class RestException(
        val situation: Situation,
        val exception: java.lang.Exception? = null,
        extra: Any? = null
    ): Exception(){
        enum class Situation{
            CLIENT_REQUEST,
            SERVER_RESPONSE,
            UNKNOWN_HOST,
            NO_TRANSFORMATION,
            DOUBLE_RECEIVE,
            NO_RESPONSE,
            UNKNOWN,
        }

        override fun toString(): String{
            return situation.name
        }
    }
    companion object{
    }
    var client: HttpClient = HttpClient(Android) {
        HttpResponseValidator {
            validateResponse { response ->
                /*response.content.read {
                    Log.d("flfjjfsjflf",String(it.array()))
                }*/
            }
            handleResponseException {exception->
                when(exception){
                    is ClientRequestException->{
                        throw RestException(RestException.Situation.CLIENT_REQUEST, exception)
                    }
                    is ServerResponseException->{
                        throw RestException(RestException.Situation.SERVER_RESPONSE, exception)
                    }
                    is UnknownHostException->{
                        throw RestException(RestException.Situation.UNKNOWN_HOST, exception)
                    }
                    is NoTransformationFoundException->{
                        throw RestException(RestException.Situation.NO_TRANSFORMATION, exception)
                    }
                    is RestException->{
                        throw RestException(exception.situation)
                    }
                    else->{
                        throw RestException(RestException.Situation.UNKNOWN, extra = exception)
                    }
                }
            }
        }
        install(Logging) {
            level = LogLevel.ALL
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    suspend inline fun<reified T,reified R> postWithBody(url: String, input: T? = null): Response<R>{
        try {
            val response = client.post<R> {
                url(url)
                contentType(ContentType.Application.Json)
                if(input!=null){
                    body = input
                }
            }
            return Response(response)
        } catch (e: Exception) {
            return when (e) {
                is RestException -> {
                    Response(exception = e)
                }
                is DoubleReceiveException -> {
                    Response(exception = RestException(RestException.Situation.DOUBLE_RECEIVE))
                }
                is NoTransformationFoundException -> {
                    Response(exception = RestException(RestException.Situation.NO_TRANSFORMATION))
                }
                else -> Response(exception = RestException(RestException.Situation.UNKNOWN))
            }
        }
    }

    suspend fun put(chatPacket: Chat): Response<PutChatResponse>{
        return postWithBody("${baseUrl}/put",chatPacket)
    }

    suspend fun puts(chatPackets: ChatPackets): Response<PutChatResponse>{
        return postWithBody("${baseUrl}/puts",chatPackets)
    }

    suspend fun putss(chatPackets: ChatPackets): Response<String>{
        return postWithBody("${baseUrl}/puts",chatPackets)
    }

    suspend fun getChat(chatId: String): Response<GetChatResponse>{
        return postWithBody("${baseUrl}/getChat/${chatId}", null as Any?)
    }

    suspend fun getNewChats(senderId: String): Response<GetChatsResponse>{
        return postWithBody("${baseUrl}/getNewChats/${senderId}", null as Any?)
    }

    suspend fun sendFcmToken(sender: String, token: String): Response<FcmTokenUpdateResponse> {
        return postWithBody("${baseUrl}/updateFcmToken/${sender}", token)
    }

    suspend fun testRest(): Response<TestRestResponse> {
        return postWithBody("${baseUrl}/testRest", null as Any?)
    }
}