package com.wavvy.app.core.playback.potoken

// JSON parsing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
// Byte utilities
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

// Helpers between the BotGuard answers and the JavaScript of the token page, adapted from Metrolist (GPL-3.0)

// The two tokens a stream needs, one for the player request and one for the stream itself
class PoTokenResult(
    val playerRequestPoToken: String,
    val streamingDataPoToken: String
)

// A token could not be made this time
class PoTokenException(message: String) : Exception(message)

// The WebView of the system cannot run the token page at all
class BadWebViewException(message: String) : Exception(message)

// A syntax error means the WebView itself is broken, anything else may pass on a new try
fun buildExceptionForJsError(error: String): Exception =
    if (error.contains("SyntaxError")) BadWebViewException(error) else PoTokenException(error)

// Challenge of the Create endpoint as an object the page can run
fun parseChallengeData(rawChallengeData: String): String {
    val scrambled = Json.parseToJsonElement(rawChallengeData).jsonArray

    val challengeData = if (scrambled.size > 1 && scrambled[1].jsonPrimitive.isString) {
        Json.parseToJsonElement(descramble(scrambled[1].jsonPrimitive.content)).jsonArray
    } else {
        scrambled[0].jsonArray
    }

    val safeScript = challengeData[1].takeIf { it !is JsonNull }?.jsonArray?.find { it.jsonPrimitive.isString }
    val trustedUrl = challengeData[2].takeIf { it !is JsonNull }?.jsonArray?.find { it.jsonPrimitive.isString }

    return Json.encodeToString(
        JsonObject.serializer(),
        JsonObject(
            mapOf(
                "messageId" to JsonPrimitive(challengeData[0].jsonPrimitive.content),
                "interpreterJavascript" to JsonObject(
                    mapOf(
                        "privateDoNotAccessOrElseSafeScriptWrappedValue" to (safeScript ?: JsonNull),
                        "privateDoNotAccessOrElseTrustedResourceUrlWrappedValue" to (trustedUrl ?: JsonNull)
                    )
                ),
                "interpreterHash" to JsonPrimitive(challengeData[3].jsonPrimitive.content),
                "program" to JsonPrimitive(challengeData[4].jsonPrimitive.content),
                "globalName" to JsonPrimitive(challengeData[5].jsonPrimitive.content),
                "clientExperimentsStateBlob" to JsonPrimitive(challengeData[7].jsonPrimitive.content)
            )
        )
    )
}

// Integrity token of the GenerateIT endpoint as a JavaScript array, and how many seconds it lasts
fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
    val integrityTokenData = Json.parseToJsonElement(rawIntegrityTokenData).jsonArray
    return base64ToU8(integrityTokenData[0].jsonPrimitive.content) to integrityTokenData[1].jsonPrimitive.long
}

// Text as a JavaScript array of bytes
fun stringToU8(identifier: String): String = newUint8Array(identifier.toByteArray())

// Bytes the page returns, separated by commas, as the base64 YouTube expects
fun u8ToBase64(poToken: String): String =
    poToken.split(",")
        .map { it.toUByte().toByte() }
        .toByteArray()
        .toByteString()
        .base64()
        .replace("+", "-")
        .replace("/", "_")

// Scrambled challenge decoded, each byte moved by the offset YouTube uses
private fun descramble(scrambledChallenge: String): String =
    base64ToByteString(scrambledChallenge)
        .map { (it + DescrambleOffset).toByte() }
        .toByteArray()
        .decodeToString()

// Base64 of YouTube as a JavaScript array of bytes
private fun base64ToU8(base64: String): String = newUint8Array(base64ToByteString(base64))

// Bytes written as a JavaScript array
private fun newUint8Array(contents: ByteArray): String =
    "new Uint8Array([" + contents.joinToString(separator = ",") { it.toUByte().toString() } + "])"

// Base64 of YouTube, which uses other symbols, decoded to bytes
private fun base64ToByteString(base64: String): ByteArray {
    val standard = base64.replace('-', '+').replace('_', '/').replace('.', '=')
    return (standard.decodeBase64() ?: throw PoTokenException("Cannot base64 decode")).toByteArray()
}

// Offset of each byte of a scrambled challenge
private const val DescrambleOffset = 97
