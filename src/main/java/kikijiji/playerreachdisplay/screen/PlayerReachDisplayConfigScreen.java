package kikijiji.playerreachdisplay.screen;



import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.DoubleConsumer;

import net.minecraft.ChatFormatting;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import kikijiji.playerreachdisplay.PlayerReachDisplay;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig;
import kikijiji.playerreachdisplay.render.PlayerReachDisplayRenderer;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfigManager;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig.DistanceColorBand;



public class PlayerReachDisplayConfigScreen extends Screen
{
    private static final Identifier TOGGLE_ON_ICON     = Identifier.fromNamespaceAndPath(PlayerReachDisplay.MOD_ID, "textures/gui/toggle_on.png");
    private static final Identifier TOGGLE_OFF_ICON    = Identifier.fromNamespaceAndPath(PlayerReachDisplay.MOD_ID, "textures/gui/toggle_off.png");
    private static final Identifier RESET_ICON         = Identifier.fromNamespaceAndPath(PlayerReachDisplay.MOD_ID, "textures/gui/reset.png");
    private static final Identifier PREVIEW_BACKGROUND = Identifier.fromNamespaceAndPath(PlayerReachDisplay.MOD_ID, "textures/gui/preview_background.png");

    private static final int PREVIEW_TEXTURE_WIDTH  = 385;
    private static final int PREVIEW_TEXTURE_HEIGHT = 215;

    private static final int START_Y = 30;

    private final Screen parent;

    private final PlayerReachDisplayConfig defaultConfig = new PlayerReachDisplayConfig();
    private PlayerReachDisplayConfig       workingConfig;

    private int scrollOffset = 0;
    private int maxScroll    = 0;



    private final List<AbstractWidget> leftControls     = new ArrayList<>();
    private final List<Integer>        leftControlBaseY = new ArrayList<>();

    private final List<SectionHeader> sectionHeaders = new ArrayList<>();



    private Button textToggle;
    private Button textReset;

    private Button shadowToggle;
    private Button shadowReset;

    private Button backgroundToggle;
    private Button backgroundReset;



    private ConfigSliderWidget scaleSlider;
    private Button             scaleReset;

    private Button positionToggle;



    private Button textColorButton;
    private Button textColorReset;

    private Button shadowColorButton;
    private Button shadowColorReset;

    private Button backgroundColorButton;
    private Button backgroundColorReset;



    private Button keepLastDistanceToggle;
    private Button keepLastDistanceReset;

    private ConfigSliderWidget resetSecondsSlider;
    private Button             resetSecondsReset;



    private Button displayModeButton;
    private Button displayModeReset;



    private Button entityFilterToggle;
    private Button entityFilterToggleReset;

    private Button entityFilterModeButton;
    private Button entityFilterModeReset;

    private Button whitelistButton;
    private Button whitelistReset;

    private Button blacklistButton;
    private Button blacklistReset;



    private Button distanceColorToggle;
    private Button distanceColorReset;

    private Button distanceBandAddButton;
    private Button distanceBandRemoveButton;

    private final List<ConfigSliderWidget> bandFromSliders                 = new ArrayList<>();
    private final List<Button>             bandFromResetButtons            = new ArrayList<>();
    private final List<Button>             bandTextColorButtons            = new ArrayList<>();
    private final List<Button>             bandTextColorResetButtons       = new ArrayList<>();
    private final List<Button>             bandShadowColorButtons          = new ArrayList<>();
    private final List<Button>             bandShadowColorResetButtons     = new ArrayList<>();
    private final List<Button>             bandBackgroundColorButtons      = new ArrayList<>();
    private final List<Button>             bandBackgroundColorResetButtons = new ArrayList<>();



    private ColorPickerPopup colorPopup;
    private EntityListPopup  entityListPopup;



    private record SectionHeader(String title, int baseY)
    {
    }

    private record PreviewLayout
    (
            int x,
            int y,
            int width,
            int height
    )
    {
    }



    private class ConfigSliderWidget extends AbstractSliderButton
    {
        private final String         label;
        private final double         min;
        private final double         max;
        private final int            decimals;
        private final DoubleConsumer setter;



        private ConfigSliderWidget
        (
                int            x,
                int            y,
                int            width,
                int            height,
                String         label,
                double         min,
                double         max,
                double         current,
                int            decimals,
                DoubleConsumer setter
        )
        {
            super
            (
                    x,
                    y,
                    width,
                    height,
                    Component.empty(),
                    toSliderValue(current, min, max)
            );

            this.label    = label;
            this.min      = min;
            this.max      = max;
            this.decimals = decimals;
            this.setter   = setter;

            updateMessage();
        }



        @Override
        protected void updateMessage()
        {
            double actualValue = getActualValue();

            String valueText;

            if (decimals <= 0)
            {
                valueText = Integer.toString((int)Math.round(actualValue));
            }
            else
            {
                valueText = String.format(Locale.ROOT, "%." + decimals + "f", actualValue);
            }

            this.setMessage(Component.literal(label + ": " + valueText));
        }

        @Override
        protected void applyValue()
        {
            double actualValue = getActualValue();

            setter.accept(actualValue);
            updateEnableStates();
        }



        private double getActualValue()
        {
            double actualValue = min + this.value * (max - min);

            if (decimals <= 0)
            {
                return Math.round(actualValue);
            }

            return actualValue;
        }

        private void setActualValue(double actualValue)
        {
            this.value = toSliderValue(actualValue, min, max);
            updateMessage();
        }
    }



    /* ----- 생성 ----- */

    public PlayerReachDisplayConfigScreen(Screen parent)
    {
        super(Component.literal("Player Reach Display Config").withStyle(ChatFormatting.BOLD));

        this.parent = parent;

        this.workingConfig = PlayerReachDisplay.CONFIG == null
                ? new PlayerReachDisplayConfig()
                : PlayerReachDisplay.CONFIG.copy();
    }



    /* ----- 준비 ----- */

    @Override
    protected void init()
    {
        this.clearWidgets();

        sectionHeaders.clear();

        leftControls.clear();
        leftControlBaseY.clear();

        bandFromSliders.clear();
        bandFromResetButtons.clear();

        bandTextColorButtons.clear();
        bandTextColorResetButtons.clear();

        bandShadowColorButtons.clear();
        bandShadowColorResetButtons.clear();

        bandBackgroundColorButtons.clear();
        bandBackgroundColorResetButtons.clear();



        int buttonHeight = 20;

        int leftWidth  = this.width / 2 + this.width / 6;
        int rightWidth = this.width - leftWidth;

        int buttonWidth     = Math.max(100, leftWidth - 40);
        int fullButtonWidth = Math.max(100, leftWidth - 20);

        int x = leftWidth / 2 - buttonWidth / 2;
        int y = START_Y + 45;

        int resetX = x - 10 + buttonWidth;

        int rightMargin = 20;
        int rightInnerX = leftWidth + rightMargin;

        int gap = 4;

        int actionTopY = this.height - 50 - buttonHeight;
        int applyY     = this.height - 25 - buttonHeight;

        int actionAreaWidth = Math.max(120, rightWidth - rightMargin * 2);
        int actionWidth     = (actionAreaWidth - gap) / 2;

        int resetActionX  = rightInnerX;
        int cancelActionX = rightInnerX + actionWidth + gap;

        int applyWidth = actionAreaWidth;
        int applyX     = rightInnerX;



        /* ----- Appearance ----- */

        addSectionHeader("Appearance", y - 20);

        textToggle = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.showReach = !workingConfig.showReach;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        textReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.showReach = defaultConfig.showReach;
                    workingConfig.textColor = defaultConfig.textColor;

                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        shadowToggle = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.showShadow = !workingConfig.showShadow;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        shadowReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.showShadow = defaultConfig.showShadow;
                    workingConfig.shadowColor = defaultConfig.shadowColor;

                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        backgroundToggle = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.showBackground = !workingConfig.showBackground;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        backgroundReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.showBackground = defaultConfig.showBackground;
                    workingConfig.backgroundColor = defaultConfig.backgroundColor;

                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Transform ----- */

        addSectionHeader("Transform", y - 15);

        scaleSlider = addLeftSlider
        (
                "Scale",
                10.0,
                400.0,
                workingConfig.scale,
                0,
                value -> workingConfig.scale = (int)Math.round(value),
                x - 10,
                y,
                buttonWidth,
                buttonHeight
        );

        scaleReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.scale = defaultConfig.scale;
                    this.init();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        positionToggle = addLeftButton(Button.builder
        (
                Component.literal(""),
                button -> Minecraft.getInstance().setScreen
                (
                        new PlayerReachDisplayPositionConfigScreen(this, this.workingConfig)
                )

        ), x - 10, y, fullButtonWidth, buttonHeight);

        y += 45;



        /* ----- Color ----- */

        addSectionHeader("Color", y - 15);

        textColorButton = addLeftButton(Button.builder
        (
                Component.literal(""),
                button -> openColorPopup
                (
                        "Text Color",
                        workingConfig.textColor,
                        defaultConfig.textColor,
                        color -> workingConfig.textColor = color
                )

        ), x - 10, y, buttonWidth, buttonHeight);

        textColorReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.textColor = defaultConfig.textColor;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        shadowColorButton = addLeftButton(Button.builder
        (
                Component.literal(""),
                button -> openColorPopup
                (
                        "Shadow Color",
                        workingConfig.shadowColor,
                        defaultConfig.shadowColor,
                        color -> workingConfig.shadowColor = color
                )

        ), x - 10, y, buttonWidth, buttonHeight);

        shadowColorReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.shadowColor = defaultConfig.shadowColor;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        backgroundColorButton = addLeftButton(Button.builder
        (
                Component.literal(""),
                button -> openColorPopup
                (
                        "Background Color",
                        workingConfig.backgroundColor,
                        defaultConfig.backgroundColor,
                        color -> workingConfig.backgroundColor = color
                )

        ), x - 10, y, buttonWidth, buttonHeight);

        backgroundColorReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.backgroundColor = defaultConfig.backgroundColor;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Distance Reset ----- */

        addSectionHeader("Distance Reset", y - 15);

        keepLastDistanceToggle = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.keepLastHitDistance = !workingConfig.keepLastHitDistance;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        keepLastDistanceReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.keepLastHitDistance = defaultConfig.keepLastHitDistance;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        resetSecondsSlider = addLeftSlider
        (
                "Reset After",
                0.0,
                60.0,
                workingConfig.resetAfterSeconds,
                1,
                value -> workingConfig.resetAfterSeconds = value,
                x - 10,
                y,
                buttonWidth,
                buttonHeight
        );

        resetSecondsReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.resetAfterSeconds = defaultConfig.resetAfterSeconds;
                    this.init();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Display Mode ----- */

        addSectionHeader("Display Mode", y - 15);

        displayModeButton = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    switch (workingConfig.displayMode)
                    {
                        case NUMBER_ONLY -> workingConfig.displayMode = PlayerReachDisplayConfig.DisplayMode.WITH_BLOCKS;
                        case WITH_BLOCKS -> workingConfig.displayMode = PlayerReachDisplayConfig.DisplayMode.WITH_M;
                        case WITH_M      -> workingConfig.displayMode = PlayerReachDisplayConfig.DisplayMode.NUMBER_ONLY;
                    }

                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        displayModeReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.displayMode = defaultConfig.displayMode;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Entity Filter ----- */

        addSectionHeader("Entity Filter", y - 20);

        entityFilterToggle = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.enableEntityFilter = !workingConfig.enableEntityFilter;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        entityFilterToggleReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.enableEntityFilter = defaultConfig.enableEntityFilter;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        entityFilterModeButton = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.useWhitelist = !workingConfig.useWhitelist;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        entityFilterModeReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.useWhitelist = defaultConfig.useWhitelist;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        whitelistButton = addLeftButton(Button.builder
        (
                Component.literal(""),
                button -> openEntityListPopup
                (
                        "Edit Whitelist",
                        workingConfig.whitelist,
                        defaultConfig.whitelist,
                        result -> workingConfig.whitelist = new ArrayList<>(result)
                )

        ), x - 10, y, buttonWidth, buttonHeight);

        whitelistReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.whitelist = new ArrayList<>(defaultConfig.whitelist);
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        blacklistButton = addLeftButton(Button.builder
        (
                Component.literal(""),
                button -> openEntityListPopup
                (
                        "Edit Blacklist",
                        workingConfig.blacklist,
                        defaultConfig.blacklist,
                        result -> workingConfig.blacklist = new ArrayList<>(result)
                )

        ), x - 10, y, buttonWidth, buttonHeight);

        blacklistReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.blacklist = new ArrayList<>(defaultConfig.blacklist);
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Distance Color ----- */

        addSectionHeader("Distance Color", y - 20);

        distanceColorToggle = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.enableDistanceColor = !workingConfig.enableDistanceColor;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        distanceColorReset = addLeftButton(Button.builder
        (
                Component.literal(""),
                button ->
                {
                    workingConfig.enableDistanceColor = defaultConfig.enableDistanceColor;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        int bandButtonGap = 4;
        int bandButtonWidth = (fullButtonWidth - bandButtonGap) / 2;

        distanceBandRemoveButton = addLeftButton(Button.builder
        (
                Component.literal("Remove Band"),
                button -> removeDistanceBand()

        ), x - 10, y, bandButtonWidth, buttonHeight);

        distanceBandAddButton = addLeftButton(Button.builder
        (
                Component.literal("Add Band"),
                button -> addDistanceBand()

        ), x - 10 + bandButtonWidth + bandButtonGap, y, bandButtonWidth, buttonHeight);

        y += 25;

        int bandRowX = x - 10;
        int bandResetX = bandRowX + fullButtonWidth - 20;
        int bandControlWidth = fullButtonWidth - 20;

        for (int i = 0; i < workingConfig.distanceBands.size(); i++)
        {
            final int bandIndex = i;
            DistanceColorBand band = workingConfig.distanceBands.get(bandIndex);

            if (bandIndex > 0)
            {
                ConfigSliderWidget fromSlider = addLeftSlider
                (
                        "Band " + (bandIndex + 1) + " From",
                        PlayerReachDisplayConfig.MIN_DISTANCE_BAND_FROM,
                        PlayerReachDisplayConfig.MAX_DISTANCE_BAND_FROM,
                        band.fromDistance,
                        2,
                        value -> setBandFromDistance(bandIndex, value),
                        bandRowX,
                        y,
                        bandControlWidth,
                        buttonHeight
                );

                bandFromSliders.add(fromSlider);

                Button fromReset = addLeftButton(Button.builder
                (
                        Component.literal(""),
                        button -> resetBandFromDistance(bandIndex)

                ), bandResetX, y, 20, buttonHeight);

                bandFromResetButtons.add(fromReset);

                y += 25;
            }

            Button textBandColor = addLeftButton(Button.builder
            (
                    Component.literal(""),
                    button -> openColorPopup
                    (
                            "Band " + (bandIndex + 1) + " Main Color",
                            workingConfig.distanceBands.get(bandIndex).textColor,
                            defaultBand(bandIndex).textColor,
                            color -> workingConfig.distanceBands.get(bandIndex).textColor = color
                    )

            ), bandRowX, y, bandControlWidth, buttonHeight);

            bandTextColorButtons.add(textBandColor);

            Button textBandColorReset = addLeftButton(Button.builder
            (
                    Component.literal(""),
                    button -> resetBandTextColor(bandIndex)

            ), bandResetX, y, 20, buttonHeight);

            bandTextColorResetButtons.add(textBandColorReset);

            y += 25;

            Button shadowBandColor = addLeftButton(Button.builder
            (
                    Component.literal(""),
                    button -> openColorPopup
                    (
                            "Band " + (bandIndex + 1) + " Shadow Color",
                            workingConfig.distanceBands.get(bandIndex).shadowColor,
                            defaultBand(bandIndex).shadowColor,
                            color -> workingConfig.distanceBands.get(bandIndex).shadowColor = color
                    )

            ), bandRowX, y, bandControlWidth, buttonHeight);

            bandShadowColorButtons.add(shadowBandColor);

            Button shadowBandColorReset = addLeftButton(Button.builder
            (
                    Component.literal(""),
                    button -> resetBandShadowColor(bandIndex)

            ), bandResetX, y, 20, buttonHeight);

            bandShadowColorResetButtons.add(shadowBandColorReset);

            y += 25;

            Button backgroundBandColor = addLeftButton(Button.builder
            (
                    Component.literal(""),
                    button -> openColorPopup
                    (
                            "Band " + (bandIndex + 1) + " Background Color",
                            workingConfig.distanceBands.get(bandIndex).backgroundColor,
                            defaultBand(bandIndex).backgroundColor,
                            color -> workingConfig.distanceBands.get(bandIndex).backgroundColor = color
                    )

            ), bandRowX, y, bandControlWidth, buttonHeight);

            bandBackgroundColorButtons.add(backgroundBandColor);

            Button backgroundBandColorReset = addLeftButton(Button.builder
            (
                    Component.literal(""),
                    button -> resetBandBackgroundColor(bandIndex)

            ), bandResetX, y, 20, buttonHeight);

            bandBackgroundColorResetButtons.add(backgroundBandColorReset);

            y += 35;
        }



        /* ----- 우측 하단 ----- */

        this.addRenderableWidget(Button.builder
        (
                Component.literal("Reset"),
                button ->
                {
                    this.workingConfig = new PlayerReachDisplayConfig();
                    this.init();
                }

        ).bounds(resetActionX, actionTopY, actionWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder
        (
                Component.literal("Cancel"),
                button -> Minecraft.getInstance().setScreen(parent)

        ).bounds(cancelActionX, actionTopY, actionWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder
        (
                Component.literal("Apply"),
                button ->
                {
                    saveWorkingConfig();
                    Minecraft.getInstance().setScreen(parent);
                }

        ).bounds(applyX, applyY, applyWidth, buttonHeight).build());



        int visibleBottom = this.height - 80;

        maxScroll = Math.max(0, y - visibleBottom);

        if (scrollOffset > maxScroll)
        {
            scrollOffset = maxScroll;
        }

        updateLeftControlPositions();
        updateEnableStates();
    }



    /* ----- 컨트롤 추가 ----- */

    private <T extends AbstractWidget> T addLeftControl(T widget, int baseY)
    {
        this.addRenderableWidget(widget);

        leftControls.add(widget);
        leftControlBaseY.add(baseY);

        return widget;
    }

    private Button addLeftButton(Button.Builder builder, int x, int baseY, int width, int height)
    {
        Button button = builder.bounds
        (
                x,
                baseY - scrollOffset,
                width,
                height
        ).build();

        return addLeftControl(button, baseY);
    }

    private ConfigSliderWidget addLeftSlider
    (
            String         label,
            double         min,
            double         max,
            double         current,
            int            decimals,
            DoubleConsumer setter,
            int            x,
            int            baseY,
            int            width,
            int            height
    )
    {
        ConfigSliderWidget slider = new ConfigSliderWidget
        (
                x,
                baseY - scrollOffset,
                width,
                height,
                label,
                min,
                max,
                current,
                decimals,
                setter
        );

        return addLeftControl(slider, baseY);
    }

    private void updateLeftControlPositions()
    {
        for (int i = 0; i < leftControls.size(); i++)
        {
            AbstractWidget widget = leftControls.get(i);

            int baseY = leftControlBaseY.get(i);

            widget.setY(baseY - scrollOffset);
        }
    }

    private void addSectionHeader(String title, int y)
    {
        sectionHeaders.add(new SectionHeader(title, y));
    }



    /* ----- Band 추가/삭제 헬퍼 ----- */

    private void addDistanceBand()
    {
        if (workingConfig.distanceBands.size() >= PlayerReachDisplayConfig.MAX_DISTANCE_BAND_COUNT)
        {
            return;
        }

        if (!canAddDistanceBand())
        {
            return;
        }

        workingConfig.distanceBands.add(createDistanceBandFromLast());

        this.init();
    }

    private boolean canAddDistanceBand()
    {
        if (workingConfig.distanceBands == null || workingConfig.distanceBands.isEmpty())
        {
            return true;
        }

        return workingConfig.distanceBands.getLast().fromDistance
               < PlayerReachDisplayConfig.MAX_DISTANCE_BAND_FROM - PlayerReachDisplayConfig.DISTANCE_BAND_GAP;
    }

    private void removeDistanceBand()
    {
        if (workingConfig.distanceBands.size() <= PlayerReachDisplayConfig.MIN_DISTANCE_BAND_COUNT)
        {
            return;
        }

        workingConfig.distanceBands.removeLast();

        this.init();
    }

    private DistanceColorBand createDistanceBandFromLast()
    {
        DistanceColorBand band = new DistanceColorBand();

        if (workingConfig.distanceBands == null || workingConfig.distanceBands.isEmpty())
        {
            band.fromDistance    = 0.0;
            band.textColor       = 0xFFFFFFFF;
            band.shadowColor     = 0xFF000000;
            band.backgroundColor = 0x50000000;

            return band;
        }

        DistanceColorBand last = workingConfig.distanceBands.getLast();

        band.fromDistance = Math.clamp
        (
                last.fromDistance + 1.0,
                PlayerReachDisplayConfig.MIN_DISTANCE_BAND_FROM,
                PlayerReachDisplayConfig.MAX_DISTANCE_BAND_FROM
        );

        band.textColor       = last.textColor;
        band.shadowColor     = last.shadowColor;
        band.backgroundColor = last.backgroundColor;

        return band;
    }



    /* ----- Band 리셋 헬퍼 ----- */

    private void resetBandFromDistance(int bandIndex)
    {
        if (bandIndex <= 0 || bandIndex >= workingConfig.distanceBands.size())
        {
            return;
        }

        setBandFromDistance(bandIndex, defaultBandFromDistance(bandIndex));
    }

    private void resetBandTextColor(int bandIndex)
    {
        if (!isValidBandIndex(bandIndex))
        {
            return;
        }

        workingConfig.distanceBands.get(bandIndex).textColor = defaultBand(bandIndex).textColor;

        updateEnableStates();
    }

    private void resetBandShadowColor(int bandIndex)
    {
        if (!isValidBandIndex(bandIndex))
        {
            return;
        }

        workingConfig.distanceBands.get(bandIndex).shadowColor = defaultBand(bandIndex).shadowColor;

        updateEnableStates();
    }

    private void resetBandBackgroundColor(int bandIndex)
    {
        if (!isValidBandIndex(bandIndex))
        {
            return;
        }

        workingConfig.distanceBands.get(bandIndex).backgroundColor = defaultBand(bandIndex).backgroundColor;

        updateEnableStates();
    }

    private boolean isValidBandIndex(int bandIndex)
    {
        return bandIndex >= 0 && bandIndex < workingConfig.distanceBands.size();
    }

    private double defaultBandFromDistance(int bandIndex)
    {
        if (bandIndex <= 0)
        {
            return 0.0;
        }

        if (bandIndex < defaultConfig.distanceBands.size())
        {
            return defaultConfig.distanceBands.get(bandIndex).fromDistance;
        }

        return Math.clamp
        (
                bandIndex,
                PlayerReachDisplayConfig.MIN_DISTANCE_BAND_FROM,
                PlayerReachDisplayConfig.MAX_DISTANCE_BAND_FROM
        );
    }



    /* ----- 렌더 ----- */

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta)
    {
        int centerX = this.width / 2;

        int leftWidth   = centerX + this.width / 6;
        int leftCenterX = leftWidth / 2;

        graphics.fill
        (
                0,
                0,
                this.width,
                this.height,
                0x88000000
        );

        drawRightPanel(graphics, leftWidth);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        renderPreview(graphics);

        drawSectionHeaders(graphics, leftWidth, leftCenterX);
        drawAllRows(graphics);

        drawScreenChrome(graphics, centerX, leftWidth);

        if (colorPopup != null)
        {
            colorPopup.render(graphics, this.font, this.width, this.height, mouseX, mouseY);
        }

        if (entityListPopup != null)
        {
            entityListPopup.render(graphics, this.font, this.width, this.height, mouseX, mouseY);
        }
    }

    private void drawRightPanel(GuiGraphicsExtractor graphics, int leftWidth)
    {
        graphics.fill
        (
                leftWidth,
                START_Y + 16,
                this.width,
                this.height,
                0x50000000
        );
    }

    private void drawSectionHeaders(GuiGraphicsExtractor graphics, int leftWidth, int leftCenterX)
    {
        for (SectionHeader header : sectionHeaders)
        {
            int headerY = header.baseY() - scrollOffset;

            if (headerY < START_Y + 16 || headerY > this.height)
            {
                continue;
            }

            graphics.text
            (
                    this.font,
                    "▼",
                    10,
                    headerY,
                    0xFFFFFFFF,
                    false
            );

            graphics.text
            (
                    this.font,
                    "▼",
                    leftWidth - 20,
                    headerY,
                    0xFFFFFFFF,
                    false
            );

            String title = header.title();

            graphics.text
            (
                    this.font,
                    title,
                    leftCenterX - this.font.width(title) / 2,
                    headerY,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    private void drawScreenChrome(GuiGraphicsExtractor graphics, int centerX, int leftWidth)
    {
        int titleBottom = START_Y + 16;

        graphics.fill
        (
                0,
                0,
                this.width,
                titleBottom,
                0xFF000000
        );

        graphics.fill
        (
                0,
                titleBottom - 2,
                this.width,
                titleBottom - 1,
                0x55FFFFFF
        );

        graphics.fill
        (
                0,
                titleBottom - 1,
                this.width,
                titleBottom,
                0xFF000000
        );

        drawCenteredText
        (
                graphics,
                this.font,
                this.title.getString(),
                centerX,
                12,
                0xFFFFFFFF
        );

        graphics.fill
        (
                leftWidth - 1,
                titleBottom,
                leftWidth,
                this.height,
                0xAAFFFFFF
        );

        graphics.fill
        (
                leftWidth,
                titleBottom,
                leftWidth + 1,
                this.height,
                0xAA000000
        );
    }



    /* ----- 프리뷰 ----- */

    private void renderPreview(GuiGraphicsExtractor graphics)
    {
        if (workingConfig == null)
        {
            return;
        }

        PreviewLayout preview = createPreviewLayout();

        graphics.fill
        (
                preview.x() - 2,
                preview.y() - 2,
                preview.x() + preview.width() + 2,
                preview.y() + preview.height() + 2,
                0xFFFFFFFF
        );

        graphics.fill
        (
                preview.x() - 1,
                preview.y() - 1,
                preview.x() + preview.width() + 1,
                preview.y() + preview.height() + 1,
                0xFF000000
        );

        graphics.blit
        (
                RenderPipelines.GUI_TEXTURED,
                PREVIEW_BACKGROUND,
                preview.x(),
                preview.y(),
                0.0f,
                0.0f,
                preview.width(),
                preview.height(),
                PREVIEW_TEXTURE_WIDTH,
                PREVIEW_TEXTURE_HEIGHT,
                PREVIEW_TEXTURE_WIDTH,
                PREVIEW_TEXTURE_HEIGHT
        );

        PlayerReachDisplayConfig previewConfig = workingConfig.copy();

        previewConfig.useRelativePosition = true;
        previewConfig.positionX = 0.5;
        previewConfig.positionY = 0.5;

        var matrices = graphics.pose();

        matrices.pushMatrix();
        matrices.translate((float)preview.x(), (float)preview.y());

        PlayerReachDisplayRenderer.render
        (
                graphics,
                this.font,
                previewConfig,
                2.88,
                preview.width(),
                preview.height()
        );

        matrices.popMatrix();
    }

    private PreviewLayout createPreviewLayout()
    {
        int centerX = this.width / 2;

        int leftWidth  = centerX + this.width / 6;
        int rightWidth = this.width - leftWidth;

        int rightMargin = 20;

        int previewY = START_Y + 40;

        int footerTop = this.height - 80;

        int maxPreviewWidth  = Math.max(80, rightWidth - rightMargin * 2);
        int maxPreviewHeight = Math.max(50, footerTop - previewY - 10);

        float aspect = PREVIEW_TEXTURE_WIDTH / (float)PREVIEW_TEXTURE_HEIGHT;

        int previewWidth  = maxPreviewWidth;
        int previewHeight = Math.round(previewWidth / aspect);

        if (previewHeight > maxPreviewHeight)
        {
            previewHeight = maxPreviewHeight;
            previewWidth  = Math.round(previewHeight * aspect);
        }

        previewWidth  = Math.max(80, previewWidth);
        previewHeight = Math.max(45, previewHeight);

        int previewX = leftWidth + (rightWidth - previewWidth) / 2;

        return new PreviewLayout
        (
                previewX,
                previewY,
                previewWidth,
                previewHeight
        );
    }



    /* ----- 행 렌더 ----- */

    private void drawAllRows(GuiGraphicsExtractor graphics)
    {
        drawAppearanceRows(graphics);
        drawTransformRows(graphics);
        drawColorRows(graphics);
        drawKeepLastDistanceRows(graphics);
        drawDisplayModeRows(graphics);
        drawEntityFilterRows(graphics);
        drawDistanceColorRows(graphics);
    }

    private void drawAppearanceRows(GuiGraphicsExtractor graphics)
    {
        drawToggleRow(graphics, textToggle, "Enable Reach", workingConfig.showReach);
        drawResetIconIfPresent(graphics, textReset);

        drawToggleRow(graphics, shadowToggle, "Text Shadow", workingConfig.showShadow);
        drawResetIconIfPresent(graphics, shadowReset);

        drawToggleRow(graphics, backgroundToggle, "Text Background", workingConfig.showBackground);
        drawResetIconIfPresent(graphics, backgroundReset);
    }

    private void drawTransformRows(GuiGraphicsExtractor graphics)
    {
        drawResetIconIfPresent(graphics, scaleReset);
        drawSimpleLabelRow(graphics, positionToggle, "Edit Position");
    }

    private void drawColorRows(GuiGraphicsExtractor graphics)
    {
        drawColorRow(graphics, textColorButton, "Main Color", workingConfig.textColor);
        drawResetIconIfPresent(graphics, textColorReset);

        drawColorRow(graphics, shadowColorButton, "Shadow Color", workingConfig.shadowColor);
        drawResetIconIfPresent(graphics, shadowColorReset);

        drawColorRow(graphics, backgroundColorButton, "Background Color", workingConfig.backgroundColor);
        drawResetIconIfPresent(graphics, backgroundColorReset);
    }

    private void drawKeepLastDistanceRows(GuiGraphicsExtractor graphics)
    {
        drawToggleRow
        (
                graphics,
                keepLastDistanceToggle,
                "Reset Timer",
                !workingConfig.keepLastHitDistance
        );

        drawResetIconIfPresent(graphics, keepLastDistanceReset);
        drawResetIconIfPresent(graphics, resetSecondsReset);
    }

    private void drawDisplayModeRows(GuiGraphicsExtractor graphics)
    {
        String modeText = switch (workingConfig.displayMode)
        {
            case NUMBER_ONLY -> "2.88";
            case WITH_BLOCKS -> "2.88 blocks";
            case WITH_M      -> "2.88 M";
        };

        drawValueRow(graphics, displayModeButton, "Display Format", modeText);
        drawResetIconIfPresent(graphics, displayModeReset);
    }

    private void drawEntityFilterRows(GuiGraphicsExtractor graphics)
    {
        drawToggleRow
        (
                graphics,
                entityFilterToggle,
                "Enable Entity Filter",
                workingConfig.enableEntityFilter
        );

        drawResetIconIfPresent(graphics, entityFilterToggleReset);

        drawValueRow
        (
                graphics,
                entityFilterModeButton,
                "Filter Mode",
                workingConfig.useWhitelist ? "Whitelist" : "Blacklist"
        );

        drawResetIconIfPresent(graphics, entityFilterModeReset);

        drawValueRow
        (
                graphics,
                whitelistButton,
                "Whitelist (" + safeSize(workingConfig.whitelist) + ")",
                describeEntityList(workingConfig.whitelist)
        );

        drawResetIconIfPresent(graphics, whitelistReset);

        drawValueRow
        (
                graphics,
                blacklistButton,
                "Blacklist (" + safeSize(workingConfig.blacklist) + ")",
                describeEntityList(workingConfig.blacklist)
        );

        drawResetIconIfPresent(graphics, blacklistReset);
    }

    private void drawDistanceColorRows(GuiGraphicsExtractor graphics)
    {
        drawToggleRow
        (
                graphics,
                distanceColorToggle,
                "Enable Distance Color",
                workingConfig.enableDistanceColor
        );

        drawResetIconIfPresent(graphics, distanceColorReset);

        for (int i = 0; i < workingConfig.distanceBands.size(); i++)
        {
            DistanceColorBand band = workingConfig.distanceBands.get(i);

            if (i > 0)
            {
                int fromResetIndex = i - 1;

                if (fromResetIndex < bandFromResetButtons.size())
                {
                    drawResetIconIfPresent(graphics, bandFromResetButtons.get(fromResetIndex));
                }
            }

            if (i < bandTextColorButtons.size())
            {
                drawColorRow
                (
                        graphics,
                        bandTextColorButtons.get(i),
                        "Band " + (i + 1) + " Main",
                        band.textColor
                );
            }

            if (i < bandTextColorResetButtons.size())
            {
                drawResetIconIfPresent(graphics, bandTextColorResetButtons.get(i));
            }

            if (i < bandShadowColorButtons.size())
            {
                drawColorRow
                (
                        graphics,
                        bandShadowColorButtons.get(i),
                        "Band " + (i + 1) + " Shadow",
                        band.shadowColor
                );
            }

            if (i < bandShadowColorResetButtons.size())
            {
                drawResetIconIfPresent(graphics, bandShadowColorResetButtons.get(i));
            }

            if (i < bandBackgroundColorButtons.size())
            {
                drawColorRow
                (
                        graphics,
                        bandBackgroundColorButtons.get(i),
                        "Band " + (i + 1) + " Background",
                        band.backgroundColor
                );
            }

            if (i < bandBackgroundColorResetButtons.size())
            {
                drawResetIconIfPresent(graphics, bandBackgroundColorResetButtons.get(i));
            }
        }
    }



    /* ----- 행 렌더 헬퍼 ----- */

    private void drawResetIconIfPresent(GuiGraphicsExtractor graphics, Button button)
    {
        if (button != null)
        {
            drawResetIcon(graphics, button);
        }
    }

    private void drawSimpleLabelRow(GuiGraphicsExtractor graphics, AbstractWidget widget, String label)
    {
        if (widget == null)
        {
            return;
        }

        int x = widget.getX();
        int y = widget.getY();

        int height = widget.getHeight();

        int color = getWidgetTextColor(widget);

        graphics.text
        (
                this.font,
                label,
                x + 4,
                y + (height - this.font.lineHeight) / 2,
                color,
                false
        );
    }

    private void drawValueRow(GuiGraphicsExtractor graphics, AbstractWidget widget, String label, String value)
    {
        if (widget == null)
        {
            return;
        }

        int color = getWidgetTextColor(widget);

        int x = widget.getX();
        int y = widget.getY();

        int width  = widget.getWidth();
        int height = widget.getHeight();

        int textY = y + (height - this.font.lineHeight) / 2;

        graphics.text
        (
                this.font,
                label,
                x + 4,
                textY,
                color,
                false
        );

        String shownValue = trimToWidth(value == null ? "" : value, Math.max(20, width / 2));

        graphics.text
        (
                this.font,
                shownValue,
                x + width - this.font.width(shownValue) - 4,
                textY,
                color,
                false
        );
    }

    private void drawToggleRow(GuiGraphicsExtractor graphics, AbstractWidget widget, String label, boolean enabled)
    {
        if (widget == null)
        {
            return;
        }

        int x = widget.getX();
        int y = widget.getY();

        int width  = widget.getWidth();
        int height = widget.getHeight();

        int color = getWidgetTextColor(widget);

        graphics.text
        (
                this.font,
                label,
                x + 4,
                y + (height - this.font.lineHeight) / 2,
                color,
                false
        );

        Identifier icon = enabled ? TOGGLE_ON_ICON : TOGGLE_OFF_ICON;

        int iconSize = 16;

        int iconX = x + width - iconSize - 4;
        int iconY = y + (height - iconSize) / 2;

        drawIcon
        (
                graphics,
                icon,
                iconX,
                iconY,
                iconSize,
                color
        );
    }

    private void drawColorRow(GuiGraphicsExtractor graphics, AbstractWidget widget, String label, int argb)
    {
        if (widget == null)
        {
            return;
        }

        int x = widget.getX();
        int y = widget.getY();

        int width  = widget.getWidth();
        int height = widget.getHeight();

        int color = getWidgetTextColor(widget);

        int labelY = y + (height - this.font.lineHeight) / 2;

        graphics.text
        (
                this.font,
                label,
                x + 4,
                labelY,
                color,
                false
        );

        int borderColor = widget.active ? 0xFF000000 : 0x66000000;
        int swatchColor = widget.active ? argb : withAlphaMultiplier(argb, 0.4f);

        graphics.fill
        (
                x + width - 16,
                y + 4,
                x + width - 4,
                y + height - 4,
                borderColor
        );

        graphics.fill
        (
                x + width - 15,
                y + 5,
                x + width - 5,
                y + height - 5,
                swatchColor
        );
    }

    private void drawResetIcon(GuiGraphicsExtractor graphics, Button button)
    {
        int x = button.getX();
        int y = button.getY();

        int width  = button.getWidth();
        int height = button.getHeight();

        int iconSize = 16;

        int iconX = x + (width - iconSize) / 2;
        int iconY = y + (height - iconSize) / 2;

        int color = getWidgetTextColor(button);

        drawIcon
        (
                graphics,
                RESET_ICON,
                iconX,
                iconY,
                iconSize,
                color
        );
    }

    private void drawIcon
    (
            GuiGraphicsExtractor graphics,
            Identifier           icon,
            int                  x,
            int                  y,
            int                  size,
            int                  color
    )
    {
        graphics.blit
        (
                RenderPipelines.GUI_TEXTURED,
                icon,
                x,
                y,
                0.0f,
                0.0f,
                size,
                size,
                size,
                size,
                color
        );
    }

    private void drawCenteredText
    (
            GuiGraphicsExtractor graphics,
            Font                 font,
            String               text,
            int                  centerX,
            int                  y,
            int                  color
    )
    {
        graphics.text
        (
                font,
                text,
                centerX - font.width(text) / 2,
                y,
                color,
                true
        );
    }



    /* ----- 활성화 ----- */

    private void updateEnableStates()
    {
        if (workingConfig == null)
        {
            return;
        }

        boolean textEnabled = workingConfig.showReach;

        setWidgetEnabled(textToggle, true);
        setWidgetEnabled(textReset, workingConfig.showReach != defaultConfig.showReach);

        setWidgetEnabled(shadowToggle, textEnabled);
        setWidgetEnabled(backgroundToggle, textEnabled);

        setWidgetEnabled
        (
                shadowReset,
                textEnabled && workingConfig.showShadow != defaultConfig.showShadow
        );

        setWidgetEnabled
        (
                backgroundReset,
                textEnabled && workingConfig.showBackground != defaultConfig.showBackground
        );

        setWidgetEnabled(scaleSlider, textEnabled);

        setWidgetEnabled
        (
                scaleReset,
                textEnabled && workingConfig.scale != defaultConfig.scale
        );

        setWidgetEnabled(positionToggle, textEnabled);

        setWidgetEnabled(textColorButton, textEnabled);

        setWidgetEnabled
        (
                textColorReset,
                textEnabled && workingConfig.textColor != defaultConfig.textColor
        );

        setWidgetEnabled(shadowColorButton, textEnabled && workingConfig.showShadow);

        setWidgetEnabled
        (
                shadowColorReset,
                textEnabled && workingConfig.showShadow && workingConfig.shadowColor != defaultConfig.shadowColor
        );

        setWidgetEnabled(backgroundColorButton, textEnabled && workingConfig.showBackground);

        setWidgetEnabled
        (
                backgroundColorReset,
                textEnabled && workingConfig.showBackground && workingConfig.backgroundColor != defaultConfig.backgroundColor
        );

        setWidgetEnabled(keepLastDistanceToggle, textEnabled);

        setWidgetEnabled
        (
                keepLastDistanceReset,
                textEnabled && workingConfig.keepLastHitDistance != defaultConfig.keepLastHitDistance
        );

        boolean resetSecondsEnabled = textEnabled && !workingConfig.keepLastHitDistance;

        setWidgetEnabled(resetSecondsSlider, resetSecondsEnabled);

        setWidgetEnabled
        (
                resetSecondsReset,
                resetSecondsEnabled && Double.compare(workingConfig.resetAfterSeconds, defaultConfig.resetAfterSeconds) != 0
        );

        setWidgetEnabled(displayModeButton, textEnabled);

        setWidgetEnabled
        (
                displayModeReset,
                textEnabled && workingConfig.displayMode != defaultConfig.displayMode
        );

        setWidgetEnabled(entityFilterToggle, textEnabled);

        setWidgetEnabled
        (
                entityFilterToggleReset,
                textEnabled && workingConfig.enableEntityFilter != defaultConfig.enableEntityFilter
        );

        boolean entityFilterEnabled = textEnabled && workingConfig.enableEntityFilter;

        setWidgetEnabled(entityFilterModeButton, entityFilterEnabled);

        setWidgetEnabled
        (
                entityFilterModeReset,
                entityFilterEnabled && workingConfig.useWhitelist != defaultConfig.useWhitelist
        );

        boolean whitelistActive = entityFilterEnabled && workingConfig.useWhitelist;
        boolean blacklistActive = entityFilterEnabled && !workingConfig.useWhitelist;

        setWidgetEnabled(whitelistButton, whitelistActive);

        setWidgetEnabled
        (
                whitelistReset,
                whitelistActive && !stringListsEqual(workingConfig.whitelist, defaultConfig.whitelist)
        );

        setWidgetEnabled(blacklistButton, blacklistActive);

        setWidgetEnabled
        (
                blacklistReset,
                blacklistActive && !stringListsEqual(workingConfig.blacklist, defaultConfig.blacklist)
        );

        setWidgetEnabled(distanceColorToggle, textEnabled);

        setWidgetEnabled
        (
                distanceColorReset,
                textEnabled && workingConfig.enableDistanceColor != defaultConfig.enableDistanceColor
        );

        boolean bandControlsEnabled = textEnabled && workingConfig.enableDistanceColor;

        setWidgetEnabled
        (
                distanceBandRemoveButton,
                bandControlsEnabled && workingConfig.distanceBands.size() > PlayerReachDisplayConfig.MIN_DISTANCE_BAND_COUNT
        );

        setWidgetEnabled
        (
                distanceBandAddButton,
                bandControlsEnabled && workingConfig.distanceBands.size() < PlayerReachDisplayConfig.MAX_DISTANCE_BAND_COUNT && canAddDistanceBand()
        );

        for (ConfigSliderWidget slider : bandFromSliders)
        {
            setWidgetEnabled(slider, bandControlsEnabled);
        }

        for (int bandIndex = 1; bandIndex < workingConfig.distanceBands.size(); bandIndex++)
        {
            int resetIndex = bandIndex - 1;

            if (resetIndex < bandFromResetButtons.size())
            {
                setWidgetEnabled
                (
                        bandFromResetButtons.get(resetIndex),
                        bandControlsEnabled && Double.compare
                        (
                                workingConfig.distanceBands.get(bandIndex).fromDistance,
                                defaultBandFromDistance(bandIndex)
                        ) != 0
                );
            }
        }

        for (int i = 0; i < workingConfig.distanceBands.size(); i++)
        {
            DistanceColorBand band = workingConfig.distanceBands.get(i);
            DistanceColorBand defaultBand = defaultBand(i);

            if (i < bandTextColorButtons.size())
            {
                setWidgetEnabled(bandTextColorButtons.get(i), bandControlsEnabled);
            }

            if (i < bandTextColorResetButtons.size())
            {
                setWidgetEnabled
                (
                        bandTextColorResetButtons.get(i),
                        bandControlsEnabled && band.textColor != defaultBand.textColor
                );
            }

            if (i < bandShadowColorButtons.size())
            {
                setWidgetEnabled(bandShadowColorButtons.get(i), bandControlsEnabled);
            }

            if (i < bandShadowColorResetButtons.size())
            {
                setWidgetEnabled
                (
                        bandShadowColorResetButtons.get(i),
                        bandControlsEnabled && band.shadowColor != defaultBand.shadowColor
                );
            }

            if (i < bandBackgroundColorButtons.size())
            {
                setWidgetEnabled(bandBackgroundColorButtons.get(i), bandControlsEnabled);
            }

            if (i < bandBackgroundColorResetButtons.size())
            {
                setWidgetEnabled
                (
                        bandBackgroundColorResetButtons.get(i),
                        bandControlsEnabled && band.backgroundColor != defaultBand.backgroundColor
                );
            }
        }
    }

    private void setWidgetEnabled(AbstractWidget widget, boolean enabled)
    {
        if (widget == null)
        {
            return;
        }

        widget.active = enabled;
        widget.setAlpha(enabled ? 1.0f : 0.4f);
    }



    /* ----- 입력 ----- */

    @Override
    public boolean charTyped(CharacterEvent event)
    {
        String text = event.codepointAsString();

        if (text.isEmpty())
        {
            return true;
        }

        for (int i = 0; i < text.length(); i++)
        {
            char chr = text.charAt(i);

            if (colorPopup != null)
            {
                colorPopup.charTyped(chr, 0);
                continue;
            }

            if (entityListPopup != null)
            {
                entityListPopup.charTyped(chr, 0);
                continue;
            }
        }

        if (colorPopup != null || entityListPopup != null)
        {
            return true;
        }

        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event)
    {
        int keyCode = event.key();

        if (colorPopup != null)
        {
            return colorPopup.keyPressed(keyCode);
        }

        if (entityListPopup != null)
        {
            return entityListPopup.keyPressed(keyCode);
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        double mouseX = event.x();
        double mouseY = event.y();

        int button = event.button();

        if (colorPopup != null)
        {
            return colorPopup.mouseClicked(mouseX, mouseY, button);
        }

        if (entityListPopup != null)
        {
            return entityListPopup.mouseClicked(mouseX, mouseY, button);
        }

        int leftWidth = this.width / 2 + this.width / 6;
        int clipTop = START_Y + 18;

        if (mouseX < leftWidth && mouseY < clipTop)
        {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy)
    {
        double mouseX = event.x();
        double mouseY = event.y();

        int button = event.button();

        if (colorPopup != null)
        {
            return colorPopup.mouseDragged(mouseX, mouseY, button, dx, dy);
        }

        if (entityListPopup != null)
        {
            return entityListPopup.mouseDragged(mouseX, mouseY, button, dx, dy);
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event)
    {
        double mouseX = event.x();
        double mouseY = event.y();

        int button = event.button();

        if (colorPopup != null)
        {
            return colorPopup.mouseReleased(mouseX, mouseY, button);
        }

        if (entityListPopup != null)
        {
            return entityListPopup.mouseReleased(mouseX, mouseY, button);
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        if (colorPopup != null)
        {
            return true;
        }

        if (entityListPopup != null)
        {
            return entityListPopup.mouseScrolled(mouseX, mouseY, verticalAmount);
        }

        if (verticalAmount != 0)
        {
            int delta = (int)(verticalAmount * -10);

            scrollOffset += delta;
            scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);

            updateLeftControlPositions();

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }



    /* ----- 팝업 ----- */

    private void openColorPopup(String title, int currentColor, int resetColor, IntConsumer setter)
    {
        entityListPopup = null;

        colorPopup = new ColorPickerPopup
        (
                Component.literal(title),
                currentColor,
                resetColor,
                color -> setter.accept(color),
                () ->
                {
                    updateEnableStates();
                    colorPopup = null;
                }
        );
    }

    private void openEntityListPopup
    (
            String           title,
            List<String>     currentEntries,
            List<String>     resetEntries,
            Consumer<List<String>> setter
    )
    {
        colorPopup = null;

        entityListPopup = new EntityListPopup
        (
                Component.literal(title),
                currentEntries,
                resetEntries,
                result ->
                {
                    setter.accept(new ArrayList<>(result));
                    updateEnableStates();
                },
                () -> entityListPopup = null
        );
    }



    /* ----- 저장 ----- */

    public void saveWorkingConfig()
    {
        PlayerReachDisplay.CONFIG = this.workingConfig.copy();
        PlayerReachDisplayConfigManager.save(PlayerReachDisplay.CONFIG);
    }



    /* ----- 헬퍼 ----- */

    private static double toSliderValue(double actualValue, double min, double max)
    {
        if (max <= min)
        {
            return 0.0;
        }

        return Math.clamp((actualValue - min) / (max - min), 0.0, 1.0);
    }

    private int getWidgetTextColor(AbstractWidget widget)
    {
        float alpha = widget.active ? 1.0f : 0.4f;

        int a = (int)(alpha * 255.0f) & 0xFF;

        return (a << 24) | 0xFFFFFF;
    }

    private int withAlphaMultiplier(int color, float multiplier)
    {
        int alpha    = (color >>> 24) & 0xFF;
        int newAlpha = Math.clamp((int)(alpha * multiplier), 0, 255);

        return (newAlpha << 24) | (color & 0x00FFFFFF);
    }

    private String trimToWidth(String text, int maxWidth)
    {
        if (text == null)
        {
            return "";
        }

        if (this.font.width(text) <= maxWidth)
        {
            return text;
        }

        String result = text;

        while (!result.isEmpty() && this.font.width(result + "...") > maxWidth)
        {
            result = result.substring(0, result.length() - 1);
        }

        return result.isEmpty() ? "..." : result + "...";
    }

    private int safeSize(List<String> list)
    {
        return list == null ? 0 : list.size();
    }

    private String describeEntityList(List<String> list)
    {
        if (list == null || list.isEmpty())
        {
            return "Click to edit...";
        }

        return String.join(", ", list);
    }

    private DistanceColorBand defaultBand(int index)
    {
        int safeIndex = Math.min(index, defaultConfig.distanceBands.size() - 1);

        return defaultConfig.distanceBands.get(safeIndex);
    }

    private boolean stringListsEqual(List<String> a, List<String> b)
    {
        if (a == b)
        {
            return true;
        }

        if (a == null || b == null)
        {
            return false;
        }

        return a.equals(b);
    }

    private void setBandFromDistance(int bandIndex, double value)
    {
        if (bandIndex <= 0 || bandIndex >= workingConfig.distanceBands.size())
        {
            return;
        }

        double min = workingConfig.distanceBands.get(bandIndex - 1).fromDistance
                   + PlayerReachDisplayConfig.DISTANCE_BAND_GAP;

        double max = PlayerReachDisplayConfig.MAX_DISTANCE_BAND_FROM
                   - PlayerReachDisplayConfig.DISTANCE_BAND_GAP * (workingConfig.distanceBands.size() - 1 - bandIndex);

        if (max < min)
        {
            max = min;
        }

        double clampedValue = Math.clamp(value, min, max);

        workingConfig.distanceBands.get(bandIndex).fromDistance = clampedValue;

        pushLaterBandsForward(bandIndex);
        syncBandFromSliders();
        updateEnableStates();
    }

    private void pushLaterBandsForward(int changedBandIndex)
    {
        for (int i = changedBandIndex + 1; i < workingConfig.distanceBands.size(); i++)
        {
            DistanceColorBand previous = workingConfig.distanceBands.get(i - 1);
            DistanceColorBand current  = workingConfig.distanceBands.get(i);

            double min = previous.fromDistance + PlayerReachDisplayConfig.DISTANCE_BAND_GAP;

            if (current.fromDistance < min)
            {
                current.fromDistance = min;
            }
        }
    }

    private void syncBandFromSliders()
    {
        int sliderIndex = 0;

        for (int bandIndex = 1; bandIndex < workingConfig.distanceBands.size(); bandIndex++)
        {
            if (sliderIndex >= bandFromSliders.size())
            {
                break;
            }

            bandFromSliders.get(sliderIndex).setActualValue
            (
                    workingConfig.distanceBands.get(bandIndex).fromDistance
            );

            sliderIndex++;
        }
    }
}
