package com.vxplore.charc.objectBox

import android.content.Context
import android.util.Log
import com.vxplore.charc.BuildConfig
import com.vxplore.charc.MyObjectBox
import io.objectbox.BoxStore
import io.objectbox.android.AndroidObjectBrowser

object ObjectBox {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        store = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .build()
        if (BuildConfig.DEBUG) {
            val started = AndroidObjectBrowser(store).start(context)
            Log.i("ObjectBrowser", "Started: $started")
        }
    }
}