package com.example.pandaapp.data.api

import java.net.CookieManager
import java.net.CookiePolicy
import okhttp3.JavaNetCookieJar
import android.util.Log
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
import okhttp3.logging.HttpLoggingInterceptor
import org.jsoup.Jsoup

class PandaApiClient {

    // -------------------------
    // Logging設定
    // -------------------------

    private val logging = HttpLoggingInterceptor { msg ->
        Log.d("PandaNet", msg)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY   // ※必要に応じて BASIC などに落とす
    }

    // -------------------------
    // CookieJar
    // -------------------------

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .cookieJar(JavaNetCookieJar(cookieManager))
        // CAS のリダイレクトは自前で処理するので false のままでよい
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    // ==========================================================================
    //  Main Public Function
    // ==========================================================================

    suspend fun fetchAssignments(username: String, password: String): List<Assignment> =
        withContext(Dispatchers.IO) {

            Log.d("Panda", "Fetching login tokens (lt/execution)...")
            val tokens = fetchLoginTokens()
            if (tokens == null) {
                Log.d("Panda", "Already logged in (302). Skipping performLogin().")
            } else {
                Log.d("Panda", "Tokens: lt=${tokens.lt}, exec=${tokens.execution}")

                Log.d("Panda", "Performing CAS login...")
                performLogin(username, password, tokens)
            }

            Log.d("Panda", "Establishing portal session (ticket 処理含む)...")
            val portalHtml = establishPortalSession()

            // まだ CAS ログイン画面だったら失敗
            if (portalHtml.contains("name=\"lt\"") && portalHtml.contains("name=\"execution\"")) {
                error("Authentication failed (still on CAS login page).")
            }

            // コース抽出
            val courses = parseCourses(portalHtml)
            Log.d("Panda", "Found total courses = ${courses.size}")
            courses.forEach { Log.d("Panda", "Course: ${it.siteId} / ${it.title}") }

            // 2025後期だけ
            val fallCourses = courses.filter { it.title.contains("2025後期") }
            Log.d("Panda", "2025後期 courses = ${fallCourses.size}")

            // 課題取得
            val allAssignments = fallCourses.flatMap { course ->
                Log.d("Panda", "Fetching assignments for site=${course.siteId}")
                fetchAssignmentsForSite(course).map { a ->
                    a.copy(courseName = formatCourseName(course.title), courseId = course.siteId)
                }
            }

            Log.d("Panda", "Total assignments fetched = ${allAssignments.size}")
            return@withContext allAssignments
        }

    // ==========================================================================
    //  Login Step 1: GET login page → extract lt / execution
    // ==========================================================================
    private suspend fun fetchLoginTokens(): LoginTokens? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(LOGIN_URL).get().build()
        client.newCall(request).execute().use { response ->
            // 302 → 既にログイン済みなので lt/execution 取得不要
            if (response.code in 300..399) {
                Log.d("Panda", "CAS login page returned ${response.code}; assuming session already valid")
                return@use null
            }

            val body = response.body?.string().orEmpty()
            val document = Jsoup.parse(body)

            val lt = document.select("input[name=lt]").attr("value")
            val execution = document.select("input[name=execution]").attr("value")

            if (lt.isBlank() || execution.isBlank()) {
                error("Failed to get lt / execution from CAS login page")
            }

            return@use LoginTokens(lt = lt, execution = execution)
        }
    }

    // ==========================================================================
    //  Login Step 2: POST username/password (CASTGC をもらう)
    // ==========================================================================
    private suspend fun performLogin(
        username: String,
        password: String,
        tokens: LoginTokens
    ) = withContext(Dispatchers.IO) {

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
            Log.d("Panda", "Login status = ${response.code}")

            if (response.code in 300..399) {
                val loc = response.header("Location")
                if (loc != null) {
                    val ticketReq = Request.Builder().url(loc).get().build()
                    client.newCall(ticketReq).execute().use { /* body 捨てる */ }
                }
            } else if (response.code in 200..299) {
                val body = response.body?.string().orEmpty()
                val document = Jsoup.parse(body)

                val errorMsg = document.select("div.errors, #msg.errors").text()
                val stillOnLogin = document.select("form#fm1 input[name=lt]").isNotEmpty()

                if (errorMsg.isNotBlank()) {
                    error("パスワードが間違っています。")
                }
                if (stillOnLogin) {
                    error("Authentication failed (still on CAS login page).")
                }
            } else {
                error("Login failed with HTTP status ${response.code}")
            }
        }
    }

    // ==========================================================================
    //  Portal セッション確立 (Bash の STEP2 を移植)
    // ==========================================================================
    private suspend fun establishPortalSession(): String = withContext(Dispatchers.IO) {
        // ① まず /portal を取得
        val firstPortalReq = Request.Builder().url(PORTAL_URL).get().build()
        val firstHtml = client.newCall(firstPortalReq).execute().use { resp ->
            resp.body?.string().orEmpty()
        }

        // ② HTML から ticket 付き URL を探す
        val ticketRegex = Regex(
            "https://panda\\.ecs\\.kyoto-u\\.ac\\.jp/sakai-login-tool/container\\?ticket[^\"']*"
        )
        val match = ticketRegex.find(firstHtml)
        val ticketUrl = match?.value

        if (ticketUrl != null) {
            Log.d("Panda", "ticket リンク経由で PandA 本体へ遷移: $ticketUrl")

            // ③ ticket を踏む（ここで ST 消費 & SAKAISESSIONID が Cookie に入る）
            val ticketReq = Request.Builder().url(ticketUrl).get().build()
            client.newCall(ticketReq).execute().use {
                // ボディは不要なので捨てる
            }

            // ④ 改めて /portal を取得 → これが本物のポータル HTML
            val finalPortalReq = Request.Builder().url(PORTAL_URL).get().build()
            return@withContext client.newCall(finalPortalReq).execute().use { resp ->
                resp.body?.string().orEmpty()
            }
        } else {
            Log.d("Panda", "ticket なしで /portal に直接アクセス完了")
            // ticket が無い場合は最初の HTML をそのまま返す
            return@withContext firstHtml
        }
    }

    // ==========================================================================
    //  予備: /portal HTML 取得（ログイン済み前提で使う場合用）
    // ==========================================================================
    @Suppress("unused")
    private suspend fun fetchPortalHtml(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(PORTAL_URL).get().build()
        client.newCall(request).execute().use { response ->
            return@use response.body?.string().orEmpty()
        }
    }

    // ==========================================================================
    //  コース抽出 (/portal HTML から siteId / title を拾う)
    // ==========================================================================
    private fun parseCourses(portalHtml: String): List<Course> {
        val doc = Jsoup.parse(portalHtml)

        // /portal/site-reset/{siteId} 形式のリンクを全部拾う
        return doc.select("a[href*=/portal/site-reset/][title]").map { a ->
            val href = a.attr("href")
            val siteId = href.substringAfter("/portal/site-reset/").substringBefore("?")
            val title = a.attr("title").trim()

            Course(siteId = siteId, title = title)
        }
    }

    // ==========================================================================
    //  課題取得 (/direct/assignment/site/{siteId}.json)
    // ==========================================================================
    private suspend fun fetchAssignmentsForSite(course: Course): List<Assignment> =
        withContext(Dispatchers.IO) {
            val url = "$ASSIGNMENT_URL/${course.siteId}.json"

            Log.d("PandaNet", "GET $url")

            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d("PandaNet", "Assignment JSON = $body")

                val parsed = try {
                    json.decodeFromString<AssignmentResponse>(body)
                } catch (e: Exception) {
                    Log.e("Panda", "JSON parse error: ${e.message}")
                    return@withContext emptyList()
                }

                return@withContext parsed.assignments.map { item ->
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

    // ==========================================================================
    //  科目名の整形（[YYYY年～期]プレフィックスを削除）
    // ==========================================================================
    private fun formatCourseName(courseName: String): String {
        return courseName.replace(Regex("""^\[[^\[\]]*\]"""), "")
    }

    // ==========================================================================
    //  Data class / constants
    // ==========================================================================
    data class LoginTokens(val lt: String, val execution: String)

    companion object {
        private const val BASE_URL = "https://panda.ecs.kyoto-u.ac.jp"

        // Bash 版と同じ: service は sakai-login-tool/container
        private const val LOGIN_URL =
            "$BASE_URL/cas/login" +
                    "?service=https%3A%2F%2Fpanda.ecs.kyoto-u.ac.jp%2Fsakai-login-tool%2Fcontainer"

        private const val PORTAL_URL = "$BASE_URL/portal"
        private const val ASSIGNMENT_URL = "$BASE_URL/direct/assignment/site"
    }
}
