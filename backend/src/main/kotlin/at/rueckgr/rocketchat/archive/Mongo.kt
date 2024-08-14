package at.rueckgr.rocketchat.archive

import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoDatabase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecRegistries
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.reflect.KClass

class MongoOperation(private val pipelineContext: PipelineContext<*, ApplicationCall>) {
    var result: () -> Any = {}
    val parameters = mutableMapOf<String, Parameter>()

    fun result(result: () -> Any) {
        this.result = result
    }

    fun parameters(parameterList: Parameters.() -> Unit) {
        Parameters().apply(parameterList)
            .forEach { parameters[it.name] = it }
    }

    fun parameter(name: String): String? = anyParameter(name) as String?

    fun intParameter(name: String): Int? = parameter(name)?.toInt()

    fun dateParameter(name: String): LocalDate? {
        val date = parameter(name)
        return if (!date.isNullOrBlank()) {
            LocalDate.parse(date.trim())
        }
        else {
            null
        }
    }

    fun boolParameter(name: String): Boolean? {
        val value = intParameter(name)
        return if (value == null) {
            null
        }
        else {
            value == 1
        }
    }

    private fun anyParameter(name: String): Any? {
        return when (parameters[name]!!.type) {
            ParameterType.URL -> pipelineContext.call.parameters[name] ?: parameters[name]?.default
            ParameterType.QUERY -> pipelineContext.call.request.queryParameters[name] ?: parameters[name]?.default
        }
    }
}

class Parameter(val type: ParameterType, val name: String, val required: Boolean, val datatype: KClass<*>, val default: Any?)

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
    var datatype: KClass<*> = String::class
    var default: Any? = null

    fun build(): Parameter = Parameter(type, name, required, datatype, default?.toString())
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

class ZonedDateTimeCodec : Codec<ZonedDateTime> {
    override fun encode(writer: BsonWriter, zonedDateTime: ZonedDateTime, encoderContext: EncoderContext) {
        writer.writeDateTime(zonedDateTime.toInstant().toEpochMilli())
    }


    override fun decode(reader: BsonReader, decoderContext: DecoderContext) =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(reader.readDateTime()), ZoneId.systemDefault())

    override fun getEncoderClass() = ZonedDateTime::class.java
}

class Mongo private constructor() {
    private val client: MongoClient = MongoClient.create(ConfigurationProvider.getConfiguration().mongoUrl)
    private val database: MongoDatabase
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

    init {
        val codecRegistry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(ZonedDateTimeCodec()),
            MongoClientSettings.getDefaultCodecRegistry()
        )
        database = this.client.getDatabase(ConfigurationProvider.getConfiguration().database).withCodecRegistry(codecRegistry)
    }

    fun getDatabase() = this.database

    fun close() {
        this.client.close()
        this.closed = true
    }
}

class MongoOperationException(override val message: String, val status: HttpStatusCode) : Exception(message)
