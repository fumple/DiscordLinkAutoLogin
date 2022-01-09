package pl.fumple.forge.discordlink.Packet;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import pl.fumple.forge.discordlink.DiscordLink;

import java.util.function.Supplier;

public record SendToken(String username, String token) {

    public static void encode(SendToken msg, FriendlyByteBuf buf) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(msg.username);
        out.writeUTF(msg.token != null ? msg.token : "none");
        buf.writeBytes(out.toByteArray());
    }

    public static SendToken decode(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ByteArrayDataInput out = ByteStreams.newDataInput(bytes);
        return new SendToken(out.readUTF(), out.readUTF());
    }

    public static void handle(SendToken msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            String hostname = context.get().getNetworkManager().getRemoteAddress().toString().split("/")[0];
            if(!hostname.matches("^([a-zA-Z]*\\.)?mc\\.fumple\\.pl$")) {
                LogManager.getLogger().warn("[DiscordLink] Wystąpił błąd w auto logowaniu, serwer ["+hostname+"] nie jest autoryzowany do auto logowania");
                return;
            }
            LogManager.getLogger().info("[sendtoken] received for " + msg.username + " from " + context.get().getNetworkManager().getRemoteAddress());
            DiscordLink.getTokens().put(msg.username.toLowerCase(), msg.token);
        }));
        context.get().setPacketHandled(true);
    }
}
