package com.example.wificonnector

class AttnLabelConverter(character: String) {
    private val listToken = listOf("[GO]", "[s]")
    private val characterList = listToken + character.toList()
    private val dict = characterList.withIndex().associate { it.value to it.index }

    fun decode(textIndex: Array<LongArray>, length: IntArray): List<String> {
        val texts = mutableListOf<String>()
        for ((index, l) in length.withIndex()) {
            val text = textIndex[index].take(l).map {
                if (it.toInt() in characterList.indices) {
                    characterList[it.toInt()]
                } else {
                    "[UNK]" // 알 수 없는 문자에 대한 대체 값
                }
            }.joinToString("")
            texts.add(text)
        }
        return texts
    }
}
