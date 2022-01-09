package pl.fumple.forge.discordlink.Packet;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.logging.log4j.LogManager;
import pl.fumple.forge.discordlink.DiscordLink;

public class SendToken implements IMessage, IMessageHandler<SendToken, IMessage> {
    public SendToken(){}
    private String username;
    private String token;
    public SendToken(String username, String token) {
        this.username = username;
        this.token = token;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(username);
        out.writeUTF(token != null ? token : "none");
        buf.writeBytes(out.toByteArray());
    }

    public void fromBytes(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ByteArrayDataInput out = ByteStreams.newDataInput(bytes);
        username = out.readUTF();
        token = out.readUTF();
    }

    @Override
    public IMessage onMessage(SendToken msg, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            String hostname = ctx.getClientHandler().getNetworkManager().getRemoteAddress().toString().split("/")[0];
            if(!hostname.matches("^([a-zA-Z]*\\.)?mc\\.fumple\\.pl$")) {
                LogManager.getLogger().warn("[DiscordLink] Wystąpił błąd w auto logowaniu, serwer ["+hostname+"] nie jest autoryzowany do auto logowania");
                return;
            }
            LogManager.getLogger().info("[sendtoken] received for " + msg.username + " from " + ctx.getClientHandler().getNetworkManager().getRemoteAddress());
            DiscordLink.getTokens().put(msg.username.toLowerCase(), msg.token);
        });
        return null;
    }
}
