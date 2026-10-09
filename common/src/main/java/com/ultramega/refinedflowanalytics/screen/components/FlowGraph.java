package com.ultramega.refinedflowanalytics.screen.components;

import com.ultramega.refinedflowanalytics.data.FlowEstimate;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.Granularity;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;

import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering;
import com.refinedmods.refinedstorage.common.support.tooltip.SmallTextClientTooltipComponent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector2i;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowGraph {
    public static final int PRODUCTION_GRAPH_COLOR = 0xff63cf78;
    public static final int CONSUMPTION_GRAPH_COLOR = 0xffef6b73;
    public static final int NET_GRAPH_COLOR = 0xff66ddff;
    private static final double NET_DASH_LENGTH = 5;
    private static final double NET_DASH_GAP = 3;
    private static final int PRODUCTION_AVERAGE_COLOR = 0xff427a49;
    private static final int CONSUMPTION_AVERAGE_COLOR = 0xff9f464e;
    private static final int ZERO_LINE_COLOR = 0xffa0a0a0;
    private static final int VERTICAL_PADDING = 3;

    private static final int HEIGHT = 95;
    private static final int WIDTH = 200;

    public LineStyle lineStyle = LineStyle.BLOCKY;

    private int left = 0;
    private int bottom = 0;
    private long maxValue = 1;
    private int pointsAmount = 0;
    private int minimumVisibleSamples = 0;

    private PlatformResourceKey itemKey;
    private Granularity granularity;
    private LocalDateTime dataTimeStamp;

    private long[] productionData = new long[0];
    private long[] consumptionData = new long[0];
    private long[] netData = new long[0];
    private FlowEstimate estimate = FlowEstimate.EMPTY;
    private Long totalStored;

    private int selectedIndex = -1;

    private boolean loading = true;

    public Vector2i getGraphSize() {
        return new Vector2i(WIDTH, HEIGHT);
    }

    public PlatformResourceKey getItemKey() {
        return this.itemKey;
    }

    public long[] getProductionData() {
        return this.productionData;
    }

    public long[] getConsumptionData() {
        return this.consumptionData;
    }

    public long[] getNetData() {
        return this.netData;
    }

    public Long getTotalStored() {
        return this.totalStored;
    }

    public boolean isLoading() {
        return this.loading;
    }

    public void setLoading(final boolean loading) {
        this.loading = loading;
        if (loading) {
            this.selectedIndex = -1;
        }
    }

    public void beginLoading(final PlatformResourceKey resource, final Granularity granularity) {
        this.itemKey = resource;
        this.granularity = granularity;
        this.setLoading(true);
    }

    public void setGraphPos(final int graphLeft, final int graphBottom) {
        this.left = graphLeft;
        this.bottom = graphBottom;
    }

    public void setData(final Map<ResourceChangeGranularityKey, long[]> data, final Granularity granularity) {
        final ResourceChangeGranularityKey rgk = data.keySet().stream().findFirst().orElse(null);
        if (rgk == null) {
            return;
        }
        this.itemKey = rgk.resourceKey();
        this.granularity = granularity;
        this.productionData = data.getOrDefault(
            new ResourceChangeGranularityKey(this.itemKey, (short) +1, granularity.getTickAmount()), new long[0]);
        this.consumptionData = data.getOrDefault(
            new ResourceChangeGranularityKey(this.itemKey, (short) -1, granularity.getTickAmount()), new long[0]);
        // Both streams share the same time buckets; an absent value means no flow.
        this.pointsAmount = Math.max(this.productionData.length, this.consumptionData.length);
        this.productionData = Arrays.copyOf(this.productionData, this.pointsAmount);
        this.consumptionData = Arrays.copyOf(this.consumptionData, this.pointsAmount);
        this.netData = IntStream.range(0, this.pointsAmount).mapToLong(i -> this.productionData[i] - this.consumptionData[i])
            .toArray();
        final long[] duration = data.getOrDefault(new ResourceChangeGranularityKey(this.itemKey, FlowEstimate.DURATION_SIGN, granularity.getTickAmount()), new long[0]);
        this.estimate = FlowEstimate.from(this.productionData, this.consumptionData, duration.length == 1 ? duration[0] : 0);
        this.totalStored = Arrays
            .stream(data.getOrDefault(
                new ResourceChangeGranularityKey(this.itemKey, (short) 0, granularity.getTickAmount()), new long[0]))
            .findAny().orElse(0L);
        // Equal positive and negative limits keep zero centered, even for one-sided flow.
        this.maxValue = Math.max(1L, Math.max(this.getMaxProduction(), this.getMaxConsumption()));
        if (this.selectedIndex >= this.pointsAmount) {
            this.selectedIndex = -1;
        }

        this.dataTimeStamp = LocalDateTime.now();
        this.loading = false;
    }

    public void drawLine(final GuiGraphics graphics,
                         final double x1,
                         final double y1,
                         final double x2,
                         final double y2,
                         final int color,
                         final int thickness,
                         final LineStyle lineStyle) {
        final double half = thickness / 2.0;
        if (x1 == x2 && y1 == y2) {
            // Both styles center isolated samples on the same coordinates.
            final int x = (int) Math.round(x1 - half);
            final int y = (int) Math.round(y1 - half);
            graphics.fill(x, y, x + thickness, y + thickness, color);
            return;
        }
        if (lineStyle == LineStyle.BLOCKY) {
            // Round the stroke's edges, not its center, to match the exact/reference lines.
            final int startX = (int) Math.round(x1 - half);
            final int endX = (int) Math.round(x2 - half);
            final int startY = (int) Math.round(y1 - half);
            final int endY = (int) Math.round(y2 - half);
            // Hold the previous sample, then step vertically to the next one.
            graphics.fill(Math.min(startX, endX), startY, Math.max(startX, endX) + thickness, startY + thickness, color);
            graphics.fill(endX, Math.min(startY, endY), endX + thickness, Math.max(startY, endY) + thickness, color);
        } else if (lineStyle == LineStyle.EXACT) {
            final double ht = thickness / 2f;
            final double rads = Math.atan2(y2 - y1, x2 - x1);
            final double tx = Math.sin(rads) * ht;
            final double ty = -Math.cos(rads) * ht;
            final List<Vector2d> points = List.of(
                new Vector2d((float) (x1 + tx), (float) (y1 + ty)),
                new Vector2d((float) (x1 - tx), (float) (y1 - ty)),
                new Vector2d((float) (x2 - tx), (float) (y2 - ty)),
                new Vector2d((float) (x2 + tx), (float) (y2 + ty)));
            final Matrix4f matrix4f = graphics.pose().last().pose();
            final VertexConsumer vc = graphics.bufferSource().getBuffer(RenderType.gui());
            points.forEach(p -> vc.addVertex(matrix4f, (float) p.x, (float) p.y, 0f).setColor(color));
            graphics.flush();
        }
    }

    private LineDrawer guiLines(final GuiGraphics graphics) {
        return (x1, y1, x2, y2, color, thickness, style) -> this.drawLine(graphics, x1, y1, x2, y2, color, thickness, style);
    }

    public void drawGraph(final LineDrawer lines, final List<Vector2d> points, final int thickness, final int color) {
        this.getPairStream(points).forEach(p -> lines.draw(p.prev.x, p.prev.y, p.cur.x, p.cur.y, color, thickness, this.lineStyle));
    }

    public void drawGraph(final LineDrawer lines, final long[] arr, final int thickness, final int color) {
        this.drawGraph(lines, this.getGuiXYArrayD(arr), thickness, color);
    }

    public void drawGraphs(final GuiGraphics graphics) {
        this.drawGraphs(this.guiLines(graphics));
    }

    public void drawGraphs(final LineDrawer lines) {
        if (this.pointsAmount > 0) {
            this.drawReferenceLine(lines, this.getAvgProduction(), PRODUCTION_AVERAGE_COLOR);
            this.drawReferenceLine(lines, -this.getAvgConsumption(), CONSUMPTION_AVERAGE_COLOR);
        }
        this.drawReferenceLine(lines, 0, ZERO_LINE_COLOR);
        this.drawGraph(lines, this.productionData, 1, PRODUCTION_GRAPH_COLOR);
        this.drawGraph(lines, IntStream.range(0, this.consumptionData.length)
            .mapToObj(i -> new Vector2d(this.getGuiX(i), this.getGuiY(-((double) this.consumptionData[i]))))
            .toList(), 1, CONSUMPTION_GRAPH_COLOR);
        this.drawNetGraph(lines);
    }

    private void drawNetGraph(final LineDrawer lines) {
        final List<Vector2d> samples = this.getGuiXYArrayD(this.netData);
        if (samples.size() <= 1) {
            this.drawGraph(lines, samples, 1, NET_GRAPH_COLOR);
            return;
        }
        final List<Vector2d> path = this.createGraphPath(samples);
        final List<Vector2d> inflow = this.createGraphPath(this.getGuiXYArrayD(this.productionData));
        final List<Vector2d> outflow = this.createGraphPath(IntStream.range(0, this.consumptionData.length)
            .mapToObj(i -> new Vector2d(this.getGuiX(i), this.getGuiY(-((double) this.consumptionData[i]))))
            .toList());

        // Carry the dash phase across samples and step corners, including dense data.
        boolean visible = true;
        double remaining = NET_DASH_LENGTH;
        for (int i = 1; i < path.size(); i++) {
            final Vector2d start = path.get(i - 1);
            final Vector2d end = path.get(i);
            final double dx = end.x - start.x;
            final double dy = end.y - start.y;
            final double length = Math.hypot(dx, dy);
            final double[] inflowOverlap = getOverlapRange(start, end, inflow.get(i - 1), inflow.get(i));
            final double[] outflowOverlap = getOverlapRange(start, end, outflow.get(i - 1), outflow.get(i));
            final List<Double> boundaries = new ArrayList<>(List.of(0.0, 1.0));
            for (final double[] overlap : new double[][]{inflowOverlap, outflowOverlap}) {
                if (overlap != null) {
                    boundaries.add(overlap[0]);
                    boundaries.add(overlap[1]);
                }
            }
            boundaries.sort(Double::compare);
            for (int part = 1; part < boundaries.size(); part++) {
                final double midpoint = (boundaries.get(part - 1) + boundaries.get(part)) / 2;
                final boolean overlapping = contains(inflowOverlap, midpoint) || contains(outflowOverlap, midpoint);
                double position = boundaries.get(part - 1) * length;
                final double limit = boundaries.get(part) * length;
                while (position < limit - 1.0e-6) {
                    final double amount = Math.min(remaining, limit - position);
                    if (visible || !overlapping) {
                        lines.draw(
                            start.x + dx * position / length, start.y + dy * position / length,
                            start.x + dx * (position + amount) / length, start.y + dy * (position + amount) / length,
                            NET_GRAPH_COLOR, 1, this.lineStyle
                        );
                    }
                    position += amount;
                    remaining -= amount;
                    if (remaining < 1.0e-6) {
                        visible = !visible;
                        remaining = visible ? NET_DASH_LENGTH : NET_DASH_GAP;
                    }
                }
            }
        }
    }

    private List<Vector2d> createGraphPath(final List<Vector2d> samples) {
        final List<Vector2d> path = new ArrayList<>();
        path.add(samples.getFirst());
        for (int i = 1; i < samples.size(); i++) {
            if (this.lineStyle == LineStyle.BLOCKY) {
                path.add(new Vector2d(samples.get(i).x, samples.get(i - 1).y));
            }
            path.add(samples.get(i));
        }
        return path;
    }

    private static boolean contains(final @Nullable double[] range, final double value) {
        return range != null && value >= range[0] && value <= range[1];
    }

    private static @Nullable double[] getOverlapRange(final Vector2d start,
                                                       final Vector2d end,
                                                       final Vector2d flowStart,
                                                       final Vector2d flowEnd) {
        // Compare rendered positions so flows sharing a pixel also count as overlapping
        final double tolerance = 0.75;
        final boolean vertical = Math.abs(end.x - start.x) < 1.0e-6;
        final double from = vertical ? start.y : start.y - flowStart.y;
        final double to = vertical ? end.y : end.y - flowEnd.y;
        final double lower = vertical ? Math.min(flowStart.y, flowEnd.y) - tolerance : -tolerance;
        final double upper = vertical ? Math.max(flowStart.y, flowEnd.y) + tolerance : tolerance;
        final double delta = to - from;
        if (Math.abs(delta) < 1.0e-6) {
            return from >= lower && from <= upper ? new double[]{0, 1} : null;
        }
        final double first = (lower - from) / delta;
        final double last = (upper - from) / delta;
        final double rangeStart = Math.clamp(Math.min(first, last), 0.0, 1.0);
        final double rangeEnd = Math.clamp(Math.max(first, last), 0.0, 1.0);
        return rangeStart < rangeEnd ? new double[]{rangeStart, rangeEnd} : null;
    }

    private void drawReferenceLine(final LineDrawer lines, final double value, final int color) {
        final double y = this.getGuiY(value);
        lines.draw(this.left, y, this.left + WIDTH, y, color, 1, LineStyle.EXACT);
    }

    public ResourceRendering getResourceRendering() {
        return RefinedStorageClientApi.INSTANCE.getResourceRendering(this.itemKey.getClass());
    }

    public Component getItemName() {
        return this.getResourceRendering().getDisplayName(this.itemKey);
    }

    public void renderItem(final GuiGraphics graphics, final int x, final int y) {
        this.getResourceRendering().render(this.itemKey, graphics, x, y);
    }

    public void setMinimumVisibleSamples(final int samples) {
        if (samples < 0) {
            throw new IllegalArgumentException("Visible sample count must not be negative");
        }
        this.minimumVisibleSamples = samples;
    }

    private double getGuiX(final double index) {
        final int visibleSamples = Math.max(this.pointsAmount, this.minimumVisibleSamples);
        final double visibleIndex = index + visibleSamples - this.pointsAmount;
        return this.left + (visibleSamples <= 1 ? WIDTH / 2.0 : WIDTH * visibleIndex / (visibleSamples - 1.0));
    }

    public double getGuiY(final double value) {
        final double halfHeight = HEIGHT / 2.0;
        return this.bottom - halfHeight - (value / this.maxValue) * (halfHeight - VERTICAL_PADDING);
    }

    private int getSampleIndex(final double mouseX) {
        if (this.pointsAmount <= 1) {
            return 0;
        }
        final int visibleSamples = Math.max(this.pointsAmount, this.minimumVisibleSamples);
        final double index = (mouseX - this.left) / WIDTH * (visibleSamples - 1)
            - (visibleSamples - this.pointsAmount);
        return (int) Math.round(Math.clamp(index, 0.0, this.pointsAmount - 1.0));
    }

    public List<Vector2d> getGuiXYArrayD(final long[] arr) {
        return IntStream.range(0, arr.length)
            .mapToObj(i -> new Vector2d(this.getGuiX(i), this.getGuiY(arr[i])))
            .toList();
    }

    public Long getMaxProduction() {
        return Arrays.stream(this.productionData).max().orElse(0L);
    }

    public Long getMinProduction() {
        return Arrays.stream(this.productionData).min().orElse(0L);
    }

    public double getAvgProduction() {
        return Arrays.stream(this.productionData).mapToDouble(value -> value).average().orElse(0.0);
    }

    public Long getMaxConsumption() {
        return Arrays.stream(this.consumptionData).max().orElse(0L);
    }

    public Long getMinConsumption() {
        return Arrays.stream(this.consumptionData).min().orElse(0L);
    }

    public double getAvgConsumption() {
        return Arrays.stream(this.consumptionData).mapToDouble(value -> value).average().orElse(0.0);
    }

    public double getNetAvg() {
        return Arrays.stream(this.netData).mapToDouble(value -> value).average().orElse(0.0);
    }

    public FlowEstimate getEstimate() {
        return this.estimate;
    }

    public boolean isInBounds(final int mouseX, final int mouseY) {
        return mouseY <= this.bottom && mouseY >= this.bottom - HEIGHT && mouseX >= this.left && mouseX <= this.left + WIDTH;
    }

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.loading || this.pointsAmount == 0) {
            return false;
        }
        if (button == 0) {
            if (this.isInBounds((int) mouseX, (int) mouseY)) {
                this.selectedIndex = this.getSampleIndex(mouseX);
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && this.selectedIndex != -1) {
            this.selectedIndex = -1;
            return true;
        }
        return false;
    }

    public void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        if (this.loading || this.pointsAmount == 0) {
            return;
        }
        if (!this.isInBounds(mouseX, mouseY) && (this.selectedIndex == -1)) {
            return;
        }
        final int index = this.getSampleIndex(mouseX);
        final int indexFrom = this.selectedIndex == -1 ? index : Math.min(index, this.selectedIndex);
        final int indexTo = (this.selectedIndex == -1 ? index : Math.max(index, this.selectedIndex)) + 1;

        // Highlight complete sample cells, including both endpoints when dragging in either direction.
        final int selectionX1 = indexFrom == 0 ? this.left : (int) Math.round(this.getGuiX(indexFrom - 0.5));
        final int selectionX2 = indexTo == this.pointsAmount ? this.left + WIDTH : (int) Math.round(this.getGuiX(indexTo - 0.5));
        graphics.fill(selectionX1, this.bottom - HEIGHT, selectionX2, this.bottom, 250, 0x22ffffff);

        final LocalDateTime indexDate = this.granularity.subtractSamples(this.dataTimeStamp, this.pointsAmount - indexFrom);
        final LocalDateTime nextIndexDate = this.granularity.subtractSamples(this.dataTimeStamp, this.pointsAmount - indexTo);
        final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern(this.granularity == Granularity.TICK ? "HH:mm:ss.SSS" : "HH:mm:ss");

        final long incvalue = Math.max(0, Arrays.stream(this.productionData).skip(indexFrom).limit(indexTo - indexFrom).sum());
        final long decvalue = Math.abs(Arrays.stream(this.consumptionData).skip(indexFrom).limit(indexTo - indexFrom).sum());

        final ResourceRendering resourceRendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(this.itemKey.getClass());
        final List<ClientTooltipComponent> lines = new ArrayList<>();

        lines.add(new ClientTextTooltip(resourceRendering.getDisplayName(this.itemKey).getVisualOrderText()));
        lines.add(new SmallTextClientTooltipComponent(createFlowAnalyticsTranslation("gui", "flow.flow_between",
            indexDate.format(timeFormat),
            nextIndexDate.format(timeFormat))));
        lines.add(new SmallTextClientTooltipComponent(
            createFlowAnalyticsTranslation("gui", "flow.inflow_value", String.format("%,d", incvalue)).withColor(0xff00ff00)));
        lines.add(new SmallTextClientTooltipComponent(
            createFlowAnalyticsTranslation("gui", "flow.outflow_value", String.format("%,d", decvalue)).withColor(0xffff0000)));
        lines.add(new SmallTextClientTooltipComponent(
            createFlowAnalyticsTranslation("gui", "flow.netflow_value", String.format("%,d", incvalue - decvalue)).withColor(0xff66ddff)));
        Platform.INSTANCE.renderTooltip(graphics, lines, mouseX, mouseY);
    }

    private Stream<PointPair> getPairStream(final List<Vector2d> points) {
        if (points.size() == 1) {
            return Stream.of(new PointPair(points.getFirst(), points.getFirst()));
        }
        // Segments already include their endpoints; do not add a separate first-point square.
        return IntStream.range(1, points.size())
            .mapToObj(i -> new PointPair(points.get(i - 1), points.get(i)));
    }

    private record PointPair(Vector2d prev, Vector2d cur) {
    }

    @FunctionalInterface
    public interface LineDrawer {
        void draw(double x1, double y1, double x2, double y2, int color, int thickness, LineStyle style);
    }
}
