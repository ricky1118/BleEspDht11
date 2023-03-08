package com.eyuanchuang.bleespdht11
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
/*******************************************************
 * 任一位置获取全局Context,只要引用MyApplication.context
 * *****************************************************/
class MyApplication: Application(){
    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}