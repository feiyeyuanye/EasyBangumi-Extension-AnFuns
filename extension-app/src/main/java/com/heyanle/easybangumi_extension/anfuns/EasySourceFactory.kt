package com.heyanle.easybangumi_extension.anfuns

import com.heyanle.bangumi_source_api.api.Source
import com.heyanle.bangumi_source_api.api.SourceFactory
import com.heyanle.easybangumi_extension.anfuns.AnFunsSource

/**
 * Created by HeYanLe on 2023/2/19 23:23.
 * https://github.com/heyanLE
 */
class EasySourceFactory: SourceFactory {

    override fun create(): List<Source> {
        return listOf(
            // 添加你的源
            AnFunsSource()
        )
    }
}