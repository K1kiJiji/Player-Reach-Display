package kikijiji.playerreachdisplay.screen;



import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.DoubleConsumer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;

import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.ClickableWidget;

import kikijiji.playerreachdisplay.PlayerReachDisplay;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig;
import kikijiji.playerreachdisplay.render.PlayerReachDisplayRenderer;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfigManager;
import kikijiji.playerreachdisplay.config.PlayerReachDisplayConfig.DistanceColorBand;



public class PlayerReachDisplayConfigScreen extends Screen
{
    private static final Identifier TOGGLE_ON_ICON     = Identifier.of(PlayerReachDisplay.MOD_ID, "textures/gui/toggle_on.png");
    private static final Identifier TOGGLE_OFF_ICON    = Identifier.of(PlayerReachDisplay.MOD_ID, "textures/gui/toggle_off.png");
    private static final Identifier RESET_ICON         = Identifier.of(PlayerReachDisplay.MOD_ID, "textures/gui/reset.png");
    private static final Identifier PREVIEW_BACKGROUND = Identifier.of(PlayerReachDisplay.MOD_ID, "textures/gui/preview_background.png");

    private static final int PREVIEW_TEXTURE_WIDTH  = 385;
    private static final int PREVIEW_TEXTURE_HEIGHT = 215;

    private static final int START_Y = 30;

    private final Screen parent;

    private final PlayerReachDisplayConfig defaultConfig = new PlayerReachDisplayConfig();
    private PlayerReachDisplayConfig       workingConfig;

    private int scrollOffset = 0;
    private int maxScroll    = 0;



    private final List<ClickableWidget> leftControls     = new ArrayList<>();
    private final List<Integer>         leftControlBaseY = new ArrayList<>();

    private final List<SectionHeader> sectionHeaders = new ArrayList<>();



    private ButtonWidget textToggle;
    private ButtonWidget textReset;

    private ButtonWidget shadowToggle;
    private ButtonWidget shadowReset;

    private ButtonWidget backgroundToggle;
    private ButtonWidget backgroundReset;



    private ConfigSliderWidget scaleSlider;
    private ButtonWidget       scaleReset;

    private ButtonWidget positionToggle;



    private ButtonWidget textColorButton;
    private ButtonWidget textColorReset;

    private ButtonWidget shadowColorButton;
    private ButtonWidget shadowColorReset;

    private ButtonWidget backgroundColorButton;
    private ButtonWidget backgroundColorReset;



    private ButtonWidget keepLastDistanceToggle;
    private ButtonWidget keepLastDistanceReset;

    private ConfigSliderWidget resetSecondsSlider;
    private ButtonWidget       resetSecondsReset;



    private ButtonWidget displayModeButton;
    private ButtonWidget displayModeReset;



    private ButtonWidget entityFilterToggle;
    private ButtonWidget entityFilterToggleReset;

    private ButtonWidget entityFilterModeButton;
    private ButtonWidget entityFilterModeReset;

    private ButtonWidget whitelistButton;
    private ButtonWidget whitelistReset;

    private ButtonWidget blacklistButton;
    private ButtonWidget blacklistReset;



    private ButtonWidget distanceColorToggle;
    private ButtonWidget distanceColorReset;

    private ButtonWidget distanceBandAddButton;
    private ButtonWidget distanceBandRemoveButton;

    private final List<ConfigSliderWidget> bandFromSliders                 = new ArrayList<>();
    private final List<ButtonWidget>       bandFromResetButtons            = new ArrayList<>();
    private final List<ButtonWidget>       bandTextColorButtons            = new ArrayList<>();
    private final List<ButtonWidget>       bandTextColorResetButtons       = new ArrayList<>();
    private final List<ButtonWidget>       bandShadowColorButtons          = new ArrayList<>();
    private final List<ButtonWidget>       bandShadowColorResetButtons     = new ArrayList<>();
    private final List<ButtonWidget>       bandBackgroundColorButtons      = new ArrayList<>();
    private final List<ButtonWidget>       bandBackgroundColorResetButtons = new ArrayList<>();



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



    private class ConfigSliderWidget extends SliderWidget
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
                    Text.empty(),
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

            this.setMessage(Text.literal(label + ": " + valueText));
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
        super(Text.literal("Player Reach Display Config").formatted(Formatting.BOLD));

        this.parent = parent;

        this.workingConfig = PlayerReachDisplay.CONFIG == null ? new PlayerReachDisplayConfig() : PlayerReachDisplay.CONFIG.copy();
    }



    /* ----- 준비 ----- */

    @Override
    protected void init()
    {
        this.clearChildren();

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

        textToggle = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.showReach = !workingConfig.showReach;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        textReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.showReach = defaultConfig.showReach;
                    workingConfig.textColor = defaultConfig.textColor;

                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        shadowToggle = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.showShadow = !workingConfig.showShadow;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        shadowReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.showShadow = defaultConfig.showShadow;
                    workingConfig.shadowColor = defaultConfig.shadowColor;

                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        backgroundToggle = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.showBackground = !workingConfig.showBackground;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        backgroundReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
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

        scaleReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.scale = defaultConfig.scale;
                    this.init();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        positionToggle = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget -> MinecraftClient.getInstance().setScreen
                        (
                                new PlayerReachDisplayPositionConfigScreen(this, this.workingConfig)
                        )

        ), x - 10, y, fullButtonWidth, buttonHeight);

        y += 45;



        /* ----- Color ----- */

        addSectionHeader("Color", y - 15);

        textColorButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget -> openColorPopup
                        (
                                "Text Color",
                                workingConfig.textColor,
                                defaultConfig.textColor,
                                color -> workingConfig.textColor = color
                        )

        ), x - 10, y, buttonWidth, buttonHeight);

        textColorReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.textColor = defaultConfig.textColor;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        shadowColorButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget -> openColorPopup
                        (
                                "Shadow Color",
                                workingConfig.shadowColor,
                                defaultConfig.shadowColor,
                                color -> workingConfig.shadowColor = color
                        )

        ), x - 10, y, buttonWidth, buttonHeight);

        shadowColorReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.shadowColor = defaultConfig.shadowColor;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        backgroundColorButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget -> openColorPopup
                        (
                                "Background Color",
                                workingConfig.backgroundColor,
                                defaultConfig.backgroundColor,
                                color -> workingConfig.backgroundColor = color
                        )

        ), x - 10, y, buttonWidth, buttonHeight);

        backgroundColorReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.backgroundColor = defaultConfig.backgroundColor;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Keep Last Distance ----- */

        addSectionHeader("Keep Last Distance", y - 15);

        keepLastDistanceToggle = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.keepLastHitDistance = !workingConfig.keepLastHitDistance;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        keepLastDistanceReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
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

        resetSecondsReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.resetAfterSeconds = defaultConfig.resetAfterSeconds;
                    this.init();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Display Mode ----- */

        addSectionHeader("Display Mode", y - 15);

        displayModeButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
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

        displayModeReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.displayMode = defaultConfig.displayMode;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Entity Filter ----- */

        addSectionHeader("Entity Filter", y - 20);

        entityFilterToggle = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.enableEntityFilter = !workingConfig.enableEntityFilter;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        entityFilterToggleReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.enableEntityFilter = defaultConfig.enableEntityFilter;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        entityFilterModeButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.useWhitelist = !workingConfig.useWhitelist;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        entityFilterModeReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.useWhitelist = defaultConfig.useWhitelist;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        whitelistButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget -> openEntityListPopup
                        (
                                "Edit Whitelist",
                                workingConfig.whitelist,
                                defaultConfig.whitelist,
                                result -> workingConfig.whitelist = new ArrayList<>(result)
                        )

        ), x - 10, y, buttonWidth, buttonHeight);

        whitelistReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.whitelist = new ArrayList<>(defaultConfig.whitelist);
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        blacklistButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget -> openEntityListPopup
                        (
                                "Edit Blacklist",
                                workingConfig.blacklist,
                                defaultConfig.blacklist,
                                result -> workingConfig.blacklist = new ArrayList<>(result)
                        )

        ), x - 10, y, buttonWidth, buttonHeight);

        blacklistReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.blacklist = new ArrayList<>(defaultConfig.blacklist);
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 45;



        /* ----- Distance Color ----- */

        addSectionHeader("Distance Color", y - 20);

        distanceColorToggle = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.enableDistanceColor = !workingConfig.enableDistanceColor;
                    updateEnableStates();
                }

        ), x - 10, y, buttonWidth, buttonHeight);

        distanceColorReset = addLeftButton(ButtonWidget.builder
        (
                Text.literal(""),
                buttonWidget ->
                {
                    workingConfig.enableDistanceColor = defaultConfig.enableDistanceColor;
                    updateEnableStates();
                }

        ), resetX, y, 20, buttonHeight);

        y += 25;

        int bandButtonGap = 4;
        int bandButtonWidth = (fullButtonWidth - bandButtonGap) / 2;

        distanceBandRemoveButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal("Remove Band"),
                buttonWidget -> removeDistanceBand()

        ), x - 10, y, bandButtonWidth, buttonHeight);

        distanceBandAddButton = addLeftButton(ButtonWidget.builder
        (
                Text.literal("Add Band"),
                buttonWidget -> addDistanceBand()

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

                ButtonWidget fromReset = addLeftButton(ButtonWidget.builder
                (
                        Text.literal(""),
                        buttonWidget -> resetBandFromDistance(bandIndex)

                ), bandResetX, y, 20, buttonHeight);

                bandFromResetButtons.add(fromReset);

                y += 25;
            }

            ButtonWidget textBandColor = addLeftButton(ButtonWidget.builder
            (
                    Text.literal(""),
                    buttonWidget -> openColorPopup
                            (
                                    "Band " + (bandIndex + 1) + " Main Color",
                                    workingConfig.distanceBands.get(bandIndex).textColor,
                                    defaultBand(bandIndex).textColor,
                                    color -> workingConfig.distanceBands.get(bandIndex).textColor = color
                            )

            ), bandRowX, y, bandControlWidth, buttonHeight);

            bandTextColorButtons.add(textBandColor);

            ButtonWidget textBandColorReset = addLeftButton(ButtonWidget.builder
            (
                    Text.literal(""),
                    buttonWidget -> resetBandTextColor(bandIndex)

            ), bandResetX, y, 20, buttonHeight);

            bandTextColorResetButtons.add(textBandColorReset);

            y += 25;

            ButtonWidget shadowBandColor = addLeftButton(ButtonWidget.builder
            (
                    Text.literal(""),
                    buttonWidget -> openColorPopup
                            (
                                    "Band " + (bandIndex + 1) + " Shadow Color",
                                    workingConfig.distanceBands.get(bandIndex).shadowColor,
                                    defaultBand(bandIndex).shadowColor,
                                    color -> workingConfig.distanceBands.get(bandIndex).shadowColor = color
                            )

            ), bandRowX, y, bandControlWidth, buttonHeight);

            bandShadowColorButtons.add(shadowBandColor);

            ButtonWidget shadowBandColorReset = addLeftButton(ButtonWidget.builder
            (
                    Text.literal(""),
                    buttonWidget -> resetBandShadowColor(bandIndex)

            ), bandResetX, y, 20, buttonHeight);

            bandShadowColorResetButtons.add(shadowBandColorReset);

            y += 25;

            ButtonWidget backgroundBandColor = addLeftButton(ButtonWidget.builder
            (
                    Text.literal(""),
                    buttonWidget -> openColorPopup
                            (
                                    "Band " + (bandIndex + 1) + " Background Color",
                                    workingConfig.distanceBands.get(bandIndex).backgroundColor,
                                    defaultBand(bandIndex).backgroundColor,
                                    color -> workingConfig.distanceBands.get(bandIndex).backgroundColor = color
                            )

            ), bandRowX, y, bandControlWidth, buttonHeight);

            bandBackgroundColorButtons.add(backgroundBandColor);

            ButtonWidget backgroundBandColorReset = addLeftButton(ButtonWidget.builder
            (
                    Text.literal(""),
                    buttonWidget -> resetBandBackgroundColor(bandIndex)

            ), bandResetX, y, 20, buttonHeight);

            bandBackgroundColorResetButtons.add(backgroundBandColorReset);

            y += 35;
        }



        /* ----- 우측 하단 ----- */

        this.addDrawableChild(ButtonWidget.builder
        (
                Text.literal("Reset"),
                buttonWidget ->
                {
                    this.workingConfig = new PlayerReachDisplayConfig();
                    this.init();
                }

        ).dimensions(resetActionX, actionTopY, actionWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder
        (
                Text.literal("Cancel"),
                buttonWidget -> MinecraftClient.getInstance().setScreen(parent)

        ).dimensions(cancelActionX, actionTopY, actionWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder
        (
                Text.literal("Apply"),
                buttonWidget ->
                {
                    saveWorkingConfig();
                    MinecraftClient.getInstance().setScreen(parent);
                }

        ).dimensions(applyX, applyY, applyWidth, buttonHeight).build());



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

    private <T extends ClickableWidget> T addLeftControl(T widget, int baseY)
    {
        this.addDrawableChild(widget);

        leftControls.add(widget);
        leftControlBaseY.add(baseY);

        return widget;
    }

    private ButtonWidget addLeftButton(ButtonWidget.Builder builder, int x, int baseY, int width, int height)
    {
        ButtonWidget buttonWidget = builder.dimensions
        (
                x,
                baseY - scrollOffset,
                width,
                height
        ).build();

        return addLeftControl(buttonWidget, baseY);
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
            ClickableWidget widget = leftControls.get(i);

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
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta)
    {
        int centerX = this.width / 2;

        int leftWidth   = centerX + this.width / 6;
        int leftCenterX = leftWidth / 2;

        this.renderBackground(drawContext, mouseX, mouseY, delta);

        drawRightPanel(drawContext, leftWidth);

        super.render(drawContext, mouseX, mouseY, delta);
        renderPreview(drawContext);

        drawSectionHeaders(drawContext, leftWidth, leftCenterX);
        drawAllRows(drawContext);

        drawScreenChrome(drawContext, centerX, leftWidth);

        var matrices = drawContext.getMatrices();

        if (colorPopup != null)
        {
            matrices.push();
            matrices.translate(0, 0, 500);

            colorPopup.render(drawContext, this.textRenderer, this.width, this.height, mouseX, mouseY);

            matrices.pop();
        }

        if (entityListPopup != null)
        {
            matrices.push();
            matrices.translate(0, 0, 500);

            entityListPopup.render(drawContext, this.textRenderer, this.width, this.height, mouseX, mouseY);

            matrices.pop();
        }
    }

    private void drawRightPanel(DrawContext drawContext, int leftWidth)
    {
        drawContext.fill
        (
                leftWidth,
                START_Y + 16,
                this.width,
                this.height,
                0x50000000
        );
    }

    private void drawSectionHeaders(DrawContext drawContext, int leftWidth, int leftCenterX)
    {
        for (SectionHeader header : sectionHeaders)
        {
            int headerY = header.baseY() - scrollOffset;

            if (headerY < START_Y + 16 || headerY > this.height)
            {
                continue;
            }

            drawContext.drawText
            (
                    this.textRenderer,
                    Text.literal("▼"),
                    10,
                    headerY,
                    0xFFFFFFFF,
                    false
            );

            drawContext.drawText
            (
                    this.textRenderer,
                    Text.literal("▼"),
                    leftWidth - 20,
                    headerY,
                    0xFFFFFFFF,
                    false
            );

            Text title = Text.literal(header.title());

            drawContext.drawText
            (
                    this.textRenderer,
                    title,
                    leftCenterX - this.textRenderer.getWidth(title) / 2,
                    headerY,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    private void drawScreenChrome(DrawContext drawContext, int centerX, int leftWidth)
    {
        var matrices = drawContext.getMatrices();

        matrices.push();
        matrices.translate(0, 0, 300);

        int titleBottom = START_Y + 16;

        drawContext.fill
        (
                0,
                0,
                this.width,
                titleBottom,
                0xFF000000
        );

        drawContext.fill
        (
                0,
                titleBottom - 2,
                this.width,
                titleBottom - 1,
                0x55FFFFFF
        );

        drawContext.fill
        (
                0,
                titleBottom - 1,
                this.width,
                titleBottom,
                0xFF000000
        );

        drawContext.drawCenteredTextWithShadow
        (
                this.textRenderer,
                this.title,
                centerX,
                12,
                0xFFFFFFFF
        );

        drawContext.fill
        (
                leftWidth - 1,
                titleBottom,
                leftWidth,
                this.height,
                0xAAFFFFFF
        );

        drawContext.fill
        (
                leftWidth,
                titleBottom,
                leftWidth + 1,
                this.height,
                0xAA000000
        );

        matrices.pop();
    }



    /* ----- 프리뷰 ----- */

    private void renderPreview(DrawContext drawContext)
    {
        if (workingConfig == null)
        {
            return;
        }

        PreviewLayout preview = createPreviewLayout();

        drawContext.fill
        (
                preview.x() - 2,
                preview.y() - 2,
                preview.x() + preview.width() + 2,
                preview.y() + preview.height() + 2,
                0xFFFFFFFF
        );

        drawContext.fill
        (
                preview.x() - 1,
                preview.y() - 1,
                preview.x() + preview.width() + 1,
                preview.y() + preview.height() + 1,
                0xFF000000
        );

        drawContext.drawTexture
        (
                RenderLayer::getGuiTextured,
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

        var matrices = drawContext.getMatrices();

        matrices.push();
        matrices.translate(preview.x(), preview.y(), 0);

        PlayerReachDisplayRenderer.render
        (
                drawContext,
                this.textRenderer,
                previewConfig,
                2.88,
                preview.width(),
                preview.height()
        );

        matrices.pop();
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

    private void drawAllRows(DrawContext drawContext)
    {
        drawAppearanceRows(drawContext);
        drawTransformRows(drawContext);
        drawColorRows(drawContext);
        drawKeepLastDistanceRows(drawContext);
        drawDisplayModeRows(drawContext);
        drawEntityFilterRows(drawContext);
        drawDistanceColorRows(drawContext);
    }

    private void drawAppearanceRows(DrawContext drawContext)
    {
        drawToggleRow(drawContext, textToggle, "Enable Reach", workingConfig.showReach);
        drawResetIconIfPresent(drawContext, textReset);

        drawToggleRow(drawContext, shadowToggle, "Text Shadow", workingConfig.showShadow);
        drawResetIconIfPresent(drawContext, shadowReset);

        drawToggleRow(drawContext, backgroundToggle, "Text Background", workingConfig.showBackground);
        drawResetIconIfPresent(drawContext, backgroundReset);
    }

    private void drawTransformRows(DrawContext drawContext)
    {
        drawResetIconIfPresent(drawContext, scaleReset);
        drawSimpleLabelRow(drawContext, positionToggle, "Edit Position");
    }

    private void drawColorRows(DrawContext drawContext)
    {
        drawColorRow(drawContext, textColorButton, "Main Color", workingConfig.textColor);
        drawResetIconIfPresent(drawContext, textColorReset);

        drawColorRow(drawContext, shadowColorButton, "Shadow Color", workingConfig.shadowColor);
        drawResetIconIfPresent(drawContext, shadowColorReset);

        drawColorRow(drawContext, backgroundColorButton, "Background Color", workingConfig.backgroundColor);
        drawResetIconIfPresent(drawContext, backgroundColorReset);
    }

    private void drawKeepLastDistanceRows(DrawContext drawContext)
    {
        drawToggleRow
        (
                drawContext,
                keepLastDistanceToggle,
                "Keep Last Distance",
                !workingConfig.keepLastHitDistance
        );

        drawResetIconIfPresent(drawContext, keepLastDistanceReset);
        drawResetIconIfPresent(drawContext, resetSecondsReset);
    }

    private void drawDisplayModeRows(DrawContext drawContext)
    {
        String modeText = switch (workingConfig.displayMode)
        {
            case NUMBER_ONLY -> "2.88";
            case WITH_BLOCKS -> "2.88 blocks";
            case WITH_M      -> "2.88 M";
        };

        drawValueRow(drawContext, displayModeButton, "Display Format", modeText);
        drawResetIconIfPresent(drawContext, displayModeReset);
    }

    private void drawEntityFilterRows(DrawContext drawContext)
    {
        drawToggleRow
        (
                drawContext,
                entityFilterToggle,
                "Enable Entity Filter",
                workingConfig.enableEntityFilter
        );

        drawResetIconIfPresent(drawContext, entityFilterToggleReset);

        drawValueRow
        (
                drawContext,
                entityFilterModeButton,
                "Filter Mode",
                workingConfig.useWhitelist ? "Whitelist" : "Blacklist"
        );

        drawResetIconIfPresent(drawContext, entityFilterModeReset);

        drawValueRow
        (
                drawContext,
                whitelistButton,
                "Whitelist (" + safeSize(workingConfig.whitelist) + ")",
                describeEntityList(workingConfig.whitelist)
        );

        drawResetIconIfPresent(drawContext, whitelistReset);

        drawValueRow
        (
                drawContext,
                blacklistButton,
                "Blacklist (" + safeSize(workingConfig.blacklist) + ")",
                describeEntityList(workingConfig.blacklist)
        );

        drawResetIconIfPresent(drawContext, blacklistReset);
    }

    private void drawDistanceColorRows(DrawContext drawContext)
    {
        drawToggleRow
        (
                drawContext,
                distanceColorToggle,
                "Enable Distance Color",
                workingConfig.enableDistanceColor
        );

        drawResetIconIfPresent(drawContext, distanceColorReset);

        for (int i = 0; i < workingConfig.distanceBands.size(); i++)
        {
            DistanceColorBand band = workingConfig.distanceBands.get(i);

            if (i > 0)
            {
                int fromResetIndex = i - 1;

                if (fromResetIndex < bandFromResetButtons.size())
                {
                    drawResetIconIfPresent(drawContext, bandFromResetButtons.get(fromResetIndex));
                }
            }

            if (i < bandTextColorButtons.size())
            {
                drawColorRow
                (
                        drawContext,
                        bandTextColorButtons.get(i),
                        "Band " + (i + 1) + " Main",
                        band.textColor
                );
            }

            if (i < bandTextColorResetButtons.size())
            {
                drawResetIconIfPresent(drawContext, bandTextColorResetButtons.get(i));
            }

            if (i < bandShadowColorButtons.size())
            {
                drawColorRow
                (
                        drawContext,
                        bandShadowColorButtons.get(i),
                        "Band " + (i + 1) + " Shadow",
                        band.shadowColor
                );
            }

            if (i < bandShadowColorResetButtons.size())
            {
                drawResetIconIfPresent(drawContext, bandShadowColorResetButtons.get(i));
            }

            if (i < bandBackgroundColorButtons.size())
            {
                drawColorRow
                (
                        drawContext,
                        bandBackgroundColorButtons.get(i),
                        "Band " + (i + 1) + " Background",
                        band.backgroundColor
                );
            }

            if (i < bandBackgroundColorResetButtons.size())
            {
                drawResetIconIfPresent(drawContext, bandBackgroundColorResetButtons.get(i));
            }
        }
    }



    /* ----- 행 렌더 헬퍼 ----- */

    private void drawResetIconIfPresent(DrawContext drawContext, ButtonWidget button)
    {
        if (button != null)
        {
            drawResetIcon(drawContext, button);
        }
    }

    private void drawSimpleLabelRow(DrawContext drawContext, ButtonWidget button, String label)
    {
        if (button == null)
        {
            return;
        }

        int x = button.getX();
        int y = button.getY();

        int height = button.getHeight();

        int color = getWidgetTextColor(button);

        drawContext.drawText
        (
                this.textRenderer,
                label,
                x + 4,
                y + (height - this.textRenderer.fontHeight) / 2,
                color,
                false
        );
    }

    private void drawValueRow(DrawContext drawContext, ButtonWidget button, String label, String value)
    {
        if (button == null)
        {
            return;
        }

        int color = getWidgetTextColor(button);

        int x = button.getX();
        int y = button.getY();

        int width  = button.getWidth();
        int height = button.getHeight();

        int textY = y + (height - this.textRenderer.fontHeight) / 2;

        drawContext.drawText
        (
                this.textRenderer,
                label,
                x + 4,
                textY,
                color,
                false
        );

        String shownValue = trimToWidth(value == null ? "" : value, Math.max(20, width / 2));

        drawContext.drawText
        (
                this.textRenderer,
                shownValue,
                x + width - this.textRenderer.getWidth(shownValue) - 4,
                textY,
                color,
                false
        );
    }

    private void drawToggleRow(DrawContext drawContext, ButtonWidget button, String label, boolean enabled)
    {
        if (button == null)
        {
            return;
        }

        int x = button.getX();
        int y = button.getY();

        int width  = button.getWidth();
        int height = button.getHeight();

        int color = getWidgetTextColor(button);

        drawContext.drawText
        (
                this.textRenderer,
                label,
                x + 4,
                y + (height - this.textRenderer.fontHeight) / 2,
                color,
                false
        );

        Identifier icon = enabled ? TOGGLE_ON_ICON : TOGGLE_OFF_ICON;

        int iconSize = 16;

        int iconX = x + width - iconSize - 4;
        int iconY = y + (height - iconSize) / 2;

        drawContext.drawTexture
        (
                RenderLayer::getGuiTextured,
                icon,
                iconX,
                iconY,
                0,
                0,
                iconSize,
                iconSize,
                iconSize,
                iconSize,
                color
        );
    }

    private void drawColorRow(DrawContext drawContext, ButtonWidget colorButton, String label, int argb)
    {
        if (colorButton == null)
        {
            return;
        }

        int x = colorButton.getX();
        int y = colorButton.getY();

        int width  = colorButton.getWidth();
        int height = colorButton.getHeight();

        int color = getWidgetTextColor(colorButton);

        int labelY = y + (height - this.textRenderer.fontHeight) / 2;

        drawContext.drawText
        (
                this.textRenderer,
                label,
                x + 4,
                labelY,
                color,
                false
        );

        int borderColor = colorButton.active ? 0xFF000000 : 0x66000000;
        int swatchColor = colorButton.active ? argb : withAlphaMultiplier(argb, 0.4f);

        drawContext.fill
        (
                x + width - 16,
                y + 4,
                x + width - 4,
                y + height - 4,
                borderColor
        );

        drawContext.fill
        (
                x + width - 15,
                y + 5,
                x + width - 5,
                y + height - 5,
                swatchColor
        );
    }

    private void drawResetIcon(DrawContext drawContext, ButtonWidget button)
    {
        int x = button.getX();
        int y = button.getY();

        int width  = button.getWidth();
        int height = button.getHeight();

        int iconSize = 16;

        int iconX = x + (width - iconSize) / 2;
        int iconY = y + (height - iconSize) / 2;

        int color = getWidgetTextColor(button);

        drawContext.drawTexture
        (
                RenderLayer::getGuiTextured,
                RESET_ICON,
                iconX,
                iconY,
                0.0f,
                0.0f,
                iconSize,
                iconSize,
                iconSize,
                iconSize,
                color
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

    private void setWidgetEnabled(ClickableWidget widget, boolean enabled)
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
    public boolean charTyped(char chr, int modifiers)
    {
        if (colorPopup != null)
        {
            return colorPopup.charTyped(chr, modifiers);
        }

        if (entityListPopup != null)
        {
            return entityListPopup.charTyped(chr, modifiers);
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (colorPopup != null)
        {
            return colorPopup.keyPressed(keyCode);
        }

        if (entityListPopup != null)
        {
            return entityListPopup.keyPressed(keyCode);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
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

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy)
    {
        if (colorPopup != null)
        {
            return colorPopup.mouseDragged(mouseX, mouseY, button, dx, dy);
        }

        if (entityListPopup != null)
        {
            return entityListPopup.mouseDragged(mouseX, mouseY, button, dx, dy);
        }

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (colorPopup != null)
        {
            return colorPopup.mouseReleased(mouseX, mouseY, button);
        }

        if (entityListPopup != null)
        {
            return entityListPopup.mouseReleased(mouseX, mouseY, button);
        }

        return super.mouseReleased(mouseX, mouseY, button);
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
                Text.literal(title),
                currentColor,
                resetColor,
                color ->
                {
                    setter.accept(color);
                    updateEnableStates();
                },
                () -> colorPopup = null
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
                Text.literal(title),
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

    private int getWidgetTextColor(ClickableWidget widget)
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

        if (this.textRenderer.getWidth(text) <= maxWidth)
        {
            return text;
        }

        String result = text;

        while (!result.isEmpty() && this.textRenderer.getWidth(result + "...") > maxWidth)
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