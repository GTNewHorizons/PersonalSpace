package me.eigenraven.personalspace.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;

import me.eigenraven.personalspace.config.AllowedBlock;
import me.eigenraven.personalspace.config.Config;
import me.eigenraven.personalspace.world.DimensionConfig;

/**
 * A block selector widget with hover-to-open dropdown panel. Displays a single button showing the selected block (or
 * "-" for air). Hovering opens a scrollable grid dropdown below, showing all available blocks with their meta variants
 * pre-rendered. Two modes: - SELECT mode (for boundary/gap/center): clicking a block changes the selection - ADD mode
 * (for layers): clicking a block fires the callback without changing the button display
 */
public class WBlockDropdown extends Widget {

    /** Size of each item cell in the dropdown grid */
    public static final int ITEM_SIZE = 20;
    /** Width of the scrollbar */
    public static final int SCROLLBAR_WIDTH = 6;
    /** Height of the search bar at top of dropdown */
    public static final int SEARCH_BAR_HEIGHT = 16;
    /** 9-patch background padding around the dropdown content */
    public static final int BG_PAD_LEFT = 6;
    public static final int BG_PAD_TOP = 4;
    public static final int BG_PAD_RIGHT = 6;
    public static final int BG_PAD_BOTTOM = 4;

    /** The currently open dropdown instance (only one can be open at a time) */
    private static WBlockDropdown openDropdown = null;
    /** Search text field for the currently open dropdown */
    private static GuiTextField searchField = null;
    /** Filtered entries based on search text */
    private static List<BlockEntry> filteredEntries = new ArrayList<>();
    /** Screen dimensions for overflow detection */
    private static int screenWidth, screenHeight, guiLeftOffset, guiTopOffset;
    /** Whether the scrollbar thumb is being dragged */
    private static boolean scrollbarDragging = false;

    /**
     * @param blockName    empty string = air
     * @param displayStack null for air
     */
    @Desugar
    public record BlockEntry(String blockName, int meta, ItemStack displayStack, String tooltip) {

    }

    private final List<BlockEntry> entries;
    private int scrollOffset = 0; // in rows
    private int selectedIndex = 0; // 0 = air entry
    private final boolean addMode; // true for layers (click adds), false for select mode
    private final Consumer<BlockEntry> onSelect;

    // Absolute position relative to GUI origin (guiLeft, guiTop)
    private int guiRelX, guiRelY;

    // Configurable dimensions from Config
    private final int itemsPerRow;
    private final int maxVisibleRows;

    // Label text displayed next to the button (optional)
    private String label = null;

    private static RenderItem renderItem;

    /**
     * Create a new block dropdown.
     *
     * @param position     trigger button position (should be ITEM_SIZE x ITEM_SIZE)
     * @param addMode      true for layers (click adds to list), false for selection mode
     * @param entries      list of available blocks (first should be air entry)
     * @param initialIndex initially selected index in entries
     * @param onSelect     callback when a block is clicked
     */
    public WBlockDropdown(Rectangle position, boolean addMode, List<BlockEntry> entries, int initialIndex,
            Consumer<BlockEntry> onSelect) {
        this.position = position;
        this.addMode = addMode;
        this.entries = entries;
        this.selectedIndex = Math.max(0, Math.min(initialIndex, entries.size() - 1));
        this.onSelect = onSelect;

        this.itemsPerRow = Math.max(1, Config.dropdownMaxVisibleColumns);
        this.maxVisibleRows = Math.max(1, Config.dropdownMaxVisibleRows);
    }

    // Dynamic dimension calculations based on display entries
    private int getTotalRows(List<BlockEntry> displayEntries) {
        return (int) Math.ceil((double) displayEntries.size() / itemsPerRow);
    }

    private int getVisibleRows(List<BlockEntry> displayEntries) {
        return Math.min(getTotalRows(displayEntries), maxVisibleRows);
    }

    private boolean getNeedsScrollbar(List<BlockEntry> displayEntries) {
        return getTotalRows(displayEntries) > maxVisibleRows;
    }

    private int getDropdownWidth(List<BlockEntry> displayEntries) {
        return itemsPerRow * ITEM_SIZE + (getNeedsScrollbar(displayEntries) ? SCROLLBAR_WIDTH : 0);
    }

    private int getGridHeight(List<BlockEntry> displayEntries) {
        return getVisibleRows(displayEntries) * ITEM_SIZE;
    }

    private int getDropdownHeight(List<BlockEntry> displayEntries) {
        return SEARCH_BAR_HEIGHT + getGridHeight(displayEntries);
    }

    private static void updateFilteredEntries() {
        if (openDropdown == null) {
            filteredEntries = new ArrayList<>();
            return;
        }
        String searchText = searchField != null ? searchField.getText() : "";
        if (searchText == null || searchText.isEmpty()) {
            filteredEntries = new ArrayList<>(openDropdown.entries);
        } else {
            String lower = searchText.toLowerCase();
            filteredEntries = new ArrayList<>();
            for (BlockEntry entry : openDropdown.entries) {
                if ((entry.tooltip() != null && entry.tooltip().toLowerCase().contains(lower))
                        || (entry.blockName() != null && entry.blockName().toLowerCase().contains(lower))) {
                    filteredEntries.add(entry);
                }
            }
        }
        openDropdown.scrollOffset = 0;
    }

    /** Set screen dimensions for overflow detection. Call before drawOpenDropdown. */
    public static void setScreenDimensions(int screenW, int screenH, int guiLeft, int guiTop) {
        screenWidth = screenW;
        screenHeight = screenH;
        guiLeftOffset = guiLeft;
        guiTopOffset = guiTop;
    }

    /** Compute dropdown position with overflow detection. Returns [ddX, ddY]. */
    private static int[] computeDropdownPosition(WBlockDropdown dd, List<BlockEntry> displayEntries) {
        int trigX = dd.guiRelX;
        int trigY = dd.guiRelY;
        int trigW = dd.position.width;
        int trigH = dd.position.height;
        int ddW = dd.getDropdownWidth(displayEntries);
        int ddH = dd.getDropdownHeight(displayEntries);

        int ddX = trigX;
        int ddY = trigY + trigH;

        // Check right overflow
        if (guiLeftOffset + ddX + ddW > screenWidth) {
            ddX = trigX + trigW - ddW;
            if (guiLeftOffset + ddX < 0) {
                ddX = -guiLeftOffset;
            }
        }

        // Check bottom overflow
        if (guiTopOffset + ddY + ddH > screenHeight) {
            ddY = trigY - ddH;
            if (guiTopOffset + ddY < 0) {
                ddY = -guiTopOffset;
            }
        }

        return new int[] { ddX, ddY };
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** Set the position relative to the GUI origin (guiLeft, guiTop) */
    public void setGuiRelativePos(int x, int y) {
        this.guiRelX = x;
        this.guiRelY = y;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = Math.max(0, Math.min(index, entries.size() - 1));
    }

    public BlockEntry getSelectedEntry() {
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            return entries.get(selectedIndex);
        }
        return entries.isEmpty() ? null : entries.get(0);
    }

    public List<BlockEntry> getEntries() {
        return entries;
    }

    public boolean isOpen() {
        return openDropdown == this;
    }

    private void open() {
        openDropdown = this;
        scrollOffset = 0;
        // Create a new GuiTextField for search; dimensions will be set at draw time
        searchField = new GuiTextField(
                Minecraft.getMinecraft().fontRenderer,
                0,
                0,
                itemsPerRow * ITEM_SIZE,
                SEARCH_BAR_HEIGHT - 2);
        searchField.setMaxStringLength(256);
        searchField.setFocused(true);
        searchField.setText("");
        updateFilteredEntries();
    }

    public static void closeDropdown() {
        if (openDropdown != null) {
            openDropdown.scrollOffset = 0;
            openDropdown = null;
            filteredEntries = new ArrayList<>();
            scrollbarDragging = false;
            searchField = null;
        }
    }

    public static boolean isAnyDropdownOpen() {
        return openDropdown != null;
    }

    public static void updateOpenDropdown() {
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    @Override
    protected void drawImpl(int mouseX, int mouseY, float partialTicks) {
        Icons.bindTexture();
        Icons icon = enabled ? Icons.BUTTON_NORMAL : Icons.BUTTON_OFF;
        if (enabled && testPoint(mouseX, mouseY)) {
            icon = Icons.BUTTON_HIGHLIGHT;
            // Only open on hover if no other dropdown is already open
            if (openDropdown == null && enabled) {
                open();
            }
        }
        icon.draw9Patch(0, 0, position.width, position.height);

        // Draw selected block or "-" for air
        BlockEntry selected = getSelectedEntry();
        if (selected != null && selected.displayStack != null && selected.displayStack.getItem() != null) {
            if (renderItem == null) {
                renderItem = new RenderItem();
            }
            Icons.drawItem(1, position.height / 2 - 9, selected.displayStack, "", renderItem);
        } else {
            // Draw "-" symbol for air
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            int textW = fr.getStringWidth("-");
            fr.drawString(
                    "-",
                    position.width / 2 - textW / 2,
                    position.height / 2 - fr.FONT_HEIGHT / 2,
                    0xFFFFFF,
                    true);
            GL11.glColor4f(1, 1, 1, 1);
        }

        // Draw label at bottom-right corner of button, on top of the item icon
        if (label != null && !label.isEmpty()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glPushMatrix();
            GL11.glTranslatef(0, 0, 200.0f);
            FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
            int labelW = fr.getStringWidth(label);
            fr.drawString(label, position.width - labelW - 1, position.height - fr.FONT_HEIGHT, 0xFFFFFF, true);
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1, 1, 1, 1);
        }
    }

    @Override
    protected void drawForegroundImpl(int mouseX, int mouseY, float partialTicks) {
        // Show tooltip for the trigger button, but not when any dropdown is open
        if (!isAnyDropdownOpen() && testPoint(mouseX, mouseY)) {
            BlockEntry selected = getSelectedEntry();
            if (selected != null && selected.tooltip != null && !selected.tooltip.isEmpty()) {
                this.drawTooltip(mouseX - position.x, mouseY - position.y, selected.tooltip);
            }
        }
    }

    @Override
    protected boolean mouseClickedImpl(int x, int y, int button) {
        // Clicking the trigger button toggles the dropdown
        if (isOpen()) {
            closeDropdown();
        } else {
            open();
        }
        return true;
    }

    /**
     * Draw the currently open dropdown overlay. Called after rootWidget.draw() in drawScreen. mouseX/mouseY are
     * relative to GUI origin (guiLeft, guiTop). Call setScreenDimensions() before this.
     */
    public static void drawOpenDropdown(int mouseX, int mouseY, float partialTicks) {
        if (openDropdown == null) return;
        WBlockDropdown dd = openDropdown;
        List<BlockEntry> displayEntries = filteredEntries;
        int itemsPerRow = dd.itemsPerRow;
        int totalRows = dd.getTotalRows(displayEntries);
        int visibleRows = dd.getVisibleRows(displayEntries);
        boolean needsScrollbar = dd.getNeedsScrollbar(displayEntries);
        int ddW = dd.getDropdownWidth(displayEntries);
        int ddH = dd.getDropdownHeight(displayEntries);
        int gridHeight = dd.getGridHeight(displayEntries);

        int trigX = dd.guiRelX;
        int trigY = dd.guiRelY;
        int trigW = dd.position.width;
        int trigH = dd.position.height;

        // Compute position with overflow detection
        int[] pos = computeDropdownPosition(dd, displayEntries);
        int ddX = pos[0];
        int ddY = pos[1];

        boolean overTrigger = mouseX >= trigX && mouseX < trigX + trigW && mouseY >= trigY && mouseY < trigY + trigH;
        // Use the rendered 9-patch box bounds (with padding) for hover detection
        boolean overDropdown = mouseX >= ddX - BG_PAD_LEFT && mouseX < ddX + ddW + BG_PAD_RIGHT
                && mouseY >= ddY - BG_PAD_TOP
                && mouseY < ddY + ddH + BG_PAD_BOTTOM;

        if (!overTrigger && !overDropdown && !scrollbarDragging) {
            closeDropdown();
            return;
        }

        // Draw dropdown background
        GL11.glPushMatrix();
        GL11.glTranslatef(ddX, ddY, 300.0f);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // Background
        Icons.bindTexture();
        Icons.GUI_BG.draw9Patch(
                -BG_PAD_LEFT,
                -BG_PAD_TOP,
                ddW + BG_PAD_LEFT + BG_PAD_RIGHT,
                ddH + BG_PAD_TOP + BG_PAD_BOTTOM);

        // Draw search bar using GuiTextField
        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        if (searchField != null) {
            searchField.width = ddW;
            searchField.height = SEARCH_BAR_HEIGHT - 2;
            searchField.drawTextBox();
        }
        GL11.glColor4f(1, 1, 1, 1);

        // Draw grid items
        if (renderItem == null) {
            renderItem = new RenderItem();
        }

        int startRow = dd.scrollOffset;
        int endRow = Math.min(startRow + visibleRows, totalRows);
        int gridWidth = itemsPerRow * ITEM_SIZE;
        int gridOffsetY = SEARCH_BAR_HEIGHT;

        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < itemsPerRow; col++) {
                int idx = row * itemsPerRow + col;
                if (idx >= displayEntries.size()) break;

                BlockEntry entry = displayEntries.get(idx);
                int cellX = col * ITEM_SIZE;
                int cellY = gridOffsetY + (row - startRow) * ITEM_SIZE;

                // Draw cell background
                Icons.bindTexture();
                boolean hovered = (mouseX - ddX >= cellX && mouseX - ddX < cellX + ITEM_SIZE
                        && mouseY - ddY >= cellY
                        && mouseY - ddY < cellY + ITEM_SIZE);
                // Match selection by content (works with filtered entries)
                boolean selected = false;
                if (!dd.addMode && dd.selectedIndex >= 0 && dd.selectedIndex < dd.entries.size()) {
                    BlockEntry selEntry = dd.entries.get(dd.selectedIndex);
                    selected = entry.blockName().equals(selEntry.blockName()) && entry.meta() == selEntry.meta();
                }

                if (selected) {
                    Icons.BUTTON_PRESSED_NORMAL.draw9Patch(cellX, cellY, ITEM_SIZE, ITEM_SIZE);
                } else if (hovered) {
                    Icons.BUTTON_HIGHLIGHT.draw9Patch(cellX, cellY, ITEM_SIZE, ITEM_SIZE);
                } else {
                    Icons.BUTTON_NORMAL.draw9Patch(cellX, cellY, ITEM_SIZE, ITEM_SIZE);
                }

                // Draw block item or "-" for air
                if (entry.displayStack() != null && entry.displayStack().getItem() != null) {
                    Icons.drawItem(cellX + 1, cellY + 1, entry.displayStack(), "", renderItem);
                } else {
                    int textW = fr.getStringWidth("-");
                    fr.drawString(
                            "-",
                            cellX + ITEM_SIZE / 2 - textW / 2,
                            cellY + ITEM_SIZE / 2 - fr.FONT_HEIGHT / 2,
                            0xFFFFFF,
                            true);
                    GL11.glColor4f(1, 1, 1, 1);
                }
            }
        }

        // Draw scrollbar if needed
        if (needsScrollbar) {
            int sbX = gridWidth;
            // Track
            Gui.drawRect(sbX, gridOffsetY, sbX + SCROLLBAR_WIDTH, gridOffsetY + gridHeight, 0xFF222222);
            // Thumb
            int maxScroll = totalRows - visibleRows;
            float thumbRatio = (float) visibleRows / totalRows;
            int thumbHeight = Math.max(8, (int) (gridHeight * thumbRatio));
            int thumbY = gridOffsetY
                    + (maxScroll > 0 ? (int) ((float) dd.scrollOffset / maxScroll * (gridHeight - thumbHeight)) : 0);
            Gui.drawRect(sbX + 1, thumbY, sbX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF888888);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();

        // Tooltip for hovered grid item
        int localMX = mouseX - ddX;
        int localMY = mouseY - ddY - gridOffsetY;
        if (localMX >= 0 && localMX < gridWidth && localMY >= 0 && localMY < gridHeight) {
            int col = localMX / ITEM_SIZE;
            int rowOffset = localMY / ITEM_SIZE;
            int row = dd.scrollOffset + rowOffset;
            int idx = row * itemsPerRow + col;
            if (idx >= 0 && idx < displayEntries.size() && col < itemsPerRow) {
                BlockEntry entry = displayEntries.get(idx);
                if (entry.tooltip() != null && !entry.tooltip().isEmpty()) {
                    Widget.drawGradientRect(0, 0, 0, 0, 0, 0, 0); // reset state
                    dd.drawTooltip(mouseX, mouseY, entry.tooltip());
                }
            }
        }
    }

    /**
     * Handle mouse click on the open dropdown overlay. Returns true if the click was consumed. mouseX/mouseY are
     * relative to GUI origin (guiLeft, guiTop).
     */
    public static boolean handleOpenDropdownClick(int mouseX, int mouseY, int button) {
        if (openDropdown == null) return false;
        WBlockDropdown dd = openDropdown;
        List<BlockEntry> displayEntries = filteredEntries;
        int itemsPerRow = dd.itemsPerRow;
        int gridWidth = itemsPerRow * ITEM_SIZE;
        int gridHeight = dd.getGridHeight(displayEntries);
        int ddW = dd.getDropdownWidth(displayEntries);
        int ddH = dd.getDropdownHeight(displayEntries);

        int[] pos = computeDropdownPosition(dd, displayEntries);
        int ddX = pos[0];
        int ddY = pos[1];

        int localX = mouseX - ddX;
        int localY = mouseY - ddY;

        // Check if click is in dropdown area (including 9-patch padding)
        if (localX >= -BG_PAD_LEFT && localX < ddW + BG_PAD_RIGHT
                && localY >= -BG_PAD_TOP
                && localY < ddH + BG_PAD_BOTTOM) {
            // Click in search bar area
            if (localY >= 0 && localY < SEARCH_BAR_HEIGHT) {
                if (searchField != null) {
                    if (button == 1) {
                        searchField.setText("");
                        searchField.setFocused(true);
                        searchField.setCursorPositionZero();
                    } else {
                        searchField.mouseClicked(localX, localY, button);
                    }
                    updateFilteredEntries();
                }
                return true;
            }

            // Click on scrollbar - start dragging
            if (dd.getNeedsScrollbar(displayEntries) && localX >= gridWidth
                    && localX < gridWidth + SCROLLBAR_WIDTH
                    && localY >= SEARCH_BAR_HEIGHT
                    && localY < SEARCH_BAR_HEIGHT + gridHeight) {
                scrollbarDragging = true;
                updateScrollFromDrag(dd, displayEntries, localY);
                return true;
            }

            // Click in grid area
            int gridLocalY = localY - SEARCH_BAR_HEIGHT;
            if (localX >= 0 && localX < gridWidth && gridLocalY >= 0 && gridLocalY < gridHeight) {
                int col = localX / ITEM_SIZE;
                int rowOffset = gridLocalY / ITEM_SIZE;
                int row = dd.scrollOffset + rowOffset;
                int idx = row * itemsPerRow + col;

                if (idx >= 0 && idx < displayEntries.size() && col < itemsPerRow) {
                    BlockEntry entry = displayEntries.get(idx);
                    if (!dd.addMode) {
                        // Find original index in full entries list
                        int origIdx = dd.entries.indexOf(entry);
                        dd.selectedIndex = origIdx >= 0 ? origIdx : 0;
                    }
                    if (dd.onSelect != null) {
                        dd.onSelect.accept(entry);
                    }
                    dd.clickSound();
                    // In select mode, close after selection. In add mode, keep open.
                    if (!dd.addMode) {
                        closeDropdown();
                    }
                    return true;
                }
            }
            return true; // Consumed in dropdown area (padding/scrollbar etc.)
        }

        // Click outside dropdown and trigger - close
        int trigX = dd.guiRelX;
        int trigY = dd.guiRelY;
        boolean overTrigger = mouseX >= trigX && mouseX < trigX + dd.position.width
                && mouseY >= trigY
                && mouseY < trigY + dd.position.height;
        if (!overTrigger) {
            closeDropdown();
        }
        return false;
    }

    /**
     * Handle mouse wheel scroll on the open dropdown. mouseX/mouseY are relative to GUI origin. delta is positive for
     * scroll up, negative for scroll down. Returns true if consumed.
     */
    public static boolean handleOpenDropdownScroll(int mouseX, int mouseY, int delta) {
        if (openDropdown == null) return false;
        WBlockDropdown dd = openDropdown;
        List<BlockEntry> displayEntries = filteredEntries;

        if (!dd.getNeedsScrollbar(displayEntries)) return false;

        int totalRows = dd.getTotalRows(displayEntries);
        int visibleRows = dd.getVisibleRows(displayEntries);
        int ddW = dd.getDropdownWidth(displayEntries);
        int ddH = dd.getDropdownHeight(displayEntries);

        int trigX = dd.guiRelX;
        int trigY = dd.guiRelY;
        int trigW = dd.position.width;
        int trigH = dd.position.height;

        int[] pos = computeDropdownPosition(dd, displayEntries);
        int ddX = pos[0];
        int ddY = pos[1];

        // Check if mouse is over the dropdown or trigger area
        boolean overTrigger = mouseX >= trigX && mouseX < trigX + trigW && mouseY >= trigY && mouseY < trigY + trigH;
        boolean overDropdown = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= ddY && mouseY < ddY + ddH;

        if (overTrigger || overDropdown) {
            int maxScroll = totalRows - visibleRows;
            if (delta > 0) {
                dd.scrollOffset = Math.max(0, dd.scrollOffset - 1);
            } else if (delta < 0) {
                dd.scrollOffset = Math.min(maxScroll, dd.scrollOffset + 1);
            }
            return true;
        }

        return false;
    }

    /**
     * Handle mouse drag on the open dropdown (for scrollbar dragging). mouseX/mouseY are relative to GUI origin.
     * Returns true if consumed.
     */
    public static boolean handleOpenDropdownDrag(int mouseX, int mouseY) {
        if (openDropdown == null || !scrollbarDragging) return false;
        WBlockDropdown dd = openDropdown;
        List<BlockEntry> displayEntries = filteredEntries;

        int[] pos = computeDropdownPosition(dd, displayEntries);
        int localY = mouseY - pos[1];
        updateScrollFromDrag(dd, displayEntries, localY);
        return true;
    }

    /**
     * Handle mouse button release on the open dropdown. Returns true if scrollbar was being dragged.
     */
    public static boolean handleOpenDropdownMouseUp() {
        if (scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        return false;
    }

    /** Update scroll offset from a Y coordinate relative to dropdown origin. */
    private static void updateScrollFromDrag(WBlockDropdown dd, List<BlockEntry> displayEntries, int localY) {
        int totalRows = dd.getTotalRows(displayEntries);
        int visibleRows = dd.getVisibleRows(displayEntries);
        int gridHeight = dd.getGridHeight(displayEntries);
        int maxScroll = totalRows - visibleRows;
        if (maxScroll <= 0) return;

        float thumbRatio = (float) visibleRows / totalRows;
        int thumbHeight = Math.max(8, (int) (gridHeight * thumbRatio));
        int scrollableRange = gridHeight - thumbHeight;
        if (scrollableRange <= 0) return;

        int relY = localY - SEARCH_BAR_HEIGHT - thumbHeight / 2;
        float ratio = (float) relY / scrollableRange;
        ratio = Math.max(0, Math.min(1, ratio));
        dd.scrollOffset = Math.round(ratio * maxScroll);
    }

    /**
     * Handle keyboard input for the open dropdown's search box. Returns true if consumed.
     */
    public static boolean handleOpenDropdownKeyTyped(char character, int key) {
        if (openDropdown == null || searchField == null) return false;

        String oldText = searchField.getText();
        boolean consumed = searchField.textboxKeyTyped(character, key);
        if (!oldText.equals(searchField.getText())) {
            updateFilteredEntries();
        }
        return consumed;
    }

    /**
     * Build entries from AllowedBlock rules. When includeNoneOption is true, the first entry is a "none" option (empty
     * blockName), used for boundary/gap/center. When false, no "none" option is added, used for layers.
     */
    public static List<BlockEntry> buildEntriesFromRules(List<AllowedBlock> rules, boolean includeNoneOption) {
        List<BlockEntry> entries = new ArrayList<>();
        if (includeNoneOption) {
            entries.add(new BlockEntry("", 0, null, "-"));
        }

        for (AllowedBlock rule : rules) {
            String blockName = rule.blockName();
            Block block = DimensionConfig.blockFromString(blockName);
            if (block == null) continue;

            int minMeta = rule.getMinAllowedMeta();
            int maxMeta = rule.getMaxAllowedMeta();
            for (int meta = minMeta; meta <= maxMeta; meta++) {
                if (!rule.isMetaAllowed(meta)) continue;
                ItemStack is = null;
                String tooltip = blockName + ":" + meta;
                try {
                    Item item = Item.getItemFromBlock(block);
                    if (item != null) {
                        is = new ItemStack(item, 1, meta);
                        String displayName = is.getDisplayName();
                        if (displayName != null && !displayName.isEmpty()) {
                            tooltip = displayName + " (" + blockName + ":" + meta + ")";
                        }
                    }
                } catch (Throwable ignored) {}
                entries.add(new BlockEntry(blockName, meta, is, tooltip));
            }
        }
        return entries;
    }

    /**
     * Find the entry index matching a given blockName and meta. Returns 0 (air) if not found.
     */
    public static int findEntryIndex(List<BlockEntry> entries, String blockName, int meta) {
        if (blockName == null || blockName.isEmpty()) return 0;
        for (int i = 0; i < entries.size(); i++) {
            BlockEntry e = entries.get(i);
            if (blockName.equals(e.blockName) && e.meta == meta) {
                return i;
            }
        }
        // Try matching just blockName with closest meta
        for (int i = 0; i < entries.size(); i++) {
            BlockEntry e = entries.get(i);
            if (blockName.equals(e.blockName)) {
                return i;
            }
        }
        return 0;
    }
}
