package net.rudoll.pygmalion.handlers.secure

import net.rudoll.pygmalion.common.SecureManager
import net.rudoll.pygmalion.handlers.Handler
import net.rudoll.pygmalion.handlers.arguments.parsedarguments.ParsedArgument
import net.rudoll.pygmalion.model.*

object SecureHandler : Handler {

    override fun getParseStage(): ParseStage {
        return ParseStage.FIRST_PASS
    }

    override fun getDocumentation(): String {
        return "secure [\$keystoreFile] [\$keystorePassword]"
    }

    override fun canHandle(input: Input): Boolean {
        return input.first() == "secure"
    }

    override fun handle(input: Input, parsedInput: ParsedInput) {
        input.consume(1)

        var secureAction = { SecureManager.secure() }

        if (input.hasNext()) {
            val keystoreFile = input.first()
            var keystorePassword = ""
            input.consume(1)

            if (!input.hasNext()) {
                parsedInput.errors.add("keystorePassword is missing.")
                return
            } else {
                keystorePassword = input.first()
                input.consume(1)
            }

            secureAction = { SecureManager.secure(keystoreFile, keystorePassword) }
        }

        parsedInput.actions.add(object : Action {
            override fun run(arguments: Set<ParsedArgument>) {
                if (StateHolder.state.secureSet) {
                    parsedInput.errors.add("Secure cannot be changed. Please use another instance of this application.")
                    return
                }
                secureAction()
            }
        })
    }

}