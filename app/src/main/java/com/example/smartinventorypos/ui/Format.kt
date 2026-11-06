package com.example.smartinventorypos.ui

import com.example.smartinventorypos.domain.model.Money
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun money(amount: Money): String = "৳%,.2f".format(amount.toBigDecimal())

/** Server time such as 2026-11-06T11:38:48.089Z, shown as local time like "6 Nov 2026, 5:38 PM". */
fun displayDate(iso: String): String = runCatching {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(parser.parse(iso.take(19))!!)
}.getOrDefault(iso)
