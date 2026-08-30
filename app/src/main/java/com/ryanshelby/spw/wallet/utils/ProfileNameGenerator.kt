package com.ryanshelby.spw.wallet.utils

import kotlin.random.Random

object ProfileNameGenerator {
    private val adjectives = listOf(
        "Brave", "Swift", "Cool", "Happy", "Lucky", 
        "Clever", "Mighty", "Loyal", "Smart", "Wild",
        "Noble", "Silent", "Fierce", "Calm", "Bright"
    )

    private val animals = listOf(
        "Tiger", "Falcon", "Dolphin", "Bear", "Wolf", 
        "Eagle", "Fox", "Lion", "Hawk", "Owl",
        "Panda", "Shark", "Whale", "Deer", "Swan"
    )

    fun generateRandomName(): String {
        var name = ""
        while (name.isEmpty() || name.length > 15) {
            val adjective = adjectives[Random.nextInt(adjectives.size)]
            val animal = animals[Random.nextInt(animals.size)]
            name = "$adjective$animal"
        }
        return name
    }

    fun isValidName(name: String): Boolean {
        if (name.isBlank() || name.length > 15) return false
        return name.all { it.isLetter() }
    }
}
