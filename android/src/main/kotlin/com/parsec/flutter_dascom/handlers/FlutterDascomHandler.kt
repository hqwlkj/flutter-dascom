package com.parsec.flutter_dascom.handlers

import android.content.Context

object FlutterDascomHandler {
    private var context: Context? = null

    fun setContext(context: Context?) {
        FlutterDascomHandler.context = context
    }

    fun getContext(): Context? {
        return this.context
    }
}