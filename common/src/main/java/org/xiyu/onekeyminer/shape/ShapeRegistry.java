package org.xiyu.onekeyminer.shape;

import net.minecraft.resources.ResourceLocation;
import org.xiyu.onekeyminer.OneKeyMiner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for built-in and addon-provided chain shapes.
 */
public final class ShapeRegistry {
    private static final Map<ResourceLocation, ChainShape> SHAPES = new ConcurrentHashMap<>();
    private static final List<ResourceLocation> SHAPE_ORDER = Collections.synchronizedList(new ArrayList<>());

    public static final ResourceLocation DEFAULT_SHAPE_ID = new ResourceLocation(OneKeyMiner.MOD_ID, "amorphous");

    private ShapeRegistry() {
    }

    public static void register(ChainShape shape) {
        Objects.requireNonNull(shape, "Shape must not be null");
        Objects.requireNonNull(shape.getId(), "Shape ID must not be null");

        ResourceLocation id = shape.getId();
        if (!SHAPES.containsKey(id)) {
            SHAPE_ORDER.add(id);
        } else {
            OneKeyMiner.LOGGER.warn("Shape {} was registered more than once; replacing previous instance", id);
        }
        SHAPES.put(id, shape);
    }

    public static ChainShape getShape(ResourceLocation id) {
        return SHAPES.get(id);
    }

    public static ChainShape getShapeOrDefault(ResourceLocation id) {
        ChainShape shape = SHAPES.get(id);
        if (shape == null) {
            shape = SHAPES.get(DEFAULT_SHAPE_ID);
        }
        if (shape == null && !SHAPES.isEmpty()) {
            shape = SHAPES.values().iterator().next();
        }
        return shape;
    }

    public static ChainShape getShapeOrDefault(String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        return getShapeOrDefault(id != null ? id : DEFAULT_SHAPE_ID);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return SHAPES.containsKey(id);
    }

    public static boolean isValidShapeId(String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        return id != null && SHAPES.containsKey(id);
    }

    public static List<ChainShape> getAllShapes() {
        List<ChainShape> result = new ArrayList<>();
        for (ResourceLocation id : SHAPE_ORDER) {
            ChainShape shape = SHAPES.get(id);
            if (shape != null) {
                result.add(shape);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<ResourceLocation> getAllShapeIds() {
        return Collections.unmodifiableList(new ArrayList<>(SHAPE_ORDER));
    }

    public static int getShapeCount() {
        return SHAPES.size();
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
