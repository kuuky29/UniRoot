package com.uniroot.app.newmethod

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Random
import java.util.zip.Deflater

class DfKsudUpdaterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun profileWithTheSameNameAndDifferentBytesMustBeReplaced() {
        val stored = temporaryFolder.newFile("3.4.2-33355")
        stored.writeBytes(byteArrayOf(1, 2, 3))

        // Same profile ID and length, but a different bundled module build.
        assertFalse(DfKsudUpdater.profileMatches(stored, byteArrayOf(1, 2, 4)))
    }

    @Test
    fun byteIdenticalProfileIsAlreadyCurrent() {
        val patched = byteArrayOf(1, 2, 3)
        val stored = temporaryFolder.newFile("3.4.2-33355")
        stored.writeBytes(patched)

        assertTrue(DfKsudUpdater.profileMatches(stored, patched.copyOf()))
    }

    @Test
    fun missingProfileIsNotCurrent() {
        val missing = File(temporaryFolder.root, "3.4.2-33355")

        assertFalse(DfKsudUpdater.profileMatches(missing, byteArrayOf(1, 2, 3)))
    }

    @Test
    fun onlyReleasesMatchingTheBundledNextModulesAreAccepted() {
        DfKsudUpdater.requireMatchingNextModules("3.4.2")
        DfKsudUpdater.requireMatchingNextModules("v3.4.2")

        val error = assertThrows(IllegalStateException::class.java) {
            DfKsudUpdater.requireMatchingNextModules("v3.4.3")
        }
        assertTrue("The error must identify the requested release", error.message.orEmpty().contains("3.4.3"))
        assertTrue("The error must identify the available module release", error.message.orEmpty().contains("3.4.2"))
    }

    @Test
    fun preservesGuardWithTheSameVermagicAsKernelSu() {
        val kmi = "android13-5.15"
        val vermagic = "5.15.202-android13-5.15.202_r00-dirty SMP preempt mod_unload modversions aarch64"
        val kernelSu = module("kernelsu", vermagic, 1)
        val guard = module("susfs_guard_lkm", vermagic, 2)
        val helper = module("helper", "", 3)
        val replacement = module("kernelsu", vermagic)
        val input = fixture(kernelSu, guard, helper)
        val original = input.copyOf()
        val before = scan(input)
        assertEquals(3, before.size)

        val output = DfKsudUpdater.patchAssets(
            input, mapOf(kmi to replacement), mapOf(sha256(kernelSu) to kmi), { _, _ -> }
        )

        assertArrayEquals("The downloaded input must not be mutated", original, input)
        assertEquals(input.size, output.size)
        val after = scan(output).associateBy { it.start }
        assertEquals(before.size, after.size)
        val target = before.single { it.payload.contentEquals(kernelSu) }
        assertArrayEquals(replacement, after.getValue(target.start).payload)
        before.filter { it.start != target.start }.forEach {
            assertArrayEquals("Unrelated embedded ELF at ${it.start}", it.payload, after.getValue(it.start).payload)
        }
        assertArrayEquals(input.copyOfRange(0, target.start), output.copyOfRange(0, target.start))
        val end = target.start + target.consumed
        assertArrayEquals(input.copyOfRange(end, input.size), output.copyOfRange(end, output.size))
    }

    @Test
    fun strictPatchingRejectsMissingModuleSlotsBeforeInjectingAnyModule() {
        val presentKmi = "android13-5.15"
        val missingKmi = "android15-6.6"
        val vermagic = "5.15.202-android13 SMP preempt mod_unload modversions aarch64"
        val kernelSu = module("kernelsu", vermagic, 7)
        val input = fixture(kernelSu)
        val original = input.copyOf()
        val progress = mutableListOf<Int>()

        val error = assertThrows(IllegalStateException::class.java) {
            DfKsudUpdater.patchAssets(
                input,
                mapOf(
                    presentKmi to module("kernelsu", vermagic),
                    missingKmi to module("kernelsu", "6.6.77 SMP preempt mod_unload modversions aarch64")
                ),
                mapOf(sha256(kernelSu) to presentKmi),
                { percent, _ -> progress.add(percent) },
                requireAllModules = true
            )
        }

        assertTrue(error.message.orEmpty().contains("Missing Samsung module slots"))
        assertTrue("The error must identify the missing KMI", error.message.orEmpty().contains(missingKmi))
        assertFalse("No injection stage may begin for an incomplete module set", 75 in progress)
        assertArrayEquals("A rejected update must leave the downloaded binary intact", original, input)
    }

    @Test
    fun exactReferenceHashTakesPriorityOverVermagic() {
        val raw = module("kernelsu", "6.6.77-android15 SMP preempt mod_unload modversions aarch64", 4)
        val classified = scan(fixture(raw), mapOf(sha256(raw) to "android13-5.15")).single()

        assertEquals("ko", classified.kind)
        assertEquals("android13-5.15", classified.kmi)
        assertEquals("reference", classified.how)
    }

    @Test
    fun otherModuleNamesCannotBecomeKernelSuSlotsEvenWithReferenceHashes() {
        val vermagic = "6.6.77-android15 SMP preempt mod_unload modversions aarch64"
        val guard = module("susfs_guard_lkm", vermagic, 5)
        val similarName = module("kernelsu_helper", vermagic, 6)
        val references = listOf(guard, similarName).associate { sha256(it) to "android15-6.6" }
        val classified = scan(fixture(guard, similarName), references)

        assertEquals(2, classified.size)
        classified.forEach {
            assertEquals("other-ko", it.kind)
            assertEquals("", it.kmi)
        }
    }

    /**
     * Offline integration test against the official v3.4.2 download. Enable with
     * KSUD_TEST_FIXTURE=/absolute/path/to/ksud-v3.4.2; never executes the binary.
     */
    @Test
    fun next342ReplacesAllEightKernelSuModulesAndPreservesTwelveOtherAssets() {
        val path = System.getenv("KSUD_TEST_FIXTURE")
        assumeTrue("Set KSUD_TEST_FIXTURE to run the official v3.4.2 binary regression", !path.isNullOrBlank())
        val fixtureFile = File(requireNotNull(path))
        assertTrue("Fixture does not exist: $fixtureFile", fixtureFile.isFile)
        val input = fixtureFile.readBytes()
        assertEquals("2771a2ae989b522b5d70f3288ad5a2f379488962d09c1fe11e13a745eba89362", sha256(input))

        // SHA-256 digests of the eight official aarch64_*_kernelsu.ko
        // assets published with KernelSU-Next v3.4.2, independently of vermagic.
        val referenceHashes = mapOf(
            "9978f9f6b791d9cae8ccab204b4084bb5f5cda5e7641c01809429a93f046a970" to "android12-5.10",
            "e29592cb7196869662df503b116e4efc48c92b0cdf8cb0dfe21e0567e59254c1" to "android13-5.10",
            "d58d71d00e1ff49998a9c6d690fa2d8aa9567959ecff399a1b9ccc20199e68f0" to "android13-5.15",
            "fac9464f54ebca4d1d7fd1ee3a4b8744bcb1965759dc1d7b68346ae9c85c9dd2" to "android14-5.15",
            "961d2ef0e135a71d2620f10ebdc6932ff5b43069c5cf7da595fba5ab666c4b72" to "android14-6.1",
            "8d610b3070d4aa6fb116008c2e2caf4e3972aa73744d9106dc257a50e9bda66d" to "android15-6.6",
            "67c5d9e9026c52ff4c9b2e681aa81d1884160282ecef71bfa72f53e7a8c211e7" to "android16-6.12",
            "71931508be33ae4a0c24e5d727299835bea5add595020adf8e19b87fd223f593" to "android17-6.18"
        )
        val before = scan(input, referenceHashes)
        assertEquals(20, before.size)
        assertEquals(8, before.count { it.kind == "ko" })
        assertEquals(8, before.count { it.kind == "other-ko" })
        assertEquals(referenceHashes.values.toSet(), before.filter { it.kind == "ko" }.map { it.kmi }.toSet())

        val assets = listOf(File("src/main/assets/df/samsung-ko"), File("app/src/main/assets/df/samsung-ko"))
            .firstOrNull { it.isDirectory } ?: error("Cannot find bundled Samsung module assets")
        val replacements = referenceHashes.values.associateWith { File(assets, "${it}_kernelsu.ko").readBytes() }
        replacements.forEach { (kmi, payload) ->
            val strings = payload.toString(Charsets.ISO_8859_1)
            assertTrue("$kmi must contain the exact v3.4.2 version tag", strings.contains("\u0000v3.4.2\u0000"))
            assertTrue("$kmi must retain Samsung DEFEX support", strings.contains("\u0000ksu_samsung_defex_init\u0000"))
        }
        val inputHash = sha256(input)
        val output = DfKsudUpdater.patchAssets(input, replacements, referenceHashes, { _, _ -> })
        assertEquals(inputHash, sha256(input))
        assertEquals(input.size, output.size)
        val after = scan(output).associateBy { it.start }
        assertEquals(20, after.size)

        var changed = 0
        before.forEach { embedded ->
            val result = after.getValue(embedded.start)
            if (embedded.kind == "ko") {
                assertArrayEquals(embedded.kmi, replacements.getValue(embedded.kmi), result.payload)
                assertFalse("Expected replacement for ${embedded.kmi}", embedded.payload.contentEquals(result.payload))
                changed++
            } else {
                assertArrayEquals("Preserved asset at ${embedded.start}", embedded.payload, result.payload)
                val end = embedded.start + embedded.consumed
                assertArrayEquals(
                    "Preserved compressed bytes at ${embedded.start}",
                    input.copyOfRange(embedded.start, end), output.copyOfRange(embedded.start, end)
                )
            }
        }
        assertEquals(8, changed)
    }

    private data class Embedded(
        val start: Int,
        val consumed: Int,
        val payload: ByteArray,
        val kind: String,
        val kmi: String,
        val how: String
    )

    private fun scan(data: ByteArray, references: Map<String, String>? = null): List<Embedded> {
        val scan = DfKsudUpdater::class.java.getDeclaredMethod("scanStreams", ByteArray::class.java)
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val streams = scan.invoke(DfKsudUpdater, data) as MutableList<Any>
        if (references != null) {
            DfKsudUpdater::class.java.getDeclaredMethod("classify", List::class.java, Map::class.java)
                .apply { isAccessible = true }.invoke(DfKsudUpdater, streams, references)
        }
        return streams.map { stream ->
            fun field(name: String): Any = stream.javaClass.getDeclaredField(name)
                .apply { isAccessible = true }.get(stream)
            Embedded(field("start") as Int, field("consumed") as Int, field("payload") as ByteArray,
                field("kind") as String, field("kmi") as String, field("how") as String)
        }
    }

    private fun module(name: String, vermagic: String, seed: Int? = null): ByteArray {
        val random = seed?.let { Random(it.toLong()) }
        val payload = ByteArray(32 * 1024) { random?.nextInt(16)?.toByte() ?: 0 }
        byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte()).copyInto(payload)
        "\u0000name=$name\u0000vermagic=$vermagic\u0000".toByteArray().copyInto(payload, 64)
        return payload
    }

    private fun fixture(vararg payloads: ByteArray): ByteArray {
        val bytes = ByteArrayOutputStream()
        bytes.write(ByteArray(64))
        payloads.forEach { payload ->
            val deflater = Deflater(9, true)
            val compressed = ByteArrayOutputStream()
            try {
                deflater.setInput(payload)
                deflater.finish()
                val buffer = ByteArray(4096)
                while (!deflater.finished()) {
                    val count = deflater.deflate(buffer)
                    check(count > 0) { "Fixture compression stalled" }
                    compressed.write(buffer, 0, count)
                }
            } finally {
                deflater.end()
            }
            val stream = compressed.toByteArray()
            assertTrue("Fixture must use a dynamic DEFLATE block", (stream[0].toInt() and 7) in listOf(4, 5))
            bytes.write(stream)
        }
        // The production scanner probes up to 256 KiB ahead of each stream.
        bytes.write(ByteArray(262144))
        return bytes.toByteArray()
    }

    private fun sha256(data: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(data).joinToString("") { "%02x".format(it) }
}
