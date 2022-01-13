package at.rueckgr.rocketchat.archive

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*
import org.litote.kmongo.KMongo

class MongoOperation {
    var result: () -> Any = {}

    fun result(result: () -> Any) {
        this.result = result
    }
}

suspend fun mongoOperation(pipelineContext: PipelineContext<*, ApplicationCall>, lambda: MongoOperation.() -> Any) {
    val database = Mongo.getInstance()

    val m = MongoOperation()
    m.lambda()
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
