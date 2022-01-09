package pl.fumple.forge.discordlink.Packet;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import pl.fumple.forge.discordlink.DiscordLink;

import java.util.function.Supplier;

public record DeleteToken(String username) {

    public static void encode(DeleteToken msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.username);
    }

    public static DeleteToken decode(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ByteArrayDataInput out = ByteStreams.newDataInput(bytes);
        return new DeleteToken(out.readUTF());
    }

    public static void handle(DeleteToken msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            String hostname = context.get().getNetworkManager().getRemoteAddress().toString().split("/")[0];
            if(!hostname.matches("^([a-zA-Z]*\\.)?mc\\.fumple\\.pl$")) {
                LogManager.getLogger().warn("[DiscordLink] Wystąpił błąd w auto logowaniu, serwer ["+hostname+"] nie jest autoryzowany do auto logowania");
                return;
            }
            LogManager.getLogger().info("[deltoken] received for " + msg.username + " from " + context.get().getNetworkManager().getRemoteAddress());
            DiscordLink.getTokens().remove(msg.username);
        }));
        context.get().setPacketHandled(true);
    }
}
