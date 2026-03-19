package com.happysg.radar.compat;

import net.createmod.catnip.lang.Lang;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.function.Supplier;

public enum Mods {
    CREATE_MECHAMAYHEM,
    VALKYRIENSKIES,
    VS_CLOCKWORK,
    COMPUTERCRAFT,
    TRACKWORK,
    CBCMODERNWARFARE,
    CBC_AT,
    CREATEBIGCANNONS,
    CREATEENERGYCANNONS,
    SHUPAPIUM,
    KABOOM;

    private final String id;

    Mods() {
        id = Lang.asId(name());
    }

    public String id() {
        return id;
    }

    public Identifier rl(String path) {
        return new Identifier(id, path);
    }

    public Block getBlock(String blockId) {
        return Registries.BLOCK.get(rl(blockId));
    }

    public boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
        if (isLoaded())
            return Optional.of(toRun.get().get());
        return Optional.empty();
    }

    public void executeIfInstalled(Supplier<Runnable> toExecute) {
        if (isLoaded()) {
            toExecute.get().run();
        }
    }
}
