package pl.fumple.forge.discordlink;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.fumple.forge.discordlink.Packet.DeleteToken;
import pl.fumple.forge.discordlink.Packet.GetToken;
import pl.fumple.forge.discordlink.Packet.SendToken;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("fpldiscordlink")
public class DiscordLink
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String PROTOCOL_VERSION = NetworkRegistry.ACCEPTVANILLA;
    public static final SimpleChannel CHANNELINSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("fpldiscordlink", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static File tokensFile;
    private static HashMap<String, String> tokens = new HashMap<>();
    public static HashMap<String, String> getTokens() {
        return tokens;
    }
    public static String pendingUserTokenSend;

    public DiscordLink() throws IOException {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupServer);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        CHANNELINSTANCE.registerMessage(0, GetToken.class, GetToken::encode, GetToken::decode, GetToken::handle);
        CHANNELINSTANCE.registerMessage(1, SendToken.class, SendToken::encode, SendToken::decode, SendToken::handle);
        CHANNELINSTANCE.registerMessage(2, DeleteToken.class, DeleteToken::encode, DeleteToken::decode, DeleteToken::handle);

        tokensFile = new File(Minecraft.getInstance().gameDirectory, "fpldiscordlink.tokens.dontopenpls.txt");
        if (!tokensFile.exists()) tokensFile.createNewFile();
        for (String line : Files.readAllLines(tokensFile.toPath())) {
            String[] splitLine = line.toLowerCase().split(":");
            if (line.length() >= 2) tokens.put(splitLine[0].toLowerCase(), splitLine[1]);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ArrayList<String> stringifiedTokens = new ArrayList<>();
            for (Map.Entry<String, String> token : tokens.entrySet()) {
                stringifiedTokens.add(token.getKey().toLowerCase() + ":" + token.getValue());
            }
            try {
                Files.write(tokensFile.toPath(), stringifiedTokens, StandardOpenOption.WRITE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }
    @SubscribeEvent
    public void loggedInEvent(final ClientPlayerNetworkEvent.LoggedInEvent event) {
        LOGGER.info("loggedInEvent");
        if(pendingUserTokenSend != null) {
            String token = tokens.get(pendingUserTokenSend.toLowerCase());
            CHANNELINSTANCE.sendToServer(new SendToken(pendingUserTokenSend.toLowerCase(), token != null ? token : "none"));
        }
    }
    @SubscribeEvent
    public void loggedOutEvent(final ClientPlayerNetworkEvent.LoggedOutEvent event) {
        pendingUserTokenSend = null;
    }
    private void setupServer(final FMLDedicatedServerSetupEvent event) {
        LOGGER.error("This mod is not for servers, remove it from your mods directory and start the server again");
        System.exit(1);
    }
}
