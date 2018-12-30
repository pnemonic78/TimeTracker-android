package com.tikalk.worktracker.net

import android.text.TextUtils
import com.tikalk.worktracker.BuildConfig
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.CookieHandler
import java.net.CookieManager

/**
 * Time Tracker web service factory.
 *
 * @author Moshe Waisberg.
 */
class TimeTrackerServiceFactory {

    companion object {
        private const val BASE_URL = "https://planet.tikalk.com/timetracker/"

        private val cookieHandler: CookieHandler = CookieManager()

        private fun createHttpClient(authToken: String? = null): OkHttpClient {
            val httpClientBuilder = OkHttpClient.Builder()

            if (BuildConfig.DEBUG) {
                val interceptorLogging = HttpLoggingInterceptor()
                interceptorLogging.level = HttpLoggingInterceptor.Level.HEADERS
                httpClientBuilder.addInterceptor(interceptorLogging)
            }

            if (!TextUtils.isEmpty(authToken)) {
                val interceptorAuth = AuthenticationInterceptor(authToken!!)
                httpClientBuilder.addInterceptor(interceptorAuth)
            }

            httpClientBuilder.cookieJar(JavaNetCookieJar(cookieHandler))

            return httpClientBuilder.build()
        }

        fun createPlain(authToken: String? = null): TimeTrackerService {
            val httpClient = createHttpClient(authToken)

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