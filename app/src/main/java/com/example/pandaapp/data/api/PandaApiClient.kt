package com.example.pandaapp.data.api

import com.example.pandaapp.data.model.Assignment
import com.example.pandaapp.data.model.AssignmentResponse
import com.example.pandaapp.data.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class PandaApiClient {
    private val cookieStore: MutableMap<String, MutableList<Cookie>> = mutableMapOf()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies.toMutableList()
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchAssignments(username: String, password: String): List<Assignment> = withContext(Dispatchers.IO) {
        val tokens = fetchLoginTokens()
        performLogin(username, password, tokens)
        establishPortalSession()

        val portalHtml = fetchPortalHtml()
        val courses = parseCourses(portalHtml)
            .filter { it.title.contains("2025後期") }

        courses.flatMap { course ->
            fetchAssignmentsForSite(course).map { assignment ->
                assignment.copy(courseName = course.title, courseId = course.siteId)
            }
        }
    }

    private suspend fun fetchLoginTokens(): LoginTokens = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LOGIN_URL)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val document = Jsoup.parse(body)
            val lt = document.select("input[name=lt]").attr("value")
            val execution = document.select("input[name=execution]").attr("value")
            return@use LoginTokens(lt = lt, execution = execution)
        }
    }

    private suspend fun performLogin(username: String, password: String, tokens: LoginTokens) = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .add("lt", tokens.lt)
            .add("execution", tokens.execution)
            .add("_eventId", "submit")
            .build()

        val request = Request.Builder()
            .url(LOGIN_URL)
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isRedirect) {
                response.header("Location")?.let { ticketLocation ->
                    followRedirectChain(ticketLocation)
                }
            } else if (response.code !in 200..299) {
                error("Login failed with status ${response.code}")
            }
        }
    }

    private suspend fun establishPortalSession() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(PORTAL_URL)
            .get()
            .build()
        client.newCall(request).execute().close()
    }

    private suspend fun fetchPortalHtml(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(PORTAL_URL)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            return@use response.body?.string().orEmpty()
        }
    }

    private fun parseCourses(portalHtml: String): List<Course> {
        val regex = Regex("/portal/site/([\\w-]+)/.*?class=\\\"siteTitle\\\">([^<]+)", RegexOption.DOT_MATCHES_ALL)
        return regex.findAll(portalHtml).map { matchResult ->
            val (siteId, title) = matchResult.destructured
            Course(siteId = siteId, title = title.trim())
        }.toList()
    }

    private suspend fun fetchAssignmentsForSite(course: Course): List<Assignment> = withContext(Dispatchers.IO) {
        val url = "$ASSIGNMENT_URL/${course.siteId}.json"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val parsed = json.decodeFromString<AssignmentResponse>(body)
            return@use parsed.assignments.map { item ->
                Assignment(
                    id = item.id,
                    title = item.title,
                    dueTimeSeconds = item.dueTime?.epochSecond,
                    status = item.status,
                    courseName = course.title,
                    courseId = course.siteId
                )
            }
        }
    }

    private fun followRedirectChain(initialLocation: String) {
        var location: String? = initialLocation
        var iterations = 0
        while (!location.isNullOrBlank() && iterations < 5) {
            val request = Request.Builder()
                .url(location)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isRedirect) {
                    location = response.header("Location")
                } else {
                    location = null
                }
            }
            iterations++
        }
    }

    data class LoginTokens(
        val lt: String,
        val execution: String
    )

    companion object {
        private const val BASE_URL = "https://panda.ecs.kyoto-u.ac.jp"
        private const val LOGIN_URL = "$BASE_URL/cas/login?service=https://panda.ecs.kyoto-u.ac.jp/portal"
        private const val PORTAL_URL = "$BASE_URL/portal"
        private const val ASSIGNMENT_URL = "$BASE_URL/direct/assignment/site"
    }
}
