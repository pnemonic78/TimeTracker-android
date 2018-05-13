package com.tikalk.worktracker.net

import com.google.gson.GsonBuilder
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

        fun create(): TimeTrackerService {
            val gson = GsonBuilder().create()

            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY

            val client = OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build()
            val retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            return retrofit.create(TimeTrackerService::class.java)
        }
    }
}