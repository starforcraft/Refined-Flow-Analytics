package com.ultramega.refinedflowanalytics.screen.components;

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

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector2i;

public class FlowScopeGraph {
    private static final int MINECRAFT_STYLE_VERTICAL_RESOLUTION = 1;
    private static final int HEIGHT = 80;
    private static final int WIDTH = 200;

    private static final int PRODUCTION_GRAPH_COLOR = 0xff4b7f52;
    private static final int CONSUMPTION_GRAPH_COLOR = 0xffad343e;
    private static final int NET_GRAPH_COLOR = 0xff66ddff;
    private static final int GRAPH_GRADIENT_FROM = 0x00313f;
    private static final int GRAPH_GRADIENT_TO = 0x66ddff;

    public LineStyle lineStyle = LineStyle.BLOCKY;

    private int left = 0;
    private int bottom = 0;
    private long maxValue = 0;
    private long minValue = 0;
    private int pointsAmount = 0;

    private PlatformResourceKey itemKey;
    private Granularity granularity;
    private LocalDateTime dataTimeStamp;

    private long[] productionData;
    private long[] consumptionData;
    private long[] netData;
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
        this.netData = IntStream.range(0, this.productionData.length).mapToLong(i -> this.productionData[i] - this.consumptionData[i])
            .toArray();
        this.totalStored = Arrays
            .stream(data.getOrDefault(
                new ResourceChangeGranularityKey(this.itemKey, (short) 0, granularity.getTickAmount()), new long[0]))
            .findAny().orElse(0L);
        this.maxValue = Math.max(Arrays.stream(this.productionData).max().orElse(0L),
            Arrays.stream(this.consumptionData).max().orElse(0L));
        this.minValue = Math.max(Arrays.stream(this.productionData).min().orElse(0L),
            Arrays.stream(this.consumptionData).min().orElse(0L));
        this.pointsAmount = (int) Arrays.stream(this.productionData).count();

        this.dataTimeStamp = LocalDateTime.now();
        this.loading = false;
    }

    public void drawLine(final GuiGraphics guiGraphics,
                         final double x1,
                         final double y1,
                         final double x2,
                         final double y2,
                         final int color,
                         final int thickness,
                         final LineStyle lineStyle) {
        // I've decided to go with .fill(), because it gives more Minecraft'y result than nice and straight GL-rendered lines.
        if (lineStyle == LineStyle.BLOCKY) {
            final Vector2d p1 = new Vector2d(x1, Math.ceil(y1 / MINECRAFT_STYLE_VERTICAL_RESOLUTION) * MINECRAFT_STYLE_VERTICAL_RESOLUTION);
            final Vector2d p2 = new Vector2d(x2, Math.floor(y2 / MINECRAFT_STYLE_VERTICAL_RESOLUTION) * MINECRAFT_STYLE_VERTICAL_RESOLUTION);
            guiGraphics.fill((int) p1.x, (int) p1.y, (int) p2.x, (int) (p2.y == p1.y ? p2.y + MINECRAFT_STYLE_VERTICAL_RESOLUTION : p2.y), color);
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
            final Matrix4f matrix4f = guiGraphics.pose().last().pose();
            final VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
            points.forEach(p -> vc.addVertex(matrix4f, (float) p.x, (float) p.y, 0f).setColor(color));
            guiGraphics.flush();

            // debug
            // guiGraphics.fill((int) (x1 - ht + 1), (int) (y1 - ht + 1), (int) (x1 + ht -
            // 1), (int) (y1 + ht - 1), color);
        }
    }

    public void drawGradientBg(final GuiGraphics guiGraphics, final List<Vector2d> points, final int baseColor, final int targetColor) {
        final int baseR = ((baseColor & 0xff0000) >> 16);
        final int baseG = ((baseColor & 0x00ff00) >> 8);
        final int baseB = baseColor & 0x0000ff;
        final int targetR = ((targetColor & 0xff0000) >> 16) - baseR;
        final int targetG = ((targetColor & 0x00ff00) >> 8) - baseG;
        final int targetB = (targetColor & 0x0000ff) - baseB;
        final int maxIdx = points.size() - 1;
        IntStream.range(0, maxIdx).forEach(i -> {
            final Vector2d a = points.get(i);
            final Vector2d b = points.get(i + 1);

            final int color = 0xff000000 + baseColor
                + ((0x010000 * Math.round(((i + 1) * 1f / maxIdx) * targetR))
                + (0x000100 * Math.round(((i + 1) * 1f / maxIdx) * targetG))
                + (Math.round(((i + 1) * 1f / maxIdx) * targetB)));
            final int prevColor = 0xff000000 + baseColor
                + ((0x010000 * Math.round((i * 1f / maxIdx) * targetR))
                + (0x000100 * Math.round((i * 1f / maxIdx) * targetG))
                + (Math.round((i * 1f / maxIdx) * targetB)));

            final Matrix4f matrix4f = guiGraphics.pose().last().pose();
            final VertexConsumer vc = guiGraphics.bufferSource().getBuffer(RenderType.gui());
            vc.addVertex(matrix4f, (float) b.x, (float) b.y, 0f).setColor(color);
            vc.addVertex(matrix4f, (float) a.x, (float) a.y, 0f).setColor(prevColor);
            vc.addVertex(matrix4f, (float) a.x, (float) this.bottom, 0f).setColor(prevColor);
            vc.addVertex(matrix4f, (float) b.x, (float) this.bottom, 0f).setColor(color);
            guiGraphics.flush();
        });
    }

    public void drawGraph(final GuiGraphics guiGraphics, final List<Vector2d> points, final int thickness, final int color) {
        this.getPairStream(points)
            .forEach(p -> this.drawLine(guiGraphics, p.prev.x, p.prev.y, p.cur.x, p.cur.y, color, thickness, this.lineStyle));
    }

    public void drawGraph(final GuiGraphics guiGraphics, final long[] arr, final int thickness, final int color) {
        this.drawGraph(guiGraphics, this.getGuiXYArrayD(arr), thickness, color);
    }

    public void drawGraphs(final GuiGraphics graphics) {
        final List<Vector2d> netPoints = this.getGuiXYArrayD(this.netData);
        this.drawGradientBg(graphics, netPoints, GRAPH_GRADIENT_FROM, GRAPH_GRADIENT_TO);
        this.drawGraph(graphics, netPoints, 1, NET_GRAPH_COLOR);
        this.drawGraph(graphics, this.productionData, 1, PRODUCTION_GRAPH_COLOR);
        this.drawGraph(graphics, this.consumptionData, 1, CONSUMPTION_GRAPH_COLOR);
    }

    public ResourceRendering getResourceRendering() {
        return RefinedStorageClientApi.INSTANCE.getResourceRendering(this.itemKey.getClass());
    }

    public Component getItemName() {
        return this.getResourceRendering().getDisplayName(this.itemKey);
    }

    public void renderItem(final GuiGraphics guiGraphics, final int x, final int y) {
        this.getResourceRendering().render(this.itemKey, guiGraphics, x, y);
    }

    public Vector2i getGuiXYFromGraphValue(final Integer index, final Long value) {
        final int x = (int) ((WIDTH / Math.max(1, this.pointsAmount - 1f)) * index);
        if (this.maxValue - this.minValue == 0) {
            return new Vector2i(this.left + x, this.bottom - (HEIGHT / 2));
        }
        final int y = (int) ((Math.max(value, 0f) / this.maxValue) * HEIGHT);
        return new Vector2i(this.left + x, this.bottom - y);
    }

    public Vector2i getGraphValueFromGuiXY(final double x, final double y) {
        final int index = (int) Math.floor(((x - this.left) * ((this.pointsAmount - 1f) / WIDTH)));
        final int value = (int) Math.max(0, this.maxValue * (this.bottom - y) / HEIGHT);
        return new Vector2i(index, value);
    }

    public List<Vector2i> getGuiXYArray(final long[] arr) {
        return IntStream.range(0, arr.length)
            .mapToObj(i -> this.getGuiXYFromGraphValue(i, arr[i]))
            .toList();
    }

    public List<Vector2d> getGuiXYArrayD(final long[] arr) {
        return this.getGuiXYArray(arr).stream().map(v -> new Vector2d(v.x, v.y)).toList();
    }

    public Long getMaxProduction() {
        return Arrays.stream(this.productionData).max().orElse(0L);
    }

    public Long getMinProduction() {
        return Arrays.stream(this.productionData).min().orElse(0L);
    }

    public double getAvgProduction() {
        return Arrays.stream(this.productionData).average().orElse(0.0);
    }

    public Long getMaxConsumption() {
        return Arrays.stream(this.consumptionData).max().orElse(0L);
    }

    public Long getMinConsumption() {
        return Arrays.stream(this.consumptionData).min().orElse(0L);
    }

    public double getAvgConsumption() {
        return Arrays.stream(this.consumptionData).average().orElse(0.0);
    }

    public double getNetAvg() {
        return Arrays.stream(this.netData).average().orElse(0.0);
    }

    public boolean isInBounds(final int mouseX, final int mouseY) {
        return mouseY <= this.bottom && mouseY >= this.bottom - HEIGHT && mouseX >= this.left && mouseX <= this.left + WIDTH;
    }

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            if (this.isInBounds((int) mouseX, (int) mouseY)) {
                this.selectedIndex = this.getGraphValueFromGuiXY(mouseX, mouseY).x;
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            this.selectedIndex = -1;
            return true;
        }
        return false;
    }

    public void renderTooltip(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        if (!this.isInBounds(mouseX, mouseY) && (this.selectedIndex == -1)) {
            return;
        }
        final int index = this.getGraphValueFromGuiXY(mouseX, mouseY).x;
        if ((index < 0 || index >= this.pointsAmount) && this.selectedIndex == -1) {
            return;
        }

        final int indexFrom = this.selectedIndex != -1 ? Math.clamp(index, 0, this.selectedIndex) : index;
        final int indexTo = this.selectedIndex != -1 ? Math.max(Math.min(index, this.pointsAmount - 1) + 1, this.selectedIndex) : index + 1;

        final int selectionX1 = this.getGuiXYFromGraphValue(indexFrom, 0L).x;
        final int selectionX2 = this.getGuiXYFromGraphValue(indexTo, 0L).x;
        guiGraphics.fill(selectionX1, this.bottom, selectionX2, this.bottom - HEIGHT, 250, 0x66ffffff);

        final LocalDateTime indexDate = this.dataTimeStamp.minus(this.pointsAmount - indexFrom, this.granularity.getChronoUnit());
        final LocalDateTime nextIndexDate = this.dataTimeStamp.minus(this.pointsAmount - (indexTo), this.granularity.getChronoUnit());

        final long incvalue = Math.max(0, Arrays.stream(this.productionData).skip(indexFrom).limit(indexTo - indexFrom).sum());
        final long decvalue = Math.abs(Arrays.stream(this.consumptionData).skip(indexFrom).limit(indexTo - indexFrom).sum());

        final ResourceRendering resourceRendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(this.itemKey.getClass());
        final List<ClientTooltipComponent> lines = new ArrayList<>();

        lines.add(new ClientTextTooltip(resourceRendering.getDisplayName(this.itemKey).getVisualOrderText()));
        lines.add(new SmallTextClientTooltipComponent(Component.literal(
            "Flow between "
                + indexDate.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                + " and "
                + nextIndexDate.format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
        lines.add(new SmallTextClientTooltipComponent(
            Component.literal(String.format("Inflow:  +%,d", incvalue)).withColor(0xff00ff00)));
        lines.add(new SmallTextClientTooltipComponent(
            Component.literal(String.format("Outflow: -%,d", decvalue)).withColor(0xffff0000)));
        lines.add(new SmallTextClientTooltipComponent(
            Component.literal(String.format("Netflow: %,d", incvalue - decvalue)).withColor(0xff66ddff)));
        Platform.INSTANCE.renderTooltip(guiGraphics, lines, mouseX, mouseY);
    }

    private Stream<PointPair> getPairStream(final List<Vector2d> points) {
        return IntStream.range(0, points.size())
            .mapToObj(i -> new PointPair(i == 0 ? points.get(i) : points.get(i - 1), points.get(i)));
    }

    private record PointPair(Vector2d prev, Vector2d cur) {
    }
}
