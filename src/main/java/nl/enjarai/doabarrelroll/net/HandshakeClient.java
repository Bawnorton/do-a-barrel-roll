package nl.enjarai.doabarrelroll.net;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import nl.enjarai.doabarrelroll.DoABarrelRoll;

import java.util.Optional;
import java.util.function.Consumer;

public class HandshakeClient<T> {
    private final Codec<T> transferCodec;
    private final Consumer<T> updateCallback;
    private T serverConfig = null;
    private boolean hasConnected = false;

    public HandshakeClient(Codec<T> transferCodec, Consumer<T> updateCallback) {
        this.transferCodec = transferCodec;
        this.updateCallback = updateCallback;
    }

    /**
     * Returns the server config if the client has received one for this server,
     * returns an empty optional in any other case.
     */
    public Optional<T> getConfig() {
        return Optional.ofNullable(serverConfig);
    }

    public boolean hasConnected() {
        return hasConnected;
    }

    public PacketByteBuf handleConfigSync(PacketByteBuf buf) {
        var protocolVersion = buf.readInt();
        if (protocolVersion != 1) {
            DoABarrelRoll.LOGGER.warn("Received config with unknown protocol version: {}, will attempt to load anyway", protocolVersion);
        }

        try {
            var data = buf.readString();
            serverConfig = transferCodec.parse(JsonOps.INSTANCE, JsonParser.parseString(data))
                    .getOrThrow(false, DoABarrelRoll.LOGGER::error);
        } catch (RuntimeException e) {
            serverConfig = null;
            DoABarrelRoll.LOGGER.error("Failed to parse config from server", e);
        }

        if (serverConfig != null) {
            updateCallback.accept(serverConfig);
            hasConnected = true;
            DoABarrelRoll.LOGGER.info("Received config from server");
        }

        var returnBuf = new PacketByteBuf(Unpooled.buffer());
        returnBuf.writeInt(1);
        returnBuf.writeBoolean(serverConfig != null);
        return returnBuf;
    }

    public void reset() {
        serverConfig = null;
        hasConnected = false;
    }
}
