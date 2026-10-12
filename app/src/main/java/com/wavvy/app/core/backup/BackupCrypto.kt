package com.wavvy.app.core.backup

// Files and bytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
// Encryption
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// Why a backup could not be opened
sealed class BackupFailure : Exception() {
    // The file is not one made by this app, or it is cut
    data object NotABackup : BackupFailure()

    // The file was made by a newer version of the app
    data object TooNew : BackupFailure()

    // The password is not the one of the file, or the file was changed after it was made
    data object WrongPassword : BackupFailure()
}

// The signature at the start of every backup file, the version of its layout, and whether the rest is locked or only compressed
private val Magic = "WAVVYBK".toByteArray(Charsets.US_ASCII)
private const val Version: Byte = 1
private const val ModePlain: Byte = 0
private const val ModeLocked: Byte = 1

// The sizes of the parts of the file, in bytes, and of the key in bits
private const val SaltLength = 16
private const val IvLength = 12
private const val KeyBits = 256
private const val TagBits = 128

// How many times the password is mixed to make the key, the file keeps the number it was made with, and the limits that a file may ask for
private const val Iterations = 600_000
private const val MinIterations = 100_000
private const val MaxIterations = 5_000_000

private const val KeyAlgorithm = "PBKDF2WithHmacSHA256"
private const val CipherName = "AES/GCM/NoPadding"
private const val KeyType = "AES"

// Makes the backup file, locked with a password when there is one, AES-256 in the GCM mode with a key made by PBKDF2, so the file can only be opened with the password and shows a change made to it
// Without a password the file is only compressed, anyone who has it can read it
// The start of the file is the signature, the version and the mode, a locked file goes on with the number of times the password is mixed, the salt and the start of the cipher
object BackupCrypto {
    private val random = SecureRandom()
    private val baseSize = Magic.size + 2

    // True when the file needs a password to be opened, and an error when it is not a backup of this app or is from a newer version
    fun isLocked(file: ByteArray): Boolean {
        if (file.size <= baseSize || !file.copyOfRange(0, Magic.size).contentEquals(Magic)) throw BackupFailure.NotABackup
        if (file[Magic.size] > Version) throw BackupFailure.TooNew

        return when (file[Magic.size + 1]) {
            ModeLocked -> true
            ModePlain -> false
            else -> throw BackupFailure.NotABackup
        }
    }

    // A password that is empty or missing leaves the file without a lock
    fun encrypt(plain: ByteArray, password: CharArray?): ByteArray {
        val data = compress(plain)
        if (password == null || password.isEmpty()) return ByteBuffer.allocate(baseSize + data.size).put(Magic).put(Version).put(ModePlain).put(data).array()

        val salt = ByteArray(SaltLength).also(random::nextBytes)
        val iv = ByteArray(IvLength).also(random::nextBytes)
        val header = ByteBuffer.allocate(baseSize + Int.SIZE_BYTES + SaltLength + IvLength)
            .put(Magic).put(Version).put(ModeLocked).putInt(Iterations).put(salt).put(iv)
            .array()

        val cipher = Cipher.getInstance(CipherName)
        cipher.init(Cipher.ENCRYPT_MODE, keyOf(password, salt, Iterations), GCMParameterSpec(TagBits, iv))
        // The header is not hidden, but any change to it makes the file fail to open
        cipher.updateAAD(header)

        return header + cipher.doFinal(data)
    }

    fun decrypt(file: ByteArray, password: CharArray?): ByteArray {
        if (!isLocked(file)) {
            return try {
                decompress(file.copyOfRange(baseSize, file.size))
            } catch (failure: IOException) {
                throw BackupFailure.NotABackup
            }
        }

        val headerSize = baseSize + Int.SIZE_BYTES + SaltLength + IvLength
        if (file.size <= headerSize + TagBits / Byte.SIZE_BITS) throw BackupFailure.NotABackup
        if (password == null || password.isEmpty()) throw BackupFailure.WrongPassword

        val buffer = ByteBuffer.wrap(file, baseSize, headerSize - baseSize)
        val iterations = buffer.getInt()
        if (iterations !in MinIterations..MaxIterations) throw BackupFailure.NotABackup
        val salt = ByteArray(SaltLength).also(buffer::get)
        val iv = ByteArray(IvLength).also(buffer::get)

        return try {
            val cipher = Cipher.getInstance(CipherName)
            cipher.init(Cipher.DECRYPT_MODE, keyOf(password, salt, iterations), GCMParameterSpec(TagBits, iv))
            cipher.updateAAD(file, 0, headerSize)
            decompress(cipher.doFinal(file, headerSize, file.size - headerSize))
        } catch (failure: AEADBadTagException) {
            throw BackupFailure.WrongPassword
        } catch (failure: GeneralSecurityException) {
            throw BackupFailure.NotABackup
        } catch (failure: IOException) {
            throw BackupFailure.NotABackup
        }
    }

    // The key that the password makes with the salt
    private fun keyOf(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KeyBits)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance(KeyAlgorithm).generateSecret(spec).encoded, KeyType)
        } finally {
            spec.clearPassword()
        }
    }

    private fun compress(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(data) }
        return output.toByteArray()
    }

    private fun decompress(data: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
}
