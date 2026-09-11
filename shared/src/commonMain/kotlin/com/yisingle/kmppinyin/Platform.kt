package com.yisingle.kmppinyin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform