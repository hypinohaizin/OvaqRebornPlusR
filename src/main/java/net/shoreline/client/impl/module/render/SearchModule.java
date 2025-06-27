package net.shoreline.client.impl.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.shoreline.client.api.config.Config;
import net.shoreline.client.api.config.setting.*;
import net.shoreline.client.api.module.ModuleCategory;
import net.shoreline.client.api.module.ToggleModule;
import net.shoreline.client.api.render.Interpolation;
import net.shoreline.client.api.render.RenderBuffers;
import net.shoreline.client.api.render.RenderManager;
import net.shoreline.client.impl.event.TickEvent;
import net.shoreline.client.impl.event.config.ConfigUpdateEvent;
import net.shoreline.client.impl.event.network.GameJoinEvent;
import net.shoreline.client.impl.event.render.RenderWorldEvent;
import net.shoreline.client.impl.event.world.LoadWorldEvent;
import net.shoreline.client.mixin.accessor.AccessorCamera;
import net.shoreline.eventbus.annotation.EventListener;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchModule extends ToggleModule
{
    //
    Config<List<Block>> blocksConfig = register(new BlockListConfig<>("Blocks", "Blocks to be highlighted by the module.",
            Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.DISPENSER,
            Blocks.DROPPER, Blocks.SHULKER_BOX, Blocks.BLACK_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX, Blocks.PINK_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX, Blocks.RED_SHULKER_BOX,
            Blocks.WHITE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.SPAWNER, Blocks.END_PORTAL_FRAME));
    Config<Float> rangeConfig = register(new NumberConfig<>("Range", "The maximum distance (in blocks) within which to highlight blocks.", 80f, 12f, 500f));
    Config<RenderMode> modeConfig = register(new EnumConfig<>("Mode", "Block highlight rendering mode (Fill, Outline, Both).", RenderMode.BOTH, RenderMode.values()));
    Config<Boolean> tracersConfig = register(new BooleanConfig("Tracers", "Draw lines (tracers) to highlighted blocks.", false));
    Config<Float> tracerWidthConfig = register(new NumberConfig<>("TracerWidth", "The width of tracer lines.", 1.5f, 1.0f, 5.0f, () -> tracersConfig.getValue()));
    Config<Color> fillColorConfig = register(new ColorConfig("FillColor", "Color used to fill highlighted blocks.", new Color(60, 120, 255, 50)));
    Config<Color> outlineColorConfig = register(new ColorConfig("OutlineColor", "Color used for the outline of highlighted blocks.", new Color(60, 120, 255, 180)));
    Config<Boolean> customColorConfig = register(new BooleanConfig("CustomColor", "Prioritize the custom fill/outline color settings over default color logic.", false)); //Config<Boolean> softReloadConfig = register(new BooleanConfig("SoftReload", "Reloads without clearing the renders", false));

    private List<SearchBlock> renderBlocks = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean lock = false;
    private long lastReload = 0L;

    public SearchModule() {
        super("Search", "Highlights specified blocks in the world", ModuleCategory.RENDER);
    }

    @Override
    public void onEnable() {
        renderBlocks.clear();
        lock = false;
        lastReload = 0L;
    }

    @Override
    public void onDisable() {
        renderBlocks.clear();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (lock) return;
        if (System.currentTimeMillis() - lastReload > 500) {
            lock = true;
            CompletableFuture.supplyAsync(this::findBlocks, executor).thenAcceptAsync(this::sync, net.minecraft.util.Util.getMainWorkerExecutor());
            lastReload = System.currentTimeMillis();
        }
    }

    @EventListener
    public void onGameJoin(GameJoinEvent event) {
        renderBlocks.clear();
    }

    @EventListener
    public void onChangeDimension(LoadWorldEvent event) {
        renderBlocks.clear();
    }

    @EventListener
    public void onConfigUpdate(ConfigUpdateEvent event) {
        if (event.getConfig() == blocksConfig) {
            renderBlocks.clear();
        }
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (mc.player == null || mc.world == null || renderBlocks.isEmpty()) return;

        if (tracersConfig.getValue()) {
            MatrixStack matrixStack = new MatrixStack();
            double fov = mc.options.getFov().getValue();
            matrixStack.multiplyPositionMatrix(mc.gameRenderer.getBasicProjectionMatrix(fov));
            Matrix4f prevProjectionMatrix = RenderSystem.getProjectionMatrix();
            RenderSystem.setProjectionMatrix(matrixStack.peek().getPositionMatrix(), VertexSorter.BY_DISTANCE);

            RenderBuffers.preRender();

            Vec3d playerPos = Interpolation.getRenderPosition(mc.player, event.getTickDelta());
            Camera camera = mc.gameRenderer.getCamera();
            double eyeHeight = MathHelper.lerp(event.getTickDelta(), ((AccessorCamera) camera).getLastCameraY(), ((AccessorCamera) camera).getCameraY());
            double px = mc.player.getX() - playerPos.x;
            double py = mc.player.getY() - playerPos.y + eyeHeight;
            double pz = mc.player.getZ() - playerPos.z;
            float pitch = mc.player.getPitch();
            float yaw = mc.player.getYaw();

            if (FreecamModule.getInstance().isEnabled()) {
                Vec3d pos1 = FreecamModule.getInstance().getCameraPosition();
                Vec3d pos2 = Interpolation.getRenderPosition(pos1, FreecamModule.getInstance().getLastCameraPosition(), event.getTickDelta());
                float[] rotations = FreecamModule.getInstance().getCameraRotations();
                px = pos1.x - pos2.x;
                py = pos1.y - pos2.y;
                pz = pos1.z - pos2.z;
                yaw = rotations[0];
                pitch = rotations[1];
            }

            Vec3d from = new Vec3d(0.0, 0.0, 1.0)
                    .rotateX(-(float) Math.toRadians(pitch))
                    .rotateY(-(float) Math.toRadians(yaw))
                    .add(new Vec3d(px, py, pz));

            for (SearchBlock block : renderBlocks) {
                Vec3d center = block.box.getCenter();
                RenderManager.renderLine(
                        event.getMatrices(),
                        from.x, from.y, from.z,
                        center.x, center.y, center.z,
                        tracerWidthConfig.getValue(),
                        getTracerColor(block.state, block.pos)
                );
            }

            RenderBuffers.postRender();
            RenderSystem.setProjectionMatrix(prevProjectionMatrix, VertexSorter.BY_DISTANCE);
        }
        RenderBuffers.preRender();
        for (SearchBlock block : renderBlocks) {
            Color fill = customColorConfig.getValue() ? fillColorConfig.getValue() : getBlockColor(block.state, block.pos, fillColorConfig.getValue().getAlpha());
            Color outline = customColorConfig.getValue() ? outlineColorConfig.getValue() : getBlockColor(block.state, block.pos, outlineColorConfig.getValue().getAlpha());
            if (modeConfig.getValue() == RenderMode.FILL || modeConfig.getValue() == RenderMode.BOTH)
                RenderManager.renderBox(event.getMatrices(), block.box, fill.getRGB());
            if (modeConfig.getValue() == RenderMode.OUTLINE || modeConfig.getValue() == RenderMode.BOTH)
                RenderManager.renderBoundingBox(event.getMatrices(), block.box, 1.7f, outline.getRGB());
        }
        RenderBuffers.postRender();
    }

    private List<SearchBlock> findBlocks() {
        List<SearchBlock> found = new ArrayList<>();
        BlockPos player = mc.player.getBlockPos();
        int r = (int) (float) rangeConfig.getValue();
        for (int x = player.getX() - r; x <= player.getX() + r; x++) {
            for (int z = player.getZ() - r; z <= player.getZ() + r; z++) {
                int minY = mc.world.getBottomY();
                int maxY = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (!state.isAir() && isSearchBlock(state)) {
                        VoxelShape shape = state.getOutlineShape(mc.world, pos);
                        if (shape.isEmpty()) continue;
                        Box box = shape.getBoundingBox().offset(pos);
                        found.add(new SearchBlock(box, pos, state));
                    }
                }
            }
        }
        return found;
    }

    private void sync(List<SearchBlock> blocks) {
        this.renderBlocks = blocks;
        lock = false;
    }

    private Color getBlockColor(BlockState state, BlockPos pos, int alpha) {
        Block b = state.getBlock();
        if (b == Blocks.NETHER_PORTAL) return new Color(100, 50, 255, alpha);
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DIAMOND_BLOCK) return new Color(70, 150, 255, alpha);
        if (b == Blocks.GOLD_ORE || b == Blocks.GOLD_BLOCK) return new Color(255, 200, 70, alpha);
        if (b == Blocks.EMERALD_ORE || b == Blocks.EMERALD_BLOCK) return new Color(70, 255, 90, alpha);
        if (b == Blocks.REDSTONE_ORE || b == Blocks.REDSTONE_BLOCK) return new Color(250, 30, 30, alpha);
        if (b == Blocks.LAPIS_ORE || b == Blocks.LAPIS_BLOCK) return new Color(30, 50, 250, alpha);
        if (b == Blocks.IRON_ORE || b == Blocks.IRON_BLOCK) return new Color(170, 150, 130, alpha);
        if (b == Blocks.COAL_ORE || b == Blocks.COAL_BLOCK) return new Color(35, 35, 35, alpha);
        if (b == Blocks.NETHERITE_BLOCK) return new Color(140, 30, 15, alpha);
        int rgb = state.getMapColor(mc.world, pos).color;
        return new Color((rgb >> 16) & 255, (rgb >> 8) & 255, (rgb) & 255, alpha);
    }

    private int getTracerColor(BlockState state, BlockPos pos) {
        Block b = state.getBlock();
        if (b == Blocks.NETHER_PORTAL) return 0xFF6432D3;
        if (b == Blocks.DIAMOND_ORE || b == Blocks.DIAMOND_BLOCK) return 0xFF4696FF;
        if (b == Blocks.GOLD_ORE || b == Blocks.GOLD_BLOCK) return 0xFFFFC846;
        if (b == Blocks.EMERALD_ORE || b == Blocks.EMERALD_BLOCK) return 0xFF46FF5A;
        if (b == Blocks.REDSTONE_ORE || b == Blocks.REDSTONE_BLOCK) return 0xFFFF3232;
        if (b == Blocks.LAPIS_ORE || b == Blocks.LAPIS_BLOCK) return 0xFF3232FF;
        if (b == Blocks.IRON_ORE || b == Blocks.IRON_BLOCK) return 0xFFAA9682;
        if (b == Blocks.COAL_ORE || b == Blocks.COAL_BLOCK) return 0xFF232323;
        if (b == Blocks.NETHERITE_BLOCK) return 0xFF8C1E0F;
        int rgb = state.getMapColor(mc.world, pos).color;
        return 0xFF000000 | rgb;
    }

    private boolean isSearchBlock(BlockState state) {
        return ((BlockListConfig) blocksConfig).contains(state.getBlock());
    }

    public enum RenderMode {
        FILL,
        OUTLINE,
        BOTH
    }

    public static class SearchBlock {
        public final Box box;
        public final BlockPos pos;
        public final BlockState state;
        public SearchBlock(Box box, BlockPos pos, BlockState state) {
            this.box = box;
            this.pos = pos;
            this.state = state;
        }
    }
}
