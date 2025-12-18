package by.riyga.shirpid.presentation.models

data class IdentifiedBird(
    val index: Int,
    val name: String,
    val confidence: Float
) {
    val conName = name.substringAfter("_")
    val latName = name.substringBefore("_")
}
