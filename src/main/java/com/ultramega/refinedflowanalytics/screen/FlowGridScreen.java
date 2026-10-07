package com.ultramega.refinedflowanalytics.screen;

import com.ultramega.refinedflowanalytics.container.FlowGridContainerMenu;
import com.ultramega.refinedflowanalytics.data.FlowEstimate;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.screen.components.FlowGraph;
import com.ultramega.refinedflowanalytics.screen.components.FlowItemButton;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.GranularitySideButtonWidget;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyleSideButtonWidget;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.ResourceView;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.ResourceViewSideButtonWidget;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingDirectionSideButtonWidget;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.SortingTypeSideButtonWidget;
import com.ultramega.refinedflowanalytics.util.TickScheduler;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering;
import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.widget.History;
import com.refinedmods.refinedstorage.common.support.widget.ScrollbarWidget;
import com.refinedmods.refinedstorage.common.support.widget.SearchFieldWidget;
import com.refinedmods.refinedstorage.common.support.widget.SearchIconWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsIdentifier;
import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowGridScreen extends AbstractBaseScreen<FlowGridContainerMenu> {
    private static final ResourceLocation GRID_DETAIL = createFlowAnalyticsIdentifier("textures/gui/flow_grid_detail.png");
    private static final ResourceLocation GRID = createFlowAnalyticsIdentifier("textures/gui/flow_grid.png");

    private static final int PRODUCTION_GREEN = 0xff00ff00;
    private static final int CONSUMPTION_RED = 0xffff0000;
    private static final int ESTIMATES_BLUE = 0xff66ddff;
    private static final int WHITE = 0xffffffff;
    private static final int GRAY = 0xffa0a0a0;

    private static final int ITEM_HEADER_Y = 11;
    private static final int ITEM_NAME_Y = 15;
    private static final int GRAPH_X = 10;
    private static final int GRAPH_BOTTOM = 134;

    // Simple generation constants
    private static final int ROW_HEIGHT = 25;
    private static final int ROW_SPACING = ROW_HEIGHT - 1;
    private static final int INNER_WIDTH = 222;
    private static final int INNER_HEIGHT = 180;
    private static final int NUMBER_OF_COLS = 3;

    // Detailed generation constants
    private static final int TEXT_LINE_HEIGHT = 12;

    private final List<FlowItemButton> itemButtons = new ArrayList<>();
    private final FlowGraph graph = new FlowGraph();
    private FlowEstimate cachedEstimate = FlowEstimate.EMPTY;
    @Nullable
    private PlatformResourceKey cachedEstimateResource;
    private String[] estimateLabels = new String[0];

    private int innerLeft = this.leftPos + 8;
    private int innerTop = this.topPos + 20;

    private Button doneButton;
    @Nullable
    private ScrollbarWidget scrollbar;
    private SearchFieldWidget searchField;
    private TickScheduler tickScheduler;
    private SearchIconWidget searchIcon;

    private Map<PlatformResourceKey, Map<Short, Long>> lastSnapshot = new HashMap<>();

    private boolean hasDetailedGenerationData = false;

    public FlowGridScreen(final FlowGridContainerMenu container, final Inventory inventory, final Component text) {
        super(container, inventory, text);
        this.imageWidth = 254;
        this.imageHeight = 231;

        this.tickScheduler = new TickScheduler(container.getGranularity().getTickAmount());
    }

    @Override
    public void init() {
        super.init();
        this.doneButton = Button.builder(Component.translatable("gui.done"), b -> {
            if (this.minecraft == null || this.minecraft.player == null) {
                return;
            }
            this.minecraft.player.closeContainer();
        }).bounds(this.leftPos + 198, this.topPos + 205, 46, 20).build();
        this.addRenderableWidget(this.doneButton);

        this.searchField = new SearchFieldWidget(this.font, this.leftPos + 97 + 1 + 58, this.topPos + 6 + 1, 67, new History(new ArrayList<>()));
        this.addRenderableWidget(this.searchField);
        this.searchIcon = this.addRenderableWidget(new SearchIconWidget(this.leftPos + 82 + 58, this.topPos + 5,
            () -> createFlowAnalyticsTranslation("gui", "flow.search"), this.searchField));

        this.scrollbar = new ScrollbarWidget(this.leftPos + 235, this.topPos + 20, ScrollbarWidget.Type.NORMAL, INNER_HEIGHT);
        this.addRenderableWidget(this.scrollbar);

        this.addSideButton(new SortingDirectionSideButtonWidget(this.getMenu()));
        this.addSideButton(new SortingTypeSideButtonWidget(this.getMenu()));
        this.addSideButton(new ResourceViewSideButtonWidget(this.getMenu(), this::onResourceViewChanged));
        this.addSideButton(new GranularitySideButtonWidget(this.getMenu(), this::onGranularityChanged));
        this.addSideButton(new LineStyleSideButtonWidget(this.getMenu()));
        this.updateControlVisibility();
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

    private void renderDetailedGenerationStats(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final Granularity granularity = this.getMenu().getGranularity();
        RenderSystem.defaultBlendFunc();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);

        this.graph.renderItem(graphics, this.leftPos + GRAPH_X, this.topPos + ITEM_HEADER_Y);
        graphics.drawString(this.font, this.graph.getItemName(), this.leftPos + 35, this.topPos + ITEM_NAME_Y, WHITE);

        final int graphLeft = this.leftPos + GRAPH_X;
        final int graphBottom = this.topPos + GRAPH_BOTTOM;
        final int graphWidth = this.graph.getGraphSize().x;
        final int graphHeight = this.graph.getGraphSize().y;
        this.graph.setGraphPos(graphLeft, graphBottom);
        if (this.graph.isLoading()) {
            final Component loadingText = createFlowAnalyticsTranslation("gui", "flow.loading");
            graphics.drawCenteredString(this.font, loadingText, graphLeft + graphWidth / 2, graphBottom - graphHeight / 2 - this.font.lineHeight / 2, WHITE);
            graphics.pose().popPose();
            return;
        }

        this.graph.drawGraphs(graphics);

        // Main stats
        final PlatformResourceKey itemKey = this.graph.getItemKey();

        final int productionRowShift = 2;
        graphics.drawString(this.font,
            createFlowAnalyticsTranslation("gui", "flow.inflow"),
            graphLeft + productionRowShift, graphBottom + TEXT_LINE_HEIGHT + 2, PRODUCTION_GREEN);
        graphics.drawString(this.font,
            createFlowAnalyticsTranslation("gui", "flow.maximum", this.formatAmount(itemKey, this.graph.getMaxProduction()) + granularity.perStr()),
            graphLeft + productionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 2, WHITE);
        graphics.drawString(this.font,
            createFlowAnalyticsTranslation("gui", "flow.minimum", this.formatAmount(itemKey, this.graph.getMinProduction()) + granularity.perStr()),
            graphLeft + productionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 3, WHITE);

        final int consumptionRowShift = 78;
        graphics.drawString(this.font,
            createFlowAnalyticsTranslation("gui", "flow.outflow"),
            graphLeft + consumptionRowShift, graphBottom + TEXT_LINE_HEIGHT + 2, CONSUMPTION_RED);
        graphics.drawString(this.font,
            createFlowAnalyticsTranslation("gui", "flow.maximum", this.formatAmount(itemKey, this.graph.getMaxConsumption()) + granularity.perStr()),
            graphLeft + consumptionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 2, WHITE);
        graphics.drawString(this.font,
            createFlowAnalyticsTranslation("gui", "flow.minimum", this.formatAmount(itemKey, this.graph.getMinConsumption()) + granularity.perStr()),
            graphLeft + consumptionRowShift, graphBottom + 8 + TEXT_LINE_HEIGHT * 3, WHITE);

        // Reference lines
        graphics.drawString(this.font, createFlowAnalyticsTranslation("gui", "flow.average"),
            graphLeft + graphWidth + 8, graphBottom - graphHeight - 14, WHITE);

        final double incAvg = this.graph.getAvgProduction();
        final double decAvg = this.graph.getAvgConsumption();
        final int zeroY = (int) Math.round(this.graph.getGuiY(0));
        final int incAvgY = (int) Math.round(this.graph.getGuiY(incAvg));
        final int decAvgY = (int) Math.round(this.graph.getGuiY(-decAvg));
        final int averageLabelX = graphLeft + graphWidth + 3;
        // Leave room for the zero label, even when one or both averages are zero.
        final int incLabelY = Math.clamp(incAvgY - 10, graphBottom - graphHeight, zeroY - 11);
        final int decLabelY = Math.clamp(decAvgY + 2, zeroY + 2, graphBottom - this.font.lineHeight);
        graphics.drawString(this.font, "+" + this.formatAmount(itemKey, (long) incAvg) + granularity.perStr(),
            averageLabelX, incLabelY, PRODUCTION_GREEN);
        graphics.drawString(this.font, "-" + this.formatAmount(itemKey, (long) decAvg) + granularity.perStr(),
            averageLabelX, decLabelY, CONSUMPTION_RED);
        graphics.drawString(this.font, "0", averageLabelX, zeroY - 4, GRAY);

        // Estimates
        final int estimatesRowShift = 160;
        graphics.drawString(this.font, createFlowAnalyticsTranslation("gui", "flow.estimates"),
            graphLeft + estimatesRowShift, graphBottom + 25, ESTIMATES_BLUE);
        final FlowEstimate estimate = this.graph.getEstimate();
        this.updateEstimateLabels(itemKey, estimate);
        graphics.drawString(this.font, this.estimateLabels[0], graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 3, WHITE);
        graphics.drawString(this.font, this.estimateLabels[1], graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 4, WHITE);
        graphics.drawString(this.font, this.estimateLabels[2], graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 5, WHITE);
        graphics.drawString(this.font, this.estimateLabels[3], graphLeft + estimatesRowShift, graphBottom + 5 + TEXT_LINE_HEIGHT * 6, WHITE);

        // Overall stats
        final long net = Arrays.stream(this.graph.getNetData()).sum();
        graphics.drawString(this.font, createFlowAnalyticsTranslation("gui", "flow.total_stored", this.graph.getTotalStored()), graphLeft + productionRowShift,
            graphBottom + 4 + TEXT_LINE_HEIGHT * 5, WHITE);
        graphics.drawString(this.font, createFlowAnalyticsTranslation("gui", "flow.net_timeframe", (net > 0 ? "+" : "") + this.formatAmount(itemKey, net)),
            graphLeft + productionRowShift, graphBottom + 4 + TEXT_LINE_HEIGHT * 6, WHITE);

        graphics.pose().popPose();
    }

    private void renderSimpleGenerationStats(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final Granularity granularity = this.getMenu().getGranularity();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        this.innerLeft = this.leftPos + 8;
        this.innerTop = this.topPos + 20;
        final int rowWidth = INNER_WIDTH / NUMBER_OF_COLS + 1;
        final int columnSpacing = rowWidth - 1;
        this.enableScissorFromGui(this.innerLeft - 1, this.innerTop - 1, INNER_WIDTH + 1, INNER_HEIGHT + 1);

        int counter = 0;
        this.itemButtons.clear();
        final var sortedSnapshot = this.getMenu().getSortingDirection().sort(this.getMenu().getSortingType().sort(this.lastSnapshot)).entrySet();
        for (final Map.Entry<PlatformResourceKey, Map<Short, Long>> item : sortedSnapshot) {
            final PlatformResourceKey itemKey = item.getKey();
            final Map<Short, Long> itemChange = item.getValue();
            final int itemX = this.innerLeft + ((counter % NUMBER_OF_COLS) * columnSpacing);
            final int itemY = this.innerTop + (Math.floorDiv(counter, NUMBER_OF_COLS) * ROW_SPACING) - (this.scrollbar != null ? (int) this.scrollbar.getOffset() : 0);
            final ResourceRendering resourceRendering = RefinedStorageClientApi.INSTANCE
                .getResourceRendering(itemKey.getClass());

            // filters
            final String nameFilter = this.searchField.getValue();
            if (!nameFilter.isEmpty()) {
                if (!resourceRendering.getDisplayName(itemKey).getString().toLowerCase(Locale.ROOT).contains(nameFilter)) {
                    continue;
                }
            }

            if (itemY >= this.innerTop + INNER_HEIGHT) {
                break;
            }
            if (itemY + ROW_HEIGHT <= this.innerTop) {
                counter++;
                continue;
            }

            // Rendering buttons
            final FlowItemButton button = new FlowItemButton(
                itemX - 1,
                itemY - 1,
                rowWidth, ROW_HEIGHT,
                itemKey, itemChange, granularity,
                () -> this.requestDetailedGenerationStats(itemKey, true),
                List.of(new ClientTextTooltip(resourceRendering.getDisplayName(itemKey).getVisualOrderText()))
            );
            this.itemButtons.add(button);

            counter++;
        }

        final FlowItemButton hoveredButton = this.findHoveredItemButton(mouseX, mouseY);
        for (final FlowItemButton button : this.itemButtons) {
            if (button != hoveredButton) {
                button.render(graphics, false);
            }
        }
        // Draw the hovered button last so its shared border stays highlighted
        if (hoveredButton != null) {
            hoveredButton.render(graphics, true);
        }

        if (this.scrollbar != null) {
            final double rowsAmount = Math.ceil(this.lastSnapshot.size() * 1f / NUMBER_OF_COLS);
            final double maxOffset = rowsAmount * ROW_SPACING;
            this.scrollbar.setEnabled(maxOffset > INNER_HEIGHT);
            this.scrollbar.setMaxOffset(Math.max(0, maxOffset - INNER_HEIGHT));
        }
        RenderSystem.disableScissor();
        RenderSystem.disableBlend();
    }

    private void requestDetailedGenerationStats(final PlatformResourceKey itemKey, final boolean manual) {
        final Player player = this.getMenu().entity;
        if (player.containerMenu == this.getMenu()) {
            if (manual) {
                this.graph.setLoading(true);
            }
            this.getMenu().sendMenuStateUpdate(player, 5, "detailedFactoryGenerationRequest",
                new ResourceChangeGranularityKey(itemKey, (short) 0, this.getMenu().getGranularity().getTickAmount()), false);
        }
    }

    private void requestSimpleGenerationStats() {
        final Player player = this.getMenu().entity;
        if (player.containerMenu == this.getMenu()) {
            final String request = this.getMenu().getResourceView() == ResourceView.ALL_STORED
                ? "storedFactoryGenerationRequest" : "simpleFactoryGenerationRequest";
            this.getMenu().sendMenuStateUpdate(player, 4, request, this.getMenu().getGranularity().getTickAmount(), false);
        }
    }

    private void onResourceViewChanged() {
        this.lastSnapshot = new HashMap<>();
        this.itemButtons.clear();
        if (this.scrollbar != null) {
            this.scrollbar.setOffset(0);
        }
        this.requestSimpleGenerationStats();
    }

    private void onGranularityChanged() {
        this.tickScheduler = new TickScheduler(this.getMenu().getGranularity().getTickAmount());
        if (this.hasDetailedGenerationData) {
            this.requestDetailedGenerationStats(this.graph.getItemKey(), true);
        } else {
            this.lastSnapshot = new HashMap<>();
            this.requestSimpleGenerationStats();
        }
    }

    @SuppressWarnings("unchecked")
    public void updateMenuState(final int elementType, final String name, final Object elementState) { //TODO: rework this
        if ("detailedFactoryGeneration".equals(name)) {
            final Map<ResourceChangeGranularityKey, long[]> data = (Map<ResourceChangeGranularityKey, long[]>) elementState;
            if (data.isEmpty() || data.keySet().stream().anyMatch(key -> key.granularity() != this.getMenu().getGranularity().getTickAmount())) {
                return;
            }
            this.graph.setData(data, this.getMenu().getGranularity());
            this.hasDetailedGenerationData = true;
        } else if (("lastSnapshot".equals(name) && this.getMenu().getResourceView() == ResourceView.CHANGED)
            || ("storedSnapshot".equals(name) && this.getMenu().getResourceView() == ResourceView.ALL_STORED)) {
            this.lastSnapshot = (Map<PlatformResourceKey, Map<Short, Long>>) elementState;
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        System.out.println(button);
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
            final FlowItemButton hoveredButton = this.findHoveredItemButton(mouseX, mouseY);
            if (hoveredButton != null && hoveredButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (this.scrollbar != null && this.scrollbar.visible && this.scrollbar.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(final double mx, final double my) {
        if (this.scrollbar != null && this.scrollbar.visible) {
            this.scrollbar.mouseMoved(mx, my);
        }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        if (this.scrollbar != null && this.scrollbar.visible && this.scrollbar.mouseReleased(mx, my, button)) {
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

    @Nullable
    private FlowItemButton findHoveredItemButton(final double mouseX, final double mouseY) {
        if (!this.isHoveringOverArea(mouseX, mouseY)) {
            return null;
        }
        for (final FlowItemButton button : this.itemButtons.reversed()) {
            if (button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }

    private boolean isHoveringOverArea(final double x, final double y) {
        return !this.hasDetailedGenerationData && this.isHovering(7, 19, INNER_WIDTH + 1, INNER_HEIGHT + 1, x, y);
    }

    private String formatAmount(final PlatformResourceKey resourceKey, final long amount) {
        return RefinedStorageClientApi.INSTANCE.getResourceRendering(resourceKey.getClass()).formatAmount(amount, true);
    }

    private void updateEstimateLabels(final PlatformResourceKey resourceKey, final FlowEstimate estimate) {
        if (estimate.equals(this.cachedEstimate) && resourceKey.equals(this.cachedEstimateResource)) {
            return;
        }
        this.cachedEstimate = estimate;
        this.cachedEstimateResource = resourceKey;
        this.estimateLabels = new String[]{
            this.formatEstimate(resourceKey, estimate, Granularity.SECOND),
            this.formatEstimate(resourceKey, estimate, Granularity.MINUTE),
            this.formatEstimate(resourceKey, estimate, Granularity.HOUR),
            this.formatEstimate(resourceKey, estimate, Granularity.DAY)
        };
    }

    private String formatEstimate(final PlatformResourceKey resourceKey, final FlowEstimate estimate, final Granularity target) {
        if (!estimate.available()) {
            return "—" + target.perStr();
        }
        final double amount = estimate.forTicks(target.getTickAmount());
        final double magnitude = Math.abs(amount);
        final String formatted;
        if (resourceKey instanceof ItemResource && magnitude < 1000) {
            formatted = FlowEstimate.formatNumber(magnitude);
        } else if (resourceKey instanceof FluidResource && magnitude < 1000) {
            formatted = FlowEstimate.formatNumber(resourceKey.getResourceType().getDisplayAmount(1) * magnitude) + " B";
        } else if (magnitude > 0 && magnitude < 1000 && magnitude != Math.rint(magnitude)) {
            formatted = FlowEstimate.formatNumber(magnitude) + " × " + this.formatAmount(resourceKey, 1);
        } else {
            formatted = this.formatAmount(resourceKey, Math.round(magnitude));
        }
        return (amount < 0 ? "-" : amount > 0 ? "+" : "") + formatted + target.perStr();
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
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        this.updateControlVisibility();
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void updateControlVisibility() {
        final boolean showListControls = !this.hasDetailedGenerationData;
        this.searchField.visible = showListControls;
        this.searchField.active = showListControls;
        if (!showListControls) {
            this.searchField.setFocused(false);
        }
        this.searchIcon.visible = showListControls;
        this.doneButton.visible = showListControls;
        if (this.scrollbar != null) {
            this.scrollbar.visible = showListControls;
        }
    }

    @Override
    protected ResourceLocation getTexture() {
        return this.hasDetailedGenerationData ? GRID_DETAIL : GRID;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);
        this.graph.lineStyle = this.getMenu().getLineStyle();
        if (this.hasDetailedGenerationData) {
            this.renderDetailedGenerationStats(graphics, mouseX, mouseY);
        } else {
            this.renderSimpleGenerationStats(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        if (!this.hasDetailedGenerationData) {
            graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        }
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        if (this.hasDetailedGenerationData) {
            this.graph.renderTooltip(graphics, mouseX, mouseY);
            final Component heading = createFlowAnalyticsTranslation("gui", "flow.estimates");
            if (!this.graph.isLoading() && this.isHovering(GRAPH_X + 160, GRAPH_BOTTOM + 25,
                this.font.width(heading), this.font.lineHeight, mouseX, mouseY)) {
                final FlowEstimate estimate = this.graph.getEstimate();
                graphics.renderTooltip(this.font, estimate.available()
                    ? createFlowAnalyticsTranslation("gui", "flow.estimates_window", estimate.observationWindow())
                    : createFlowAnalyticsTranslation("gui", "flow.estimates_empty"), mouseX, mouseY);
            }
        } else {
            final FlowItemButton hoveredButton = this.findHoveredItemButton(mouseX, mouseY);
            if (hoveredButton != null) {
                hoveredButton.renderTooltip(graphics, mouseX, mouseY);
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }
}
