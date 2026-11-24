package com.comixon.editor.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies

/** The caller owns the client and closes it when its editor session ends. */
fun createEditorHttpClient(): HttpClient =
    HttpClient {
        install(HttpCookies)
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000
            requestTimeoutMillis = 300_000
            socketTimeoutMillis = 300_000
        }
    }
