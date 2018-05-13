package com.tikalk.worktracker.net

import android.text.TextUtils
import com.google.gson.GsonBuilder
import com.tikalk.worktracker.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Time Tracker web service factory.
 *
 * @author Moshe Waisberg.
 */
class TimeTrackerServiceFactory {

    companion object {
        const val BASE_URL = "https://planet.tikalk.com/timetracker/"

        fun create(authToken: String? = null): TimeTrackerService {
            val gson = GsonBuilder().create()

            val httpClientBuilder = OkHttpClient.Builder()

            if (BuildConfig.DEBUG) {
                val interceptorLogging = HttpLoggingInterceptor()
                interceptorLogging.level = HttpLoggingInterceptor.Level.BODY
                httpClientBuilder.addInterceptor(interceptorLogging)
            }

            if (!TextUtils.isEmpty(authToken)) {
                val interceptorAuth = AuthenticationInterceptor(authToken!!)
                httpClientBuilder.addInterceptor(interceptorAuth)
            }

            val httpClient = httpClientBuilder.build()

            val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            return retrofit.create(TimeTrackerService::class.java)
        }
    }
}