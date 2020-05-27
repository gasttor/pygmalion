package net.rudoll.pygmalion.common

import net.rudoll.pygmalion.model.ParsedInput
import net.rudoll.pygmalion.model.StateHolder
import spark.Spark

object SecureManager {

    fun secure(keystoreFile: String =  "pygmalion.jks", keystorePassword: String = "pygmalion") {
        Spark.secure(keystoreFile, keystorePassword, keystoreFile, keystorePassword)
        StateHolder.state.secureSet = true
    }

    fun ensureSecureIsHandled(parsedInput: ParsedInput) {
        if (!StateHolder.state.secureSet) {
            parsedInput.logs.add("Secure will be deactivated as default")
        }
    }
}