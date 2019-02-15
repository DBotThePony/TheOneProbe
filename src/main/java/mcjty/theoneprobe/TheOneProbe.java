package mcjty.theoneprobe;

import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.apiimpl.TheOneProbeImp;
import mcjty.theoneprobe.apiimpl.providers.*;
import mcjty.theoneprobe.config.Config;
import mcjty.theoneprobe.items.ModItems;
import mcjty.theoneprobe.network.PacketHandler;
import mcjty.theoneprobe.playerdata.PlayerGotNote;
import mcjty.theoneprobe.proxy.ClientProxy;
import mcjty.theoneprobe.proxy.IProxy;
import mcjty.theoneprobe.proxy.ServerProxy;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod("theoneprobe")
//
//@Mod(modid = TheOneProbe.MODID, name="TheOneProbe",
//        dependencies =
//                "after:forge@[" + TheOneProbe.MIN_FORGE11_VER + ",);" +
//                "after:tesla",
//        version = TheOneProbe.VERSION,
//        acceptedMinecraftVersions = "[1.12,1.13)",
//        guiFactory = "mcjty.theoneprobe.config.TopModGuiFactory")
public class TheOneProbe {
    public static final String MODID = "theoneprobe";
    public static final String VERSION = "1.4.28";
    public static final String MIN_FORGE11_VER = "13.19.0.2176";

    public static IProxy proxy = DistExecutor.runForDist(() -> () -> new ClientProxy(), () -> () -> new ServerProxy());
    public static final Logger logger = LogManager.getLogger();

//    public static TheOneProbe instance;

    public static File mainConfigDir;
    public static File modConfigDir;
//    public static Configuration config;

    public static TheOneProbeImp theOneProbeImp = new TheOneProbeImp();

    public static boolean baubles = false;
    public static boolean tesla = false;
    public static boolean redstoneflux = false;

    public static ItemGroup tabProbe = new ItemGroup("Probe") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.probe);
        }
    };


    public TheOneProbe() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::init);

        // Register the enqueueIMC method for modloading
//        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);

        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::initClient);

        // Register ourselves for server, registry and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void init(final FMLCommonSetupEvent event) {
//        mainConfigDir = e.getModConfigurationDirectory();
//        modConfigDir = new File(mainConfigDir.getPath());
//        config = new Configuration(new File(modConfigDir, "theoneprobe.cfg"));

        tesla = ModList.get().isLoaded("tesla");
        if (tesla) {
            logger.log(Level.INFO, "The One Probe Detected TESLA: enabling support");
        }

        redstoneflux = ModList.get().isLoaded("redstoneflux");
        if (redstoneflux) {
            logger.log(Level.INFO, "The One Probe Detected RedstoneFlux: enabling support");
        }

        baubles = ModList.get().isLoaded("baubles");
        if (baubles) {
            if (Config.supportBaubles) {
                logger.log(Level.INFO, "The One Probe Detected Baubles: enabling support");
            } else {
                logger.log(Level.INFO, "The One Probe Detected Baubles but support disabled in config");
                baubles = false;
            }
        }

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandlers());

        registerCapabilities();
        TheOneProbeImp.registerElements();
        TheOneProbe.theOneProbeImp.registerProvider(new DefaultProbeInfoProvider());
        TheOneProbe.theOneProbeImp.registerProvider(new DebugProbeInfoProvider());
        TheOneProbe.theOneProbeImp.registerProvider(new BlockProbeInfoProvider());
        TheOneProbe.theOneProbeImp.registerEntityProvider(new DefaultProbeInfoEntityProvider());
        TheOneProbe.theOneProbeImp.registerEntityProvider(new DebugProbeInfoEntityProvider());
        TheOneProbe.theOneProbeImp.registerEntityProvider(new EntityProbeInfoEntityProvider());

        readMainConfig();
        PacketHandler.registerMessages("theoneprobe");

        configureProviders();
        configureEntityProviders();

        proxy.setup(event);
    }

    private void initClient(final FMLClientSetupEvent event) {
    }

    private void processIMC(final InterModProcessEvent event) {
        event.getIMCStream().forEach(message -> {
            if ("getTheOneProbe".equalsIgnoreCase(message.getMethod())) {
                // @todo ?
            }
        });
//        for (FMLInterModComms.IMCMessage message : event.getMessages()) {
//            if (message.key.equalsIgnoreCase("getTheOneProbe")) {
//                Optional<Function<ITheOneProbe, Void>> value = message.getFunctionValue(ITheOneProbe.class, Void.class);
//                if (value.isPresent()) {
//                    value.get().apply(theOneProbeImp);
//                } else {
//                    logger.warn("Some mod didn't return a valid result with getTheOneProbe!");
//                }
//            }
//        }
    }


    private static void registerCapabilities(){
        CapabilityManager.INSTANCE.register(PlayerGotNote.class, new Capability.IStorage<PlayerGotNote>() {

            @Override
            public void readNBT(Capability<PlayerGotNote> capability, PlayerGotNote instance, EnumFacing side, INBTBase nbt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public INBTBase writeNBT(Capability<PlayerGotNote> capability, PlayerGotNote instance, EnumFacing side) {
                throw new UnsupportedOperationException();
            }

        }, () -> {
            throw new UnsupportedOperationException();
        });
    }

    private void readMainConfig() {
//        Configuration cfg = TheOneProbe.config;
//        try {
//            cfg.load();
//            cfg.addCustomCategoryComment(Config.CATEGORY_THEONEPROBE, "The One Probe configuration");
//            cfg.addCustomCategoryComment(Config.CATEGORY_PROVIDERS, "Provider configuration");
//            cfg.addCustomCategoryComment(Config.CATEGORY_CLIENT, "Client-side settings");
//            Config.init(cfg);
//        } catch (Exception e1) {
//            TheOneProbe.logger.log(Level.ERROR, "Problem loading config file!", e1);
//        } finally {
//            if (TheOneProbe.config.hasChanged()) {
//                TheOneProbe.config.save();
//            }
//        }
    }

    private void configureProviders() {
        List<IProbeInfoProvider> providers = TheOneProbe.theOneProbeImp.getProviders();
        String[] defaultValues = new String[providers.size()];
        int i = 0;
        for (IProbeInfoProvider provider : providers) {
            defaultValues[i++] = provider.getID();
        }

        String[] sortedProviders = defaultValues; // @todo TheOneProbe.config.getStringList("sortedProviders", Config.CATEGORY_PROVIDERS, defaultValues, "Order in which providers should be used");
        String[] excludedProviders = new String[] {}; // @todo TheOneProbe.config.getStringList("excludedProviders", Config.CATEGORY_PROVIDERS, new String[] {}, "Providers that should be excluded");
        Set<String> excluded = new HashSet<>();
        Collections.addAll(excluded, excludedProviders);

        TheOneProbe.theOneProbeImp.configureProviders(sortedProviders, excluded);
    }

    private void configureEntityProviders() {
        List<IProbeInfoEntityProvider> providers = TheOneProbe.theOneProbeImp.getEntityProviders();
        String[] defaultValues = new String[providers.size()];
        int i = 0;
        for (IProbeInfoEntityProvider provider : providers) {
            defaultValues[i++] = provider.getID();
        }

        String[] sortedProviders = defaultValues; // @todo TheOneProbe.config.getStringList("sortedEntityProviders", Config.CATEGORY_PROVIDERS, defaultValues, "Order in which entity providers should be used");
        String[] excludedProviders = new String[] {}; // @todo TheOneProbe.config.getStringList("excludedEntityProviders", Config.CATEGORY_PROVIDERS, new String[] {}, "Entity providers that should be excluded");
        Set<String> excluded = new HashSet<>();
        Collections.addAll(excluded, excludedProviders);

        TheOneProbe.theOneProbeImp.configureEntityProviders(sortedProviders, excluded);
    }

}
