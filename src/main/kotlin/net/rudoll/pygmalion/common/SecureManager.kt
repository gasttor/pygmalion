package net.rudoll.pygmalion.common

import net.rudoll.pygmalion.model.ParsedInput
import net.rudoll.pygmalion.model.StateHolder
import spark.Spark

object SecureManager {

    fun secure(on: Boolean) {
        if (on) {
            Spark.secure("pygmalion.jks","pygmalion", "pygmalion.jks", "pygmalion")
        }
        StateHolder.state.secureSet = true
    }

    fun ensureSecureIsHandled(parsedInput: ParsedInput) {
        if (!StateHolder.state.secureSet) {
            parsedInput.logs.add("Setting secure default to false")
            SecureManager.secure(false)
        }
    }
}