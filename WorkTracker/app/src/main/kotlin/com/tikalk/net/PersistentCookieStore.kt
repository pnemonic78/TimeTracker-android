package com.tikalk.net

import android.content.Context
import android.content.SharedPreferences
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Store cookies as shared preferences.
 * <p/>
 * Based on `InMemoryCookieStore` class.
 */
class PersistentCookieStore(context: Context) : CookieStore {

    private val prefs: SharedPreferences = context.getSharedPreferences("cookies", Context.MODE_PRIVATE)
    private val uriIndex: MutableMap<URI, MutableList<HttpCookie>> = HashMap()
    private val lock: ReentrantLock = ReentrantLock(false)

    init {
        load()
    }

    override fun add(uri: URI?, cookie: HttpCookie) {
        lock.lock()
        try {
            val effectiveURI = getEffectiveURI(uri)
            if (effectiveURI != null) {
                addIndex(uriIndex, effectiveURI, cookie)
            }
        } finally {
            lock.unlock()
        }
    }

    override fun getCookies(): List<HttpCookie> {
        val cookies: MutableList<HttpCookie> = ArrayList()

        lock.lock()
        try {
            for (list in uriIndex.values) {
                val it = list.iterator()
                while (it.hasNext()) {
                    val cookie = it.next()
                    if (cookie.hasExpired()) {
                        continue
                    } else if (!cookies.contains(cookie)) {
                        cookies.add(cookie)
                    }
                }
            }
        } finally {
            lock.unlock()
        }

        return Collections.unmodifiableList(cookies)
    }

    override fun getURIs(): List<URI> {
        val uris = ArrayList<URI>()

        lock.lock()
        try {
            uris.addAll(uriIndex.keys)
        } finally {
            lock.unlock()
        }
        return Collections.unmodifiableList(uris)
    }

    override fun get(uri: URI): List<HttpCookie> {
        val cookies = ArrayList<HttpCookie>()
        lock.lock()
        try {
            // check domainIndex first
            getInternal1(cookies, uriIndex, uri.host)
            // check uriIndex then
            val effectiveURI = getEffectiveURI(uri)
            if (effectiveURI != null) {
                getInternal2(cookies, uriIndex, effectiveURI)
            }
        } finally {
            lock.unlock()
        }
        return cookies
    }

    override fun remove(uri: URI?, cookie: HttpCookie): Boolean {
        lock.lock()
        try {
            val effectiveURI = getEffectiveURI(uri)
            val cookies = uriIndex[effectiveURI] ?: return false
            if (cookies.remove(cookie)) {
                val set = cookies.map { it.toString() }.toSet()
                prefs.edit().putStringSet(effectiveURI.toString(), set).apply()
                return true
            }
        } finally {
            lock.unlock()
        }
        return false
    }

    override fun removeAll(): Boolean {
        lock.lock()
        val result = uriIndex.isNotEmpty()

        try {
            uriIndex.clear()
            prefs.edit().clear().apply()
        } finally {
            lock.unlock()
        }

        return result
    }

    // add 'cookie' indexed by 'index' into 'indexStore'
    private fun <T> addIndex(indexStore: MutableMap<T, MutableList<HttpCookie>>,
                             index: T,
                             cookie: HttpCookie) {
        // Android-changed: "index" can be null. We only use the URI based
        // index on Android and we want to support null URIs. The underlying
        // store is a HashMap which will support null keys anyway.
        // if (index != null) {
        var cookies: MutableList<HttpCookie>? = indexStore[index]
        if (cookies != null) {
            // there may already have the same cookie, so remove it first
            cookies.remove(cookie)
            cookies.add(cookie)
        } else {
            cookies = ArrayList()
            cookies.add(cookie)
            indexStore[index] = cookies

            val set = cookies.map { it.toString() }.toSet()
            prefs.edit().putStringSet(index.toString(), set).apply()
        }
    }

    //
    // for cookie purpose, the effective uri should only be http://host
    // the path will be taken into account when path-match algorithm applied
    //
    private fun getEffectiveURI(uri: URI?): URI? {
        // Android-added: Fix NullPointerException
        if (uri == null) {
            return null
        }

        return try {
            URI("http", uri.host, null, null, null)
        } catch (ignored: URISyntaxException) {
            uri
        }
    }

    private fun getInternal1(cookies: MutableList<HttpCookie>, cookieIndex: Map<URI, MutableList<HttpCookie>>,
                             host: String) {
        // Use a separate list to handle cookies that need to be removed so
        // that there is no conflict with iterators.
        val toRemove = ArrayList<HttpCookie>()
        for ((_, lst) in cookieIndex) {
            for (c in lst) {
                val domain = c.domain
                if (c.version == 0 && netscapeDomainMatches(domain, host) || c.version == 1 && HttpCookie.domainMatches(domain, host)) {
                    // the cookie still in main cookie store
                    if (!c.hasExpired()) {
                        // don't add twice
                        if (!cookies.contains(c)) {
                            cookies.add(c)
                        }
                    } else {
                        toRemove.add(c)
                    }
                }
            }
            // Clear up the cookies that need to be removed
            for (c in toRemove) {
                lst.remove(c)
            }
            toRemove.clear()
        }
    }

    // @param cookies           [OUT] contains the found cookies
    // @param cookieIndex       the index
    // @param comparator        the prediction to decide whether or not
    //                          a cookie in index should be returned
    private fun <T : Comparable<T>> getInternal2(cookies: MutableList<HttpCookie>, cookieIndex: Map<T, List<HttpCookie>>,
                                                 comparator: T) {
        // Removed cookieJar
        for (index in cookieIndex.keys) {
            if (index === comparator || comparator.compareTo(index) == 0) {
                val indexedCookies = cookieIndex[index]
                // check the list of cookies associated with this domain
                if (indexedCookies != null) {
                    val it = indexedCookies.iterator()
                    while (it.hasNext()) {
                        val cookie = it.next()
                        // the cookie still in main cookie store
                        if (!cookie.hasExpired()) {
                            // don't add twice
                            if (!cookies.contains(cookie)) {
                                cookies.add(cookie)
                            }
                        }
                    }
                } // end of indexedCookies != null
            } // end of comparator.compareTo(index) == 0
        } // end of cookieIndex iteration
    }

    /*
     * This is almost the same as HttpCookie.domainMatches except for
     * one difference: It won't reject cookies when the 'H' part of the
     * domain contains a dot ('.').
     * I.E.: RFC 2965 section 3.3.2 says that if host is x.y.domain.com
     * and the cookie domain is .domain.com, then it should be rejected.
     * However that's not how the real world works. Browsers don't reject and
     * some sites, like yahoo.com do actually expect these cookies to be
     * passed along.
     * And should be used for 'old' style cookies (aka Netscape type of cookies)
     */
    private fun netscapeDomainMatches(domain: String?, host: String?): Boolean {
        if (domain == null || host == null) {
            return false
        }

        // if there's no embedded dot in domain and domain is not .local
        val isLocalDomain = ".local".equals(domain, ignoreCase = true)
        var embeddedDotInDomain = domain.indexOf('.')
        if (embeddedDotInDomain == 0) {
            embeddedDotInDomain = domain.indexOf('.', 1)
        }
        if (!isLocalDomain && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length - 1)) {
            return false
        }

        // if the host name contains no dot and the domain name is .local
        val firstDotInHost = host.indexOf('.')
        if (firstDotInHost == -1 && isLocalDomain) {
            return true
        }

        val domainLength = domain.length
        val lengthDiff = host.length - domainLength
        when {
            lengthDiff == 0 -> // if the host name and the domain name are just string-compare euqal
                return host.equals(domain, ignoreCase = true)
            lengthDiff > 0 -> {
                // need to check H & D component
                val d = host.substring(lengthDiff)

                // Android-changed: b/26456024 targetSdkVersion based compatibility for domain matching
                // Android M and earlier: Cookies with domain "foo.com" would not match "bar.foo.com".
                // The RFC dictates that the user agent must treat those domains as if they had a
                // leading period and must therefore match "bar.foo.com".
                return if (!domain.startsWith(".")) false else d.equals(domain, ignoreCase = true)

            }
            lengthDiff == -1 -> // if domain is actually .host
                return domain[0] == '.' && host.equals(domain.substring(1), ignoreCase = true)
            else -> return false
        }
    }

    private fun load() {
        prefs.all.forEach { (key, value) ->
            if (value is Collection<*>) {
                val index = URI.create(key)
                uriIndex[index] = value.flatMap { header -> HttpCookie.parse(header as String) }.toMutableList()
            }
        }
    }
}