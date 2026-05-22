package kikijiji.playerreachdisplay.render;



import java.text.DecimalFormat;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig.DistanceColorBand;



public final class PlayerReachDisplayRenderer
{
    private static final float MIN_SCALE = 0.1f;
    public  static final int   HUD_MARGIN = 4;
    private static final int   BACKGROUND_PADDING = 2;

    private static final DecimalFormat FORMAT = new DecimalFormat("0.00");



    /* ----- 생성자 차단 ----- */

    private PlayerReachDisplayRenderer()
    {
    }



    /* ----- 정보 묶음 ----- */

    public record HudLayout
    (
            int   x,
            int   y,
            int   padding,
            int   rawWidth,
            int   rawHeight,
            int   scaledWidth,
            int   scaledHeight,
            float scale
    )
    {
    }

    public record HudColors
    (
            int textColor,
            int shadowColor,
            int backgroundColor
    )
    {
    }



    /* ----- 렌더 ----- */

    public static void render
    (
            GuiGraphicsExtractor   graphics,
            Font                   font,
            PlayerReachDisplayConfig config,
            double                 distance,
            int                    screenWidth,
            int                    screenHeight
    )
    {
        if (config == null || !config.showReach)
        {
            return;
        }

        String text = formatDistance(distance, config.displayMode);

        HudLayout layout = measure(font, config, text, screenWidth, screenHeight);
        HudColors colors = resolveColors(config, distance);

        Matrix3x2fStack matrices = graphics.pose();

        matrices.pushMatrix();

        matrices.translate
        (
                (float)layout.x(),
                (float)layout.y()
        );

        matrices.scale
        (
                layout.scale(),
                layout.scale()
        );

        if (config.showBackground)
        {
            graphics.fill
            (
                    0,
                    0,
                    layout.rawWidth(),
                    layout.rawHeight(),
                    colors.backgroundColor()
            );
        }

        if (config.showShadow)
        {
            graphics.text
            (
                    font,
                    text,
                    layout.padding() + 1,
                    layout.padding() + 1,
                    colors.shadowColor(),
                    false
            );
        }

        graphics.text
        (
                font,
                text,
                layout.padding(),
                layout.padding(),
                colors.textColor(),
                false
        );

        matrices.popMatrix();
    }



    /* ----- 표시 포맷 ----- */

    public static String formatDistance(double distance, PlayerReachDisplayConfig.DisplayMode mode)
    {
        String text = FORMAT.format(distance);

        return switch (mode)
        {
            case NUMBER_ONLY -> text;
            case WITH_BLOCKS -> text + " blocks";
            case WITH_M      -> text + " M";
        };
    }



    /* ----- 측정 ----- */

    public static HudLayout measure
    (
            Font                   font,
            PlayerReachDisplayConfig config,
            String                 text,
            int                    screenWidth,
            int                    screenHeight
    )
    {
        float scale = Math.max(MIN_SCALE, config.scale / 100.0f);

        int padding = config.showBackground ? BACKGROUND_PADDING : 0;

        int shadowExtra = config.showShadow ? 1 : 0;

        int rawWidth  = font.width(text) + padding * 2 + shadowExtra;
        int rawHeight = font.lineHeight + padding * 2 + shadowExtra;

        int scaledWidth  = (int)Math.ceil(rawWidth * scale);
        int scaledHeight = (int)Math.ceil(rawHeight * scale);

        int x = resolveX(config, screenWidth, scaledWidth);
        int y = resolveY(config, screenHeight, scaledHeight);

        return new HudLayout
        (
                x,
                y,
                padding,
                rawWidth,
                rawHeight,
                scaledWidth,
                scaledHeight,
                scale
        );
    }



    /* ----- 측정 헬퍼 ----- */

    private static int resolveX(PlayerReachDisplayConfig config, int screenWidth, int scaledWidth)
    {
        if (config.useRelativePosition)
        {
            return pixelFromRatio(config.positionX, screenWidth, scaledWidth);
        }

        return clamp
        (
                HUD_MARGIN + config.offsetX,
                HUD_MARGIN,
                Math.max(HUD_MARGIN, screenWidth - scaledWidth - HUD_MARGIN)
        );
    }

    private static int resolveY(PlayerReachDisplayConfig config, int screenHeight, int scaledHeight)
    {
        if (config.useRelativePosition)
        {
            return pixelFromRatio(config.positionY, screenHeight, scaledHeight);
        }

        return clamp
        (
                HUD_MARGIN + config.offsetY,
                HUD_MARGIN,
                Math.max(HUD_MARGIN, screenHeight - scaledHeight - HUD_MARGIN)
        );
    }

    public static double ratioFromPixel(int pixel, int screenSize, int hudSize)
    {
        int available = Math.max(1, screenSize - hudSize - HUD_MARGIN * 2);

        return clamp01((pixel - HUD_MARGIN) / (double)available);
    }

    private static int pixelFromRatio(double ratio, int screenSize, int hudSize)
    {
        int available = Math.max(0, screenSize - hudSize - HUD_MARGIN * 2);

        return HUD_MARGIN + (int)Math.round(clamp01(ratio) * available);
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.clamp(value, min, max);
    }

    private static double clamp01(double value)
    {
        return Math.clamp(value, 0.0, 1.0);
    }



    /* ----- 색상 결정 ----- */

    private static HudColors resolveColors(PlayerReachDisplayConfig config, double distance)
    {
        HudColors colors = defaultColors(config);

        if (!config.enableDistanceColor || config.distanceBands == null || config.distanceBands.isEmpty())
        {
            return colors;
        }

        for (DistanceColorBand band : config.distanceBands)
        {
            if (band == null)
            {
                continue;
            }

            if (distance >= band.fromDistance)
            {
                colors = colorsFromBand(band);
            }
            else
            {
                break;
            }
        }

        return colors;
    }



    /* ----- 색상 결정 헬퍼 ----- */

    private static HudColors defaultColors(PlayerReachDisplayConfig config)
    {
        return new HudColors
        (
                config.textColor,
                config.shadowColor,
                config.backgroundColor
        );
    }

    private static HudColors colorsFromBand(DistanceColorBand band)
    {
        return new HudColors
        (
                band.textColor,
                band.shadowColor,
                band.backgroundColor
        );
    }
}