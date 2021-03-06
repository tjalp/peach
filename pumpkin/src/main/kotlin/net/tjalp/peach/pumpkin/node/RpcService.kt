package net.tjalp.peach.pumpkin.node

import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import net.tjalp.peach.peel.network.ServerNodeInterceptor
import net.tjalp.peach.pumpkin.PumpkinServer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * The central RPC service responsible for
 * connecting to melon & apple nodes.
 */
class RpcService(
    private val pumpkin: PumpkinServer
) {

    var active: Boolean = false; private set
    private var server: Server? = null
    private val port: Int = pumpkin.config.pumpkin.port
    private val builder: NettyServerBuilder = NettyServerBuilder.forPort(port)
    private val logger; get() = pumpkin.logger
    private val lock: Any = Any()

    /**
     * The [ServerNodeInterceptor] instance used to
     * validate and verify incoming connections.
     */
    val nodeInterceptor = ServerNodeInterceptor(pumpkin.logger)

    init {
        registerDefaults()
    }

    /**
     * Start the RPC Service on the configured port
     * This is a blocking method.
     */
    fun start() {
        synchronized(lock) {
            if (active) {
                throw IllegalStateException("RPC Service already started!")
            }

            server = builder.build()
            active = true

            thread(name = "Pumpkin RPC Service") {
                logger.info("Starting RPC Service (port: $port)...")

                server!!.start().awaitTermination()
            }
        }
    }

    /**
     * Terminate (end, shutdown, stop) the RPC service
     * This is a blocking method.
     */
    fun stop() {
        synchronized(lock) {
            logger.info("Shutting down RPC Service...")

            server!!.shutdown().awaitTermination(5, TimeUnit.SECONDS)
            active = false

            logger.info("Successfully terminated RPC Service!")
        }
    }

    /**
     * Perform custom configuration to the
     * RPC service before it has started.
     *
     * @param callback The configurator
     */
    fun configure(callback: (NettyServerBuilder) -> Unit) {
        if(server != null) {
            throw IllegalStateException("RPC Server already started!")
        }

        callback(builder)
    }

    private fun registerDefaults() {
        builder.apply {
            intercept(PumpkinSyncInterceptor(pumpkin))
            intercept(nodeInterceptor)
        }
    }
}