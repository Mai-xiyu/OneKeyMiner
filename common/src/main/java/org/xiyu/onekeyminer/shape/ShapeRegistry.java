package org.xiyu.onekeyminer.shape;

import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry for built-in and addon-provided chain shapes.
 */
public final class ShapeRegistry {
    private static final Object WRITE_LOCK = new Object();
    private static volatile Snapshot snapshot = new Snapshot(Map.of(), List.of());

    private record Snapshot(
            Map<ResourceLocation, ChainShape> shapes,
            List<ResourceLocation> order
    ) {
    }

    public static final ResourceLocation DEFAULT_SHAPE_ID =
            ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "amorphous");

    private ShapeRegistry() {
    }

    public static void register(ChainShape shape) {
        Objects.requireNonNull(shape, "Shape must not be null");
        ResourceLocation id = Objects.requireNonNull(shape.getId(), "Shape ID must not be null");

        synchronized (WRITE_LOCK) {
            Snapshot current = snapshot;
            Map<ResourceLocation, ChainShape> nextShapes = new LinkedHashMap<>(current.shapes());
            List<ResourceLocation> nextOrder = new ArrayList<>(current.order());
            if (!nextShapes.containsKey(id)) {
                nextOrder.add(id);
            } else {
                OneKeyMiner.LOGGER.warn(
                        "Shape {} was registered more than once; replacing previous instance",
                        id
                );
            }
            nextShapes.put(id, shape);
            snapshot = new Snapshot(Map.copyOf(nextShapes), List.copyOf(nextOrder));
        }
    }

    public static ChainShape getShape(ResourceLocation id) {
        return snapshot.shapes().get(id);
    }

    public static ChainShape getShapeOrDefault(ResourceLocation id) {
        Snapshot current = snapshot;
        ChainShape shape = current.shapes().get(id);
        if (shape == null) {
            shape = current.shapes().get(DEFAULT_SHAPE_ID);
        }
        if (shape == null && !current.order().isEmpty()) {
            shape = current.shapes().get(current.order().getFirst());
        }
        return shape;
    }

    public static ChainShape getShapeOrDefault(String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        return getShapeOrDefault(id != null ? id : DEFAULT_SHAPE_ID);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return snapshot.shapes().containsKey(id);
    }

    public static boolean isValidShapeId(String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        return id != null && isRegistered(id);
    }

    public static List<ChainShape> getAllShapes() {
        Snapshot current = snapshot;
        List<ChainShape> result = new ArrayList<>();
        for (ResourceLocation id : current.order()) {
            ChainShape shape = current.shapes().get(id);
            if (shape != null) {
                result.add(shape);
            }
        }
        return List.copyOf(result);
    }

    public static List<ResourceLocation> getAllShapeIds() {
        return snapshot.order();
    }

    public static int getShapeCount() {
        return snapshot.shapes().size();
    }

    public static String getNextShapeId(String currentId) {
        List<ResourceLocation> ids = snapshot.order();
        if (ids.isEmpty()) {
            return DEFAULT_SHAPE_ID.toString();
        }
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i).toString().equals(currentId)) {
                return ids.get((i + 1) % ids.size()).toString();
            }
        }
        return ids.get(0).toString();
    }
}
