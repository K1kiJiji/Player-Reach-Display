package kikijiji.playerreachdisplay;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.resources.Identifier;

import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;

import net.minecraft.util.Mth;

import kikijiji.playerreachdisplay.filter.EntityFilters;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfigManager;
import kikijiji.playerreachdisplay.screen.PlayerReachDisplayPositionConfigScreen;

import kikijiji.playerreachdisplay.render.PlayerReachDisplayRenderer;



public class PlayerReachDisplay implements ClientModInitializer
{
    public static final String MOD_ID = "player-reach-display";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static PlayerReachDisplayConfig CONFIG;

    private static double lastHitDistance   = 0.0;
    private static long   lastHitTimeMillis = 0L;



    @Override
    public void onInitializeClient()
    {
        CONFIG = PlayerReachDisplayConfigManager.load();

        AttackEntityCallback.EVENT.register(PlayerReachDisplay::onAttackEntity);

        HudElementRegistry.addLast
        (
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                PlayerReachDisplay::renderHud
        );
    }



    /* ----- 공격 기록 ----- */

    @SuppressWarnings("SameReturnValue")
    private static InteractionResult onAttackEntity
    (
            Player          player,
            Level           level,
            InteractionHand hand,
            Entity          entity,
            EntityHitResult hitResult
    )
    {
        if (!level.isClientSide() || entity == null)
        {
            return InteractionResult.PASS;
        }

        if (CONFIG == null)
        {
            return InteractionResult.PASS;
        }

        if (!EntityFilters.shouldTrack(entity, CONFIG))
        {
            return InteractionResult.PASS;
        }

        Minecraft client = Minecraft.getInstance();

        lastHitDistance   = computeHitDistance(client, entity);
        lastHitTimeMillis = System.currentTimeMillis();

        return InteractionResult.PASS;
    }



    /* ----- 거리 계산 ----- */

    private static double computeHitDistance(Minecraft client, Entity target)
    {
        Player player = client.player;

        if (player == null || target == null)
        {
            return 0.0;
        }

        Vec3 eyePosition = player.getEyePosition(1.0F);

        AABB hitBox = target.getBoundingBox();

        double clampedX = Mth.clamp(eyePosition.x, hitBox.minX, hitBox.maxX);
        double clampedY = Mth.clamp(eyePosition.y, hitBox.minY, hitBox.maxY);
        double clampedZ = Mth.clamp(eyePosition.z, hitBox.minZ, hitBox.maxZ);

        double dx = clampedX - eyePosition.x;
        double dy = clampedY - eyePosition.y;
        double dz = clampedZ - eyePosition.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }



    /* ----- HUD 표시 ----- */

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker tickCounter)
    {
        Minecraft client = Minecraft.getInstance();

        if (shouldSkipHud(client))
        {
            return;
        }

        PlayerReachDisplayConfig config = CONFIG;

        if (!config.showReach)
        {
            return;
        }

        long now = System.currentTimeMillis();

        double displayDistance = getDisplayDistance(config, now);

        int screenWidth  = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        PlayerReachDisplayRenderer.render
        (
                graphics,
                client.font,
                config,
                displayDistance,
                screenWidth,
                screenHeight
        );
    }



    /* ----- HUD 표시 헬퍼 ----- */

    private static boolean shouldSkipHud(Minecraft client)
    {
        if (client == null || client.player == null)
        {
            return true;
        }

        if (CONFIG == null)
        {
            return true;
        }

        return client.screen instanceof PlayerReachDisplayPositionConfigScreen;
    }



    /* ----- 표시 거리 결정 ----- */

    private static double getDisplayDistance(PlayerReachDisplayConfig config, long now)
    {
        if (lastHitTimeMillis == 0L)
        {
            return 0.0;
        }

        if (config.keepLastHitDistance)
        {
            return lastHitDistance;
        }

        double elapsedSeconds = (now - lastHitTimeMillis) / 1000.0;

        if (elapsedSeconds > config.resetAfterSeconds)
        {
            return 0.0;
        }

        return lastHitDistance;
    }
}