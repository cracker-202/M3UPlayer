package com.example.m3uplayer.data

object M3uParser {

    const val DEFAULT_GROUP = "Sin categoría"

    private val attributeRegex = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")

    fun parse(text: String): List<Channel> {
        val channels = ArrayList<Channel>()
        var name = ""
        var logo: String? = null
        var group: String? = null

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim().trimStart('﻿')
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val attrs = attributeRegex.findAll(line).associate {
                        it.groupValues[1].lowercase() to it.groupValues[2]
                    }
                    logo = attrs["tvg-logo"]?.trim()?.takeIf { it.isNotEmpty() }
                    group = attrs["group-title"]?.trim()?.takeIf { it.isNotEmpty() }
                    name = titleOf(line).ifEmpty { attrs["tvg-name"].orEmpty().trim() }
                }

                line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                    if (group == null) {
                        group = line.substringAfter(':').trim().takeIf { it.isNotEmpty() }
                    }
                }

                line.startsWith("#") -> Unit

                else -> {
                    // Algunas listas añaden cabeceras HTTP tras "|"; se ignoran.
                    val url = line.substringBefore('|').trim()
                    if (url.isNotEmpty()) {
                        val finalName = name.ifEmpty {
                            url.substringBefore('?').substringAfterLast('/').ifEmpty { url }
                        }
                        channels += Channel(
                            id = url,
                            name = finalName,
                            url = url,
                            logo = logo,
                            group = group ?: DEFAULT_GROUP,
                        )
                    }
                    name = ""
                    logo = null
                    group = null
                }
            }
        }
        return channels
    }

    /** Devuelve el texto tras la primera coma que no esté entre comillas. */
    private fun titleOf(line: String): String {
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                return line.substring(i + 1).trim()
            }
        }
        return ""
    }
}
