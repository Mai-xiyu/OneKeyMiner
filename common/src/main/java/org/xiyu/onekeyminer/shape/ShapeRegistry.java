package org.xiyu.onekeyminer.shape;

import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry for built-in and addon-provided chain shapes.
 */
public final class ShapeRegistry {
    private static final Object REGISTRY_LOCK = new Object();
    private record RegistryState(
            Map<ResourceLocation, ChainShape> shapes,
            List<ResourceLocation> order
    ) {
        private static RegistryState empty() {
            return new RegistryState(Map.of(), List.of());
        }
    }

    private static volatile RegistryState state = RegistryState.empty();

    public static final ResourceLocation DEFAULT_SHAPE_ID = ResourceLocation.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "amorphous");
    public static final int MAX_SHAPE_ID_LENGTH = 128;

    private ShapeRegistry() {
    }

    public static void register(ChainShape shape) {
        Objects.requireNonNull(shape, "Shape must not be null");
        ResourceLocation id = Objects.requireNonNull(shape.getId(), "Shape ID must not be null");
        if (id.toString().length() > MAX_SHAPE_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "Shape ID exceeds " + MAX_SHAPE_ID_LENGTH + " characters: " + id
            );
        }

        synchronized (REGISTRY_LOCK) {
            RegistryState snapshot = state;
            Map<ResourceLocation, ChainShape> nextShapes =
                    new LinkedHashMap<>(snapshot.shapes());
            List<ResourceLocation> nextOrder = new ArrayList<>(snapshot.order());
            if (!nextShapes.containsKey(id)) {
                nextOrder.add(id);
            } else {
                OneKeyMiner.LOGGER.warn(
                        "Shape {} was registered more than once; replacing previous instance",
                        id
                );
            }
            nextShapes.put(id, shape);
            state = new RegistryState(
                    Collections.unmodifiableMap(nextShapes),
                    List.copyOf(nextOrder)
            );
        }
    }

    public static ChainShape getShape(ResourceLocation id) {
        return state.shapes().get(id);
    }

    /**
     * Removes an add-on shape. The built-in default remains available so
     * server-side preference validation always has a deterministic fallback.
     */
    public static boolean unregister(ResourceLocation id) {
        if (id == null || DEFAULT_SHAPE_ID.equals(id)) {
            return false;
        }
        synchronized (REGISTRY_LOCK) {
            RegistryState snapshot = state;
            if (!snapshot.shapes().containsKey(id)) {
                return false;
            }
            Map<ResourceLocation, ChainShape> nextShapes =
                    new LinkedHashMap<>(snapshot.shapes());
            List<ResourceLocation> nextOrder = new ArrayList<>(snapshot.order());
            nextShapes.remove(id);
            nextOrder.remove(id);
            state = new RegistryState(
                    Collections.unmodifiableMap(nextShapes),
                    List.copyOf(nextOrder)
            );
            return true;
        }
    }

    public static ChainShape getShapeOrDefault(ResourceLocation id) {
        Map<ResourceLocation, ChainShape> snapshot = state.shapes();
        ChainShape shape = snapshot.get(id);
        if (shape == null) {
            shape = snapshot.get(DEFAULT_SHAPE_ID);
        }
        if (shape == null && !snapshot.isEmpty()) {
            shape = snapshot.values().iterator().next();
        }
        return shape;
    }

    public static ChainShape getShapeOrDefault(String idStr) {
        ResourceLocation id = idStr == null || idStr.isBlank()
                ? null
                : ResourceLocation.tryParse(idStr);
        return getShapeOrDefault(id != null ? id : DEFAULT_SHAPE_ID);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return id != null && state.shapes().containsKey(id);
    }

    public static boolean isValidShapeId(String idStr) {
        if (idStr == null || idStr.isBlank()) {
            return false;
        }
        if (idStr.length() > MAX_SHAPE_ID_LENGTH) {
            return false;
        }
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        return id != null && state.shapes().containsKey(id);
    }

    public static List<ChainShape> getAllShapes() {
        RegistryState snapshot = state;
        List<ChainShape> result = new ArrayList<>(snapshot.order().size());
        for (ResourceLocation id : snapshot.order()) {
            ChainShape shape = snapshot.shapes().get(id);
            if (shape != null) {
                result.add(shape);
            }
        }
        return List.copyOf(result);
    }

    public static List<ResourceLocation> getAllShapeIds() {
        return state.order();
    }

    public static int getShapeCount() {
        return state.shapes().size();
    }

    public static String getNextShapeId(String currentId) {
        List<ResourceLocation> ids = getAllShapeIds();
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
