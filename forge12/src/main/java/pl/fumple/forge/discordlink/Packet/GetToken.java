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

public class GetToken implements IMessage, IMessageHandler<GetToken, IMessage> {
    public GetToken(){}
    private String username;
    public GetToken(String username) {
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
    public IMessage onMessage(GetToken msg, MessageContext ctx) {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    String hostname = ctx.getClientHandler().getNetworkManager().getRemoteAddress().toString().split("/")[0];
                    if(!hostname.matches("^([a-zA-Z0-9]*\\.)?mc\\.fumple\\.pl$")) {
                        LogManager.getLogger().warn("[DiscordLink] Wystąpił błąd w auto logowaniu, serwer ["+hostname+"] nie jest autoryzowany do auto logowania");
                        return;
                    }
                    LogManager.getLogger().info("[gettoken] received for " + msg.username + " from " + ctx.getClientHandler().getNetworkManager().getRemoteAddress());
                    //if(Minecraft.getMinecraft().getConnection() == null) {
                    //    LogManager.getLogger().info("pending added");
                    //    DiscordLink.pendingUserTokenSend = msg.username;
                    //}
                    //else {
                    String token = DiscordLink.getTokens().get(msg.username.toLowerCase());
                    DiscordLink.CHANNELINSTANCE.sendToServer(new SendToken(msg.username.toLowerCase(), token));
                    //}
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        return null;
    }
}
