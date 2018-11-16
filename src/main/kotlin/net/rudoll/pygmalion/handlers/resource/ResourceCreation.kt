package net.rudoll.pygmalion.handlers.resource

import net.rudoll.pygmalion.handlers.arguments.parsedarguments.ParsedArgument
import net.rudoll.pygmalion.model.Action
import net.rudoll.pygmalion.model.ParsedInput
import net.rudoll.pygmalion.util.HttpCallMapperUtil
import net.rudoll.pygmalion.util.PortUtil
import spark.Request
import spark.Response


class ResourceCreation(private val portAndRoute: PortUtil.PortAndRoute, private val parsedInput: ParsedInput) : Action {

    private val resourceContainer = ResourceContainer()

    override fun run(arguments: Set<ParsedArgument>) {
        if (!PortUtil.setPort(portAndRoute.port)) {
            parsedInput.errors.add("Port was already set. Route cannot be set.")
            return
        }
        val route = portAndRoute.route
        parsedInput.logs.add("Creating resource mapping for $route:${portAndRoute.port ?: "80"}")
        HttpCallMapperUtil.map("get", route, parsedInput, getAllCallback())
        HttpCallMapperUtil.map("get", "$route/:id", parsedInput, getByIdCallback())
        HttpCallMapperUtil.map("post", route, parsedInput, createCallback())
        HttpCallMapperUtil.map("put", "$route/:id", parsedInput, updateCallback())
        HttpCallMapperUtil.map("delete", "$route/:id", parsedInput, deleteCallback())
    }

    private fun deleteCallback(): HttpCallMapperUtil.ResultCallback {
        return object : HttpCallMapperUtil.ResultCallback {
            override fun getResult(request: Request, response: Response): String {
                return resourceContainer.delete(request.params(":id"), response)
            }
        }
    }

    private fun updateCallback(): HttpCallMapperUtil.ResultCallback {
        return object : HttpCallMapperUtil.ResultCallback {
            override fun getResult(request: Request, response: Response): String {
                return resourceContainer.set(request.params(":id"), request.body(), response)
            }
        }
    }

    private fun createCallback(): HttpCallMapperUtil.ResultCallback {
        return object : HttpCallMapperUtil.ResultCallback {
            override fun getResult(request: Request, response: Response): String {
                return resourceContainer.new(request.body(), response)
            }
        }
    }

    private fun getByIdCallback(): HttpCallMapperUtil.ResultCallback {
        return object : HttpCallMapperUtil.ResultCallback {
            override fun getResult(request: Request, response: Response): String {
                return resourceContainer.get(request.params(":id"), response);
            }
        }
    }

    private fun getAllCallback(): HttpCallMapperUtil.ResultCallback {
        return object : HttpCallMapperUtil.ResultCallback {
            override fun getResult(request: Request, response: Response): String {
                return resourceContainer.getAll()
            }
        }
    }


}