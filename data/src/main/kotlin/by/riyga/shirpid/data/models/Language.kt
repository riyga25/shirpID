package by.riyga.shirpid.data.models

enum class Language(val code: String, val displayName: String, val assets: String) {
    ENGLISH("en", "English", "labels_en.txt"),
    RUSSIAN("ru", "Русский", "labels_ru.txt");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }

        fun getAllLanguages(): List<Language> = entries.toList()
    }
}