package kikijiji.playerreachdisplay.screen;



import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

import net.minecraft.network.chat.Component;

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



    /* ----- 생성 ----- */

    public PlayerReachDisplayPositionConfigScreen(Screen parent, PlayerReachDisplayConfig config)
    {
        super(Component.literal("Adjust Position"));

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

        int x = (this.width - (3 * 80 + 2 * 8)) / 2;
        int y = this.height - 40;

        this.addRenderableWidget(Button.builder
        (
                Component.literal("Reset"),
                button ->
                {
                    tempPositionX = 0.0;
                    tempPositionY = 0.0;
                }

        ).bounds(x, y, 80, 20).build());

        this.addRenderableWidget(Button.builder
        (
                Component.literal("Cancel"),
                button -> Minecraft.getInstance().setScreen(parent)

        ).bounds(x + 80 + 8, y, 80, 20).build());

        this.addRenderableWidget(Button.builder
        (
                Component.literal("Apply"),
                button ->
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

                    Minecraft.getInstance().setScreen(parent);
                }

        ).bounds(x + (80 + 8) * 2, y, 80, 20).build());
    }



    /* ----- 임시 위치 초기화 ----- */

    private void initTempPosition()
    {
        String text = PlayerReachDisplayRenderer.formatDistance(2.88, config.displayMode);

        PlayerReachDisplayRenderer.HudLayout layout = PlayerReachDisplayRenderer.measure
        (
                this.font,
                config,
                text,
                this.width,
                this.height
        );

        tempPositionX = PlayerReachDisplayRenderer.ratioFromPixel
        (
                layout.x(),
                this.width,
                layout.scaledWidth()
        );

        tempPositionY = PlayerReachDisplayRenderer.ratioFromPixel
        (
                layout.y(),
                this.height,
                layout.scaledHeight()
        );

        tempPositionInitialized = true;
    }



    /* ----- 표시 ----- */

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta)
    {
        graphics.fill
        (
                0,
                0,
                this.width,
                this.height,
                0x88000000
        );

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        drawTitle(graphics);

        PlayerReachDisplayRenderer.render
        (
                graphics,
                this.font,
                makePreviewConfig(),
                2.88,
                this.width,
                this.height
        );
    }



    /* ----- 표시 헬퍼 ----- */

    private void drawTitle(GuiGraphicsExtractor graphics)
    {
        String titleText = "Adjust Position";

        Font font = this.font;

        graphics.text
        (
                font,
                titleText,
                this.width / 2 - font.width(titleText) / 2,
                20,
                0xFFFFFFFF,
                true
        );
    }

    private PlayerReachDisplayConfig makePreviewConfig()
    {
        PlayerReachDisplayConfig preview = config.copy();

        preview.useRelativePosition = true;

        preview.positionX = tempPositionX;
        preview.positionY = tempPositionY;

        return preview;
    }



    /* ----- 닫기 ----- */

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(parent);
    }



    /* ----- 키 입력 ----- */

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        // ESC
        if (event.key() == 256)
        {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        return super.keyPressed(event);
    }



    /* ----- 마우스 클릭 ----- */

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        double mouseX = event.x();
        double mouseY = event.y();

        int button = event.button();

        if (button == 0)
        {
            PlayerReachDisplayConfig previewConfig = makePreviewConfig();

            String text = PlayerReachDisplayRenderer.formatDistance(2.88, previewConfig.displayMode);

            PlayerReachDisplayRenderer.HudLayout layout = PlayerReachDisplayRenderer.measure
            (
                    this.font,
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

        return super.mouseClicked(event, doubleClick);
    }



    /* ----- 마우스 드래그 ----- */

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
    {
        double mouseX = event.x();
        double mouseY = event.y();

        int button = event.button();

        if (dragging && button == 0)
        {
            PlayerReachDisplayConfig previewConfig = makePreviewConfig();

            String text = PlayerReachDisplayRenderer.formatDistance(2.88, previewConfig.displayMode);

            PlayerReachDisplayRenderer.HudLayout layout = PlayerReachDisplayRenderer.measure
            (
                    this.font,
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

            tempPositionX = PlayerReachDisplayRenderer.ratioFromPixel
            (
                    clampedX,
                    this.width,
                    layout.scaledWidth()
            );

            tempPositionY = PlayerReachDisplayRenderer.ratioFromPixel
            (
                    clampedY,
                    this.height,
                    layout.scaledHeight()
            );

            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }



    /* ----- 마우스 릴리즈 ----- */

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        if (event.button() == 0)
        {
            dragging = false;
        }

        return super.mouseReleased(event);
    }
}