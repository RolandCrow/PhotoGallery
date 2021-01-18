package com.bignerdranch.android.photogallery.api

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val API_KEY = "a45282158602ec1f6c122a1d2728a86a"

class PhotoInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response { // перехват информации
        val request = chain.request()

        val newUrl = request.url().newBuilder() // постановка нового запроса
            .addQueryParameter("api_key", API_KEY)
            .addQueryParameter("format", "json")
            .addQueryParameter("nojsoncallback", "1")
            .addQueryParameter("extras", "url_s")
            .build()

        val newRequest: Request = request.newBuilder()
            .url(newUrl)
            .build()
        return chain.proceed(newRequest)
    }

}