package com.ultramega.refinedflowanalytics.screen;

import com.ultramega.refinedflowanalytics.block.FlowScopeBlock;
import com.ultramega.refinedflowanalytics.container.FlowScopeMenu;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.screen.components.DynamicButton;
import com.ultramega.refinedflowanalytics.screen.components.FlowScopeGraph;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingDirection;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingType;
import com.ultramega.refinedflowanalytics.util.TickScheduler;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering;
import com.refinedmods.refinedstorage.common.support.widget.History;
import com.refinedmods.refinedstorage.common.support.widget.ScrollbarWidget;
import com.refinedmods.refinedstorage.common.support.widget.SearchFieldWidget;
import com.refinedmods.refinedstorage.common.support.widget.SearchIconWidget;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;

public class FlowScopeScreen extends AbstractContainerScreen<FlowScopeMenu> {
    private static final ResourceLocation SCOPE_DETAIL = createFlowAnalyticsIdentifier("textures/screens/flow_scope_detail");
    private static final ResourceLocation SCOPE = createFlowAnalyticsIdentifier("textures/screens/flow_scope");

    private static final int PRODUCTION_GREEN = 0xff00ff00;
    private static final int CONSUMPTION_RED = 0xffff0000;
    private static final int ESTIMATES_BLUE = 0xff66ddff;
    private static final int WHITE = 0xffffffff;

    private static final int SIDE_BUTTON_ROW_HEIGHT = 20;

    // Simple generation constants
    private static final int ROW_HEIGHT = 25;
    private static final int INNER_WIDTH = 222;
    private static final int INNER_HEIGHT = 180;
    private static final int NUMBER_OF_COLS = 3;

    // Detailed generation constants
    private static final int TEXT_LINE_HEIGHT = 12;
    private final WidgetSprites sideButtonSprites;

    private final Level world;
    private final int x;
    private final int y;
    private final int z;

    private final Player entity;
    private final List<DynamicButton> itemButtons = new ArrayList<>();
    private final List<DynamicButton> sideButtons = new ArrayList<>();
    private final FlowScopeGraph graph = new FlowScopeGraph();

    private int innerLeft = this.leftPos + 5;
    private int innerTop = this.topPos + 20;

    private SortingType sortingType = SortingType.QUANTITY;
    private SortingDirection sortingDirection = SortingDirection.DESCENDING;

    private Button buttonDone;
    @Nullable
    private ScrollbarWidget scrollbar;
    private SearchFieldWidget searchField;
    private Granularity granularity = Granularity.MINUTE;
    private TickScheduler tickScheduler = new TickScheduler(this.granularity.getTickAmount());

    private Map<PlatformResourceKey, Map<Short, Long>> lastSnapshot = new HashMap<>();

    private boolean hasDetailedGenerationData = false;

    public FlowScopeScreen(final FlowScopeMenu container, final Inventory inventory, final Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.getX();
        this.y = container.getY();
        this.z = container.getZ();
        this.entity = container.entity;
        this.imageWidth = 250;
        this.imageHeight = 225;

        this.sideButtonSprites = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("refinedstorage", "widget/side_button/base"),
            null,
            ResourceLocation.fromNamespaceAndPath("refinedstorage", "widget/side_button/hovered"),
            null
        );
    }

    private void renderDetailedGenerationStats(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        RenderSystem.defaultBlendFunc();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 200);

        guiGraphics.blit(SCOPE_DETAIL, this.leftPos - 3, this.topPos, 0, 0, 256, 256, 256, 256);

        final PlatformResourceKey itemKey = this.graph.getItemKey();
        guiGraphics.drawString(this.font, this.title.getString() + " - Flow details", this.leftPos + 8, this.topPos + 6, 4210752, false);

        this.graph.renderItem(guiGraphics, this.leftPos + 7, this.topPos + 26);
        guiGraphics.drawString(this.font, this.graph.getItemName(), this.leftPos + 36, this.topPos + 30, WHITE);

        final int graphLeft = this.leftPos + 7;
        final int graphBottom = this.topPos + 134;
        final int graphWidth = this.graph.getGraphSize().x;
        final int graphHeight = this.graph.getGraphSize().y;
        this.graph.setGraphPos(graphLeft, graphBottom);
        if (this.graph.isLoading()) {
            guiGraphics.drawString(this.font, "Loading...", graphLeft + (graphWidth / 4), graphBottom - (graphHeight / 2), WHITE);
            return;
        }

        this.graph.drawGraphs(guiGraphics);

        // Main stats
        final int productionRowShift = 2;
        guiGraphics.drawString(this.font, "Inflow", graphLeft + productionRowShift, graphBottom + TEXT_LINE_HEIGHT + 2, PRODUCTION_GREEN);
        guiGraphics.drawString(this.font, "Max: " + this.formatAmount(itemKey, this.graph.getMaxProduction()) + this.granularity.perStr(),
            graphLeft + productionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 2, WHITE);
        guiGraphics.drawString(this.font, "Min:  " + this.formatAmount(itemKey, this.graph.getMinProduction()) + this.granularity.perStr(),
            graphLeft + productionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 3, WHITE);

        final int consumptionRowShift = 78;
        guiGraphics.drawString(this.font, "Outflow", graphLeft + consumptionRowShift, graphBottom + TEXT_LINE_HEIGHT + 2, CONSUMPTION_RED);
        guiGraphics.drawString(this.font, "Max: " + this.formatAmount(itemKey, this.graph.getMaxConsumption()) + this.granularity.perStr(),
            graphLeft + consumptionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 2, WHITE);
        guiGraphics.drawString(this.font, "Min:  " + this.formatAmount(itemKey, this.graph.getMinConsumption()) + this.granularity.perStr(),
            graphLeft + consumptionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 3, WHITE);

        // Average lines
        guiGraphics.drawString(this.font, "Avg", graphLeft + graphWidth + 8, graphBottom - graphHeight - 14, WHITE);

        final double decAvg = this.graph.getAvgConsumption();
        final Vector2i decAvgPoint = this.graph.getGuiXYFromGraphValue(0, (long) decAvg);
        this.graph.drawLine(guiGraphics, graphLeft, decAvgPoint.y, graphLeft + graphWidth + 20, decAvgPoint.y, CONSUMPTION_RED, 1, LineStyle.EXACT);

        final double incAvg = this.graph.getAvgProduction();
        final Vector2i incAvgPoint = this.graph.getGuiXYFromGraphValue(0, (long) incAvg);
        this.graph.drawLine(guiGraphics, graphLeft, incAvgPoint.y, graphLeft + graphWidth + 20, incAvgPoint.y, PRODUCTION_GREEN, 1, LineStyle.EXACT);

        // Average line values
        final int avgValueShift = 3;
        if (incAvg >= decAvg) {
            guiGraphics.drawString(this.font, "+" + this.formatAmount(itemKey, (long) incAvg) + this.granularity.perStr(),
                graphLeft + graphWidth + avgValueShift, incAvgPoint.y - 10, PRODUCTION_GREEN);
            guiGraphics.drawString(this.font, "-" + this.formatAmount(itemKey, (long) decAvg) + this.granularity.perStr(),
                graphLeft + graphWidth + avgValueShift, decAvgPoint.y + 2, CONSUMPTION_RED);
        } else {
            guiGraphics.drawString(this.font, "+" + this.formatAmount(itemKey, (long) incAvg) + this.granularity.perStr(),
                graphLeft + graphWidth + avgValueShift, incAvgPoint.y + 2, PRODUCTION_GREEN);
            guiGraphics.drawString(this.font, "-" + this.formatAmount(itemKey, (long) decAvg) + this.granularity.perStr(),
                graphLeft + graphWidth + avgValueShift, decAvgPoint.y - 10, CONSUMPTION_RED);
        }

        // Estimates
        final int estimatesRowShift = 159;
        guiGraphics.drawString(this.font, "Estimates (net)", graphLeft + estimatesRowShift, graphBottom + 25,
            ESTIMATES_BLUE);
        final long seconds = (long) Granularity.SECOND.convertFrom(this.granularity, this.graph.getNetAvg());
        guiGraphics.drawString(this.font, this.formatSignedAmount(itemKey, seconds) + Granularity.SECOND.perStr(),
            graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 3, WHITE);
        final long minutes = (long) Granularity.MINUTE.convertFrom(this.granularity, this.graph.getNetAvg());
        guiGraphics.drawString(this.font, this.formatSignedAmount(itemKey, minutes) + Granularity.MINUTE.perStr(),
            graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 4, WHITE);
        final long hours = (long) Granularity.HOUR.convertFrom(this.granularity, this.graph.getNetAvg());
        guiGraphics.drawString(this.font, this.formatSignedAmount(itemKey, hours) + Granularity.HOUR.perStr(),
            graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 5, WHITE);
        final long days = (long) Granularity.DAY.convertFrom(this.granularity, this.graph.getNetAvg());
        guiGraphics.drawString(this.font, this.formatSignedAmount(itemKey, days) + Granularity.DAY.perStr(),
            graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 6, WHITE);

        // Overall stats
        final long net = Arrays.stream(this.graph.getNetData()).sum();
        guiGraphics.drawString(this.font, "Total in storage: " + this.graph.getTotalStored(), graphLeft + productionRowShift,
            graphBottom + 4 + TEXT_LINE_HEIGHT * 5, WHITE);
        guiGraphics.drawString(this.font, "Net in this timeframe: " + (net > 0 ? "+" : "") + this.formatAmount(itemKey, net),
            graphLeft + productionRowShift, graphBottom + 4 + TEXT_LINE_HEIGHT * 6, WHITE);

        guiGraphics.pose().popPose();
    }

    private void renderSimpleGenerationStats(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        this.innerLeft = this.leftPos + 5;
        this.innerTop = this.topPos + 20;
        final int rowWidth = INNER_WIDTH / NUMBER_OF_COLS;
        this.enableScissorFromGui(this.innerLeft, this.innerTop, INNER_WIDTH, INNER_HEIGHT);

        int counter = 0;
        this.itemButtons.clear();
        for (final Map.Entry<PlatformResourceKey, Map<Short, Long>> item : this.sortingDirection.sort(this.sortingType.sort(this.lastSnapshot)).entrySet()) {
            final PlatformResourceKey itemKey = item.getKey();
            final Map<Short, Long> itemChange = item.getValue();
            final int itemX = this.innerLeft + ((counter % NUMBER_OF_COLS) * rowWidth);
            final int itemY = this.innerTop + (Math.floorDiv(counter, NUMBER_OF_COLS) * ROW_HEIGHT) - (this.scrollbar != null ? (int) this.scrollbar.getOffset() : 0);
            final ResourceRendering resourceRendering = RefinedStorageClientApi.INSTANCE
                .getResourceRendering(itemKey.getClass());

            // filters
            final String nameFilter = this.searchField.getValue();
            if (!nameFilter.isEmpty()) {
                if (!resourceRendering.getDisplayName(itemKey).getString().toLowerCase(Locale.ROOT).contains(nameFilter)) {
                    continue;
                }
            }

            if (itemY > this.topPos + this.height) {
                break;
            }
            if (itemY < this.topPos - ROW_HEIGHT) {
                counter++;
                continue;
            }

            // Rendering buttons
            final DynamicButton button = new DynamicButton(
                itemX,
                itemY,
                rowWidth - 1, ROW_HEIGHT - 1, "",
                () -> this.requestDetailedGenerationStats(itemKey, true),
                null,
                List.of(new ClientTextTooltip(resourceRendering.getDisplayName(itemKey).getVisualOrderText()))
            );
            button.render(guiGraphics, mouseX, mouseY);
            this.itemButtons.add(button);

            // Adding margins
            final int left = itemX + 4;
            final int top = itemY + 4;

            // Rendering item texture
            resourceRendering.render(itemKey, guiGraphics, left, top);

            // Rendering generation stats
            final long net = (itemChange.get((short) +1)) - (itemChange.get((short) -1));
            guiGraphics.drawString(this.font,
                this.formatSignedAmount(itemKey, net) + this.granularity.perStr(),
                left + 20, top + 5,
                (net > 0 ? PRODUCTION_GREEN : net < 0 ? CONSUMPTION_RED : WHITE));
            // guiGraphics.drawString(font,
            // "+"+ItemResourceRendering.INSTANCE.formatAmount(itemChange.get((short) +1),
            // true)+granularity.perStr(), left + 20, top, PRODUCTION_GREEN);
            // guiGraphics.drawString(font,
            // "-"+ItemResourceRendering.INSTANCE.formatAmount(itemChange.get((short) -1),
            // true)+granularity.perStr(), left + 20, top + 10, CONSUMPTION_RED);

            counter++;
        }
        if (this.scrollbar != null) {
            final double rowsAmount = Math.ceil(this.lastSnapshot.size() * 1f / NUMBER_OF_COLS);
            final double maxOffset = rowsAmount * ROW_HEIGHT;
            this.scrollbar.setEnabled(maxOffset > 180);
            this.scrollbar.setMaxOffset(maxOffset - 180);
        }
        RenderSystem.disableScissor();
        RenderSystem.disableBlend();
    }

    private void requestDetailedGenerationStats(final PlatformResourceKey itemKey, final boolean manual) {
        if (this.entity instanceof Player player && player.containerMenu instanceof FlowScopeMenu flowMenu) {
            if (manual) {
                this.graph.setLoading(true);
            }
            flowMenu.sendMenuStateUpdate(player, 5, "detailedFactoryGenerationRequest",
                new ResourceChangeGranularityKey(itemKey, (short) 0, this.granularity.getTickAmount()), false);
        }
    }

    private void requestSimpleGenerationStats() {
        if (this.entity instanceof Player player && player.containerMenu instanceof FlowScopeMenu flowMenu) {
            flowMenu.sendMenuStateUpdate(player, 4, "simpleFactoryGenerationRequest", this.granularity.getTickAmount(), false);
        }
    }

    @SuppressWarnings("unchecked")
    public void updateMenuState(final int elementType, final String name, final Object elementState) { //TODO: rework this
        if ("detailedFactoryGeneration".equals(name)) {
            this.graph.setData((Map<ResourceChangeGranularityKey, long[]>) elementState, this.granularity);
            this.hasDetailedGenerationData = true;
        } else if ("lastSnapshot".equals(name)) {
            this.lastSnapshot = (Map<PlatformResourceKey, Map<Short, Long>>) elementState;
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.hasDetailedGenerationData) {
            // back button
            if (button == 3) {
                this.hasDetailedGenerationData = false;
                return true;
            }
            if (this.graph.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        } else {
            // forward button
            if (button == 4) {
                this.hasDetailedGenerationData = !this.graph.isLoading();
                return true;
            }
            for (final DynamicButton b : this.itemButtons) {
                if (this.innerLeft < mouseX && mouseX < this.innerLeft + INNER_WIDTH && this.innerTop < mouseY
                    && mouseY < this.innerTop + INNER_HEIGHT) {
                    if (b.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                }
            }
        }

        for (final DynamicButton b : this.sideButtons) {
            if (b.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (this.scrollbar != null && this.scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(final double mx, final double my) {
        if (this.scrollbar != null) {
            this.scrollbar.mouseMoved(mx, my);
        }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        if (this.scrollbar != null && this.scrollbar.mouseReleased(mx, my, button)) {
            return true;
        }
        if (this.hasDetailedGenerationData) {
            if (this.graph.mouseReleased(mx, my, button)) {
                return true;
            }
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double z, final double delta) {
        final boolean didScroll = this.scrollbar != null
            && this.isHoveringOverArea(x, y)
            && this.scrollbar.mouseScrolled(x, y, z, delta);
        return didScroll || super.mouseScrolled(x, y, z, delta);
    }

    @Override
    public boolean keyPressed(final int key, final int b, final int c) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (this.hasDetailedGenerationData) {
                this.hasDetailedGenerationData = false;
            } else if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
            }
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    @Override
    public void removed() {
        super.removed(); //TODO: ??
        final BlockState blockState = this.world.getBlockState(new BlockPos(this.x, this.y, this.z))
            .setValue(FlowScopeBlock.SORT_TYPE, this.sortingType.ordinal())
            .setValue(FlowScopeBlock.SORT_DIRECTION, this.sortingDirection.ordinal())
            .setValue(FlowScopeBlock.GRANULARITY, this.granularity.ordinal());
        this.world.setBlock(new BlockPos(this.x, this.y, this.z), blockState, 0); // temporary, client-side only
    }

    @Override
    public void init() {
        super.init();
        this.buttonDone = Button.builder(Component.translatable("gui.done"), b -> {
            if (this.minecraft == null || this.minecraft.player == null) {
                return;
            }
            this.minecraft.player.closeContainer();
        }).bounds(this.leftPos + 195, this.topPos + 205, 46, 20).build();
        this.addRenderableWidget(this.buttonDone);

        this.searchField = new SearchFieldWidget(this.font, this.leftPos + 94 + 1 + 58, this.topPos + 6 + 1, 67, new History(new ArrayList<>()));
        this.addWidget(this.searchField);
        this.addRenderableWidget(new SearchIconWidget(this.leftPos + 79 + 58, this.topPos + 5,
            () -> Component.literal("Search").withStyle(ChatFormatting.GRAY), this.searchField)
        );

        this.scrollbar = new ScrollbarWidget(this.leftPos + 232, this.topPos + 20, ScrollbarWidget.Type.NORMAL, 180);
        this.addRenderableWidget(this.scrollbar);

        final BlockState blockState = this.world.getBlockState(new BlockPos(this.x, this.y, this.z));
        // TODO: maybe save that data back?
        this.sortingType = SortingType.values()[blockState.getValue(FlowScopeBlock.SORT_TYPE)];
        this.sortingDirection = SortingDirection.values()[blockState.getValue(FlowScopeBlock.SORT_DIRECTION)];
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.tickScheduler.shouldRun()) {
            if (this.hasDetailedGenerationData) {
                final PlatformResourceKey itemKey = this.graph.getItemKey();
                this.requestDetailedGenerationStats(itemKey, false);
            } else {
                this.requestSimpleGenerationStats();
            }
        }
    }

    private boolean isHoveringOverArea(final double x, final double y) {
        return this.isHovering(8, 20, 221, 180, x, y);
    }

    private String formatAmount(final PlatformResourceKey resourceKey, final long amount) {
        return RefinedStorageClientApi.INSTANCE.getResourceRendering(resourceKey.getClass()).formatAmount(amount, true);
    }

    private String formatSignedAmount(final PlatformResourceKey resourceKey, final long amount) {
        final long unsignedAmount = Math.abs(amount);
        final String formatted = this.formatAmount(resourceKey, unsignedAmount);
        if (amount < 0) {
            return "-" + formatted;
        } else {
            return "+" + formatted;
        }
    }

    public void enableScissorFromGui(final int guiX, final int guiY, final int guiWidth, final int guiHeight) {
        final Minecraft mc = Minecraft.getInstance();
        final Window window = mc.getWindow();
        final double scale = window.getGuiScale();

        final int x = (int) (guiX * scale);
        final int y = (int) (window.getHeight() - (guiY + guiHeight) * scale);
        final int width = (int) (guiWidth * scale);
        final int height = (int) (guiHeight * scale);

        RenderSystem.enableScissor(x, y, width, height);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderSimpleGenerationStats(guiGraphics, mouseX, mouseY);
        this.renderSideButtons(guiGraphics, mouseX, mouseY);
        if (this.hasDetailedGenerationData) {
            this.renderDetailedGenerationStats(guiGraphics, mouseX, mouseY);
        }
        this.searchField.render(guiGraphics, 0, 0, 0.0F);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTicks, final int mouseX, final int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(SCOPE, this.leftPos - 3, this.topPos, 0, 0, 256, 256, 256, 256);
        RenderSystem.disableBlend();
    }

    private void renderSideButtons(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        this.sideButtons.clear();
        final Function<String, String> capitalCase = a -> a.substring(0, 1).toUpperCase() + a.substring(1).toLowerCase();
        final DynamicButton sortingDirectionButton = new DynamicButton(this.leftPos - 24, this.topPos + 6, 18, 18, "",
            this.sideButtonSprites, this.sortingDirection.getSprite(),
            () -> this.sortingDirection = SortingDirection.next(this.sortingDirection),
            null,
            List.of(
                new ClientTextTooltip(Component.literal("Sorting direction").getVisualOrderText()),
                new ClientTextTooltip(Component.literal(capitalCase.apply(this.sortingDirection.toString()))
                    .withColor(Color.LIGHT_GRAY.getRGB()).getVisualOrderText())));
        sortingDirectionButton.render(guiGraphics, mouseX, mouseY);
        this.sideButtons.add(sortingDirectionButton);

        final DynamicButton sortingTypeButton = new DynamicButton(this.leftPos - 24, this.topPos + 6 + SIDE_BUTTON_ROW_HEIGHT, 18, 18,
            "",
            this.sideButtonSprites, this.sortingType.getResourceLocation(),
            () -> this.sortingType = SortingType.next(this.sortingType),
            null,
            List.of(
                new ClientTextTooltip(Component.literal("Sorting Type").getVisualOrderText()),
                new ClientTextTooltip(Component.literal(capitalCase.apply(this.sortingType.toString()))
                    .withColor(Color.LIGHT_GRAY.getRGB()).getVisualOrderText())));
        sortingTypeButton.render(guiGraphics, mouseX, mouseY);
        this.sideButtons.add(sortingTypeButton);

        final DynamicButton granularityButton = new DynamicButton(this.leftPos - 24, this.topPos + 6 + (SIDE_BUTTON_ROW_HEIGHT * 2), 18,
            18, "",
            this.sideButtonSprites, this.granularity.getSprite(),
            () -> {
                this.granularity = Granularity.next(this.granularity);
                this.tickScheduler = new TickScheduler(this.granularity.getTickAmount());
            },
            () -> {
                this.granularity = Granularity.prev(this.granularity);
                this.tickScheduler = new TickScheduler(this.granularity.getTickAmount());
                this.graph.setLoading(true);
            },
            List.of(
                new ClientTextTooltip(Component.literal("Granularity").getVisualOrderText()),
                new ClientTextTooltip(Component.literal(capitalCase.apply(this.granularity.toString()))
                    .withColor(Color.LIGHT_GRAY.getRGB()).getVisualOrderText())));
        granularityButton.render(guiGraphics, mouseX, mouseY);
        this.sideButtons.add(granularityButton);

        final DynamicButton styleButton = new DynamicButton(this.leftPos - 24, this.topPos + 6 + (SIDE_BUTTON_ROW_HEIGHT * 3), 18, 18,
            "",
            this.sideButtonSprites, this.graph.lineStyle.getSprite(),
            () -> {
                this.graph.lineStyle = LineStyle.next(this.graph.lineStyle);
            },
            null,
            List.of(
                new ClientTextTooltip(Component.literal("Line Style").getVisualOrderText()),
                new ClientTextTooltip(Component.literal(capitalCase.apply(this.graph.lineStyle.toString()))
                    .withColor(Color.LIGHT_GRAY.getRGB()).getVisualOrderText())));
        styleButton.render(guiGraphics, mouseX, mouseY);
        this.sideButtons.add(styleButton);

    }

    @Override
    protected void renderLabels(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderTooltip(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        if (this.hasDetailedGenerationData) {
            this.graph.renderTooltip(guiGraphics, mouseX, mouseY);
        } else {
            for (final DynamicButton button : this.itemButtons) {
                button.renderTooltip(guiGraphics, mouseX, mouseY);
            }
        }
        for (final DynamicButton button : this.sideButtons) {
            button.renderTooltip(guiGraphics, mouseX, mouseY);
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
