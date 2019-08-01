package com.tikalk.worktracker.net

import android.content.Context
import com.tikalk.net.PersistentCookieStore
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

        private var cookieHandlerDefault: CookieHandler? = null
        private var cookieHandlerPersistent: CookieHandler? = null

        private fun createCookieHandler(context: Context?): CookieHandler {
            val cookieHandler: CookieHandler
            if (context == null) {
                if (cookieHandlerDefault == null) {
                    cookieHandlerDefault = CookieManager()
                }
                cookieHandler = cookieHandlerDefault!!
            } else {
                if (cookieHandlerPersistent == null) {
                    cookieHandlerPersistent = CookieManager(PersistentCookieStore(context), null)
                }
                cookieHandler = cookieHandlerPersistent!!
            }
            return cookieHandler
        }

        private fun createHttpClient(context: Context?, authToken: String? = null): OkHttpClient {
            val httpClientBuilder = OkHttpClient.Builder()

            if (BuildConfig.DEBUG) {
                val interceptorLogging = HttpLoggingInterceptor()
                interceptorLogging.level = HttpLoggingInterceptor.Level.HEADERS
                httpClientBuilder.addInterceptor(interceptorLogging)
            }

            if (authToken != null) {
                val interceptorAuth = AuthenticationInterceptor(authToken)
                httpClientBuilder.addInterceptor(interceptorAuth)
            }

            httpClientBuilder.cookieJar(JavaNetCookieJar(createCookieHandler(context)))

            return httpClientBuilder.build()
        }

        fun createPlain(context: Context?, authToken: String? = null): TimeTrackerService {
            val httpClient = createHttpClient(context, authToken)

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