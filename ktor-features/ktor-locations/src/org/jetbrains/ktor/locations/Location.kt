package org.jetbrains.ktor.locations

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.pipeline.*
import org.jetbrains.ktor.request.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.util.*
import kotlin.reflect.*

annotation class location(val path: String)

fun PipelineContext<Unit>.locations() = call.application.feature(Locations)

inline fun <reified T : Any> Route.location(noinline body: Route.() -> Unit): Route {
    return location(T::class, body)
}

inline fun <reified T : Any> Route.get(noinline body: suspend PipelineContext<Unit>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Get) {
            handle(body)
        }
    }
}

inline fun <reified T : Any> Route.post(noinline body: suspend PipelineContext<Unit>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Post) {
            handle {
                val formPostData = call.tryReceive<ValuesMap>() ?: ValuesMap.Empty
                body(this, locations().resolve(T::class, call.parameters + formPostData))
            }
        }
    }
}

fun <T : Any> Route.location(data: KClass<T>, body: Route.() -> Unit): Route {
    val entry = application.feature(Locations).createEntry(this, data)
    return entry.apply(body)
}

inline fun <reified T : Any> Route.handle(noinline body: suspend PipelineContext<Unit>.(T) -> Unit) {
    return handle(T::class, body)
}

fun <T : Any> Route.handle(dataClass: KClass<T>, body: suspend PipelineContext<Unit>.(T) -> Unit) {
    handle {
        val location = locations().resolve<T>(dataClass, call)
        body(location)
    }
}
