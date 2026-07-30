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
 * Copy-on-write shape registry.
 *
 * <p>Readers always observe one ordered, immutable snapshot. Registration is
 * rare and serialized, while preview and server packet validation stay
 * lock-free.</p>
 */
public final class ShapeRegistry {

    public static final ResourceLocation DEFAULT_SHAPE_ID =
            new ResourceLocation(OneKeyMiner.MOD_ID, "amorphous");

    private static final Object WRITE_LOCK = new Object();
    private static volatile Map<ResourceLocation, ChainShape> shapes =
            Collections.emptyMap();

    private ShapeRegistry() {
    }

    public static void register(ChainShape shape) {
        Objects.requireNonNull(shape, "shape");
        ResourceLocation id = Objects.requireNonNull(shape.getId(), "shape id");

        synchronized (WRITE_LOCK) {
            Map<ResourceLocation, ChainShape> next = new LinkedHashMap<>(shapes);
            if (next.put(id, shape) != null) {
                OneKeyMiner.LOGGER.warn("Shape {} was replaced", id);
            }
            shapes = Collections.unmodifiableMap(next);
        }
    }

    public static ChainShape getShape(ResourceLocation id) {
        return shapes.get(id);
    }

    public static ChainShape getShapeOrDefault(ResourceLocation id) {
        Map<ResourceLocation, ChainShape> snapshot = shapes;
        ChainShape shape = snapshot.get(id);
        if (shape == null) {
            shape = snapshot.get(DEFAULT_SHAPE_ID);
        }
        if (shape == null && !snapshot.isEmpty()) {
            shape = snapshot.values().iterator().next();
        }
        return shape;
    }

    public static ChainShape getShapeOrDefault(String id) {
        ResourceLocation parsed = id == null ? null : ResourceLocation.tryParse(id);
        if (parsed == null) {
            OneKeyMiner.LOGGER.warn("Invalid shape id {}, using default", id);
            parsed = DEFAULT_SHAPE_ID;
        }
        return getShapeOrDefault(parsed);
    }

    public static boolean isRegistered(ResourceLocation id) {
        return id != null && shapes.containsKey(id);
    }

    public static boolean isValidShapeId(String id) {
        return id != null && isRegistered(ResourceLocation.tryParse(id));
    }

    public static List<ChainShape> getAllShapes() {
        return List.copyOf(shapes.values());
    }

    public static List<ResourceLocation> getAllShapeIds() {
        return List.copyOf(shapes.keySet());
    }

    public static int getShapeCount() {
        return shapes.size();
    }

    public static String getNextShapeId(String currentId) {
        List<ResourceLocation> ids = new ArrayList<>(shapes.keySet());
        if (ids.isEmpty()) {
            return DEFAULT_SHAPE_ID.toString();
        }

        for (int index = 0; index < ids.size(); index++) {
            if (ids.get(index).toString().equals(currentId)) {
                return ids.get((index + 1) % ids.size()).toString();
            }
        }
        return ids.get(0).toString();
    }
}
