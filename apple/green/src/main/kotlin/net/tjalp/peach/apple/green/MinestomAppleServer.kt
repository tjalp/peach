package net.tjalp.peach.apple.green

import net.minestom.server.MinecraftServer
import net.minestom.server.extras.bungee.BungeeCordProxy
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.Instance
import net.tjalp.peach.apple.green.command.SkinCommand
import net.tjalp.peach.apple.green.command.StopCommand
import net.tjalp.peach.apple.green.command.TeleportCommand
import net.tjalp.peach.apple.green.config.MinestomAppleConfig
import net.tjalp.peach.apple.green.generator.SimpleGenerator
import net.tjalp.peach.apple.green.listener.AppleEventListener
import net.tjalp.peach.apple.green.old.command.GamemodeCommand
import net.tjalp.peach.apple.green.registry.OVERWORLD
import net.tjalp.peach.apple.green.registry.registerBiomes
import net.tjalp.peach.apple.green.registry.registerDimensions
import net.tjalp.peach.apple.pit.AppleServer
import net.tjalp.peach.peel.config.JsonConfig
import java.io.File

fun main(args: Array<String>) {
    val server = MinestomAppleServer()

    // Initialize the Minestom server
    server.init()
    server.start()
}

class MinestomAppleServer : AppleServer() {

    /** The [MinecraftServer] that will be used */
    lateinit var server: MinecraftServer

    /** The main instance which is loaded at all times */
    lateinit var overworld: Instance

    override fun init() {
        super.init()

        appleConfig = JsonConfig(File("config.json"), MinestomAppleConfig::class.java)

        // Initialize the Minestom server
        server = MinecraftServer.init()
        val instanceManager = MinecraftServer.getInstanceManager()

        // Set the logger
        logger = MinecraftServer.LOGGER

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
        MinecraftServer.getSchedulerManager().buildShutdownTask(this::shutdown).schedule()
    }

    override fun start() {
        super.start()

        // Enable Mojang authentication (disabled because we're using a proxy, see below for Velocity)
        // MojangAuth.init();

        // Enable the proxy (must be done after redis has connected)
        val velocitySecret = redis.query().get("velocitySecret").block()
        if (velocitySecret != null) VelocityProxy.enable(velocitySecret) else BungeeCordProxy.enable()

        // Start the server
        server.start("0.0.0.0", 25000)
    }

    /**
     * Register the commands the server should use
     */
    private fun registerCommands() {
        val man = MinecraftServer.getCommandManager()

        man.register(GamemodeCommand())
        man.register(SkinCommand())
        man.register(StopCommand())
        man.register(TeleportCommand())
    }
}