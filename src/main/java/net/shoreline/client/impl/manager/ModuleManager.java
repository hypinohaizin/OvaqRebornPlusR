package net.shoreline.client.impl.manager;

import net.shoreline.client.OvaqRebornPlus;
import net.shoreline.client.OvaqRebornPlusMod;
import net.shoreline.client.api.module.Module;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.impl.module.client.*;
import net.shoreline.client.impl.module.combat.*;
import net.shoreline.client.impl.module.exploit.*;
import net.shoreline.client.impl.module.misc.*;
import net.shoreline.client.impl.module.movement.*;
import net.shoreline.client.impl.module.render.*;
import net.shoreline.client.impl.module.world.*;
import net.shoreline.client.init.Managers;

import java.util.*;

/**
 * @author h_ypi
 * @since 1.0
 */
public final class ModuleManager
{
    // The client module register. Keeps a list of modules and their ids for
    // easy retrieval by id.
    private final Map<String, Module> modules = Collections.synchronizedMap(new LinkedHashMap<>());
    private final EnumMap<ModuleCategory, Integer> categoryCounts = new EnumMap<>(ModuleCategory.class);
    /**
     * Initializes the module register.
     */
    public ModuleManager()
    {
        // MAINTAIN ALPHABETICAL ORDER
        register(
                // Client
                new AnticheatModule(),
                new CapesModule(),
                new ClickGuiModule(),
                new ColorsModule(),
                new FontModule(),
                new HUDModule(),
                new IRCModule(),
                new RotationsModule(),
                new RPCModule(),
                new SocialsModule(),
                // Combat
                new AuraModule(),
                new AutoAnchorModule(),
                new AutoArmorModule(),
                new AutoBowReleaseModule(),
                new AutoCrawlTrapModule(),
                new AutoCrystalModule(),
                new AutoLogModule(),
                new AutoObsidianModule(),
                // new AutoRegearModule(),
                new AutoTotemModule(),
                new AutoTrapModule(),
                new AutoWebModule(),
                new AutoXPModule(),
                //new BasePlaceModule(),
                new BedAuraModule(),
                new BlockerModule(),
                new BowAimModule(),
                new ClickCrystalModule(),
                new CriticalsModule(),
                new HoleFillModule(),
                new HoleSnapModule(),
                new KeepSprintModule(),
                new NoHitDelayModule(),
                new PistonPushModule(),
                new ReplenishModule(),
                new SelfBowModule(),
                new SelfFillModule(),
                new SelfTrapModule(),
                // new SelfWebModule(),
                new SurroundModule(),
                new TriggerModule(),
                // Exploit
                new AntiHungerModule(),
                // new BacktrackModule(),
                new BoatExploitModule(),
                new ChorusControlModule(),
                new ChorusInvincibilityModule(),
                new ClientSpoofModule(),
                new CrasherModule(),
                new DisablerModule(),
                new ExtendedFireworkModule(),
                new FakeLatencyModule(),
                new FastLatencyModule(),
                new FastProjectileModule(),
                new GodModeModule(),
                new InventorySyncModule(),
                new MaceExploitModule(),
                new NewChunksModule(),
                new NoMineAnimationModule(),
                new PacketCancelerModule(),
                new PacketFlyModule(),
                new PearlPhaseModule(),
                new PhaseModule(),
                new PlayerTPModule(),
                new PluginsDetectorModule(),
                new ReachModule(),
                // Misc
                new AntiAFKModule(),
                new AntiAimModule(),
                new AntiAttackModule(),
                new AntiSpamModule(),
                new AntiVanishModule(),
                new AutoAcceptModule(),
                new AutoAnvilRenameModule(),
                new AutoDSModule(),
                new AutoEatModule(),
                new AutoEzModule(),
                new AutoFishModule(),
                new AutoMountModule(),
                new AutoReconnectModule(),
                new AutoRespawnModule(),
                // new BeaconSelectorModule(),
                new BetterChatModule(),
                new BetterInvModule(),
                new ChatSuffixModule(),
                new ChatNotifierModule(),
                new ChestSwapModule(),
                new ChestStealerModule(),
                new FakePlayerModule(),
                new InvCleanerModule(),
                new MiddleClickModule(),
                new NoEntityTraceModule(),
                new NoPacketKickModule(),
                new NoSoundLagModule(),
                new PacketLoggerModule(),
                new PMSoundModule(),
                new PosLoggerModule(),
                new ServerModule(),
                new ShulkerceptionModule(),
                new SkinBlinkModule(),
                new SpammerModule(),
                new SwingModule(),
                new TimerModule(),
                new ToggleSoundModule(),
                new ToolSaverModule(),
                new TPDetectorModule(),
                new TrueDurabilityModule(),
                new UnfocusedFPSModule(),
                new WelcomerModule(),
                new XCarryModule(),
                // Movement
                new AntiLevitationModule(),
                new AutoWalkModule(),
                new BlockMoveModule(),
                new BoatFlyModule(),
                new ElytraFlyModule(),
                new EntityControlModule(),
                new EntitySpeedModule(),
                new FakeLagModule(),
                new FastFallModule(),
                new FastSwimModule(),
                new FireworkBoostModule(),
                new FlightModule(),
                new FollowModule(),
                //new GrimFlightModule(),
                new IceSpeedModule(),
                new JesusModule(),
                new LongJumpModule(),
                new NoAccelModule(),
                new NoFallModule(),
                new NoJumpDelayModule(),
                new NoSlowModule(),
                new ParkourModule(),
                new SafeWalkModule(),
                new SpeedModule(),
                new SprintModule(),
                new StepModule(),
                new TickShiftModule(),
                new TridentFlyModule(),
                new VelocityModule(),
                new YawModule(),
                // Render
                new AmbienceModule(),
                new AnimationsModule(),
                new BlockHighlightModule(),
                new BreadcrumbsModule(),
                new BreakHighlightModule(),
                new ChamsModule(),
                new CrosshairModule(),
                new CrystalModelModule(),
                new ESPModule(),
                new ExtraTabModule(),
                new FreecamModule(),
                new FreeLookModule(),
                new FullbrightModule(),
                new HoleESPModule(),
                new KillEffectsModule(),
                new NameProtectModule(),
                new NametagsModule(),
                new NoBobModule(),
                new NoRenderModule(),
                new NoRotateModule(),
                new NoWeatherModule(),
                new ParticlesModule(),
                new PhaseESPModule(),
                new PopChamsModule(),
                new SearchModule(),
                new ShadersModule(),
                // new SkeletonModule(),
                new SkyboxModule(),
                new StorageESPModule(),
                new SmartF3Module(),
                new TooltipsModule(),
                new TracersModule(),
                new TrajectoriesModule(),
                new TrueSightModule(),
                new ViewClipModule(),
                new ViewModelModule(),
                new WaypointsModule(),
                new ZoomModule(),
                // World
                new AirPlaceModule(),
                new AntiInteractModule(),
                new AutoMineModule(),
                new AutoToolModule(),
                new AutoTunnelModule(),
                new AvoidModule(),
                new FastPlaceModule(),
                new MultitaskModule(),
                new NoGlitchBlocksModule(),
                new NukerModule(),
                new ScaffoldModule(),
                new SpeedmineModule()
                // new XRayModule()
        );

        if (OvaqRebornPlusMod.isBaritonePresent())
        {
            register(new BaritoneModule());
        }
        // Register keybinds
        for (Module module : getModules())
        {
            if (module instanceof ToggleModule t)
            {
                Managers.MACRO.register(t.getKeybinding());
            }
        }
        OvaqRebornPlus.info("Registered {} modules!", modules.size());
    }

    /**
     *
     */
    public void postInit()
    {
        // TODO
    }

    /**
     * @param modules
     * @see #register(Module)
     */
    private void register(Module... modules)
    {
        for (Module module : modules)
        {
            register(module);
        }
    }

    /**
     * @param module
     */
    private void register(Module module)
    {
        modules.put(module.getId(), module);
        categoryCounts.merge(module.getCategory(), 1, Integer::sum);
    }

    /**
     * @param id
     * @return
     */
    public Module getModuleById(String id)
    {
        return modules.get(id);
    }

    /**
     * @return
     */
    public List<Module> getModules()
    {
        return new ArrayList<>(modules.values());
    }

    public int getCategoryModuleCount(ModuleCategory category)
    {
        return categoryCounts.getOrDefault(category, 0);
    }

    public Map<ModuleCategory, Integer> getCategoryModuleCounts()
    {
        return Collections.unmodifiableMap(categoryCounts);
    }
}
