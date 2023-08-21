package com.heyanle.easybangumi_extension.anfuns

import android.util.Log
import com.heyanle.bangumi_source_api.api.SourceResult
import com.heyanle.bangumi_source_api.api.component.ComponentWrapper
import com.heyanle.bangumi_source_api.api.component.search.SearchComponent
import com.heyanle.bangumi_source_api.api.entity.CartoonCover
import com.heyanle.bangumi_source_api.api.entity.CartoonCoverImpl
import com.heyanle.bangumi_source_api.api.withResult
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import org.jsoup.select.Elements

/**
 * Created by HeYanLe on 2023/2/25 16:25.
 * https://github.com/heyanLE
 */

class AnFunsSearchComponent(source: AnFunsSource) : ComponentWrapper(source), SearchComponent {
    override fun getFirstSearchKey(keyword: String): Int {
        return 0
    }

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> {
        return withResult(Dispatchers.IO) {
            val url = url("/search/page/${pageKey+1}/wd/${keyword}.html")
            Log.e("TAG","--->${url}")
            val d = networkHelper.cloudflareUserClient.newCall(GET(url(url))).execute().body?.string()!!
            val doc = Jsoup.parse(d)
            val r = arrayListOf<CartoonCover>()
            val lpic = doc.select("#conch-content").select(".row").select("ul")[0]
            val results: Elements = lpic.select("li")
            if (results.size == 0) {
                Log.e("TAG","已经加载完毕～")
                return@withResult Pair(null, r)
            }
            for (i in results.indices) {
                var cover = results[i].select("a").attr("data-original")
                if (cover.startsWith("//")) {
                    cover = "https:${cover}"
                }
                val title = results[i].select("a").attr("title")
                val itemUrl = results[i].select("a").attr("href")
                val episode = results[i].select(".hl-pic-text").select("span").text()
                val describe = results[i].select("p[class='hl-item-sub hl-text-muted hl-lc-2']").text()

                val b = CartoonCoverImpl(
                    id = "${this@AnFunsSearchComponent.source.key}-$itemUrl",
                    title = title,
                    url = itemUrl,
                    intro = episode,
                    coverUrl = url(cover),
                    source = this@AnFunsSearchComponent.source.key,
                )
                r.add(b)
            }
            val pages = doc.select(".hl-list-wrap").select(".hl-page-wrap").select("li")
            return@withResult if (pages.isEmpty()) {
                Pair(null, r)
            } else {
                var hasNext = false
                for (p in pages) {
                    if (p.text() == (pageKey + 2).toString() || p.text() == "下一页") {
                        hasNext = true
                        break
                    }
                }
                if (!hasNext) {
                    Pair(null, r)
                } else {
                    Pair(pageKey + 1, r)
                }
            }
        }
    }
}