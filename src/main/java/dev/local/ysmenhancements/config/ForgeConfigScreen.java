package dev.local.ysmenhancements.config;

import java.util.EnumMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge 47 has no NeoForge ConfigurationScreen. Uses vanilla widgets in its Mods config entry. */
public final class ForgeConfigScreen extends Screen {
    private final Screen parent;
    private final EnumMap<Feature, Boolean> pending = new EnumMap<>(Feature.class);
    private int page;

    private ForgeConfigScreen(Screen parent) {
        super(Component.m_237115_("ysm_enhancements.configuration.title"));
        this.parent = parent;
        for (Feature feature : Feature.values()) pending.put(feature, feature.enabled());
    }
    public static Object create(Object parent) { return new ForgeConfigScreen((Screen) parent); }
    private Component label(Feature feature) {
        return Component.m_237110_("ysm_enhancements.configuration.toggle",
                Component.m_237115_(feature.translationKey()),
                Component.m_237115_(pending.get(feature) ? "options.on" : "options.off"));
    }
    @Override protected void m_7856_() {
        m_169413_();
        int width = Math.min(380, f_96543_ - 32), x = (f_96543_ - width) / 2;
        int rows = Math.max(1, Math.min(Feature.values().length, (f_96544_ - 102) / 24));
        int pages = (Feature.values().length + rows - 1) / rows;
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = page * rows; i < Math.min(Feature.values().length, (page + 1) * rows); i++) {
            Feature feature = Feature.values()[i];
            Button button = Button.m_253074_(label(feature), b -> {
                pending.put(feature, !pending.get(feature)); b.m_93666_(label(feature));
            }).m_252987_(x, 36 + (i - page * rows) * 24, width, 20)
              .m_257505_(Tooltip.m_257550_(Component.m_237115_(feature.translationKey() + ".tooltip"))).m_253136_();
            m_142416_(button);
        }
        if (pages > 1) {
            m_142416_(Button.m_253074_(Component.m_237113_("<"), b -> { page--; m_7856_(); })
                    .m_252987_(x, f_96544_ - 60, 40, 20).m_253136_()).f_93623_ = page > 0;
            m_142416_(Button.m_253074_(Component.m_237113_(">"), b -> { page++; m_7856_(); })
                    .m_252987_(x + width - 40, f_96544_ - 60, 40, 20).m_253136_()).f_93623_ = page + 1 < pages;
        }
        int gap = 6, bw = (width - gap * 2) / 3;
        m_142416_(Button.m_253074_(Component.m_237115_("controls.reset"), b -> {
            for (Feature feature : Feature.values()) pending.put(feature, true); m_7856_();
        }).m_252987_(x, f_96544_ - 30, bw, 20).m_253136_());
        m_142416_(Button.m_253074_(Component.m_237115_("gui.cancel"), b -> m_7379_())
                .m_252987_(x + bw + gap, f_96544_ - 30, bw, 20).m_253136_());
        m_142416_(Button.m_253074_(Component.m_237115_("gui.done"), b -> {
            pending.forEach(NativeConfig::set); NativeConfig.save(); m_7379_();
        }).m_252987_(x + (bw + gap) * 2, f_96544_ - 30, bw, 20).m_253136_());
    }
    @Override public void m_88315_(GuiGraphics graphics, int x, int y, float partialTick) {
        m_280273_(graphics);
        graphics.m_280653_(f_96547_, f_96539_, f_96543_ / 2, 15, 0xFFFFFF);
        super.m_88315_(graphics, x, y, partialTick);
    }
    @Override public void m_7379_() { f_96541_.m_91152_(parent); }
}
