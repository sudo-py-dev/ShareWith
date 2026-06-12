package com.share.with

import io.ktor.server.application.call
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun testKtor3() {
    embeddedServer(Netty, configure = {
        connector { port = 8080 }
    }) {
        routing {
            get("/") {
                val ip = call.request.local.remoteHost
                call.respondText(ip)
            }
        }
    }.start(wait = false)
}
