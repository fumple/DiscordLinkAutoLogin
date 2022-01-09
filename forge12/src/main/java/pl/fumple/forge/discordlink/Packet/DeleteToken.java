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

public class DeleteToken implements IMessage, IMessageHandler<DeleteToken, IMessage> {
    public DeleteToken(){}
    private String username;
    public DeleteToken(String username) {
        this.username = username;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(username);
        buf.writeBytes(out.toByteArray());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ByteArrayDataInput out = ByteStreams.newDataInput(bytes);
        username = out.readUTF();
    }

    @Override
    public IMessage onMessage(DeleteToken msg, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            String hostname = ctx.getClientHandler().getNetworkManager().getRemoteAddress().toString().split("/")[0];
            if(!hostname.matches("^([a-zA-Z]*\\.)?mc\\.fumple\\.pl$")) {
                LogManager.getLogger().warn("[DiscordLink] Wystąpił błąd w auto logowaniu, serwer ["+hostname+"] nie jest autoryzowany do auto logowania");
                return;
            }
            LogManager.getLogger().info("[deltoken] received for " + msg.username + " from " + ctx.getClientHandler().getNetworkManager().getRemoteAddress());
            DiscordLink.getTokens().remove(msg.username);
        });
        return null;
    }
}
