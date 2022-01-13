package at.rueckgr.rocketchat.archive

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import org.litote.kmongo.KMongo

class MongoOperation(private val pipelineContext: PipelineContext<*, ApplicationCall>) {
    var result: () -> Any = {}
    var parameters = mutableMapOf<String, Parameter>()

    fun result(result: () -> Any) {
        this.result = result
    }

    fun parameters(parameterList: Parameters.() -> Unit) {
        Parameters().apply(parameterList)
            .forEach { parameters[it.name] = it }
    }

    fun parameter(name: String): String? {
        return when (parameters[name]!!.type) {
            ParameterType.URL -> pipelineContext.call.parameters[name]
            ParameterType.QUERY -> pipelineContext.call.request.queryParameters[name]
        }
    }
}

class Parameter(var type: ParameterType, var name: String, var required: Boolean)

class Parameters : ArrayList<Parameter>() {
    fun urlParameter(parameterBuilder: ParameterBuilder.() -> Unit) {
        add(ParameterBuilder(ParameterType.URL).apply(parameterBuilder).build())
    }

    fun queryParameter(parameterBuilder: ParameterBuilder.() -> Unit) {
        add(ParameterBuilder(ParameterType.QUERY).apply(parameterBuilder).build())
    }
}

class ParameterBuilder(private val type: ParameterType) {
    var name: String = ""
    var required: Boolean = false

    fun build(): Parameter = Parameter(type, name, required)
}

enum class ParameterType {
    URL,
    QUERY
}

suspend fun mongoOperation(pipelineContext: PipelineContext<*, ApplicationCall>, lambda: MongoOperation.() -> Any) {
    val database = Mongo.getInstance()

    val m = MongoOperation(pipelineContext)
    m.lambda()
    m.parameters
        .values
        .filter { it.required }
        .forEach {
            when (it.type) {
                ParameterType.URL -> pipelineContext.call.parameters[it.name]
                ParameterType.QUERY -> pipelineContext.call.request.queryParameters[it.name]
            } ?: pipelineContext.call.respond(HttpStatusCode.BadRequest, "Missing parameter '${it.name}'")
        }

    try {
        pipelineContext.call.respond(m.result())
    }
    catch (e: MongoOperationException) {
        pipelineContext.call.respond(e.status, e.message)
    }

    database.close()
}

class Mongo private constructor() {
    private val client: MongoClient = KMongo.createClient(ConfigurationProvider.getConfiguration().mongoUrl)
    private val database: MongoDatabase = this.client.getDatabase(ConfigurationProvider.getConfiguration().database)
    private var closed = false

    companion object {
        private val instance = ThreadLocal.withInitial { Mongo() }

        fun getInstance(): Mongo {
            if (instance.get().closed) {
                instance.set(Mongo())
            }
            return instance.get()
        }
    }

    fun getDatabase() = this.database

    fun close() {
        this.client.close()
        this.closed = true
    }
}

class MongoOperationException(override val message: String, val status: HttpStatusCode) : Exception(message)
