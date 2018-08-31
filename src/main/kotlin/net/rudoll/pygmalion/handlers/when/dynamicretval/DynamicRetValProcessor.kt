package net.rudoll.pygmalion.handlers.`when`.dynamicretval

import com.google.gson.Gson
import spark.Request
import java.util.regex.Pattern
import javax.script.ScriptContext
import javax.script.ScriptEngineManager

class DynamicRetValProcessor {
    private val EXPRESSION_REGEX = "\\\$\\{(.+)\\}"
    private val EXPRESSION_PATTERN = Pattern.compile(EXPRESSION_REGEX)
    private val retValCounter = RetValCounter()
    private val gson = Gson()

    fun process(pattern: String, request: Request): String {
        return try {
            val bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE)
            bindings["counter"] = retValCounter.call()
            bindings["timestamp"] = System.currentTimeMillis().toString()
            bindings["body"] = request.body()
            bindings["headers"] = gson.toJson(getHeaderMap(request))
            bindings["queryParams"] = gson.toJson(getQueryParamMap(request))
            bindings["cookies"] = gson.toJson(request.cookies())
            bindings["uri"] = request.uri()

            val matcher = EXPRESSION_PATTERN.matcher(pattern)
            val processed = StringBuffer()
            while (matcher.find()) {
                val key = matcher.group(1) //binding
                val value = engine.eval(key, bindings)
                matcher.appendReplacement(processed, value?.toString() ?: "")
            }
            matcher.appendTail(processed)
            processed.toString()
        } catch (e: Exception) {
            pattern
        }
    }

    private fun getQueryParamMap(request: Request): Map<String, String> {
        val queryParams = request.queryParams()
        val queryParamMap = mutableMapOf<String, String>()
        queryParams.forEach { queryParamMap[it] = request.queryParams(it) }
        return queryParamMap.toMap()
    }

    private fun getHeaderMap(request: Request): Map<String, String> {
        val headers = request.headers()
        val headerMap = mutableMapOf<String, String>()
        headers.forEach { headerMap[it] = request.headers(it) }
        return headerMap.toMap()
    }

    companion object {
        private val engine = ScriptEngineManager().getEngineByName("nashorn")
    }
}