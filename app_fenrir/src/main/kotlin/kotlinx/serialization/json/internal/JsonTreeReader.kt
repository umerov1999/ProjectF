/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.internal.lexer.AbstractJsonLexer
import kotlinx.serialization.json.internal.lexer.NULL
import kotlinx.serialization.json.internal.lexer.TC_BEGIN_LIST
import kotlinx.serialization.json.internal.lexer.TC_BEGIN_OBJ
import kotlinx.serialization.json.internal.lexer.TC_COLON
import kotlinx.serialization.json.internal.lexer.TC_COMMA
import kotlinx.serialization.json.internal.lexer.TC_END_LIST
import kotlinx.serialization.json.internal.lexer.TC_END_OBJ
import kotlinx.serialization.json.internal.lexer.TC_OTHER
import kotlinx.serialization.json.internal.lexer.TC_STRING
import kotlinx.serialization.json.internal.lexer.tokenDescription

@OptIn(ExperimentalSerializationApi::class)
internal class JsonTreeReader(
    configuration: JsonConfiguration,
    private val lexer: AbstractJsonLexer
) {
    private val isLenient = configuration.isLenient
    private val trailingCommaAllowed = configuration.allowTrailingComma
    private var stackDepth = 0

    private fun readObject(): JsonElement = readObjectImpl {
        read()
    }

    private suspend fun DeepRecursiveScope<Unit, JsonElement>.readObject(): JsonElement =
        readObjectImpl { callRecursive(Unit) }

    private inline fun readObjectImpl(reader: () -> JsonElement): JsonObject {
        var lastToken = lexer.consumeNextToken(TC_BEGIN_OBJ)
        if (lexer.peekNextToken() == TC_COMMA) lexer.fail("Unexpected leading comma")
        val result = linkedMapOf<String, JsonElement>()
        while (lexer.canConsumeValue()) {
            // Read key and value
            val key = if (isLenient) lexer.consumeStringLenient() else lexer.consumeString()
            lexer.consumeNextToken(TC_COLON)
            val element = reader()
            result[key] = element
            // Verify the next token
            lastToken = lexer.consumeNextToken()
            when (lastToken) {
                TC_COMMA -> Unit // no-op, can continue with `canConsumeValue` that verifies the token after comma
                TC_END_OBJ -> break // `canConsumeValue` can return incorrect result, since it checks token _after_ TC_END_OBJ
                else -> lexer.fail("Expected end of the object or comma")
            }
        }
        // Check for the correct ending
        if (lastToken == TC_BEGIN_OBJ) { // Case of empty object
            lexer.consumeNextToken(TC_END_OBJ)
        } else if (lastToken == TC_COMMA) { // Trailing comma
            if (!trailingCommaAllowed) lexer.invalidTrailingComma()
            lexer.consumeNextToken(TC_END_OBJ)
        } // else unexpected token?
        return JsonObject(result)
    }

    private fun readArray(): JsonElement {
        var lastToken = lexer.consumeNextToken()
        // Prohibit leading comma
        if (lexer.peekNextToken() == TC_COMMA) lexer.fail("Unexpected leading comma")
        val result = arrayListOf<JsonElement>()
        while (lexer.canConsumeValue()) {
            val element = read()
            result.add(element)
            lastToken = lexer.consumeNextToken()
            if (lastToken != TC_COMMA) {
                lexer.require(lastToken == TC_END_LIST) { "Expected end of the array or comma" }
            }
        }
        // Check for the correct ending
        if (lastToken == TC_BEGIN_LIST) { // Case of empty object
            lexer.consumeNextToken(TC_END_LIST)
        } else if (lastToken == TC_COMMA) { // Trailing comma
            if (!trailingCommaAllowed) lexer.invalidTrailingComma("array")
            lexer.consumeNextToken(TC_END_LIST)
        }
        return JsonArray(result)
    }

    private fun readValue(isString: Boolean): JsonPrimitive {
        val string = if (isLenient || !isString) {
            lexer.consumeStringLenient()
        } else {
            lexer.consumeString()
        }
        if (!isString && string == NULL) return JsonNull
        return JsonLiteral(string, isString)
    }

    fun read(): JsonElement {
        return when (val token = lexer.peekNextToken()) {
            TC_STRING -> readValue(isString = true)
            TC_OTHER -> readValue(isString = false)
            TC_BEGIN_OBJ -> {
                /*
                 * If the object has the depth of 200 (an arbitrary "good enough" constant), it means
                 * that it's time to switch to stackless recursion to avoid StackOverflowError.
                 * This case is quite rare and specific, so more complex nestings (e.g. through
                 * the chain of JsonArray and JsonElement) are not supported.
                 */
                val result = if (++stackDepth == 200) {
                    readDeepRecursive()
                } else {
                    readObject()
                }
                --stackDepth
                result
            }

            TC_BEGIN_LIST -> readArray()
            else -> lexer.fail(
                "Cannot read Json element because of unexpected ${
                    tokenDescription(
                        token
                    )
                }"
            )
        }
    }

    private fun readDeepRecursive(): JsonElement = DeepRecursiveFunction {
        when (lexer.peekNextToken()) {
            TC_STRING -> readValue(isString = true)
            TC_OTHER -> readValue(isString = false)
            TC_BEGIN_OBJ -> readObject()
            TC_BEGIN_LIST -> readArray()
            else -> lexer.fail("Can't begin reading element, unexpected token")
        }
    }.invoke(Unit)
}
