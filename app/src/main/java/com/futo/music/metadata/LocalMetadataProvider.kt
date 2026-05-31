package com.futo.music.metadata

import com.futo.music.storage.db.MetadataType

class LocalMetadataProvider: IMetadataProvider {

    companion object {
        val VERSION = 1;
        val METADATA_TYPE = MetadataType.INTERPRETED;
    }

    override fun determine(input: String): MetadataGuess? {
        val raw = input.substringBeforeLast(".")
            .replace('_', ' ')
            .replace('.', ' ')
            .trim()

        // Common separators in music filenames
        val separators = listOf(" - ", " – ", " — ", " ~ ", " | ")

        for (sep in separators) {
            if (raw.contains(sep)) {
                val parts = raw.split(sep)

                if (parts.size >= 2) {
                    val left = clean(parts[0])
                    val right = clean(parts.drop(1).joinToString(sep))

                    val artist = left
                    val title = sanitize(right)

                    var certainty = 0.75f

                    if (artist.length < 2 || title.length < 2)
                        certainty -= 0.2f

                    if (containsNoise(raw))
                        certainty -= 0.15f

                    certainty = certainty.coerceIn(0f, 1f)

                    return MetadataGuess(
                        title = title,
                        artist = artist,
                        certainty = certainty,
                        type = METADATA_TYPE,
                        version = VERSION
                    )
                }
            }
        }

        // Fallback: cannot confidently split artist/title
        return null;
    }

    fun sanitize(title: String): String {
        return clean(title
                .replace(Regex("\\(.*?\\)"), "") // remove brackets
                .replace(Regex("\\[.*?]"), "")
                .replace(Regex("(?i)official video"), "")
                .replace(Regex("(?i)lyrics"), "")
                .replace(Regex("(?i)audio"), "")
                .replace(Regex("(?i)hd"), "")
        );
    }

    private fun clean(value: String): String {
        return value
            .replace(Regex("\\s+"), " ")
            .trim()
    }


    private fun containsNoise(value: String): Boolean {
        val lowered = value.lowercase()

        val noiseWords = listOf(
            "official video",
            "lyrics",
            "audio",
            "hd",
            "1080p",
            "feat.",
            "ft.",
            "remix"
        )

        return noiseWords.any { lowered.contains(it) }
    }
}