package com.happysg.radar.item.targetfilter;

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
 * Targeting Filter screen — lets the player configure auto-targeting behaviour.
 * Config stored as item NBT under Filters.targeting.
 * When placed in NetworkFilterer slot 2, the targeting tag is written to the
 * network group so weapon controllers can read it.
 */
@Environment(EnvType.CLIENT)
public class AutoTargetScreen extends Screen {

    private static final int WINDOW_W = 220;
    private static final int WINDOW_H = 160;
    private static final int BTN_W = 200;
    private static final int BTN_H = 20;
    private static final int BTN_GAP = 6;

    private final Hand hand;
    private final ItemStack stack;

    private boolean autoTarget;
    private boolean autoFire;
    private boolean lineOfSight;
    private boolean artilleryMode;

    public AutoTargetScreen(Hand hand, ItemStack stack) {
        super(Text.translatable("create_radar.target_filter.title"));
        this.hand = hand;
        this.stack = stack;
        loadFromNbt();
    }

    // -------------------------------------------------------------------------
    // NBT I/O
    // -------------------------------------------------------------------------

    private void loadFromNbt() {
        NbtCompound tag = extractTargetingTag(stack.getOrCreateNbt());
        autoTarget   = tag.contains("autoTarget")   ? tag.getBoolean("autoTarget")   : false;
        autoFire     = tag.contains("autoFire")     ? tag.getBoolean("autoFire")     : false;
        lineOfSight  = tag.contains("lineOfSight")  ? tag.getBoolean("lineOfSight")  : true;
        artilleryMode = tag.contains("artilleryMode") ? tag.getBoolean("artilleryMode") : false;
    }

    static NbtCompound extractTargetingTag(NbtCompound nbt) {
        if (nbt.contains("Filters")) {
            NbtCompound filters = nbt.getCompound("Filters");
            if (filters.contains("targeting")) return filters.getCompound("targeting");
        }
        return new NbtCompound();
    }

    private NbtCompound buildNbt() {
        NbtCompound targeting = new NbtCompound();
        targeting.putBoolean("autoTarget",    autoTarget);
        targeting.putBoolean("autoFire",      autoFire);
        targeting.putBoolean("lineOfSight",   lineOfSight);
        targeting.putBoolean("artilleryMode", artilleryMode);

        NbtCompound nbt = stack.getOrCreateNbt().copy();
        NbtCompound filters = nbt.contains("Filters") ? nbt.getCompound("Filters").copy() : new NbtCompound();
        filters.put("targeting", targeting);
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
        int col = wx + 10;
        int row0 = wy + 32;
        int step = BTN_H + BTN_GAP;

        addToggle(col, row0,        "create_radar.radar_button.auto_target",   () -> autoTarget,    v -> autoTarget = v);
        addToggle(col, row0 + step, "create_radar.radar_button.auto_fire",     () -> autoFire,      v -> autoFire = v);
        addToggle(col, row0 + step*2, "create_radar.radar_button.lineofsight", () -> lineOfSight,   v -> lineOfSight = v);
        addToggle(col, row0 + step*3, "create_radar.radar_button.artillery_mode", () -> artilleryMode, v -> artilleryMode = v);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions((width - 80) / 2, wy + WINDOW_H - 26, 80, 20).build());
    }

    // -------------------------------------------------------------------------
    // Toggle helpers
    // -------------------------------------------------------------------------

    private interface BoolGetter { boolean get(); }
    private interface BoolSetter { void set(boolean v); }

    private void addToggle(int x, int y, String labelKey, BoolGetter get, BoolSetter set) {
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
