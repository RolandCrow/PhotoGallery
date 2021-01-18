package com.bignerdranch.android.photogallery

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.bignerdranch.android.photogallery.api.FlickrFetchr
import java.util.concurrent.ConcurrentHashMap


private const val TAG = "ThumbnailDownloader"
private const val MESSAGE_DOWNLOAD = 0


class ThumbnailDownloader<in T>(
        private val responseHandler: Handler,
        private val onThumbnailDownloaded: (T, Bitmap) -> Unit // передача изображений
): HandlerThread(TAG) {

    val fragmentLifecycleObserver: LifecycleObserver =
        object : LifecycleObserver { // наблюдатель за жизненным циклом

            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun setup() { // установка
                Log.i(TAG, "Starting background thread") // создание фонового потока
                start()
                looper
            }
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun tearDown() {
                Log.i(TAG, "Destroying background thread") // уничтожение фонового потока
            }


        }

    val viewLifecycleObserver: LifecycleObserver = // добавление нового наблюдателя
        object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun clearQueue() {
                Log.i(TAG, "Clearing all requests from queue")
                requestHandler.removeMessages(MESSAGE_DOWNLOAD)
                requestMap.clear()
            }
        }

    private var hasQuit = false
    private lateinit var requestHandler: Handler // иницируем сообщения один раз за поток
    private var requestMap = ConcurrentHashMap<T, String>() // содержание сообщения
    private val flickrFetchr = FlickrFetchr() // соединяем

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("HandlerLeak") // защита от утечек
    override fun onLooperPrepared() { // перед циклом
        requestHandler = object : Handler() {
            override fun handleMessage(msg: Message) {
                if(msg.what == MESSAGE_DOWNLOAD) { // работа сообщений на автомате
                    val target = msg.obj as T // Т из конкурента
                    Log.i(TAG, "Got a request for URL: ${requestMap[target]}")
                    handleRequest(target)

                }
            }
        }
    }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }



    fun queueThumbnail(target: T, url: String) {
        Log.i(TAG, "Got a URL: $url")
        requestMap[target] = url
        requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget() //  тип сообщения
        // в фоновом потоке на автомате

    }

    fun clearQueue() {
        requestHandler.removeMessages(MESSAGE_DOWNLOAD)
        requestMap.clear()
    }

    private fun handleRequest(target: T) { // запрос на проверку содержания сообщения
        val url = requestMap[target] ?: return
        val bitmap = flickrFetchr.fetchPhoto(url) ?: return

        responseHandler.post(Runnable {
            if(requestMap[target] != url || hasQuit) { // проверка изображений
                return@Runnable
            }
            requestMap.remove(target)
            onThumbnailDownloaded(target, bitmap)
        })
    }

}