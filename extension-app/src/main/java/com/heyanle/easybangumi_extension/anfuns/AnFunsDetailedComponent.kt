package com.heyanle.easybangumi_extension.anfuns

import android.util.Log
import com.heyanle.bangumi_source_api.api.ParserException
import com.heyanle.bangumi_source_api.api.SourceResult
import com.heyanle.bangumi_source_api.api.component.ComponentWrapper
import com.heyanle.bangumi_source_api.api.component.detailed.DetailedComponent
import com.heyanle.bangumi_source_api.api.component.play.PlayComponent
import com.heyanle.bangumi_source_api.api.component.update.UpdateComponent
import com.heyanle.bangumi_source_api.api.entity.Cartoon
import com.heyanle.bangumi_source_api.api.entity.CartoonImpl
import com.heyanle.bangumi_source_api.api.entity.CartoonSummary
import com.heyanle.bangumi_source_api.api.entity.PlayLine
import com.heyanle.bangumi_source_api.api.entity.PlayerInfo
import com.heyanle.bangumi_source_api.api.withResult
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import com.heyanle.lib_anim.utils.network.webview_helper.WebViewHelperImpl
import com.heyanle.lib_anim.utils.network.webview_helper.webViewHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Created by HeYanLe on 2023/3/4 14:39.
 * https://github.com/heyanLE
 */
class AnFunsDetailedComponent(
    source: AnFunsSource
) : ComponentWrapper(source), DetailedComponent, UpdateComponent, PlayComponent {
    private val playUrlTemp: MutableList<MutableList<String>> = ArrayList()

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {
            detailed(getDoc(summary), summary)
        }
    }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> {
        return withResult(Dispatchers.IO) {
            playLine(getDoc(summary), summary)
        }
    }

    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> {
        return withResult(Dispatchers.IO) {
            detailed(getDoc(summary), summary) to playLine(getDoc(summary), summary)
        }
    }

    override suspend fun update(
        cartoon: Cartoon,
        oldPlayLine: List<PlayLine>
    ): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {

            when (val n = getAll(CartoonSummary(cartoon.id, cartoon.source, cartoon.url))) {
                is SourceResult.Complete -> {
                    n.data.first.apply {

                        val newPlayLine = n.data.second

                        if (oldPlayLine.size != newPlayLine.size) {
                            isUpdate = true
                        } else {
                            isUpdate = false
                            for (i in oldPlayLine.indices) {
                                if (oldPlayLine[i].episode.size != newPlayLine[i].episode.size) {
                                    isUpdate = true
                                    break
                                }
                            }
                        }
                    }
                }
                is SourceResult.Error -> {
                    throw n.throwable
                }
            }
        }
    }

    private fun getDoc(summary: CartoonSummary): Document {
        val d = networkHelper.cloudflareUserClient.newCall(GET(url(summary.url)))
            .execute().body?.string() ?: throw NullPointerException()
        return Jsoup.parse(d)
    }
    private fun playLine(document: Document, summary: CartoonSummary): List<PlayLine> {
        Log.e("TAG","------->>>>>>>playLine")
        if (playUrlTemp.size>0) playUrlTemp.clear()
        val res = arrayListOf<PlayLine>()
        val module = document.select(".hl-play-source").first() ?: return res
        val playNameList = module.select(".hl-plays-wrap").first()?.select("a") ?: return res
        val playEpisodeList = module.select(".hl-tabs-box")
        for (index in 0..playNameList.size) {
            val playName = playNameList.getOrNull(index)?.text()
            val playEpisode = playEpisodeList.getOrNull(index)
            if (playName != null && playEpisode != null) {
                val results = playEpisode.select("li").select("a")
                val es = arrayListOf<String>()
                val dataApiList: MutableList<String> = ArrayList()
                for (i in results.indices) {
                    es.add(results[i].text())  // title
                    dataApiList.add(results[i].attr("href"))  // url
                }
                val playLine = PlayLine(
                    id = index.toString(),
                    label = playName,
                    episode = es
                )
                playUrlTemp.add(dataApiList)
                res.add(playLine)
            }
        }
        return res
    }

    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episodeIndex: Int
    ): SourceResult<PlayerInfo> {
        Log.e("TAG","------->>>>>>>开始播放")
        return withResult(Dispatchers.IO) {
            if (episodeIndex < 0) {
                throw IndexOutOfBoundsException()
            }
//            Log.e("TAG","${playUrlTemp[playLine.id.toInt()]}") // [/play/632-1-1.html]
            val url = url(playUrlTemp[playLine.id.toInt()][episodeIndex])
//            Log.e("TAG", url) // https://www.anfuns.cc/play/632-1-1.html
            var videoUrl = withTimeoutOrNull(10 * 1000) {
                webViewHelper.interceptResource(
                    url, ".*\\b(mp4|m3u8)\\b.*",
                )
            } ?: ""
            Log.e("TAG", "地址：$videoUrl")
            if (videoUrl.isNotEmpty()) {
                when {
                    videoUrl.contains(".m3u8&") -> videoUrl = videoUrl.substringAfter("url=")
                        .substringBefore("&")
                    videoUrl.contains(".mp4") -> videoUrl = videoUrl.substringAfter("url=")
                        .substringBefore("&next=")
                }
                Log.e("TAG", "解析后url：$videoUrl")
                if (videoUrl.indexOf(".mp4") != -1){
                    PlayerInfo(
                        decodeType = PlayerInfo.DECODE_TYPE_OTHER,
                        uri = url(videoUrl)
                    )
                }else{
                    PlayerInfo(
                        decodeType = PlayerInfo.DECODE_TYPE_HLS,
                        uri = url(videoUrl)
                    )
                }
            }else{
                throw ParserException("Unknown")
            }
        }
    }
    private fun detailed(document: Document, summary: CartoonSummary): Cartoon {
        Log.e("TAG","------->>>>>>>detailed")

        var desc = ""
        var update = 0
        var status = 0

        val cover = document.select(".hl-dc-pic").select("span").attr("data-original")
        val title = document.select(".hl-dc-headwrap").select(".hl-dc-title").text() + "\n" +
                document.select(".hl-dc-headwrap").select(".hl-dc-sub").text()
        // 更新状况
        val upStateItems = document.select(".hl-dc-content")
            .select(".hl-vod-data").select(".hl-full-box").select("ul").select("li")
        for (upStateEm in upStateItems){
            val t = upStateEm.text()
            when{
                t.contains("状态：") -> {
                    status =
                        if (t.startsWith("连载")) Cartoon.STATUS_ONGOING
                        else if (t.startsWith("全")) Cartoon.STATUS_COMPLETED
                        else Cartoon.STATUS_UNKNOWN
                    val isTheater = title.contains("剧场版")
                    update =
                        if (isTheater) {
                            if (status == Cartoon.STATUS_COMPLETED) {
                                Cartoon.UPDATE_STRATEGY_NEVER
                            } else {
                                Cartoon.UPDATE_STRATEGY_ONLY_STRICT
                            }
                        } else {
                            if (status == Cartoon.STATUS_COMPLETED) {
                                Cartoon.UPDATE_STRATEGY_ONLY_STRICT
                            } else {
                                Cartoon.UPDATE_STRATEGY_ALWAYS
                            }
                        }
                }
                t.contains("简介：") -> desc = t
            }
        }

        return CartoonImpl(
            id = summary.id,
            url = summary.url,
            source = summary.source,

            title = title,
            coverUrl = cover,

            intro = "",
            description = desc,

            genre = "",

            status = status,
            updateStrategy = update,
        )
    }
}