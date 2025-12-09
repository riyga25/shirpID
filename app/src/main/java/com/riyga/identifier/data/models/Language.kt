package com.riyga.identifier.data.models

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    RUSSIAN("ru", "Русский");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }

        fun getAllLanguages(): List<Language> = entries.toList()
    }
}