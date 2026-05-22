package kikijiji.playerreachdisplay;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.EntityHitResult;

import net.minecraft.world.World;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import kikijiji.playerreachdisplay.filter.EntityFilters;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig;
import kikijiji.playerreachdisplay.render.PlayerReachDisplayRenderer;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfigManager;
import kikijiji.playerreachdisplay.screen.PlayerReachDisplayPositionConfigScreen;



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
        HudRenderCallback.EVENT.register(PlayerReachDisplay::renderHud);
    }



    /* ----- 공격 기록 ----- */

    @SuppressWarnings("SameReturnValue")
    private static ActionResult onAttackEntity
    (
            PlayerEntity    player,
            World           world,
            Hand            hand,
            Entity          entity,
            EntityHitResult hitResult
    )
    {
        if (!world.isClient() || entity == null)
        {
            return ActionResult.PASS;
        }

        if (CONFIG == null)
        {
            return ActionResult.PASS;
        }

        if (!EntityFilters.shouldTrack(entity, CONFIG))
        {
            return ActionResult.PASS;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        lastHitDistance   = computeHitDistance(client, entity);
        lastHitTimeMillis = System.currentTimeMillis();

        return ActionResult.PASS;
    }



    /* ----- 거리 계산 ----- */

    private static double computeHitDistance(MinecraftClient client, Entity target)
    {
        PlayerEntity player = client.player;

        if (player == null || target == null)
        {
            return 0.0;
        }

        Vec3d eyePosition = player.getCameraPosVec(1.0F);

        Box hitBox = target.getBoundingBox();

        double clampedX = MathHelper.clamp(eyePosition.x, hitBox.minX, hitBox.maxX);
        double clampedY = MathHelper.clamp(eyePosition.y, hitBox.minY, hitBox.maxY);
        double clampedZ = MathHelper.clamp(eyePosition.z, hitBox.minZ, hitBox.maxZ);

        double dx = clampedX - eyePosition.x;
        double dy = clampedY - eyePosition.y;
        double dz = clampedZ - eyePosition.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }



    /* ----- HUD 표시 ----- */

    private static void renderHud(DrawContext drawContext, RenderTickCounter tickCounter)
    {
        MinecraftClient client = MinecraftClient.getInstance();

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

        int screenWidth  = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        PlayerReachDisplayRenderer.render
        (
                drawContext,
                client.textRenderer,
                config,
                displayDistance,
                screenWidth,
                screenHeight
        );
    }

    /* ----- HUD 표시 헬퍼 ----- */

    private static boolean shouldSkipHud(MinecraftClient client)
    {
        if (client == null || client.player == null)
        {
            return true;
        }

        if (CONFIG == null)
        {
            return true;
        }

        return client.currentScreen instanceof PlayerReachDisplayPositionConfigScreen;
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