package net.tjalp.minestom.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.tjalp.minestom.proxy.listener.ProxyEventListener;
import org.slf4j.Logger;

import java.net.InetSocketAddress;

@Plugin(
        id = "tjalp",
        name = "tjalp proxy",
        description = "The proxy plugin needed for tjalp's network",
        version = "0.1.0",
        url = "https://tjalp.net/",
        authors = {"tjalp"}
)
public class Proxy {

    @Inject
    private ProxyServer proxy;

    @Inject
    private Logger logger;

    /**
     * Gets the proxy
     *
     * @return the proxy
     */
    public ProxyServer proxy() {
        return this.proxy;
    }

    /**
     * Gets the logger
     *
     * @return the logger
     */
    public Logger logger() {
        return this.logger;
    }

    /*
     *
     * ----- LOGIC STARTS HERE -----
     *
     */

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        // Register listeners
        proxy.getEventManager().register(this, new ProxyEventListener(this));

        logger.info("Registered listeners");

        // Register active servers
        registerServer("minestom", "127.0.0.1", 25000);

        logger.info("Registered active servers");
    }

    /**
     * Register a server globally (on all proxies)
     *
     * @param id the server id to use
     * @param address the target server address
     * @param port the target server port
     */
    private void registerServer(String id, String address, int port) {
        InetSocketAddress inet = new InetSocketAddress(address, port);
        proxy.registerServer(new ServerInfo(id, inet));
    }
}