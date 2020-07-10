package com.unidy2002.thuinfo.data.network

import android.util.Log
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.unidy2002.thuinfo.data.model.login.loggedInUser
import com.unidy2002.thuinfo.data.model.report.ReportItem
import com.unidy2002.thuinfo.data.model.tables.ECardRecord
import com.unidy2002.thuinfo.data.model.tables.JoggingRecord
import com.unidy2002.thuinfo.data.util.Alipay.generalGetPayCode
import com.unidy2002.thuinfo.ui.home.ReportFragment
import com.unidy2002.thuinfo.ui.home.ReportFragment.Mode.*
import jxl.Workbook
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLEncoder.encode

fun Network.getReport(mode: ReportFragment.Mode = NORMAL): List<ReportItem>? = retryTemplate(792) {
    val s = if (mode == GRADUATE) "yjs" else "bks"
    lateinit var bxSet: Set<String>
    if (mode == BX) {
        connect(
            "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421eaff4b8b69336153301c9aa596522b20bc86e6e559a9b290/cj.cjCjbAll.do?m=${s}_yxkccj",
            "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421f9f9479369247b59700f81b9991b2631506205de/tag.e9676e2c6fc0928.render.userLayoutRootNode.uP",
            loggedInUser.vpnTicket
        ).getData("GBK").run {
            bxSet = Jsoup.parse(this).getElementsByClass("table-striped")[0].child(0).children()
                .drop(1).dropLast(10)
                .filter { it.child(8).text() == "必修" || it.child(8).text() == "限选" }
                .map { it.child(0).text() }.toSet()
        }
    }
    connect(
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421eaff4b8b69336153301c9aa596522b20bc86e6e559a9b290/cj.cjCjbAll.do?m=${s}_cjdcx&cjdlx=zw",
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421f9f9479369247b59700f81b9991b2631506205de/render.userLayoutRootNode.uP",
        loggedInUser.vpnTicket
    ).inputStream.run {
        val reader = BufferedReader(InputStreamReader(this, "GBK"))
        val stringBuilder = StringBuilder()
        var readLine: String?
        while (reader.readLine().also { readLine = it } != null) {
            stringBuilder.append(readLine).append('\n')
        }
        reader.close()
        close()

        fun nullIfNA(string: String): String? = if (string.matches(Regex("\\*+|N/A"))) null else string

        Jsoup.parse(stringBuilder.toString()).getElementById("table1").child(0).children().drop(1).filter {
            mode != BX || it.child(0).ownText() in bxSet
        }.map {
            ReportItem(
                it.child(1).ownText(),
                it.child(2).ownText().toInt(),
                nullIfNA(it.child(3).ownText()),
                nullIfNA(it.child(4).ownText())?.toDouble(),
                it.child(5).ownText(),
                it.child(0).ownText()
            )
        }.sortedBy {
            (it.semester.substring(0, 4).toInt() * 10 + when (it.semester[5]) {
                '春' -> 0
                '夏' -> 1
                '秋' -> 2
                else -> 3
            })
        }
    }
}

fun Network.getReportPayCode(email: String) = retryTemplate(2005) {
    connect(
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421faef469069336153301c9aa596522b20e33c1eb39606919f/jxpg/f/zzzc/v_zzfw_zzdy_dd/bksDzcjdzf",
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421faef469069336153301c9aa596522b20e33c1eb39606919f/jxpg/f/zzzc/v_zzfw_zzdy_dd/bksDzcjdMain",
        loggedInUser.vpnTicket,
        post = "cjdlxs=01%2C&czrlb=bks&p_email=${encode(email, "UTF-8")}"
    ).inputStream.close()
    val result = connect(
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421faef469069336153301c9aa596522b20e33c1eb39606919f/jxpg/b/zzzc/v_zzfw_zzdy_dd/dzcjd_pay",
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421faef469069336153301c9aa596522b20e33c1eb39606919f/jxpg/f/zzzc/v_zzfw_zzdy_dd/bksDzcjdzf",
        loggedInUser.vpnTicket,
        post = "",
        followRedirects = false
    ).headerFields["Location"]!![0]!!
    if (!result.contains("pay")) throw Exception()
    result
}?.run {
    Log.i("get report pay redirect", this)
    generalGetPayCode(
        this,
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421faef469069336153301c9aa596522b20e33c1eb39606919f/jxpg/f/zzzc/v_zzfw_zzdy_dd/bksDzcjdzf",
        true
    )
}

fun Network.getPhysicalExamResult(): Map<String, String?>? = retryTemplate(792) {
    connect(
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421eaff4b8b69336153301c9aa596522b20bc86e6e559a9b290/tyjx.tyjx_tc_xscjb.do?vpn-12-o1-zhjw.cic.tsinghua.edu.cn&m=jsonCj",
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421eaff4b8b69336153301c9aa596522b20bc86e6e559a9b290/tyjx.tyjx_tc_xscjb.do?m=tc_bkscjcx",
        loggedInUser.vpnTicket
    ).inputStream.run {
        val reader = BufferedReader(InputStreamReader(this, "GBK"))
        val stringBuilder = StringBuilder()
        var readLine: String?
        while (reader.readLine().also { readLine = it } != null)
            stringBuilder.append(readLine!!)

        val json = JSON.parse(stringBuilder.toString().drop(1).dropLast(1)) as JSONObject
        if (json.getString("success") == "false")
            null
        else {
            mutableMapOf<String, String?>(
                "是否免测" to "sfmc",
                "免测原因" to "mcyy",
                "总分" to "zf",
                "标准分" to "bzf",
                "附加分" to "fjf",
                "长跑附加分" to "cpfjf",
                "身高" to "sg",
                "体重" to "tz",
                "身高体重分数" to "sgtzfs",
                "肺活量" to "fhl",
                "肺活量分数" to "fhltzfs",
                "800M跑" to "bbmp",
                "800M跑分数" to "bbmpfs",
                "1000M跑" to "yqmp",
                "1000M跑分数" to "yqmpfs",
                "50M跑" to "wsmp",
                "50M跑分数" to "wsmpfs",
                "立定跳远" to "ldty",
                "立定跳远分数" to "ldtyfs",
                "坐位体前屈" to "zwtqq",
                "坐位体前屈分数" to "zwtqqfs",
                "仰卧起坐" to "ywqz",
                "仰卧起坐分数" to "ywqzfs",
                "引体向上" to "ytxs",
                "引体向上分数" to "ytxsfs",
                "体育课成绩" to "tykcj"
            )
        }
    }
}

fun Network.getJoggingRecord(): List<JoggingRecord>? = retryTemplate(792) {
    connect(
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421eaff4b8b69336153301c9aa596522b20bc86e6e559a9b290/tyjx.tyjx_kw_xscjb.do?m=queryXsCjAll",
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421eaff4b8b69336153301c9aa596522b20bc86e6e559a9b290/jxmh.do?url=jxmh.do&m=bks_tyjx_tcyy",
        loggedInUser.vpnTicket
    ).inputStream.run {
        val reader = BufferedReader(InputStreamReader(this, "GBK"))
        val stringBuilder = StringBuilder()
        var readLine: String?
        while (reader.readLine().also { readLine = it } != null)
            stringBuilder.append(readLine!!).append('\n')

        Jsoup.parse(stringBuilder.toString()).getElementsByClass("table_list")[0].child(0).children()
            .drop(1)
            .map {
                JoggingRecord(
                    it.child(0).ownText(),
                    it.child(4).ownText().toInt(),
                    it.child(5).ownText().toInt(),
                    it.child(6).ownText().toInt()
                )
            }
    }
}

fun Network.getClassroomState(classroom: String, week: Int): List<Pair<String, List<Int>>>? = retryTemplate(792) {
    connect(
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421eaff4b8b69336153301c9aa596522b20bc86e6e559a9b290/pk.classroomctrl.do?m=qyClassroomState&classroom=$classroom&weeknumber=$week",
        null,
        loggedInUser.vpnTicket
    ).inputStream.run {
        val reader = BufferedReader(InputStreamReader(this, "GBK"))
        var readLine: String?
        val stringBuilder = StringBuilder()
        while (reader.readLine().also { readLine = it } != null)
            stringBuilder.append(readLine).append('\n')

        fun mapClassName(element: Element) =
            with(element.classNames().apply { remove("colBound") }) {
                when (this.size) {
                    0 -> 5
                    1 ->
                        when (this.first()) {
                            "onteaching" -> 1
                            "onexam" -> 2
                            "onborrowed" -> 3
                            "ondisabled" -> 4
                            else -> 0
                        }
                    else -> 0
                }
            }

        Jsoup.parse(stringBuilder.toString()).getElementById("scrollContent").child(0).children()
            .flatMap { it.children() }
            .map { (it.child(0).textNodes()[1].text() to it.children().drop(1).map(::mapClassName)) }
    }
}

fun Network.getECard() = retryTemplate(824) {
    connect(
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421f5f4408e237e7c4377068ea48d546d303341e9882a/user/ExDetailsDown.do",
        null,
        loggedInUser.vpnTicket
    ).inputStream.run {
        val sheet = Workbook.getWorkbook(this).getSheet(0)
        val rowCount = sheet.rows
        val table = ECardRecord()
        for (i in rowCount - 2 downTo 1) {
            with(sheet.getRow(i)) {
                table.addElement(
                    this[1].contents,
                    this[2].contents,
                    this[4].contents,
                    this[5].contents.toDouble()
                )
            }
        }
        close()
        table
    }
}

fun Network.loseCard() = retryTemplate(824) {
    connect(
        "https://webvpn.tsinghua.edu.cn/http/77726476706e69737468656265737421f5f4408e237e7c4377068ea48d546d303341e9882a/user/RambleConsumeLog.do?losscard=true",
        null,
        loggedInUser.vpnTicket
    ).inputStream.run {
        val reader = BufferedReader(InputStreamReader(this))
        var readLine: String?
        while (reader.readLine().also { readLine = it } != null) {
            if (readLine!!.contains("var result")) {
                readLine = readLine!!.substring(readLine!!.indexOf('=') + 1).trim()
                return@run if (readLine == "null") null else readLine?.toInt()
            }
        }
        null
    }
}