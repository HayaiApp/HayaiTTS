package dev.ahmedmohamed.hayaitts.data.storage

import java.io.File
import java.io.IOException

/**
 * Resolves an archive entry path under this directory, rejecting entries
 * that would land outside it (`../` or absolute paths in a crafted bundle).
 */
fun File.safeChild(relative: String): File {
    val out = File(this, relative)
    if (!out.canonicalPath.startsWith(canonicalPath + File.separator)) {
        throw IOException("Archive entry escapes the voice directory: $relative")
    }
    return out
}
