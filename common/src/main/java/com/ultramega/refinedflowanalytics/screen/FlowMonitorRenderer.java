package com.ultramega.refinedflowanalytics.screen;

import com.ultramega.refinedflowanalytics.block.entity.FlowMonitorBlockEntity;
import com.ultramega.refinedflowanalytics.network.FlowHistoryNetworkComponent;
import com.ultramega.refinedflowanalytics.resource.ResourceChangeGranularityKey;
import com.ultramega.refinedflowanalytics.screen.components.FlowGraph;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.LineStyle;
import com.ultramega.refinedflowanalytics.screen.sidebuttons.MonitorItemVisibility;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.direction.OrientedDirection;
import com.refinedmods.refinedstorage.common.support.direction.OrientedDirectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import static com.ultramega.refinedflowanalytics.util.RefinedFlowAnalyticsIdentifierUtil.createFlowAnalyticsTranslation;

public class FlowMonitorRenderer implements BlockEntityRenderer<FlowMonitorBlockEntity, FlowMonitorRenderer.RenderState> {
    private static final float GRAPH_WIDTH = 200;
    private static final float GRAPH_HEIGHT = 95;
    private static final float MIN_LINE_PIXELS = 1.25f;
    private static final float LAYER_DEPTH = 0.0005f;
    private static final Quaternionf ROTATE_TO_FRONT = new Quaternionf().rotationY((float) Math.PI);
    private final Map<FlowMonitorBlockEntity, Display> displays = new WeakHashMap<>();

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(final FlowMonitorBlockEntity monitor, final RenderState state,
                                   final float partialTicks, final Vec3 cameraPosition,
                                   final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(monitor, state, partialTicks, cameraPosition, breakProgress);
        state.active = monitor.getLevel() != null && monitor.isDisplayActive();
        state.resource = monitor.getConfiguredResource();
        state.direction = monitor.getBlockState().getValue(OrientedDirectionType.INSTANCE.getProperty());
        state.itemVisibility = monitor.getItemVisibility();
        state.seed = monitor.getBlockPos().asLong();
        final PlatformResourceKey resource = state.resource;
        if (!state.active || resource == null) {
            state.display = null;
            return;
        }
        final LineStyle style = monitor.getLineStyle();
        Display display = this.displays.get(monitor);
        if (display == null || display.revision() != monitor.getDisplayRevision() || display.style() != style) {
            display = this.createDisplay(monitor, resource, style);
            this.displays.put(monitor, display);
        }

        state.display = display;
        final var rendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(resource.getClass());
        final long value = monitor.getFlowText().getValue(display.inflow(), display.outflow());
        state.caption = createFlowAnalyticsTranslation("gui", "flow_monitor." + monitor.getFlowText().name().toLowerCase(java.util.Locale.ROOT),
            rendering.formatAmount(Math.abs(value)), value > 0 ? "+" : value < 0 ? "-" : "", monitor.getGranularity().perStr()).getString();
        state.captionColor = monitor.getFlowText().getColor();
    }

    @Override
    public void submit(final RenderState state, final PoseStack pose, final SubmitNodeCollector nodes,
                       final CameraRenderState camera) {
        final PlatformResourceKey resource = state.resource;
        final Display display = state.display;
        if (!state.active || resource == null || display == null || state.direction == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(state.direction.getQuaternion());
        pose.mulPose(ROTATE_TO_FRONT);
        pose.translate(0, 0, 0.502);
        pose.pushPose();
        pose.translate(-0.375, 0.14, 0);
        pose.scale(0.75f / GRAPH_WIDTH, -0.0033f, 1);
        final float strokeScale = getStrokeScale(pose.last().pose(), camera);
        nodes.submitCustomGeometry(pose, RenderTypes.debugQuads(), (submittedPose, vertices) -> {
            for (final Quad quad : display.quads()) {
                quad.render(vertices, submittedPose.pose(), strokeScale);
            }
        });
        final Font font = Minecraft.getInstance().font;
        nodes.submitText(pose, -6, 44, Component.literal("0").getVisualOrderText(), false,
            Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, 0xffa0a0a0, 0, 0);
        pose.popPose();

        final var rendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(resource.getClass());
        if (state.itemVisibility == MonitorItemVisibility.SHOW) {
            pose.pushPose();
            pose.translate(0, 0.26, 0.01);
            pose.scale(0.5f, 0.5f, 0.5f);
            rendering.render(resource, pose, nodes, LightCoordsUtil.FULL_BRIGHT, state.seed);
            pose.popPose();
        }

        final String caption = state.caption;
        final int width = font.width(caption);
        final float scale = Math.min(0.0075f, 0.66f / Math.max(1, width));
        pose.pushPose();
        pose.translate(0, -0.255, 0.003);
        pose.scale(scale, -scale, scale);
        nodes.submitText(pose, -width / 2f, 0, Component.literal(caption).getVisualOrderText(), false,
            Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, state.captionColor, 0, 0);
        pose.popPose();
        pose.popPose();
    }

    private Display createDisplay(final FlowMonitorBlockEntity monitor, final PlatformResourceKey resource, final LineStyle style) {
        final long[] inflow = monitor.getDisplayInflow();
        final long[] outflow = monitor.getDisplayOutflow();
        final FlowGraph graph = new FlowGraph();
        graph.setMinimumVisibleSamples(FlowHistoryNetworkComponent.MONITOR_SAMPLES);
        graph.lineStyle = style;
        graph.setGraphPos(0, (int) GRAPH_HEIGHT);
        graph.setData(Map.of(
            new ResourceChangeGranularityKey(resource, (short) 1, monitor.getGranularity().getTickAmount()), inflow,
            new ResourceChangeGranularityKey(resource, (short) -1, monitor.getGranularity().getTickAmount()), outflow
        ), monitor.getGranularity());
        final List<Quad> quads = new ArrayList<>();
        graph.drawGraphs((x1, y1, x2, y2, color, thickness, lineStyle) -> this.line(quads, x1, y1, x2, y2, color, thickness, lineStyle));
        return new Display(monitor.getDisplayRevision(), style, List.copyOf(quads),
            inflow.length == 0 ? 0 : inflow[inflow.length - 1],
            outflow.length == 0 ? 0 : outflow[outflow.length - 1]);
    }

    private void line(final List<Quad> quads,
                      final double x1,
                      final double y1,
                      final double x2,
                      final double y2,
                      final int color,
                      final int thickness,
                      final LineStyle style) {
        final Quad previous = quads.isEmpty() ? null : quads.getLast();
        final float z = previous == null ? LAYER_DEPTH : previous.z() + (previous.color() == color ? 0 : LAYER_DEPTH);
        final double half = thickness / 2.0;
        if (style == LineStyle.BLOCKY) {
            // Keep the final vertical step visible inside the last column of the panel
            final float sx = Math.clamp((float) Math.round(x1 - half), 0, GRAPH_WIDTH - thickness);
            final float ex = Math.clamp((float) Math.round(x2 - half), 0, GRAPH_WIDTH - thickness);
            final float sy = Math.round(y1 - half);
            final float ey = Math.round(y2 - half);
            quads.add(Quad.rectangle(Math.min(sx, ex), sy, Math.max(sx, ex) + thickness, sy + thickness, z, color, false));
            quads.add(Quad.rectangle(ex, Math.min(sy, ey), ex + thickness, Math.max(sy, ey) + thickness, z, color, true));
            return;
        }
        final double length = Math.hypot(x2 - x1, y2 - y1);
        if (length < 1.0e-6) {
            quads.add(Quad.rectangle((float) (x1 - half), (float) (y1 - half), (float) (x1 + half), (float) (y1 + half), z, color, false));
            return;
        }
        final double tx = (y2 - y1) / length * half;
        final double ty = -(x2 - x1) / length * half;
        quads.add(new Quad((float) (x1 + tx), (float) (y1 + ty), (float) (x1 - tx), (float) (y1 - ty),
            (float) (x2 - tx), (float) (y2 - ty), (float) (x2 + tx), (float) (y2 + ty), (float) tx, (float) ty, z, color));
    }

    private static float getStrokeScale(final Matrix4f model, final CameraRenderState camera) {
        final Matrix4f projection = new Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix).mul(model);
        final Vector3f center = projection.transformProject(new Vector3f(GRAPH_WIDTH / 2, GRAPH_HEIGHT / 2, 0));
        final Vector3f x = projection.transformProject(new Vector3f(GRAPH_WIDTH / 2 + 1, GRAPH_HEIGHT / 2, 0));
        final Vector3f y = projection.transformProject(new Vector3f(GRAPH_WIDTH / 2, GRAPH_HEIGHT / 2 + 1, 0));
        final var target = Minecraft.getInstance().getMainRenderTarget();
        final double xx = (x.x - center.x) * target.width / 2;
        final double xy = (x.y - center.y) * target.height / 2;
        final double yx = (y.x - center.x) * target.width / 2;
        final double yy = (y.y - center.y) * target.height / 2;
        // The smallest singular value gives pixels per graph unit along the most foreshortened axis
        final double determinant = xx * yy - xy * yx;
        final double squaredLength = xx * xx + xy * xy + yx * yx + yy * yy;
        final double largest = Math.sqrt((squaredLength + Math.sqrt(Math.max(0, squaredLength * squaredLength - 4 * determinant * determinant))) / 2);
        final double pixels = Math.abs(determinant) / largest;
        if (!Double.isFinite(pixels) || pixels < 1.0e-6) {
            return 1;
        }
        return (float) Math.max(1, MIN_LINE_PIXELS / pixels);
    }

    public static final class RenderState extends BlockEntityRenderState {
        private boolean active;
        @Nullable
        private PlatformResourceKey resource;
        @Nullable
        private OrientedDirection direction;
        @Nullable
        private Display display;
        private MonitorItemVisibility itemVisibility = MonitorItemVisibility.SHOW;
        private String caption = "";
        private int captionColor;
        private long seed;
    }

    private record Display(long revision, LineStyle style, List<Quad> quads, long inflow, long outflow) {
    }

    private record Quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4,
                        float halfWidthX, float halfWidthY, float z, int color) {
        private static Quad rectangle(final float left,
                                      final float top,
                                      final float right,
                                      final float bottom,
                                      final float z,
                                      final int color,
                                      final boolean vertical) {
            if (vertical) {
                return new Quad(left, bottom, right, bottom, right, top, left, top, (left - right) / 2, 0, z, color);
            }
            return new Quad(left, top, left, bottom, right, bottom, right, top, 0, (top - bottom) / 2, z, color);
        }

        private void render(final VertexConsumer vertices, final Matrix4f matrix, final float strokeScale) {
            final float dx = this.halfWidthX * (strokeScale - 1);
            final float dy = this.halfWidthY * (strokeScale - 1);
            this.vertex(vertices, matrix, this.x1 + dx, this.y1 + dy);
            this.vertex(vertices, matrix, this.x2 - dx, this.y2 - dy);
            this.vertex(vertices, matrix, this.x3 - dx, this.y3 - dy);
            this.vertex(vertices, matrix, this.x4 + dx, this.y4 + dy);
        }

        private void vertex(final VertexConsumer vertices, final Matrix4f matrix, final float x, final float y) {
            vertices.addVertex(matrix, Math.clamp(x, 0, GRAPH_WIDTH), Math.clamp(y, 0, GRAPH_HEIGHT), this.z)
                .setColor(this.color);
        }
    }
}
