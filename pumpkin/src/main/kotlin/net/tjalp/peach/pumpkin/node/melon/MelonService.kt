package net.tjalp.peach.pumpkin.node.melon

import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import net.tjalp.peach.peel.network.PeachRPC
import net.tjalp.peach.proto.melon.Melon.*
import net.tjalp.peach.proto.melon.MelonServiceGrpc.MelonServiceImplBase
import net.tjalp.peach.pumpkin.PumpkinServer
import net.tjalp.peach.pumpkin.node.apple.AppleNode
import net.tjalp.peach.pumpkin.node.apple.AppleServerNode
import net.tjalp.peach.pumpkin.player.ConnectedPlayer
import java.util.*

class MelonService(
    private val pumpkin: PumpkinServer
) : MelonServiceImplBase() {

    private val tempAppleNode: AppleNode = AppleServerNode(pumpkin, "apple-1", "localhost", 25000).also {
        pumpkin.nodeService.register(it)
    }

    override fun healthStatus(response: StreamObserver<Empty>): StreamObserver<MelonHealthReport> {
        return current().healthMonitor.listen(response)
    }

    override fun melonHandshake(
        request: MelonHandshakeRequest,
        response: StreamObserver<MelonHandshakeResponse>
    ) {
        val melonNode = MelonServerNode(pumpkin, request.nodeIdentifier)

        // Register the melon node
        pumpkin.nodeService.register(melonNode)

        for (player in request.playerList) {
            pumpkin.playerService.register(
                uniqueId = UUID.fromString(player.uniqueId),
                username = player.username,
                melonNode = melonNode,
                appleNode = tempAppleNode // TODO Apple nodes
            )
        }

        val appleNodeRegistrations = pumpkin.nodeService.appleNodes
            //.filter { it.isOnline }
            .map {
                AppleNodeRegistration.newBuilder()
                    .setNodeId(it.nodeId)
                    .setServer(it.server)
                    .setPort(it.port)
                    .build()
            }

        val res = MelonHandshakeResponse.newBuilder()
            .addAllAppleNodeRegistration(appleNodeRegistrations)
            .build()

        response.onNext(res)
        response.onCompleted()
    }

    override fun playerHandshake(request: PlayerHandshakeRequest, response: StreamObserver<Empty>) {
        pumpkin.playerService.register(
            UUID.fromString(request.uniqueId),
            request.username,
            current(),
            tempAppleNode
        )

        response.onNext(Empty.getDefaultInstance())
        response.onCompleted()
    }

    override fun playerDisconnect(request: PlayerQuit, response: StreamObserver<Empty>) {
        val player = pumpkin.playerService.getPlayer(UUID.fromString(request.uniqueId))

        if (player != null) {
            pumpkin.playerService.unregister(player as ConnectedPlayer)
        }

        response.onNext(Empty.getDefaultInstance())
        response.onCompleted()
    }

    /**
     * Resolves the melon instance that sent
     * a call by parsing the Node ID from the
     * current context
     *
     * @return The melon node which sent the request
     */
    private fun current() : MelonNode {
        return pumpkin.nodeService.getMelonNode(PeachRPC.NODE_ID_CTX.get())!!
    }
}