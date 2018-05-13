package com.tikalk.worktracker.net

import android.text.TextUtils
import com.tikalk.worktracker.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Time Tracker web service factory.
 *
 * @author Moshe Waisberg.
 */
class TimeTrackerServiceFactory {

    companion object {
        private const val BASE_URL = "https://planet.tikalk.com/timetracker/"

        fun createPlain(authToken: String? = null): TimeTrackerService {
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
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .build()
            return retrofit.create(TimeTrackerService::class.java)
        }
    }
}