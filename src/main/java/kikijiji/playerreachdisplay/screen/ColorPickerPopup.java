package kikijiji.playerreachdisplay.screen;



import java.util.function.IntConsumer;

import net.minecraft.text.Text;

import net.minecraft.sound.SoundEvents;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;



public class ColorPickerPopup
{
    private static final int POPUP_WIDTH  = 190;
    private static final int POPUP_HEIGHT = 250;

    private static final int PREVIEW_WIDTH  = 40;
    private static final int PREVIEW_HEIGHT = 20;

    private static final int SV_WIDTH  = 120;
    private static final int SV_HEIGHT = 80;

    private static final int HUE_WIDTH  = 120;
    private static final int HUE_HEIGHT = 10;

    private static final int ALPHA_WIDTH  = 120;
    private static final int ALPHA_HEIGHT = 8;

    private static final int HEX_WIDTH  = 110;
    private static final int HEX_HEIGHT = 18;

    private static final int BUTTON_WIDTH  = 50;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP    = 5;

    private static final int OVERLAY_COLOR = 0xCC000000;
    private static final int POPUP_COLOR   = 0xFF111111;
    private static final int BORDER_COLOR  = 0xFFFFFFFF;



    private final Text        title;
    private final int         originalColor;
    private final int         resetColor;
    private final IntConsumer onChanged;
    private final Runnable    onClose;

    private int argb;
    private int lastNotifiedColor;

    private float hue;
    private float saturation;
    private float value;
    private float alpha;

    private boolean draggingHue;
    private boolean draggingSV;
    private boolean draggingAlpha;

    private int popupX;
    private int popupY;

    private boolean editingHex       = false;
    private boolean hexReplaceOnType = false;
    private String  hexBuffer        = "";



    private record PickerLayout
    (
            int previewX,
            int previewY,

            int svX,
            int svY,

            int hueX,
            int hueY,

            int alphaX,
            int alphaY,

            int hexX,
            int hexY,

            int buttonY
    )
    {
    }



    /* ----- 생성 ----- */

    public ColorPickerPopup
    (
            Text        title,
            int         initialColor,
            int         resetColor,
            IntConsumer onChanged,
            Runnable    onClose
    )
    {
        this.title         = title;
        this.originalColor = initialColor;
        this.resetColor    = resetColor;
        this.onChanged     = onChanged;
        this.onClose       = onClose;

        setFromColor(initialColor);
        lastNotifiedColor = argb;
    }



    /* ----- 렌더 ----- */

    public void render
    (
            DrawContext  drawContext,
            TextRenderer textRenderer,
            int          screenWidth,
            int          screenHeight,
            int          mouseX,
            int          mouseY
    )
    {
        updatePopupPosition(screenWidth, screenHeight);

        PickerLayout layout = createLayout();

        drawOverlay(drawContext, screenWidth, screenHeight);
        drawPopupFrame(drawContext);
        drawTitle(drawContext, textRenderer);
        drawPreview(drawContext, layout);

        drawSaturationValueArea(drawContext, layout);
        drawHueControl(drawContext, layout);
        drawAlphaControl(drawContext, layout);

        drawHexInput(drawContext, textRenderer, layout);
        drawActionButtons(drawContext, textRenderer, layout, mouseX, mouseY);
    }



    private void drawOverlay(DrawContext drawContext, int screenWidth, int screenHeight)
    {
        drawContext.fill
        (
                0,
                0,
                screenWidth,
                screenHeight,
                OVERLAY_COLOR
        );
    }

    private void drawPopupFrame(DrawContext drawContext)
    {
        drawContext.fill
        (
                popupX,
                popupY,
                popupX + POPUP_WIDTH,
                popupY + POPUP_HEIGHT,
                POPUP_COLOR
        );

        drawContext.drawStrokedRectangle
        (
                popupX,
                popupY,
                POPUP_WIDTH,
                POPUP_HEIGHT,
                BORDER_COLOR
        );
    }

    private void drawTitle(DrawContext drawContext, TextRenderer textRenderer)
    {
        drawContext.drawCenteredTextWithShadow
        (
                textRenderer,
                title,
                popupX + POPUP_WIDTH / 2,
                popupY + 10,
                0xFFFFFFFF
        );
    }

    private void drawPreview(DrawContext drawContext, PickerLayout layout)
    {
        drawContext.fill
        (
                layout.previewX() - 1,
                layout.previewY() - 1,
                layout.previewX() + PREVIEW_WIDTH + 1,
                layout.previewY() + PREVIEW_HEIGHT + 1,
                0xFFFFFFFF
        );

        drawContext.fill
        (
                layout.previewX(),
                layout.previewY(),
                layout.previewX() + PREVIEW_WIDTH,
                layout.previewY() + PREVIEW_HEIGHT,
                argb
        );
    }

    private void drawSaturationValueArea(DrawContext drawContext, PickerLayout layout)
    {
        drawSVArea(drawContext, layout.svX(), layout.svY());

        int cursorX = layout.svX() + (int)(saturation * (SV_WIDTH - 1));
        int cursorY = layout.svY() + (int)((1.0f - value) * (SV_HEIGHT - 1));

        drawContext.drawStrokedRectangle
        (
                cursorX - 2,
                cursorY - 2,
                4,
                4,
                0xFFFFFFFF
        );
    }

    private void drawHueControl(DrawContext drawContext, PickerLayout layout)
    {
        drawHueBar(drawContext, layout.hueX(), layout.hueY());

        int cursorX = layout.hueX() + (int)(hue * (HUE_WIDTH - 1));

        drawContext.drawStrokedRectangle
        (
                cursorX - 2,
                layout.hueY() - 2,
                4,
                HUE_HEIGHT + 4,
                0xFFFFFFFF
        );
    }

    private void drawAlphaControl(DrawContext drawContext, PickerLayout layout)
    {
        drawAlphaBar(drawContext, layout.alphaX(), layout.alphaY());

        int cursorX = layout.alphaX() + (int)(alpha * (ALPHA_WIDTH - 1));

        drawContext.drawStrokedRectangle
        (
                cursorX - 2,
                layout.alphaY() - 2,
                4,
                ALPHA_HEIGHT + 4,
                0xFFFFFFFF
        );
    }

    private void drawActionButtons
    (
            DrawContext  drawContext,
            TextRenderer textRenderer,
            PickerLayout layout,
            int          mouseX,
            int          mouseY
    )
    {
        drawButton
        (
                drawContext,
                textRenderer,
                getResetButtonX(),
                layout.buttonY(),
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "Reset",
                mouseX,
                mouseY,
                canReset()
        );

        drawButton
        (
                drawContext,
                textRenderer,
                getCancelButtonX(),
                layout.buttonY(),
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "Cancel",
                mouseX,
                mouseY,
                true
        );

        drawButton
        (
                drawContext,
                textRenderer,
                getDoneButtonX(),
                layout.buttonY(),
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "Apply",
                mouseX,
                mouseY,
                true
        );
    }


    private boolean canReset()
    {
        return argb != resetColor;
    }



    /* ----- 레이아웃 ----- */

    private void updatePopupPosition(int screenWidth, int screenHeight)
    {
        popupX = screenWidth  / 2 - POPUP_WIDTH  / 2;
        popupY = screenHeight / 2 - POPUP_HEIGHT / 2;
    }

    private PickerLayout createLayout()
    {
        int centerX = popupX + POPUP_WIDTH / 2;

        int previewX = centerX - PREVIEW_WIDTH / 2;
        int previewY = popupY + 25;

        int svX = centerX - SV_WIDTH / 2;
        int svY = popupY + 55;

        int hueX = centerX - HUE_WIDTH / 2;
        int hueY = svY + SV_HEIGHT + 8;

        int alphaX = centerX - ALPHA_WIDTH / 2;
        int alphaY = hueY + HUE_HEIGHT + 8;

        int hexX = centerX - HEX_WIDTH / 2;
        int hexY = alphaY + ALPHA_HEIGHT + 16;

        int buttonY = popupY + POPUP_HEIGHT - 30;

        return new PickerLayout
        (
                previewX,
                previewY,
                svX,
                svY,
                hueX,
                hueY,
                alphaX,
                alphaY,
                hexX,
                hexY,
                buttonY
        );
    }

    private int getResetButtonX()
    {
        return popupX + 15;
    }

    private int getCancelButtonX()
    {
        return getResetButtonX() + BUTTON_WIDTH + BUTTON_GAP;
    }

    private int getDoneButtonX()
    {
        return getCancelButtonX() + BUTTON_WIDTH + BUTTON_GAP;
    }



    /* ----- HEX 입력 ----- */

    private void drawHexInput
    (
            DrawContext  drawContext,
            TextRenderer textRenderer,
            PickerLayout layout
    )
    {
        int inputX = layout.hexX();
        int inputY = layout.hexY();

        drawContext.drawText
        (
                textRenderer,
                Text.literal("ARGB Hex"),
                inputX,
                inputY - 10,
                0xFFAAAAAA,
                false
        );

        drawContext.fill
        (
                inputX,
                inputY,
                inputX + HEX_WIDTH,
                inputY + HEX_HEIGHT,
                0xFF050505
        );

        drawContext.drawStrokedRectangle
        (
                inputX,
                inputY,
                HEX_WIDTH,
                HEX_HEIGHT,
                editingHex ? 0xFFFFFFFF : 0xFF777777
        );

        String shownText = getShownHexText();

        int color = getHexTextColor();

        shownText = trimToWidth(textRenderer, shownText, HEX_WIDTH - 8);

        int textX = inputX + 4;
        int textY = inputY + (HEX_HEIGHT - textRenderer.fontHeight) / 2;

        drawContext.drawText
        (
                textRenderer,
                Text.literal(shownText),
                textX,
                textY,
                color,
                false
        );

        if (editingHex)
        {
            drawHexCursor(drawContext, textRenderer, inputX, inputY);
        }
    }

    private String getShownHexText()
    {
        if (editingHex)
        {
            if (hexBuffer.isEmpty())
            {
                return "#AARRGGBB";
            }

            return "#" + hexBuffer;
        }

        return String.format("#%08X", argb);
    }

    private int getHexTextColor()
    {
        if (editingHex && hexBuffer.isEmpty())
        {
            return 0xFF777777;
        }

        return 0xFFFFFFFF;
    }

    private void drawHexCursor
    (
            DrawContext  drawContext,
            TextRenderer textRenderer,
            int          inputX,
            int          inputY
    )
    {
        String cursorBase = hexBuffer.isEmpty() ? "#" : "#" + hexBuffer;

        int cursorX = inputX + 4 + Math.min
        (
                textRenderer.getWidth(cursorBase),
                HEX_WIDTH - 10
        );

        drawContext.fill
        (
                cursorX,
                inputY + 4,
                cursorX + 1,
                inputY + HEX_HEIGHT - 4,
                0xFFFFFFFF
        );
    }

    private void startHexEditing()
    {
        editingHex       = true;
        hexReplaceOnType = true;
        hexBuffer        = String.format("%08X", argb);
    }

    private void stopHexEditing()
    {
        editingHex       = false;
        hexReplaceOnType = false;
        hexBuffer        = String.format("%08X", argb);
    }

    public boolean charTyped(char chr, int modifiers)
    {
        if (!editingHex)
        {
            return true;
        }

        if (!isHexChar(chr))
        {
            return true;
        }

        if (hexReplaceOnType)
        {
            hexBuffer = "";
            hexReplaceOnType = false;
        }

        if (hexBuffer.length() < 8)
        {
            hexBuffer += Character.toUpperCase(chr);
            applyHexBufferIfValid();
        }

        return true;
    }

    private boolean isHexChar(char chr)
    {
        return (chr >= '0' && chr <= '9') ||
               (chr >= 'a' && chr <= 'f') ||
               (chr >= 'A' && chr <= 'F');
    }

    private void applyHexBufferIfValid()
    {
        String value = hexBuffer.trim();

        if (value.length() == 6)
        {
            value = "FF" + value;
        }

        if (value.length() != 8)
        {
            return;
        }

        try
        {
            int parsed = (int)Long.parseLong(value, 16);

            setFromColor(parsed);
            notifyColorChanged();
        }
        catch (NumberFormatException ignored)
        {
        }
    }



    /* ----- 마우스 입력 ----- */

    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button != 0)
        {
            return true;
        }

        PickerLayout layout = createLayout();

        if (handleButtonClick(mouseX, mouseY, layout))
        {
            return true;
        }

        if (handleHexClick(mouseX, mouseY, layout))
        {
            return true;
        }

        stopHexEditing();

        if (handlePickerClick(mouseX, mouseY, layout))
        {
            return true;
        }

        return true;
    }

    private boolean handleButtonClick(double mouseX, double mouseY, PickerLayout layout)
    {
        if (isInside(mouseX, mouseY, getResetButtonX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT))
        {
            if (canReset())
            {
                playClickSound();

                setFromColor(resetColor);
                notifyColorChanged();
            }

            return true;
        }

        if (isInside(mouseX, mouseY, getCancelButtonX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT))
        {
            playClickSound();

            onChanged.accept(originalColor);
            onClose.run();

            return true;
        }

        if (isInside(mouseX, mouseY, getDoneButtonX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT))
        {
            playClickSound();

            onClose.run();

            return true;
        }

        return false;
    }

    private boolean handleHexClick(double mouseX, double mouseY, PickerLayout layout)
    {
        if (!isInside(mouseX, mouseY, layout.hexX(), layout.hexY(), HEX_WIDTH, HEX_HEIGHT))
        {
            return false;
        }

        playClickSound();
        startHexEditing();

        return true;
    }

    private boolean handlePickerClick(double mouseX, double mouseY, PickerLayout layout)
    {
        if (isInside(mouseX, mouseY, layout.svX(), layout.svY(), SV_WIDTH, SV_HEIGHT))
        {
            playClickSound();

            draggingSV = true;
            updateSVFromMouse(mouseX, mouseY, layout.svX(), layout.svY());
            notifyColorChanged();

            return true;
        }

        if (isInside(mouseX, mouseY, layout.hueX(), layout.hueY(), HUE_WIDTH, HUE_HEIGHT))
        {
            playClickSound();

            draggingHue = true;
            updateHueFromMouse(mouseX, layout.hueX());
            notifyColorChanged();

            return true;
        }

        if (isInside(mouseX, mouseY, layout.alphaX(), layout.alphaY(), ALPHA_WIDTH, ALPHA_HEIGHT))
        {
            playClickSound();

            draggingAlpha = true;
            updateAlphaFromMouse(mouseX, layout.alphaX());
            notifyColorChanged();

            return true;
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy)
    {
        if (button != 0)
        {
            return true;
        }

        PickerLayout layout = createLayout();

        if (draggingSV)
        {
            updateSVFromMouse(mouseX, mouseY, layout.svX(), layout.svY());
            notifyColorChanged();

            return true;
        }

        if (draggingHue)
        {
            updateHueFromMouse(mouseX, layout.hueX());
            notifyColorChanged();

            return true;
        }

        if (draggingAlpha)
        {
            updateAlphaFromMouse(mouseX, layout.alphaX());
            notifyColorChanged();

            return true;
        }

        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            draggingSV    = false;
            draggingHue   = false;
            draggingAlpha = false;
        }

        return true;
    }


    private void notifyColorChanged()
    {
        if (argb == lastNotifiedColor)
        {
            return;
        }

        lastNotifiedColor = argb;
        onChanged.accept(argb);
    }



    /* ----- 키 입력 ----- */

    public boolean keyPressed(int keyCode)
    {
        if (editingHex)
        {
            // Backspace
            if (keyCode == 259)
            {
                if (hexReplaceOnType)
                {
                    hexBuffer = "";
                    hexReplaceOnType = false;

                    return true;
                }

                if (!hexBuffer.isEmpty())
                {
                    hexBuffer = hexBuffer.substring(0, hexBuffer.length() - 1);
                    applyHexBufferIfValid();
                }

                return true;
            }

            // Enter / Numpad Enter
            if (keyCode == 257 || keyCode == 335)
            {
                applyHexBufferIfValid();
                stopHexEditing();

                return true;
            }

            // ESC: 팝업 닫지 말고 HEX 편집만 종료
            if (keyCode == 256)
            {
                stopHexEditing();

                return true;
            }

            return true;
        }

        // ESC: 편집 중이 아닐 때만 팝업 취소
        if (keyCode == 256)
        {
            onChanged.accept(originalColor);
            onClose.run();

            return true;
        }

        return true;
    }



    /* ----- 공통 UI ----- */

    private void playClickSound()
    {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client != null)
        {
            client.getSoundManager().play
            (
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
            );
        }
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height)
    {
        return mouseX >= x &&
               mouseX <  x + width &&
               mouseY >= y &&
               mouseY <  y + height;
    }

    private void drawButton
    (
            DrawContext  drawContext,
            TextRenderer textRenderer,
            int          x,
            int          y,
            int          width,
            int          height,
            String       label,
            int          mouseX,
            int          mouseY,
            boolean      active
    )
    {
        boolean hovered = active && isInside(mouseX, mouseY, x, y, width, height);

        int background = active ? hovered ? 0xFF666666 : 0xFF444444 : 0xFF222222;

        int border = active ? 0xFFFFFFFF : 0xFF777777;
        int color  = active ? 0xFFFFFFFF : 0xFF777777;

        drawContext.fill(x, y, x + width, y + height, background);
        drawContext.drawStrokedRectangle(x, y, width, height, border);

        drawContext.drawCenteredTextWithShadow
        (
                textRenderer,
                Text.literal(label),
                x + width / 2,
                y + (height - textRenderer.fontHeight) / 2,
                color
        );
    }

    private String trimToWidth(TextRenderer textRenderer, String text, int maxWidth)
    {
        if (text == null)
        {
            return "";
        }

        if (textRenderer.getWidth(text) <= maxWidth)
        {
            return text;
        }

        String result = text;

        while (!result.isEmpty() && textRenderer.getWidth(result + "...") > maxWidth)
        {
            result = result.substring(0, result.length() - 1);
        }

        return result.isEmpty() ? "..." : result + "...";
    }



    /* ----- HSV ----- */

    private static int hsvToRgb(float h, float s, float v)
    {
        float r;
        float g;
        float b;

        int i = (int)(h * 6.0f);

        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);

        switch (i % 6)
        {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            case 5 -> { r = v; g = p; b = q; }

            default -> { r = v; g = t; b = p; }
        }

        int ri = (int)(r * 255.0f) & 0xFF;
        int gi = (int)(g * 255.0f) & 0xFF;
        int bi = (int)(b * 255.0f) & 0xFF;

        return (ri << 16) | (gi << 8) | bi;
    }

    private void updateColorFromHSV()
    {
        int rgb = hsvToRgb(hue, saturation, value);
        int a = (int)(alpha * 255.0f) & 0xFF;

        argb = (a << 24) | rgb;
    }

    private void setFromColor(int color)
    {
        this.argb = color;

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8)  & 0xFF;
        int b = color & 0xFF;

        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));

        float delta = max - min;

        value = max;

        if (max == 0)
        {
            saturation = 0;
        }
        else
        {
            saturation = delta / max;
        }

        if (delta == 0)
        {
            hue = 0;
        }
        else if (max == rf)
        {
            hue = ((gf - bf) / delta) % 6.0f;
        }
        else if (max == gf)
        {
            hue = (bf - rf) / delta + 2.0f;
        }
        else
        {
            hue = (rf - gf) / delta + 4.0f;
        }

        hue /= 6.0f;

        if (hue < 0)
        {
            hue += 1.0f;
        }

        alpha = a / 255.0f;

        updateColorFromHSV();
    }



    /* ----- HUE ----- */

    private void drawHueBar(DrawContext drawContext, int hueX, int hueY)
    {
        for (int x = 0; x < HUE_WIDTH; x++)
        {
            float h = x / (float)(HUE_WIDTH - 1);
            int rgb = hsvToRgb(h, 1.0f, 1.0f);

            drawContext.fill
            (
                    hueX + x,
                    hueY,
                    hueX + x + 1,
                    hueY + HUE_HEIGHT,
                    0xFF000000 | rgb
            );
        }
    }

    private void updateHueFromMouse(double mouseX, int hueX)
    {
        double localX = Math.clamp(mouseX - hueX, 0.0, HUE_WIDTH - 1.0);

        hue = (float)(localX / (HUE_WIDTH - 1));

        updateColorFromHSV();
    }



    /* ----- SV ----- */

    private void drawSVArea(DrawContext drawContext, int svX, int svY)
    {
        for (int x = 0; x < SV_WIDTH; x++)
        {
            float s = x / (float)(SV_WIDTH - 1);

            int topRgb = hsvToRgb(hue, s, 1.0f);

            drawContext.fillGradient
            (
                    svX + x,
                    svY,
                    svX + x + 1,
                    svY + SV_HEIGHT,
                    0xFF000000 | topRgb,
                    0xFF000000
            );
        }
    }

    private void updateSVFromMouse(double mouseX, double mouseY, int svX, int svY)
    {
        double localX = Math.clamp(mouseX - svX, 0.0, SV_WIDTH  - 1.0);
        double localY = Math.clamp(mouseY - svY, 0.0, SV_HEIGHT - 1.0);

        saturation = (float)(localX / (SV_WIDTH - 1));
        value      = 1.0f - (float)(localY / (SV_HEIGHT - 1));

        updateColorFromHSV();
    }



    /* ----- Alpha ----- */

    private void drawAlphaBar(DrawContext drawContext, int alphaX, int alphaY)
    {
        for (int x = 0; x < ALPHA_WIDTH; x++)
        {
            float a   = x / (float)(ALPHA_WIDTH - 1);
            int rgb   = argb & 0x00FFFFFF;
            int color = ((int)(a * 255) << 24) | rgb;

            drawContext.fill
            (
                    alphaX + x,
                    alphaY,
                    alphaX + x + 1,
                    alphaY + ALPHA_HEIGHT,
                    color
            );
        }
    }

    private void updateAlphaFromMouse(double mouseX, int alphaX)
    {
        double localX = Math.clamp(mouseX - alphaX, 0.0, ALPHA_WIDTH - 1.0);

        alpha = (float)(localX / (ALPHA_WIDTH - 1));

        updateColorFromHSV();
    }
}