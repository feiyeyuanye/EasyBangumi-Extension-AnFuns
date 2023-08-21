package com.heyanle.easybangumi_extension.anfuns

import android.util.Log
import com.heyanle.bangumi_source_api.api.Source
import com.heyanle.bangumi_source_api.api.entity.CartoonCover
import com.heyanle.bangumi_source_api.api.entity.CartoonCoverImpl
import com.heyanle.lib_anim.utils.network.GET
import com.heyanle.lib_anim.utils.network.networkHelper
import org.jsoup.Jsoup
import org.jsoup.select.Elements

/**
 * Created by HeYanLe on 2023/2/25 16:26.
 * https://github.com/heyanLE
 */
suspend fun Source.listPage(
    element: Elements,
): Pair<Int?, List<CartoonCover>> {
    val r = arrayListOf<CartoonCover>()
    for (video in element){
        video.apply {
            val name = select("a").attr("title")
            val videoUrl = select("a").attr("href")
            val coverUrl = select("a").attr("data-original")
            val episode = select(".remarks").text()

            if (!name.isNullOrBlank() && !videoUrl.isNullOrBlank() && !coverUrl.isNullOrBlank()) {
                val b = CartoonCoverImpl(
                    id = "${key}-$videoUrl",
                    source = key,
                    url = videoUrl,
                    title = name,
                    intro = episode ?: "",
                    coverUrl = url(coverUrl)
                )
                r.add(b)
            }
        }
    }
    return Pair(null, r)
}

