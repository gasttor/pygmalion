package net.rudoll.pygmalion.handlers.openapi.export

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import net.rudoll.pygmalion.common.HttpCallMapper
import net.rudoll.pygmalion.model.StateHolder

object OpenApiMonitor {

    private val om = ObjectMapper()

    fun add(method: String, route: String, resultCallback: HttpCallMapper.ResultCallback) {
        val resultCallbackDescription = resultCallback.getResultCallbackDescription() ?: return
        val paths = StateHolder.state.openAPISpec.paths
        if (paths == null || paths[route] == null) {
            StateHolder.state.openAPISpec.path(route, PathItem())
        }
        addMethod(StateHolder.state.openAPISpec.paths[route]!!, method, resultCallbackDescription)
    }

    private fun addMethod(pathItem: PathItem, method: String, resultCallbackDescription: HttpCallMapper.ResultCallback.ResultCallbackDescription) {
        val operation = getOperation(resultCallbackDescription)
        when (method.toLowerCase()) {
            "get" -> pathItem.get(operation)
            "post" -> pathItem.post(operation)
            "put" -> pathItem.put(operation)
            "delete" -> pathItem.delete(operation)
            "options" -> pathItem.options(operation)
        }
    }

    private fun getOperation(resultCallbackDescription: HttpCallMapper.ResultCallback.ResultCallbackDescription): Operation {
        if (resultCallbackDescription.operation != null) {
            return resultCallbackDescription.operation
        }
        val operation = Operation()
        addApiResponses(operation, resultCallbackDescription)
        return operation
    }

    private fun addApiResponses(operation: Operation, resultCallbackDescription: HttpCallMapper.ResultCallback.ResultCallbackDescription) {
        val mediaType = MediaType().addExamples("example1", getExample(resultCallbackDescription.exampleValue))
        val content = Content().addMediaType(resultCallbackDescription.contentType, mediaType)
        val apiResponses = ApiResponses().addApiResponse(resultCallbackDescription.statusCode.toString(), ApiResponse().description(resultCallbackDescription.description).content(content))
        operation.responses(apiResponses)
    }

    private fun getExample(rawExampleValue: String): Example {
        val example = Example()
        var exampleValue: Any = rawExampleValue
        try {
            exampleValue = om.readTree(rawExampleValue)
        } catch (e: Exception) {
            //ignore
        }
        example.value = exampleValue
        return example
    }

    fun getPrototype(): OpenAPI {
        val openApi = OpenAPI()
        openApi.info(getInfo())
        return openApi
    }

    private fun getInfo(): Info {
        val info = Info()
        info.version = "1.0"
        info.title = "Demo API"
        info.description = "Generated by Pygmalion"
        return info
    }

    fun addSecurityScheme(name: String, securityScheme: SecurityScheme) {
        ensureOpenApiSpecComponentsNotNull()
        StateHolder.state.openAPISpec.components.addSecuritySchemes(name, securityScheme)
    }

    fun addComponentSchemas(components: Components) {
        if (components.schemas == null) {
            return
        }
        ensureOpenApiSpecComponentsNotNull()
        components.schemas.forEach { StateHolder.state.openAPISpec.components.addSchemas(it.key, it.value) }
    }

    private fun ensureOpenApiSpecComponentsNotNull() {
        if (StateHolder.state.openAPISpec.components == null) {
            StateHolder.state.openAPISpec.components = Components()
        }
    }

    fun setPort(port: Int) {
        val server = Server()
        server.url = "http://localhost:$port"
        StateHolder.state.openAPISpec.addServersItem(server)
    }
}