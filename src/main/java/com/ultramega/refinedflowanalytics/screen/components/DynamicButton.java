package com.ultramega.refinedflowanalytics.screen.components;

import com.refinedmods.refinedstorage.common.Platform;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;

public class DynamicButton {
    public final Runnable onClick;
    @Nullable
    public final Runnable onRightClick;
    private final WidgetSprites sprites;
    @Nullable
    private final ResourceLocation overlay;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final String label;
    private final Font font;
    private final List<ClientTooltipComponent> tooltipLines;

    public DynamicButton(final int x,
                         final int y,
                         final int width,
                         final int height,
                         final String label,
                         final Runnable onClick,
                         @Nullable final Runnable onRightClick,
                         final List<ClientTooltipComponent> lines) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
        this.onRightClick = onRightClick;
        this.font = Minecraft.getInstance().font;
        this.sprites = new WidgetSprites(
            ResourceLocation.withDefaultNamespace("widget/button"),
            ResourceLocation.withDefaultNamespace("widget/button_disabled"),
            ResourceLocation.withDefaultNamespace("widget/button_highlighted")
        );
        this.overlay = null;
        this.tooltipLines = lines;
    }

    public DynamicButton(final int x,
                         final int y,
                         final int width,
                         final int height,
                         final String label,
                         final WidgetSprites sprites,
                         final ResourceLocation overlay,
                         final Runnable onClick,
                         @Nullable final Runnable onRightClick,
                         final List<ClientTooltipComponent> lines) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.onClick = onClick;
        this.onRightClick = onRightClick;
        this.font = Minecraft.getInstance().font;
        this.sprites = sprites;
        this.overlay = overlay;
        this.tooltipLines = lines;
    }

    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final boolean hovered = this.isMouseOver(mouseX, mouseY);
        graphics.blitSprite(this.sprites.get(true, hovered), this.x, this.y, this.width, this.height);
        graphics.drawString(this.font, this.label, this.x + 5, this.y + 6, 0xFFFFFF);
        if (this.overlay != null) {
            graphics.blitSprite(this.overlay, this.x, this.y, this.width, this.height);
        }
    }

    public void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        if (this.isMouseOver(mouseX, mouseY)) {
            Platform.INSTANCE.renderTooltip(graphics, this.tooltipLines, mouseX, mouseY);
        }
    }

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            if (button == 0) {
                this.onClick.run();
                return true;
            } else if (button == 1 && this.onRightClick != null) {
                this.onRightClick.run();
                return true;
            }
        }
        return false;
    }

    public boolean isMouseOver(final double mx, final double my) {
        return mx >= this.x && mx <= this.x + this.width && my >= this.y && my <= this.y + this.height;
    }
}
