package pl.fumple.forge.discordlink;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
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

@Mod(
        modid = DiscordLink.MOD_ID,
        name = DiscordLink.MOD_NAME,
        version = DiscordLink.VERSION
)
public class DiscordLink {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    public static final SimpleNetworkWrapper CHANNELINSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(
            "fpldiscordlink:main"
    );

    private static File tokensFile;
    private static HashMap<String, String> tokens = new HashMap<>();
    public static HashMap<String, String> getTokens() {
        return tokens;
    }
    public static String pendingUserTokenSend;

    public static final String MOD_ID = "discordlink-forge12";
    public static final String MOD_NAME = "DiscordLink Auto Login";
    public static final String VERSION = "1.0-SNAPSHOT";

    /**
     * This is the instance of your mod as created by Forge. It will never be null.
     */
    @Mod.Instance(MOD_ID)
    public static DiscordLink INSTANCE;

    /**
     * This is the first initialization event. Register tile entities here.
     * The registry events below will have fired prior to entry to this method.
     */
    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {

    }

    /**
     * This is the second initialization event. Register custom recipes
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) throws IOException {

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        CHANNELINSTANCE.registerMessage(GetToken.class, GetToken.class, 0, Side.CLIENT);
        CHANNELINSTANCE.registerMessage(SendToken.class, SendToken.class, 1, Side.CLIENT);
        CHANNELINSTANCE.registerMessage(DeleteToken.class, DeleteToken.class, 2, Side.CLIENT);

        tokensFile = new File(Minecraft.getMinecraft().gameDir, "fpldiscordlink.tokens.dontopenpls.txt");
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

    /**
     * This is the final initialization event. Register actions from other mods here
     */
    @Mod.EventHandler
    public void postinit(FMLPostInitializationEvent event) {

    }
    @SubscribeEvent
    public void loggedInEvent(final FMLNetworkEvent.ClientConnectedToServerEvent event) {
        LOGGER.info("loggedInEvent");
        if(pendingUserTokenSend != null) {
            LOGGER.info("sending pending");
            String token = tokens.get(pendingUserTokenSend.toLowerCase());
            CHANNELINSTANCE.sendToServer(new SendToken(pendingUserTokenSend.toLowerCase(), token != null ? token : "none"));
        }
    }
    @SubscribeEvent
    public void loggedOutEvent(final FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        pendingUserTokenSend = null;
    }
    @Mod.EventHandler
    private void setupServer(final FMLServerStartedEvent event) {
        LOGGER.error("This mod is not for servers, remove it from your mods directory and start the server again");
        FMLCommonHandler.instance().exitJava(1, false);
    }
}
