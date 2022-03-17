package com.vxplore.charc

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.*

val newUid: String
get(){
    return UUID.randomUUID().toString()+utcTimestamp.toString()
}

val utcTimestamp: Long
get(){
    return DateTime(DateTimeZone.UTC).millis
}