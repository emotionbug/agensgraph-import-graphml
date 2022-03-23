package io.github.emotionbug.agtools

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import java.io.IOException

object JsonUtils {
    val OBJECT_MAPPER: ObjectMapper =
        JsonMapper.builder()
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .enable(DeserializationFeature.USE_LONG_FOR_INTS)
            .build()
    private val defaultJsonPathOptions = setOf(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS)

    fun <T> parse(json: String?, path: String?, type: Class<T>): T? {
        return if (json == null || json.isEmpty()) null else try {
            val jsonPathConf: Configuration =
                Configuration.builder().options(defaultJsonPathOptions).jsonProvider(JacksonJsonProvider(OBJECT_MAPPER))
                    .mappingProvider(JacksonMappingProvider(OBJECT_MAPPER)).build()
            if (path == null || path.isEmpty()) {
                OBJECT_MAPPER.readValue(json, type)
            } else JsonPath.parse(json, jsonPathConf).read(path, type)
        } catch (e: IOException) {
            throw RuntimeException("Can't convert " + json + " to " + type.simpleName + " with path " + path, e)
        }
    }
}