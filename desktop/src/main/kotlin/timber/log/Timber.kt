package timber.log

/**
 * Pulse desktop: the Android app logs through Timber, which exists only on Android. The shared
 * YouTube client calls just these, so they print to stderr when PULSE_DEBUG is set.
 */
object Timber {
    private val enabled = System.getenv("PULSE_DEBUG") != null

    class Tree internal constructor(private val tag: String?) {
        fun d(message: String, vararg args: Any?) = log("D", message, null)
        fun i(message: String, vararg args: Any?) = log("I", message, null)
        fun w(message: String, vararg args: Any?) = log("W", message, null)
        fun w(t: Throwable?, message: String, vararg args: Any?) = log("W", message, t)
        fun e(message: String, vararg args: Any?) = log("E", message, null)
        fun e(t: Throwable?, message: String, vararg args: Any?) = log("E", message, t)
        fun e(t: Throwable?) = log("E", "", t)

        private fun log(level: String, message: String, t: Throwable?) {
            if (!enabled) return
            System.err.println("$level/${tag ?: "Pulse"}: $message")
            t?.printStackTrace()
        }
    }

    private val default = Tree(null)

    fun tag(tag: String): Tree = Tree(tag)
    fun d(message: String, vararg args: Any?) = default.d(message)
    fun i(message: String, vararg args: Any?) = default.i(message)
    fun w(message: String, vararg args: Any?) = default.w(message)
    fun w(t: Throwable?, message: String, vararg args: Any?) = default.w(t, message)
    fun e(message: String, vararg args: Any?) = default.e(message)
    fun e(t: Throwable?, message: String, vararg args: Any?) = default.e(t, message)
    fun e(t: Throwable?) = default.e(t)
}
