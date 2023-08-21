package com.heyanle.easybangumi_extension.anfuns

import com.heyanle.bangumi_source_api.api.component.Component
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import com.heyanle.lib_anim.utils.SourceUtils

/**
 * Created by HeYanLe on 2023/2/25 15:19.
 * https://github.com/heyanLE
 */

fun url(source: String): String {
    return SourceUtils.urlParser(AnFunsSource.ROOT_URL, source)
}
class AnFunsSource: ExtensionSource(), ExtensionIconSource {

    companion object {
        const val ROOT_URL = "https://www.anfuns.cc"
    }

    override fun getIconResourcesId(): Int? {
        return R.drawable.ic_launcher
    }

    override val describe: String?
        get() = null
    override val label: String
        get() = "AnFuns动漫"
    override val version: String
        get() = "1.0"
    override val versionCode: Int
        get() = 0

    override val sourceKey: String
        get() = "anfuns"

    /**
     * 拆分
     */
    override fun components(): List<Component> {
        return listOf(
            AnFunsSearchComponent(this),
            AnFunsPageComponent(this),
            AnFunsDetailedComponent(this),
        )
    }
}