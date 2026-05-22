package kikijiji.playerreachdisplay.screen;



import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import net.minecraft.network.chat.Component;

import net.minecraft.sounds.SoundEvents;



public class EntityListPopup
{
    private static final int POPUP_WIDTH  = 280;
    private static final int POPUP_HEIGHT = 235;

    private static final int LIST_HEIGHT = 90;
    private static final int ROW_HEIGHT  = 18;

    private static final int INPUT_HEIGHT = 20;

    private static final int BUTTON_WIDTH  = 55;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP    = 6;
    private static final int BUTTON_COUNT  = 4;

    private static final int BUTTON_GROUP_WIDTH = BUTTON_WIDTH * BUTTON_COUNT + BUTTON_GAP * (BUTTON_COUNT - 1);

    private static final int OVERLAY_COLOR = 0xCC000000;
    private static final int POPUP_COLOR   = 0xFF111111;
    private static final int BORDER_COLOR  = 0xFFFFFFFF;



    private final Component             title;
    private final List<String>          originalEntries;
    private final List<String>          resetEntries;
    private final List<String>          workingEntries = new ArrayList<>();
    private final Consumer<List<String>> onDone;
    private final Runnable              onClose;

    private String  inputBuffer  = "";
    private boolean inputFocused = true;

    private int scrollOffset = 0;

    private int popupX;
    private int popupY;



    private record PopupLayout
    (
            int listX,
            int listY,
            int listWidth,
            int listHeight,

            int inputX,
            int inputY,
            int inputWidth,
            int inputHeight,

            int buttonY
    )
    {
    }



    /* ----- 생성 ----- */

    public EntityListPopup
    (
            Component              title,
            List<String>           initialEntries,
            List<String>           resetEntries,
            Consumer<List<String>> onDone,
            Runnable               onClose
    )
    {
        this.title = title;

        this.originalEntries = copyList(initialEntries);
        this.resetEntries    = copyList(resetEntries);

        this.workingEntries.addAll(copyList(initialEntries));

        this.onDone  = onDone;
        this.onClose = onClose;
    }



    /* ----- 렌더 ----- */

    public void render
    (
            GuiGraphicsExtractor graphics,
            Font                 font,
            int                  screenWidth,
            int                  screenHeight,
            int                  mouseX,
            int                  mouseY
    )
    {
        updatePopupPosition(screenWidth, screenHeight);

        PopupLayout layout = createLayout();

        drawOverlay(graphics, screenWidth, screenHeight);
        drawPopupFrame(graphics);
        drawTitle(graphics, font);
        drawList(graphics, font, layout, mouseX, mouseY);
        drawInput(graphics, font, layout);
        drawButtons(graphics, font, layout, mouseX, mouseY);
    }



    private void drawOverlay(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight)
    {
        graphics.fill
        (
                0,
                0,
                screenWidth,
                screenHeight,
                OVERLAY_COLOR
        );
    }

    private void drawPopupFrame(GuiGraphicsExtractor graphics)
    {
        graphics.fill
        (
                popupX,
                popupY,
                popupX + POPUP_WIDTH,
                popupY + POPUP_HEIGHT,
                POPUP_COLOR
        );

        drawBorder
        (
                graphics,
                popupX,
                popupY,
                POPUP_WIDTH,
                POPUP_HEIGHT,
                BORDER_COLOR
        );
    }

    private void drawTitle(GuiGraphicsExtractor graphics, Font font)
    {
        drawCenteredText
        (
                graphics,
                font,
                title.getString(),
                popupX + POPUP_WIDTH / 2,
                popupY + 10,
                0xFFFFFFFF
        );
    }



    /* ----- 리스트 ----- */

    private void drawList
    (
            GuiGraphicsExtractor graphics,
            Font                 font,
            PopupLayout          layout,
            int                  mouseX,
            int                  mouseY
    )
    {
        graphics.fill
        (
                layout.listX(),
                layout.listY(),
                layout.listX() + layout.listWidth(),
                layout.listY() + layout.listHeight(),
                0xFF050505
        );

        drawBorder
        (
                graphics,
                layout.listX(),
                layout.listY(),
                layout.listWidth(),
                layout.listHeight(),
                0xFF777777
        );

        int visibleRows = layout.listHeight() / ROW_HEIGHT;

        scrollOffset = Math.clamp(scrollOffset, 0, getMaxScroll());

        if (workingEntries.isEmpty())
        {
            drawCenteredText
            (
                    graphics,
                    font,
                    "No entries",
                    layout.listX() + layout.listWidth() / 2,
                    layout.listY() + layout.listHeight() / 2 - font.lineHeight / 2,
                    0xFFAAAAAA
            );

            return;
        }

        for (int visibleIndex = 0; visibleIndex < visibleRows; visibleIndex++)
        {
            int entryIndex = scrollOffset + visibleIndex;

            if (entryIndex >= workingEntries.size())
            {
                break;
            }

            drawEntryRow
            (
                    graphics,
                    font,
                    layout,
                    visibleIndex,
                    entryIndex,
                    mouseX,
                    mouseY
            );
        }
    }

    private void drawEntryRow
    (
            GuiGraphicsExtractor graphics,
            Font                 font,
            PopupLayout          layout,
            int                  visibleIndex,
            int                  entryIndex,
            int                  mouseX,
            int                  mouseY
    )
    {
        int rowY = layout.listY() + visibleIndex * ROW_HEIGHT;

        boolean hovered = isInside
        (
                mouseX,
                mouseY,
                layout.listX(),
                rowY,
                layout.listWidth(),
                ROW_HEIGHT
        );

        graphics.fill
        (
                layout.listX() + 1,
                rowY + 1,
                layout.listX() + layout.listWidth() - 1,
                rowY + ROW_HEIGHT,
                hovered ? 0xFF333333 : 0xFF1A1A1A
        );

        String entry = trimToWidth
        (
                font,
                workingEntries.get(entryIndex),
                layout.listWidth() - 35
        );

        graphics.text
        (
                font,
                entry,
                layout.listX() + 5,
                rowY + (ROW_HEIGHT - font.lineHeight) / 2,
                0xFFFFFFFF,
                false
        );

        int removeX = getRemoveButtonX(layout);
        int removeY = rowY + 2;

        graphics.fill
        (
                removeX,
                removeY,
                removeX + 14,
                removeY + 14,
                0xFF550000
        );

        drawBorder
        (
                graphics,
                removeX,
                removeY,
                14,
                14,
                0xFFFFAAAA
        );

        drawCenteredText
        (
                graphics,
                font,
                "x",
                removeX + 7,
                removeY + 2,
                0xFFFFFFFF
        );
    }



    /* ----- 입력 ----- */

    private void drawInput
    (
            GuiGraphicsExtractor graphics,
            Font                 font,
            PopupLayout          layout
    )
    {
        graphics.fill
        (
                layout.inputX(),
                layout.inputY(),
                layout.inputX() + layout.inputWidth(),
                layout.inputY() + layout.inputHeight(),
                0xFF050505
        );

        drawBorder
        (
                graphics,
                layout.inputX(),
                layout.inputY(),
                layout.inputWidth(),
                layout.inputHeight(),
                inputFocused ? 0xFFFFFFFF : 0xFF777777
        );

        String text  = getInputText();
        int    color = getInputTextColor();

        text = trimToWidth(font, text, layout.inputWidth() - 8);

        graphics.text
        (
                font,
                text,
                layout.inputX() + 4,
                layout.inputY() + (layout.inputHeight() - font.lineHeight) / 2,
                color,
                false
        );

        if (inputFocused)
        {
            drawInputCursor(graphics, font, layout);
        }
    }

    private String getInputText()
    {
        if (inputBuffer.isEmpty())
        {
            return "Example: zombie or minecraft:zombie";
        }

        return inputBuffer;
    }

    private int getInputTextColor()
    {
        if (inputBuffer.isEmpty())
        {
            return 0xFF777777;
        }

        return 0xFFFFFFFF;
    }

    private void drawInputCursor
    (
            GuiGraphicsExtractor graphics,
            Font                 font,
            PopupLayout          layout
    )
    {
        int cursorX;

        if (inputBuffer.isEmpty())
        {
            cursorX = layout.inputX() + 4;
        }
        else
        {
            cursorX = layout.inputX() + 4 + Math.min
            (
                    font.width(inputBuffer),
                    layout.inputWidth() - 10
            );
        }

        graphics.fill
        (
                cursorX,
                layout.inputY() + 4,
                cursorX + 1,
                layout.inputY() + layout.inputHeight() - 4,
                0xFFFFFFFF
        );
    }



    /* ----- 버튼 ----- */

    private void drawButtons
    (
            GuiGraphicsExtractor graphics,
            Font                 font,
            PopupLayout          layout,
            int                  mouseX,
            int                  mouseY
    )
    {
        drawButton
        (
                graphics,
                font,
                getAddButtonX(),
                layout.buttonY(),
                BUTTON_WIDTH,
                BUTTON_HEIGHT,
                "Add",
                mouseX,
                mouseY,
                hasInput()
        );

        drawButton
        (
                graphics,
                font,
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
                graphics,
                font,
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
                graphics,
                font,
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



    /* ----- 레이아웃 ----- */

    private void updatePopupPosition(int screenWidth, int screenHeight)
    {
        popupX = screenWidth  / 2 - POPUP_WIDTH  / 2;
        popupY = screenHeight / 2 - POPUP_HEIGHT / 2;
    }

    private PopupLayout createLayout()
    {
        int listX = popupX + 15;
        int listY = popupY + 35;

        int listWidth  = POPUP_WIDTH - 30;
        int listHeight = LIST_HEIGHT;

        int inputX = popupX + 15;
        int inputY = popupY + 135;

        int inputWidth  = POPUP_WIDTH - 30;
        int inputHeight = INPUT_HEIGHT;

        int buttonY = popupY + POPUP_HEIGHT - 30;

        return new PopupLayout
        (
                listX,
                listY,
                listWidth,
                listHeight,
                inputX,
                inputY,
                inputWidth,
                inputHeight,
                buttonY
        );
    }

    private int getButtonGroupX()
    {
        return popupX + (POPUP_WIDTH - BUTTON_GROUP_WIDTH) / 2;
    }

    private int getAddButtonX()
    {
        return getButtonGroupX();
    }

    private int getResetButtonX()
    {
        return getAddButtonX() + BUTTON_WIDTH + BUTTON_GAP;
    }

    private int getCancelButtonX()
    {
        return getResetButtonX() + BUTTON_WIDTH + BUTTON_GAP;
    }

    private int getDoneButtonX()
    {
        return getCancelButtonX() + BUTTON_WIDTH + BUTTON_GAP;
    }

    private int getRemoveButtonX(PopupLayout layout)
    {
        return layout.listX() + layout.listWidth() - 18;
    }



    /* ----- 마우스 입력 ----- */

    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button != 0)
        {
            return true;
        }

        PopupLayout layout = createLayout();

        if (handleButtonClick(mouseX, mouseY, layout))
        {
            return true;
        }

        if (handleInputClick(mouseX, mouseY, layout))
        {
            return true;
        }

        inputFocused = false;

        if (handleListClick(mouseX, mouseY, layout))
        {
            return true;
        }

        return true;
    }

    private boolean handleButtonClick(double mouseX, double mouseY, PopupLayout layout)
    {
        if (isInside(mouseX, mouseY, getAddButtonX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT))
        {
            if (hasInput())
            {
                playClickSound();

                tryAddInput();
                inputFocused = true;
            }

            return true;
        }

        if (isInside(mouseX, mouseY, getResetButtonX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT))
        {
            if (canReset())
            {
                playClickSound();

                workingEntries.clear();
                workingEntries.addAll(copyList(resetEntries));

                scrollOffset = Math.clamp(scrollOffset, 0, getMaxScroll());
                inputFocused = true;
            }

            return true;
        }

        if (isInside(mouseX, mouseY, getCancelButtonX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT))
        {
            playClickSound();

            workingEntries.clear();
            workingEntries.addAll(copyList(originalEntries));

            onClose.run();

            return true;
        }

        if (isInside(mouseX, mouseY, getDoneButtonX(), layout.buttonY(), BUTTON_WIDTH, BUTTON_HEIGHT))
        {
            playClickSound();

            onDone.accept(copyList(workingEntries));
            onClose.run();

            return true;
        }

        return false;
    }

    private boolean handleInputClick(double mouseX, double mouseY, PopupLayout layout)
    {
        if (!isInside(mouseX, mouseY, layout.inputX(), layout.inputY(), layout.inputWidth(), layout.inputHeight()))
        {
            return false;
        }

        playClickSound();

        inputFocused = true;

        return true;
    }

    private boolean handleListClick(double mouseX, double mouseY, PopupLayout layout)
    {
        if (!isInside(mouseX, mouseY, layout.listX(), layout.listY(), layout.listWidth(), layout.listHeight()))
        {
            return false;
        }

        int localY = (int)mouseY - layout.listY();

        int visibleIndex = localY / ROW_HEIGHT;

        int entryIndex = scrollOffset + visibleIndex;

        if (entryIndex < 0 || entryIndex >= workingEntries.size())
        {
            return true;
        }

        int rowY = layout.listY() + visibleIndex * ROW_HEIGHT;

        int removeX = getRemoveButtonX(layout);
        int removeY = rowY + 2;

        if (isInside(mouseX, mouseY, removeX, removeY, 14, 14))
        {
            playClickSound();

            workingEntries.remove(entryIndex);
            scrollOffset = Math.clamp(scrollOffset, 0, getMaxScroll());
        }

        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount)
    {
        PopupLayout layout = createLayout();

        if (!isInside(mouseX, mouseY, layout.listX(), layout.listY(), layout.listWidth(), layout.listHeight()))
        {
            return true;
        }

        if (verticalAmount > 0)
        {
            scrollOffset--;
        }
        else if (verticalAmount < 0)
        {
            scrollOffset++;
        }

        scrollOffset = Math.clamp(scrollOffset, 0, getMaxScroll());

        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy)
    {
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        return true;
    }



    /* ----- 키 입력 ----- */

    public boolean charTyped(char chr, int modifiers)
    {
        if (!inputFocused)
        {
            return true;
        }

        if (isAllowedInputChar(chr) && inputBuffer.length() < 128)
        {
            inputBuffer += Character.toLowerCase(chr);
        }

        return true;
    }

    public boolean keyPressed(int keyCode)
    {
        // ESC
        if (keyCode == 256)
        {
            workingEntries.clear();
            workingEntries.addAll(copyList(originalEntries));

            onClose.run();

            return true;
        }

        if (!inputFocused)
        {
            return true;
        }

        // Backspace
        if (keyCode == 259)
        {
            if (!inputBuffer.isEmpty())
            {
                inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
            }

            return true;
        }

        // Enter / Numpad Enter
        if (keyCode == 257 || keyCode == 335)
        {
            tryAddInput();

            return true;
        }

        return true;
    }



    /* ----- 입력 처리 ----- */

    private void tryAddInput()
    {
        String[] parts = inputBuffer.split(",");

        for (String part : parts)
        {
            String normalized = normalizeEntry(part);

            if (!normalized.isEmpty() && !workingEntries.contains(normalized))
            {
                workingEntries.add(normalized);
            }
        }

        inputBuffer = "";
        scrollOffset = getMaxScroll();
    }

    private String normalizeEntry(String raw)
    {
        if (raw == null)
        {
            return "";
        }

        String value = raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");

        if (value.isEmpty())
        {
            return "";
        }

        if (!isValidEntityId(value))
        {
            return "";
        }

        if (!value.contains(":"))
        {
            return "minecraft:" + value;
        }

        return value;
    }

    private boolean isValidEntityId(String value)
    {
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean valid = Character.isLetterOrDigit(c) ||
                    c == ':' ||
                    c == '_' ||
                    c == '-' ||
                    c == '.' ||
                    c == '/';

            if (!valid)
            {
                return false;
            }
        }

        return true;
    }

    private boolean isAllowedInputChar(char chr)
    {
        return Character.isLetterOrDigit(chr) ||
                chr == ':' ||
                chr == '_' ||
                chr == '-' ||
                chr == '.' ||
                chr == '/' ||
                chr == ',' ||
                chr == ' ';
    }

    private boolean hasInput()
    {
        return !inputBuffer.trim().isEmpty();
    }

    private boolean canReset()
    {
        return !workingEntries.equals(resetEntries);
    }



    /* ----- 공통 UI ----- */

    private void playClickSound()
    {
        Minecraft client = Minecraft.getInstance();

        if (client != null)
        {
            client.getSoundManager().play
            (
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
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
            GuiGraphicsExtractor graphics,
            Font                 font,
            int                  x,
            int                  y,
            int                  width,
            int                  height,
            String               label,
            int                  mouseX,
            int                  mouseY,
            boolean              active
    )
    {
        boolean hovered = active && isInside(mouseX, mouseY, x, y, width, height);

        int background;

        if (active)
        {
            background = hovered ? 0xFF666666 : 0xFF444444;
        }
        else
        {
            background = 0xFF222222;
        }

        int border = active ? 0xFFFFFFFF : 0xFF777777;
        int color  = active ? 0xFFFFFFFF : 0xFF777777;

        graphics.fill
        (
                x,
                y,
                x + width,
                y + height,
                background
        );

        drawBorder
        (
                graphics,
                x,
                y,
                width,
                height,
                border
        );

        drawCenteredText
        (
                graphics,
                font,
                label,
                x + width / 2,
                y + (height - font.lineHeight) / 2,
                color
        );
    }

    private void drawBorder
    (
            GuiGraphicsExtractor graphics,
            int                  x,
            int                  y,
            int                  width,
            int                  height,
            int                  color
    )
    {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
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

    private String trimToWidth(Font font, String text, int maxWidth)
    {
        if (text == null)
        {
            return "";
        }

        if (font.width(text) <= maxWidth)
        {
            return text;
        }

        String result = text;

        while (!result.isEmpty() && font.width(result + "...") > maxWidth)
        {
            result = result.substring(0, result.length() - 1);
        }

        return result.isEmpty() ? "..." : result + "...";
    }



    /* ----- 리스트 헬퍼 ----- */

    private int getMaxScroll()
    {
        int visibleRows = LIST_HEIGHT / ROW_HEIGHT;

        return Math.max(0, workingEntries.size() - visibleRows);
    }

    private static List<String> copyList(List<String> source)
    {
        if (source == null)
        {
            return new ArrayList<>();
        }

        return new ArrayList<>(source);
    }
}