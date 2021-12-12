package pl.fumple.forge.discordlink.Packet;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import pl.fumple.forge.discordlink.DiscordLink;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record GetToken(String username) {

    public static void encode(GetToken msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.username);
    }

    public static GetToken decode(FriendlyByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ByteArrayDataInput out = ByteStreams.newDataInput(bytes);
        return new GetToken(out.readUTF());
    }

    public static void handle(GetToken msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            String hostname = context.get().getNetworkManager().getRemoteAddress().toString().split("/")[0];
            if(!hostname.matches("^([a-zA-Z0-9]*\\.)?mc\\.fumple\\.pl$")) {
                LogManager.getLogger().warn("[DiscordLink] Wystąpił błąd w auto logowaniu, serwer ["+hostname+"] nie jest autoryzowany do auto logowania");
                return;
            }
            LogManager.getLogger().info("[gettoken] received for " + msg.username + " from " + context.get().getNetworkManager().getRemoteAddress());
            if(Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().getConnection().getConnection() == null) {
                LogManager.getLogger().info("pending added");
                DiscordLink.pendingUserTokenSend = msg.username;
            }
            else {
                String token = DiscordLink.getTokens().get(msg.username.toLowerCase());
                DiscordLink.CHANNELINSTANCE.sendToServer(new SendToken(msg.username.toLowerCase(), token));
            }
        }));
        context.get().setPacketHandled(true);
    }
}
