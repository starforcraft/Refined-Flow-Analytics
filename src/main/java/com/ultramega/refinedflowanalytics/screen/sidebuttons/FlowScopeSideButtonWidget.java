package com.ultramega.refinedflowanalytics.screen.sidebuttons;

import com.refinedmods.refinedstorage.common.support.widget.AbstractSideButtonWidget;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

abstract class FlowScopeSideButtonWidget<T extends Enum<T>> extends AbstractSideButtonWidget {
    private final String translationKey;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final Function<T, ResourceLocation> sprite;
    @Nullable
    private final UnaryOperator<T> previous;

    protected FlowScopeSideButtonWidget(final String translationKey,
                                        final Supplier<T> getter,
                                        final Consumer<T> setter,
                                        final UnaryOperator<T> next,
                                        @Nullable final UnaryOperator<T> previous,
                                        final Function<T, ResourceLocation> sprite) {
        super(button -> setter.accept(next.apply(getter.get())));
        this.translationKey = "flow_scope." + translationKey;
        this.getter = getter;
        this.setter = setter;
        this.previous = previous;
        this.sprite = sprite;
        this.setMessage(this.getTitle());
    }

    @Override
    protected ResourceLocation getSprite() {
        return this.sprite.apply(this.getter.get());
    }

    @Override
    protected MutableComponent getTitle() {
        return createFlowAnalyticsTranslation("gui", this.translationKey);
    }

    @Override
    protected List<MutableComponent> getSubText() {
        return List.of(createFlowAnalyticsTranslation("gui", this.translationKey + "." + this.getter.get().name().toLowerCase(Locale.ROOT))
            .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 1 && this.previous != null && this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.setter.accept(this.previous.apply(this.getter.get()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
