package com.tikalk.worktracker.net

import android.annotation.SuppressLint
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Time Tracker web service factory.
 *
 * @author Moshe Waisberg.
 */
class TimeTrackerServiceFactory {

    companion object {
        private const val BASE_URL = "https://planet.tikalk.com/timetracker/"
        //private const val BASE_URL = "https://192.168.1.8/timetracker/"

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

        @SuppressLint("TrustAllX509TrustManager")
        private fun createUnsafeHttpClient(authToken: String? = null): OkHttpClient {
            try {
                // Create a trust manager that does not validate certificate chains
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return emptyArray()
                    }
                })

                // Install the all-trusting trust manager
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())

                // Create an ssl socket factory with our all-trusting manager
                val sslSocketFactory = sslContext.socketFactory

                val httpClientBuilder = OkHttpClient.Builder()
                httpClientBuilder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                httpClientBuilder.hostnameVerifier(object : HostnameVerifier {
                    @SuppressLint("BadHostnameVerifier")
                    override fun verify(hostname: String?, session: SSLSession?): Boolean {
                        return true
                    }
                })

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
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
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