package app.pulse.desktop

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.runBlocking

/** `./gradlew -p desktop probe -Pquery="..."`: search, resolve and decode a few seconds, no window. */
fun main(args: Array<String>) = runBlocking {
    val query = args.firstOrNull() ?: "Кино Спутник"
    YouTubeSetup.init()
    println("visitorData: ${if (YouTube.visitorData != null) "present" else "missing"}")
    // An 11-character argument is a video id: YouTube Music search answers data-centre addresses
    // with "No results", so the rest of the chain can still be checked from a server.
    val videoId = if (Regex("[A-Za-z0-9_-]{11}").matches(query)) query else {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow()
        println("items: ${result.items.size} ${result.items.take(3).map { it::class.simpleName + ":" + it.title }}")
        result.items.filterIsInstance<SongItem>().first().also { println("found: ${it.title} — ${it.artists.joinToString { a -> a.name }}") }.id
    }
    val stream = StreamResolver.resolve(videoId)
    println("stream: client=${stream.clientName} mime=${stream.mimeType} codecs=${stream.codecs} bytes=${stream.contentLengthBytes} chunks=${stream.useRangeChunks}/${stream.rangeChunkSizeBytes}")
    val url = java.net.URI(stream.audioUrl)
    val params = url.rawQuery.split("&").associate { it.substringBefore("=") to it.substringAfter("=") }
    println("host=${url.host} ip=${params["ip"]} c=${params["c"]} headers=${stream.headers.keys}")
    java.io.File("/tmp/claude-0/stream_url.txt").writeText(stream.audioUrl + "\n" + stream.headers.entries.joinToString("\n") { "${it.key}: ${it.value}" })
    val started = System.currentTimeMillis()
    val file = AudioDownloader.download(videoId, stream) { _, _ -> }
    println("download took ${System.currentTimeMillis() - started} ms")
    println("downloaded: ${file.length()} bytes")
    val seconds = Decoder.decodeSeconds(file, 5.0)
    println("decoded: ${"%.1f".format(seconds)} s of 48 kHz stereo audio")
}
