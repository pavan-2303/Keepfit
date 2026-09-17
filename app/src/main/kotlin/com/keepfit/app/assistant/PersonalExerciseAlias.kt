package com.keepfit.app.assistant

import java.security.MessageDigest

internal object PersonalExerciseAlias {
    fun forId(exerciseId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(exerciseId.toByteArray(Charsets.UTF_8))
        return "exercise-" + digest.take(12).joinToString("") { byte -> "%02x".format(byte) }
    }
}
