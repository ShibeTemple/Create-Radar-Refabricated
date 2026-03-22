package com.happysg.radar.item.identfilter;

import com.happysg.radar.network.FilterNbtPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Identification Filter screen — lets the player mark specific players and VS2 ships
 * as Friendly or Enemy. Config stored as item NBT (playerList, vs2Ships).
 * When placed in NetworkFilterer slot 1, these lists are merged into the detection
 * tag so that DetectionConfig.getColor() can colour tracks correctly.
 */
@Environment(EnvType.CLIENT)
public class IdentificationFilterScreen extends Screen {

    private static final int WINDOW_W = 240;
    private static final int WINDOW_H = 220;
    private static final int MAX_VISIBLE = 5;
    private static final int ROW_H = 18;

    private final Hand hand;
    private final ItemStack stack;

    /** true = friendly, false = enemy */
    private final Map<String, Boolean> playerMap = new LinkedHashMap<>();
    private final Map<String, Boolean> shipMap   = new LinkedHashMap<>();

    private boolean showPlayers = true;   // false = show ships
    private int scrollOffset = 0;

    private TextFieldWidget inputField;
    private ButtonWidget addFriendBtn;
    private ButtonWidget addEnemyBtn;
    private ButtonWidget scrollUpBtn;
    private ButtonWidget scrollDownBtn;

    // Remove-button hitboxes, rebuilt each frame
    private final List<int[]> removeBoxes = new ArrayList<>();

    public IdentificationFilterScreen(Hand hand, ItemStack stack) {
        super(Text.translatable("create_radar.identification_filter.title"));
        this.hand = hand;
        this.stack = stack;
        loadFromNbt();
    }

    // -------------------------------------------------------------------------
    // NBT I/O
    // -------------------------------------------------------------------------

    private void loadFromNbt() {
        NbtCompound nbt = stack.getOrCreateNbt();
        loadMap(nbt, "playerList", playerMap);
        loadMap(nbt, "vs2Ships",   shipMap);
    }

    private void loadMap(NbtCompound nbt, String key, Map<String, Boolean> map) {
        map.clear();
        if (!nbt.contains(key)) return;
        NbtCompound sub = nbt.getCompound(key);
        for (String entry : sub.getKeys()) {
            map.put(entry, sub.getBoolean(entry));
        }
    }

    private NbtCompound buildNbt() {
        NbtCompound nbt = stack.getOrCreateNbt().copy();
        nbt.put("playerList", mapToNbt(playerMap));
        nbt.put("vs2Ships",   mapToNbt(shipMap));
        return nbt;
    }

    private NbtCompound mapToNbt(Map<String, Boolean> map) {
        NbtCompound tag = new NbtCompound();
        map.forEach(tag::putBoolean);
        return tag;
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        scrollOffset = 0;
        int wx = (width - WINDOW_W) / 2;
        int wy = (height - WINDOW_H) / 2;

        // Tab buttons
        addDrawableChild(ButtonWidget.builder(tabText("create_radar.player_list.title", showPlayers), b -> {
            showPlayers = true;
            scrollOffset = 0;
            clearAndReinit();
        }).dimensions(wx + 10, wy + 24, 106, 18).build());

        addDrawableChild(ButtonWidget.builder(tabText("create_radar.ship_list.title", !showPlayers), b -> {
            showPlayers = false;
            scrollOffset = 0;
            clearAndReinit();
        }).dimensions(wx + 124, wy + 24, 106, 18).build());

        // Input field
        int inputY = wy + 50;
        inputField = new TextFieldWidget(textRenderer, wx + 10, inputY, 130, 18,
                Text.translatable(showPlayers ? "create_radar.enter_username" : "create_radar.ship_list.enter_code"));
        inputField.setMaxLength(48);
        inputField.setPlaceholder(Text.translatable(
                showPlayers ? "create_radar.enter_username" : "create_radar.ship_list.enter_code")
                .formatted(Formatting.DARK_GRAY));
        addDrawableChild(inputField);

        int addX = wx + 148;
        addFriendBtn = ButtonWidget.builder(
                Text.translatable("create_radar.filter_isfriend").formatted(Formatting.GREEN), b -> addEntry(true))
                .dimensions(addX, inputY, 40, 18).build();
        addDrawableChild(addFriendBtn);

        addEnemyBtn = ButtonWidget.builder(
                Text.translatable("create_radar.filter_isfoe").formatted(Formatting.RED), b -> addEntry(false))
                .dimensions(addX + 42, inputY, 40, 18).build();
        addDrawableChild(addEnemyBtn);

        // Scroll buttons
        int scrollX = wx + WINDOW_W - 22;
        int listTop = wy + 76;
        scrollUpBtn = ButtonWidget.builder(Text.literal("▲"), b -> {
            if (scrollOffset > 0) scrollOffset--;
        }).dimensions(scrollX, listTop, 16, 16).build();
        addDrawableChild(scrollUpBtn);

        scrollDownBtn = ButtonWidget.builder(Text.literal("▼"), b -> {
            if (scrollOffset < currentMap().size() - MAX_VISIBLE) scrollOffset++;
        }).dimensions(scrollX, listTop + MAX_VISIBLE * ROW_H - 16, 16, 16).build();
        addDrawableChild(scrollDownBtn);

        // Done
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions((width - 80) / 2, wy + WINDOW_H - 26, 80, 20).build());
    }

    private Text tabText(String key, boolean active) {
        Text t = Text.translatable(key);
        return active ? t.copy().formatted(Formatting.YELLOW, Formatting.UNDERLINE) : t.copy().formatted(Formatting.GRAY);
    }

    private Map<String, Boolean> currentMap() {
        return showPlayers ? playerMap : shipMap;
    }

    private void addEntry(boolean friendly) {
        String name = inputField.getText().trim();
        if (name.isEmpty()) return;
        currentMap().put(name, friendly);
        inputField.setText("");
        scrollOffset = Math.max(0, currentMap().size() - MAX_VISIBLE);
    }

    /** Clear widgets and re-run init to refresh button state. */
    private void clearAndReinit() {
        clearChildren();
        init();
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int wx = (width - WINDOW_W) / 2;
        int wy = (height - WINDOW_H) / 2;

        // Background panel
        context.fill(wx, wy, wx + WINDOW_W, wy + WINDOW_H, 0xC0101010);
        context.drawBorder(wx, wy, WINDOW_W, WINDOW_H, 0xFF555555);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, wy + 8, 0xFFFFFF);

        // Render list entries
        Map<String, Boolean> map = currentMap();
        List<String> keys = new ArrayList<>(map.keySet());
        removeBoxes.clear();

        int listTop = wy + 76;
        int visibleEnd = Math.min(scrollOffset + MAX_VISIBLE, keys.size());
        for (int i = scrollOffset; i < visibleEnd; i++) {
            int row = i - scrollOffset;
            int ry = listTop + row * ROW_H;
            String name = keys.get(i);
            boolean friendly = map.get(name);

            // Row background
            int rowBg = (row % 2 == 0) ? 0x20FFFFFF : 0x10FFFFFF;
            context.fill(wx + 10, ry, wx + WINDOW_W - 26, ry + ROW_H - 2, rowBg);

            // Name + faction
            Formatting color = friendly ? Formatting.GREEN : Formatting.RED;
            Text label = Text.literal(name).formatted(color)
                    .append(Text.literal(" (").formatted(Formatting.GRAY))
                    .append(Text.translatable(friendly ? "create_radar.faction.friendly" : "create_radar.faction.enemy").formatted(color))
                    .append(Text.literal(")").formatted(Formatting.GRAY));
            context.drawTextWithShadow(textRenderer, label, wx + 14, ry + 4, 0xFFFFFF);

            // [x] remove button area
            int rx = wx + WINDOW_W - 42;
            context.fill(rx, ry + 1, rx + 14, ry + ROW_H - 3, 0xFF802020);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("x"), rx + 7, ry + 4, 0xFFFFFF);
            removeBoxes.add(new int[]{rx, ry + 1, rx + 14, ry + ROW_H - 3, i});
        }

        // Empty hint
        if (keys.isEmpty()) {
            String hint = showPlayers ? "No players added" : "No ships added";
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(hint).formatted(Formatting.DARK_GRAY),
                    width / 2, listTop + 20, 0xFFFFFF);
        }

        // Scroll indicators
        scrollUpBtn.active = scrollOffset > 0;
        scrollDownBtn.active = scrollOffset < keys.size() - MAX_VISIBLE;

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int[] box : removeBoxes) {
            if (mouseX >= box[0] && mouseX <= box[2] && mouseY >= box[1] && mouseY <= box[3]) {
                int idx = box[4];
                List<String> keys = new ArrayList<>(currentMap().keySet());
                if (idx < keys.size()) {
                    currentMap().remove(keys.get(idx));
                    scrollOffset = Math.min(scrollOffset, Math.max(0, currentMap().size() - MAX_VISIBLE));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // -------------------------------------------------------------------------
    // Close / save
    // -------------------------------------------------------------------------

    @Override
    public void close() {
        FilterNbtPacket.send(hand, buildNbt());
        super.close();
    }

    @Override
    public boolean shouldPause() { return false; }
}
