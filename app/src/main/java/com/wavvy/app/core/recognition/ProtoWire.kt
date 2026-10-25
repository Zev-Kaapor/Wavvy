package com.wavvy.app.core.recognition

// Bytes
import java.io.ByteArrayOutputStream

// The kinds of value a field of protobuf is written as
private const val WireVarint = 0
private const val WireFixed64 = 1
private const val WireLength = 2
private const val WireFixed32 = 5
private const val VarintBits = 7
private const val VarintMask = 0x7F
private const val VarintMore = 0x80
private const val Fixed32Size = 4
private const val ByteMask = 0xFF
private const val Fixed64Size = 8

// Writes a message of protobuf by hand, only the kinds of field the sound search needs
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun varint(field: Int, value: Long) = apply {
        key(field, WireVarint)
        raw(value)
    }

    fun bytes(field: Int, value: ByteArray) = apply {
        key(field, WireLength)
        raw(value.size.toLong())
        out.write(value)
    }

    fun string(field: Int, value: String) = bytes(field, value.toByteArray())

    fun float32(field: Int, value: Float) = apply {
        key(field, WireFixed32)
        val bits = java.lang.Float.floatToIntBits(value)
        repeat(Fixed32Size) { out.write((bits ushr (it * Byte.SIZE_BITS)) and ByteMask) }
    }

    fun message(field: Int, build: ProtoWriter.() -> Unit) = bytes(field, ProtoWriter().apply(build).toByteArray())

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun key(field: Int, wire: Int) = raw(((field shl 3) or wire).toLong())

    private fun raw(value: Long) {
        var rest = value
        while (rest and VarintMask.toLong().inv() != 0L) {
            out.write(((rest and VarintMask.toLong()) or VarintMore.toLong()).toInt())
            rest = rest ushr VarintBits
        }
        out.write(rest.toInt())
    }
}

// A message of protobuf that was read, each field with the values it came with, numbers as Long and the rest as bytes
class ProtoMessage private constructor(private val fields: Map<Int, List<Any>>) {
    fun varint(field: Int): Long? = fields[field]?.firstOrNull { it is Long } as Long?

    fun bytes(field: Int): ByteArray? = fields[field]?.firstOrNull { it is ByteArray } as ByteArray?

    fun string(field: Int): String? = bytes(field)?.toString(Charsets.UTF_8)

    fun message(field: Int): ProtoMessage? = bytes(field)?.let(::parse)

    fun messages(field: Int): List<ProtoMessage> = fields[field].orEmpty().filterIsInstance<ByteArray>().mapNotNull(::parse)

    // Every message of a field at any depth inside, in the order they were written
    fun deepMessages(field: Int): List<ProtoMessage> = buildList {
        fields.forEach { (number, values) ->
            values.filterIsInstance<ByteArray>().forEach { value ->
                val inner = parse(value) ?: return@forEach
                if (number == field) add(inner)
                addAll(inner.deepMessages(field))
            }
        }
    }

    // Every field and what it holds, a message inside as its own lines, used to see what an answer brought
    fun describe(indent: String = ""): String = fields.entries.joinToString("\n") { (field, values) ->
        values.joinToString("\n") { value ->
            val inner = (value as? ByteArray)?.let(::parse)?.takeIf { value.size > 0 && !value.isText() }
            when {
                inner != null -> "$indent$field:\n${inner.describe("$indent  ")}"
                value is ByteArray -> "$indent$field: \"${value.toString(Charsets.UTF_8)}\""
                else -> "$indent$field: $value"
            }
        }
    }

    companion object {
        // The message read from its bytes, null when the bytes are not a message
        fun parse(data: ByteArray): ProtoMessage? {
            val fields = LinkedHashMap<Int, MutableList<Any>>()
            var position = 0

            fun readVarint(): Long {
                var result = 0L
                var shift = 0
                while (true) {
                    if (position >= data.size || shift > Long.SIZE_BITS) throw IllegalArgumentException()
                    val byte = data[position++].toInt()
                    result = result or ((byte and VarintMask).toLong() shl shift)
                    if (byte and VarintMore == 0) return result
                    shift += VarintBits
                }
            }

            return runCatching {
                while (position < data.size) {
                    val key = readVarint()
                    val field = (key ushr 3).toInt()
                    if (field == 0) throw IllegalArgumentException()
                    val value: Any = when ((key and 7).toInt()) {
                        WireVarint -> readVarint()
                        WireLength -> {
                            val length = readVarint().toInt()
                            if (length < 0 || position + length > data.size) throw IllegalArgumentException()
                            data.copyOfRange(position, position + length).also { position += length }
                        }
                        WireFixed32 -> data.copyOfRange(position, position + Fixed32Size).also { position += Fixed32Size }
                        WireFixed64 -> data.copyOfRange(position, position + Fixed64Size).also { position += Fixed64Size }
                        else -> throw IllegalArgumentException()
                    }
                    fields.getOrPut(field) { mutableListOf() } += value
                }
                ProtoMessage(fields)
            }.getOrNull()
        }

        // Bytes that read as plain text are shown as text and not opened as a message
        private fun ByteArray.isText(): Boolean = all { it >= ' '.code.toByte() || it == '\n'.code.toByte() } && size < 200
    }
}
