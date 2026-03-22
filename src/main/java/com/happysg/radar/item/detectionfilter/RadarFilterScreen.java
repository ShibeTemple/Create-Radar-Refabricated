package com.happysg.radar.item.detectionfilter;

import com.happysg.radar.network.FilterNbtPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

/**
 * Detection Filter screen — lets the player toggle which entity categories
 * are shown on the radar monitor (player, mob, animal, projectile, contraption, vs2, item).
 * Config is stored as item NBT under Filters.detection and applied by
 * NetworkFiltererBlockEntity when the item is placed in slot 0.
 */
@Environment(EnvType.CLIENT)
public class RadarFilterScreen extends Screen {

    private static final int WINDOW_W = 220;
    private static final int WINDOW_H = 164;
    private static final int BTN_W = 98;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 4;

    private final Hand hand;
    private final ItemStack stack;

    private boolean player;
    private boolean mob;
    private boolean animal;
    private boolean projectile;
    private boolean contraption;
    private boolean vs2;
    private boolean item;

    public RadarFilterScreen(Hand hand, ItemStack stack) {
        super(Text.translatable("create_radar.detection_filter.title"));
        this.hand = hand;
        this.stack = stack;
        loadFromNbt();
    }

    // -------------------------------------------------------------------------
    // NBT I/O
    // -------------------------------------------------------------------------

    private void loadFromNbt() {
        NbtCompound det = extractDetectionTag(stack.getOrCreateNbt());
        player      = getOrTrue(det, "player");
        mob         = getOrTrue(det, "mob");
        animal      = getOrTrue(det, "animal");
        projectile  = getOrTrue(det, "projectile");
        contraption = getOrTrue(det, "contraption");
        vs2         = getOrTrue(det, "vs2");
        item        = getOrTrue(det, "item");
    }

    private boolean getOrTrue(NbtCompound tag, String key) {
        return !tag.contains(key) || tag.getBoolean(key);
    }

    /** Read detection sub-tag from item NBT (supports both legacy and Filters-nested layout). */
    static NbtCompound extractDetectionTag(NbtCompound nbt) {
        if (nbt.contains("Filters")) {
            NbtCompound filters = nbt.getCompound("Filters");
            if (filters.contains("detection")) return filters.getCompound("detection");
        }
        if (nbt.contains("detection")) return nbt.getCompound("detection");
        return new NbtCompound();
    }

    private NbtCompound buildNbt() {
        NbtCompound det = new NbtCompound();
        det.putBoolean("player",      player);
        det.putBoolean("mob",         mob);
        det.putBoolean("animal",      animal);
        det.putBoolean("projectile",  projectile);
        det.putBoolean("contraption", contraption);
        det.putBoolean("vs2",         vs2);
        det.putBoolean("item",        item);

        NbtCompound nbt = stack.getOrCreateNbt().copy();
        NbtCompound filters = nbt.contains("Filters") ? nbt.getCompound("Filters").copy() : new NbtCompound();
        filters.put("detection", det);
        nbt.put("Filters", filters);
        return nbt;
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        int wx = (width - WINDOW_W) / 2;
        int wy = (height - WINDOW_H) / 2;
        int col1 = wx + 10;
        int col2 = wx + 10 + BTN_W + BTN_GAP;
        int row0 = wy + 32;
        int rowStep = BTN_H + BTN_GAP;

        addToggleButton(col1, row0,             "create_radar.radar_button.player",      () -> player,      v -> player = v);
        addToggleButton(col2, row0,             "create_radar.radar_button.mob",         () -> mob,         v -> mob = v);
        addToggleButton(col1, row0 + rowStep,   "create_radar.radar_button.animal",      () -> animal,      v -> animal = v);
        addToggleButton(col2, row0 + rowStep,   "create_radar.radar_button.projectile",  () -> projectile,  v -> projectile = v);
        addToggleButton(col1, row0 + rowStep*2, "create_radar.radar_button.contraption", () -> contraption, v -> contraption = v);
        addToggleButton(col2, row0 + rowStep*2, "create_radar.radar_button.vs2",         () -> vs2,         v -> vs2 = v);
        addToggleButton(col1, row0 + rowStep*3, "create_radar.radar_button.item",        () -> item,        v -> item = v);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions((width - 80) / 2, wy + WINDOW_H - 26, 80, 20).build());
    }

    // -------------------------------------------------------------------------
    // Toggle button helpers
    // -------------------------------------------------------------------------

    private interface BoolGetter { boolean get(); }
    private interface BoolSetter { void set(boolean v); }

    private void addToggleButton(int x, int y, String labelKey, BoolGetter get, BoolSetter set) {
        ButtonWidget[] ref = new ButtonWidget[1];
        ref[0] = ButtonWidget.builder(toggleText(labelKey, get.get()), b -> {
            boolean newVal = !get.get();
            set.set(newVal);
            b.setMessage(toggleText(labelKey, newVal));
        }).dimensions(x, y, BTN_W, BTN_H).build();
        addDrawableChild(ref[0]);
    }

    private Text toggleText(String labelKey, boolean enabled) {
        Formatting color = enabled ? Formatting.GREEN : Formatting.RED;
        return Text.translatable(labelKey).formatted(color)
                .append(Text.literal(": ").formatted(Formatting.GRAY))
                .append(Text.translatable(enabled ? "options.on" : "options.off").formatted(color));
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int wx = (width - WINDOW_W) / 2;
        int wy = (height - WINDOW_H) / 2;
        context.fill(wx, wy, wx + WINDOW_W, wy + WINDOW_H, 0xC0101010);
        context.drawBorder(wx, wy, WINDOW_W, WINDOW_H, 0xFF555555);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, wy + 10, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
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
