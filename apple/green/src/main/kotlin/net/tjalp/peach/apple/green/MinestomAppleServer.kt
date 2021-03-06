package net.tjalp.peach.apple.green

import net.minestom.server.MinecraftServer
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.Instance
import net.tjalp.peach.apple.green.command.*
import net.tjalp.peach.apple.green.generator.SimpleGenerator
import net.tjalp.peach.apple.green.listener.AppleEventListener
import net.tjalp.peach.apple.green.old.command.GamemodeCommand
import net.tjalp.peach.apple.green.registry.OVERWORLD
import net.tjalp.peach.apple.green.registry.registerBiomes
import net.tjalp.peach.apple.green.registry.registerDimensions
import net.tjalp.peach.apple.green.scheduler.MinestomAppleScheduler
import net.tjalp.peach.apple.pit.AppleServer
import net.tjalp.peach.apple.pit.scheduler.AppleScheduler
import net.tjalp.peach.peel.config.MinestomAppleConfig
import net.tjalp.peach.peel.node.NodeType
import net.tjalp.peach.peel.util.GsonHelper

fun main(args: Array<String>) {
    val server = MinestomAppleServer()

    // Initialize the Minestom server
    server.init()
    server.start()
}

class MinestomAppleServer : AppleServer() {

    override val nodeType: NodeType = NodeType.APPLE_GREEN

    private lateinit var globalScheduler: MinestomAppleScheduler

    /** The [MinecraftServer] that will be used */
    lateinit var server: MinecraftServer

    /** The main instance which is loaded at all times */
    lateinit var overworld: Instance

    override val scheduler: AppleScheduler
        get() = globalScheduler

    override fun init() {
        super.init()

        // Override the apple config because there will be some custom values in here
        config = GsonHelper.global().fromJson(System.getenv("NODE_CONFIG"), MinestomAppleConfig::class.java)

        // Initialize the Minestom server
        server = MinecraftServer.init()
        val instanceManager = MinecraftServer.getInstanceManager()

        // Set the logger
        logger = MinecraftServer.LOGGER

        // Initialize the scheduler
        globalScheduler = MinestomAppleScheduler()

        // Set some Minestom properties
        // System.setProperty("minestom.chunk-view-distance", "8")
        MinecraftServer.setBrandName("apple")

        // Register the main listener
        AppleEventListener(this)

        // Register the services
        registerCommands()
        registerDimensions()
        registerBiomes()

        // Create the instance
        overworld = instanceManager.createInstanceContainer(OVERWORLD)
        overworld.chunkGenerator = SimpleGenerator()

        // Specify a shutdown task
        MinecraftServer.getSchedulerManager().buildShutdownTask(this::shutdown)
    }

    override fun start() {
        super.start()

        // Enable Mojang authentication (disabled because we're using a proxy, see below for Velocity)
        // MojangAuth.init();

        // Enable the proxy (must be done after redis has connected)
        setVelocitySecret()

        // Set the secret on healthreporter connect
        // TODO Remove this; this is temporary because
        // for some reason after a few hours the secret
        // cannot be resolved anymore, only when rebooting
        // pumpkin, which is not really a good option
        healthReporter.onConnectionOpen.subscribe {
            setVelocitySecret()
        }

        // Start the server
        server.start("0.0.0.0", config.port)
    }

    /**
     * Register the commands the server should use
     */
    private fun registerCommands() {
        val man = MinecraftServer.getCommandManager()

        man.register(GamemodeCommand())
        man.register(PeachCommand(this))
        man.register(SkinCommand())
        man.register(StopCommand())
        man.register(SwitchCommand())
        man.register(TeleportCommand())
    }

    /**
     * Set the Velocity secret, which is queried
     * from the redis connection
     */
    private fun setVelocitySecret() {
        redis.query().get("velocitySecret").subscribe { secret ->
            if (secret == null) {
                logger.error("Tried to get the velocity secret, but it does not exist!")
                return@subscribe
            }
            VelocityProxy.enable(secret)
        }
    }
}