package me.eigenraven.personalspace.gui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;

import me.eigenraven.personalspace.config.AllowedBlock;
import me.eigenraven.personalspace.world.DimensionConfig;

/**
 * A block selector widget with hover-to-open dropdown panel. Displays a single button showing the selected block (or
 * "-" for air). Hovering opens a scrollable grid dropdown below, showing all available blocks with their meta variants
 * pre-rendered. Two modes: - SELECT mode (for boundary/gap/center): clicking a block changes the selection - ADD mode
 * (for layers): clicking a block fires the callback without changing the button display
 */
public class WBlockDropdown extends Widget {

    /** Maximum number of items per row in the dropdown grid */
    public static final int ITEMS_PER_ROW = 9;
    /** Size of each item cell in the dropdown grid */
    public static final int ITEM_SIZE = 20;
    /** Maximum number of visible rows before scrolling is needed */
    public static final int MAX_VISIBLE_ROWS = 4;
    /** Width of the scrollbar */
    public static final int SCROLLBAR_WIDTH = 6;

    /** The currently open dropdown instance (only one can be open at a time) */
    private static WBlockDropdown openDropdown = null;

    /**
     * @param blockName    empty string = air
     * @param displayStack null for air
     */
    @Desugar
    public record BlockEntry(String blockName, int meta, ItemStack displayStack, String tooltip) {

    }

    private final List<BlockEntry> entries;
    private int scrollOffset = 0; // in rows
    private final int totalRows;
    private int selectedIndex = 0; // 0 = air entry
    private final boolean addMode; // true for layers (click adds), false for select mode
    private final Consumer<BlockEntry> onSelect;

    // Absolute position relative to GUI origin (guiLeft, guiTop)
    private int guiRelX, guiRelY;

    // Dropdown dimensions
    private final int dropdownWidth;
    private final int dropdownHeight;
    private final int visibleRows;
    private final boolean needsScrollbar;

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

        this.totalRows = (int) Math.ceil((double) entries.size() / ITEMS_PER_ROW);
        this.visibleRows = Math.min(totalRows, MAX_VISIBLE_ROWS);
        this.needsScrollbar = totalRows > MAX_VISIBLE_ROWS;
        this.dropdownWidth = ITEMS_PER_ROW * ITEM_SIZE + (needsScrollbar ? SCROLLBAR_WIDTH : 0);
        this.dropdownHeight = visibleRows * ITEM_SIZE;
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
    }

    public static void closeDropdown() {
        if (openDropdown != null) {
            openDropdown.scrollOffset = 0;
            openDropdown = null;
        }
    }

    public static boolean isAnyDropdownOpen() {
        return openDropdown != null;
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
     * relative to GUI origin (guiLeft, guiTop).
     */
    public static void drawOpenDropdown(int mouseX, int mouseY, float partialTicks) {
        if (openDropdown == null) return;
        WBlockDropdown dd = openDropdown;

        // Check if mouse is still over trigger or dropdown area
        int trigX = dd.guiRelX;
        int trigY = dd.guiRelY;
        int trigW = dd.position.width;
        int trigH = dd.position.height;
        int ddX = trigX;
        int ddY = trigY + trigH;
        int ddW = dd.dropdownWidth;
        int ddH = dd.dropdownHeight;

        boolean overTrigger = mouseX >= trigX && mouseX < trigX + trigW && mouseY >= trigY && mouseY < trigY + trigH;
        boolean overDropdown = mouseX >= ddX && mouseX < ddX + ddW && mouseY >= ddY && mouseY < ddY + ddH;

        if (!overTrigger && !overDropdown) {
            closeDropdown();
            return;
        }

        // Draw dropdown background
        GL11.glPushMatrix();
        GL11.glTranslatef(ddX, ddY, 300.0f);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        // Background
        Icons.bindTexture();
        Icons.GUI_BG.draw9Patch(-6, -4, ddW + 12, ddH + 8);

        // Draw grid items
        if (renderItem == null) {
            renderItem = new RenderItem();
        }

        int startRow = dd.scrollOffset;
        int endRow = Math.min(startRow + dd.visibleRows, dd.totalRows);
        int gridWidth = ITEMS_PER_ROW * ITEM_SIZE;

        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < ITEMS_PER_ROW; col++) {
                int idx = row * ITEMS_PER_ROW + col;
                if (idx >= dd.entries.size()) break;

                BlockEntry entry = dd.entries.get(idx);
                int cellX = col * ITEM_SIZE;
                int cellY = (row - startRow) * ITEM_SIZE;

                // Draw cell background
                Icons.bindTexture();
                boolean hovered = (mouseX - ddX >= cellX && mouseX - ddX < cellX + ITEM_SIZE
                        && mouseY - ddY >= cellY
                        && mouseY - ddY < cellY + ITEM_SIZE);
                boolean selected = (idx == dd.selectedIndex && !dd.addMode);

                if (selected) {
                    Icons.BUTTON_PRESSED_NORMAL.draw9Patch(cellX, cellY, ITEM_SIZE, ITEM_SIZE);
                } else if (hovered) {
                    Icons.BUTTON_HIGHLIGHT.draw9Patch(cellX, cellY, ITEM_SIZE, ITEM_SIZE);
                } else {
                    Icons.BUTTON_NORMAL.draw9Patch(cellX, cellY, ITEM_SIZE, ITEM_SIZE);
                }

                // Draw block item or "-" for air
                if (entry.displayStack != null && entry.displayStack.getItem() != null) {
                    Icons.drawItem(cellX + 1, cellY + 1, entry.displayStack, "", renderItem);
                } else {
                    FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
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
        if (dd.needsScrollbar) {
            int sbX = gridWidth;
            int sbH = ddH;
            // Track
            Gui.drawRect(sbX, 0, sbX + SCROLLBAR_WIDTH, sbH, 0xFF222222);
            // Thumb
            int maxScroll = dd.totalRows - dd.visibleRows;
            float thumbRatio = (float) dd.visibleRows / dd.totalRows;
            int thumbHeight = Math.max(8, (int) (sbH * thumbRatio));
            int thumbY = maxScroll > 0 ? (int) ((float) dd.scrollOffset / maxScroll * (sbH - thumbHeight)) : 0;
            Gui.drawRect(sbX + 1, thumbY, sbX + SCROLLBAR_WIDTH - 1, thumbY + thumbHeight, 0xFF888888);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
        int localMX = mouseX - ddX;
        int localMY = mouseY - ddY;
        if (localMX >= 0 && localMX < gridWidth && localMY >= 0 && localMY < ddH) {
            int col = localMX / ITEM_SIZE;
            int rowOffset = localMY / ITEM_SIZE;
            int row = dd.scrollOffset + rowOffset;
            int idx = row * ITEMS_PER_ROW + col;
            if (idx >= 0 && idx < dd.entries.size() && col < ITEMS_PER_ROW) {
                BlockEntry entry = dd.entries.get(idx);
                if (entry.tooltip != null && !entry.tooltip.isEmpty()) {
                    Widget.drawGradientRect(0, 0, 0, 0, 0, 0, 0); // reset state
                    dd.drawTooltip(mouseX, mouseY, entry.tooltip);
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

        int ddX = dd.guiRelX;
        int ddY = dd.guiRelY + dd.position.height;
        int ddW = dd.dropdownWidth;
        int ddH = dd.dropdownHeight;
        int gridWidth = ITEMS_PER_ROW * ITEM_SIZE;

        int localX = mouseX - ddX;
        int localY = mouseY - ddY;

        // Check if click is in dropdown area
        if (localX >= 0 && localX < gridWidth && localY >= 0 && localY < ddH) {
            int col = localX / ITEM_SIZE;
            int rowOffset = localY / ITEM_SIZE;
            int row = dd.scrollOffset + rowOffset;
            int idx = row * ITEMS_PER_ROW + col;

            if (idx >= 0 && idx < dd.entries.size() && col < ITEMS_PER_ROW) {
                BlockEntry entry = dd.entries.get(idx);
                if (!dd.addMode) {
                    dd.selectedIndex = idx;
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

        // Click was in dropdown area but not on an item (e.g. scrollbar area) - consume it
        if (localX >= 0 && localX < ddW && localY >= 0 && localY < ddH) {
            return true;
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

        if (!dd.needsScrollbar) return false;

        int ddX = dd.guiRelX;
        int ddY = dd.guiRelY;
        int trigW = dd.position.width;
        int trigH = dd.position.height;
        int ddW = dd.dropdownWidth;
        int ddH = dd.dropdownHeight;

        // Check if mouse is over the dropdown or trigger area
        boolean overTrigger = mouseX >= ddX && mouseX < ddX + trigW && mouseY >= ddY && mouseY < ddY + trigH;
        boolean overDropdown = mouseX >= ddX && mouseX < ddX + ddW
                && mouseY >= ddY + trigH
                && mouseY < ddY + trigH + ddH;

        if (overTrigger || overDropdown) {
            int maxScroll = dd.totalRows - dd.visibleRows;
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
