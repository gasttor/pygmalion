package net.rudoll.pygmalion.handlers.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponses
import net.rudoll.pygmalion.handlers.arguments.parsedarguments.ParsedArgument
import net.rudoll.pygmalion.model.Action
import net.rudoll.pygmalion.model.ParsedInput
import net.rudoll.pygmalion.util.HttpCallMapperUtil
import spark.Request
import spark.Response

class OpenApiContext(private val openAPI: OpenAPI) {

    fun apply(parsedInput: ParsedInput) {
        val paths = openAPI.paths
        paths.forEach { path -> this.applyPath(path, parsedInput) }
    }

    private fun applyPath(path: Map.Entry<String, PathItem>, parsedInput: ParsedInput) {
        applyOperation(path.key, "get", path.value.get, parsedInput)
        applyOperation(path.key, "post", path.value.post, parsedInput)
        applyOperation(path.key, "put", path.value.put, parsedInput)
        applyOperation(path.key, "options", path.value.options, parsedInput)
        applyOperation(path.key, "delete", path.value.delete, parsedInput)
    }

    private fun applyOperation(path: String, method: String, operation: Operation?, parsedInput: ParsedInput) {
        if (operation != null) {
            val resultCallback = getResultCallback(operation)
            parsedInput.actions.add(object : Action {
                override fun run(arguments: Set<ParsedArgument>) {
                    val pathWithArguments = getPathWithArguments(path)
                    parsedInput.logs.add("Mapping OpenAPI route $pathWithArguments with method $method")
                    HttpCallMapperUtil.map(method, pathWithArguments, parsedInput, resultCallback)
                }
            })
        }
    }

    private fun getPathWithArguments(path: String): String {
        var result = path.replace("}", "")
        result = result.replace("{", ":")
        return result
    }

    private fun getResultCallback(operation: Operation): HttpCallMapperUtil.ResultCallback {
        return object : HttpCallMapperUtil.ResultCallback {
            override fun getResult(request: Request, response: Response): String {
                return try {
                    val parameterValidationResult = RequestValidator(openAPI).validateRequest(request, operation)
                    if (!parameterValidationResult.isOk) {
                        response.status(400)
                        return parameterValidationResult.message
                    } else respond(response, operation.responses)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }
        }
    }

    private fun respond(response: Response, responses: ApiResponses): String {
        val firstMappedStatusCode = responses.keys.first()
        val apiResponse = responses[firstMappedStatusCode]!!
        response.status(firstMappedStatusCode.toInt())
        if (apiResponse.content == null) {
            return ""
        }
        val content = apiResponse.content
        val firstContentType = content.keys.first()
        response.header("Content-Type", firstContentType)
        val mediaType = content[firstContentType]!!
        if (mediaType.example != null) {
            return mediaType.example.toString()
        }
        val jsonObject = ExampleResponseGenerator(openAPI).getFromSchema(mediaType.schema)
        return jsonObject.toString()
    }

    companion object {

        fun getSchemaByRef(ref: String, openAPI: OpenAPI): Schema<*> {
            val components = openAPI.components
            val refWithoutPrefix = ref.removePrefix("#/components/schemas/")
            return components.schemas[refWithoutPrefix]!!
        }
    }
}