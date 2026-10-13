package dev.kardipopo.rickportalgun.client;

import dev.kardipopo.rickportalgun.PortalMarker;
import dev.kardipopo.rickportalgun.RickPortalGun;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class PortalVisualRenderer {
    private static final Identifier PORTAL_TEXTURE =
            Identifier.of(RickPortalGun.MOD_ID, "textures/misc/portal_green.png");
    private static final RenderLayer LAYER = RenderLayers.entityTranslucentEmissive(PORTAL_TEXTURE);

    private PortalVisualRenderer() {}

    public static void initialize() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(PortalVisualRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || context.matrices() == null || context.consumers() == null) return;

        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
        Box area = new Box(
                cameraPos.x - 96.0, cameraPos.y - 96.0, cameraPos.z - 96.0,
                cameraPos.x + 96.0, cameraPos.y + 96.0, cameraPos.z + 96.0
        );
        float tickDelta = client.getRenderTickCounter().getTickProgress(false);
        MatrixStack matrices = context.matrices();
        VertexConsumer consumer = context.consumers().getBuffer(LAYER);

        for (ArmorStandEntity stand : client.world.getEntitiesByType(
                EntityType.ARMOR_STAND, area, PortalMarker::is)) {
            PortalMarker marker = PortalMarker.read(stand);
            if (marker == null) continue;

            Vec3d normal = Vec3d.of(marker.normal().getVector()).normalize();
            Vec3d up;
            Vec3d right;
            if (marker.normal() == Direction.UP || marker.normal() == Direction.DOWN) {
                right = new Vec3d(1, 0, 0);
                up = marker.normal() == Direction.UP ? new Vec3d(0, 0, -1) : new Vec3d(0, 0, 1);
            } else {
                up = new Vec3d(0, 1, 0);
                right = up.crossProduct(normal).normalize();
            }

            Vec3d center = new Vec3d(stand.getX(), stand.getY(), stand.getZ()).add(normal.multiply(0.01));
            float phase = RickPortalGunClient.time(stand.age + tickDelta) + stand.getId() * 0.41f;

            matrices.push();
            matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
            MatrixStack.Entry pose = matrices.peek();
            quad(consumer, pose, Vec3d.ZERO.add(normal.multiply(0.002)), right, up, normal, 0.52, 0.78, 0.93f, phase);
            quad(consumer, pose, Vec3d.ZERO.add(normal.multiply(0.004)), right, up, normal, 0.48, 0.73, 0.48f, -phase * 1.27f);
            quad(consumer, pose, Vec3d.ZERO.add(normal.multiply(0.006)), right, up, normal, 0.43, 0.66, 0.29f, phase * 0.67f);
            matrices.pop();
        }
    }

    private static void quad(VertexConsumer consumer, MatrixStack.Entry pose, Vec3d center,
                             Vec3d right, Vec3d up, Vec3d normal,
                             double halfW, double halfH, float alpha, double rotation) {
        Vec3d r = rotate(right, up, rotation);
        Vec3d u = rotate(up, right, -rotation);
        Vec3d p1 = center.add(r.multiply(-halfW)).add(u.multiply(-halfH));
        Vec3d p2 = center.add(r.multiply(halfW)).add(u.multiply(-halfH));
        Vec3d p3 = center.add(r.multiply(halfW)).add(u.multiply(halfH));
        Vec3d p4 = center.add(r.multiply(-halfW)).add(u.multiply(halfH));

        vertex(consumer, pose, p1, 0, 1, alpha, normal);
        vertex(consumer, pose, p2, 1, 1, alpha, normal);
        vertex(consumer, pose, p3, 1, 0, alpha, normal);
        vertex(consumer, pose, p4, 0, 0, alpha, normal);
    }

    private static Vec3d rotate(Vec3d a, Vec3d b, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return a.multiply(c).add(b.multiply(s));
    }

    private static void vertex(VertexConsumer consumer, MatrixStack.Entry pose,
                               Vec3d pos, float u, float v, float alpha, Vec3d normal) {
        consumer.vertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(190, 255, 210, Math.round(255.0f * alpha))
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
