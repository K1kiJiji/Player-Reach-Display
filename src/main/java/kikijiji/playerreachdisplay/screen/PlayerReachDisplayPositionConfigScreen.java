package kikijiji.playerreachdisplay.screen;



import net.minecraft.text.Text;

import net.minecraft.util.Formatting;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import kikijiji.playerreachdisplay.PlayerReachDisplay;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig;
import kikijiji.playerreachdisplay.render.PlayerReachDisplayRenderer;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfigManager;



public class PlayerReachDisplayPositionConfigScreen extends Screen
{
    private final Screen parent;
    private final PlayerReachDisplayConfig config;

    private double tempPositionX;
    private double tempPositionY;

    private boolean tempPositionInitialized = false;

    private int dragStartX;
    private int dragStartY;

    private int startHudX;
    private int startHudY;

    private boolean dragging = false;



    /* ----- 제목 ----- */

    public PlayerReachDisplayPositionConfigScreen(Screen parent, PlayerReachDisplayConfig config)
    {
        super(Text.literal("Adjust Position").formatted(Formatting.BOLD));

        this.parent = parent;

        this.config = config;
    }



    /* ----- 준비 ----- */

    @Override
    protected void init()
    {
        if (!tempPositionInitialized)
        {
            initTempPosition();
        }

        this.clearChildren();

        int x = (this.width - (3 * 80 + 2 * 8)) / 2;
        int y = this.height - 40;

        this.addDrawableChild(ButtonWidget.builder
        (
                Text.literal("Reset"),
                buttonWidget ->
                {
                    tempPositionX = 0.0;
                    tempPositionY = 0.0;
                }

        ).dimensions(x, y, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder
        (
                Text.literal("Cancel"),
                buttonWidget -> MinecraftClient.getInstance().setScreen(parent)

        ).dimensions(x + 80 + 8, y, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder
        (
                Text.literal("Apply"),
                buttonWidget ->
                {
                    config.useRelativePosition = true;

                    config.positionX = tempPositionX;
                    config.positionY = tempPositionY;

                    config.offsetX = 0;
                    config.offsetY = 0;

                    if (parent instanceof PlayerReachDisplayConfigScreen configScreen)
                    {
                        configScreen.saveWorkingConfig();
                    }
                    else
                    {
                        PlayerReachDisplay.CONFIG = config.copy();
                        PlayerReachDisplayConfigManager.save(PlayerReachDisplay.CONFIG);
                    }

                    MinecraftClient.getInstance().setScreen(parent);
                }

        ).dimensions(x + (80 + 8) * 2, y, 80, 20).build());
    }

    /* ----- 임시 위치 초기화 ----- */

    private void initTempPosition()
    {
        String text = PlayerReachDisplayRenderer.formatDistance(2.88, config.displayMode);

        PlayerReachDisplayRenderer.HudLayout layout = PlayerReachDisplayRenderer.measure
        (
                this.textRenderer,
                config,
                text,
                this.width,
                this.height
        );

        tempPositionX = PlayerReachDisplayRenderer.ratioFromPixel(layout.x(), this.width, layout.scaledWidth());
        tempPositionY = PlayerReachDisplayRenderer.ratioFromPixel(layout.y(), this.height, layout.scaledHeight());

        tempPositionInitialized = true;
    }



    /* ----- 표시 ----- */

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta)
    {
        this.renderBackground(drawContext, mouseX, mouseY, delta);

        super.render(drawContext, mouseX, mouseY, delta);

        drawContext.drawCenteredTextWithShadow
        (
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );

        PlayerReachDisplayRenderer.render
        (
                drawContext,
                this.textRenderer,
                makePreviewConfig(),
                2.88,
                this.width,
                this.height
        );
    }

    /* ----- 표시 헬퍼 ----- */

    private PlayerReachDisplayConfig makePreviewConfig()
    {
        PlayerReachDisplayConfig preview = config.copy();

        preview.useRelativePosition = true;

        preview.positionX = tempPositionX;
        preview.positionY = tempPositionY;

        return preview;
    }



    /* ----- 키 입력 ----- */

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        // ESC
        if (keyCode == 256)
        {
            MinecraftClient.getInstance().setScreen(parent);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /* ----- 마우스 클릭 ----- */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            PlayerReachDisplayConfig previewConfig = makePreviewConfig();

            String text = PlayerReachDisplayRenderer.formatDistance(2.88, previewConfig.displayMode);

            PlayerReachDisplayRenderer.HudLayout layout = PlayerReachDisplayRenderer.measure
            (
                    this.textRenderer,
                    previewConfig,
                    text,
                    this.width,
                    this.height
            );

            boolean inside = mouseX >= layout.x() &&
                    mouseX <  layout.x() + layout.scaledWidth() &&
                    mouseY >= layout.y() &&
                    mouseY <  layout.y() + layout.scaledHeight();

            if (inside)
            {
                dragging = true;

                dragStartX = (int)mouseX;
                dragStartY = (int)mouseY;

                startHudX = layout.x();
                startHudY = layout.y();

                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /* ----- 마우스 드래그 ----- */

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy)
    {
        if (dragging && button == 0)
        {
            PlayerReachDisplayConfig previewConfig = makePreviewConfig();

            String text = PlayerReachDisplayRenderer.formatDistance(2.88, previewConfig.displayMode);

            PlayerReachDisplayRenderer.HudLayout layout = PlayerReachDisplayRenderer.measure
            (
                    this.textRenderer,
                    previewConfig,
                    text,
                    this.width,
                    this.height
            );

            int rawX = startHudX + (int)(mouseX - dragStartX);
            int rawY = startHudY + (int)(mouseY - dragStartY);

            int maxX = Math.max
            (
                    PlayerReachDisplayRenderer.HUD_MARGIN,
                    this.width - layout.scaledWidth() - PlayerReachDisplayRenderer.HUD_MARGIN
            );

            int maxY = Math.max
            (
                    PlayerReachDisplayRenderer.HUD_MARGIN,
                    this.height - layout.scaledHeight() - PlayerReachDisplayRenderer.HUD_MARGIN
            );

            int clampedX = Math.clamp(rawX, PlayerReachDisplayRenderer.HUD_MARGIN, maxX);
            int clampedY = Math.clamp(rawY, PlayerReachDisplayRenderer.HUD_MARGIN, maxY);

            tempPositionX = PlayerReachDisplayRenderer.ratioFromPixel(clampedX, this.width, layout.scaledWidth());
            tempPositionY = PlayerReachDisplayRenderer.ratioFromPixel(clampedY, this.height, layout.scaledHeight());

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    /* ----- 마우스 릴리즈 ----- */

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            dragging = false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }
}