package org.xiyu.onekeyminer.shape;

import net.minecraft.resources.Identifier;
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
    private static final Map<Identifier, ChainShape> SHAPES = new ConcurrentHashMap<>();
    private static final List<Identifier> SHAPE_ORDER = Collections.synchronizedList(new ArrayList<>());

    public static final Identifier DEFAULT_SHAPE_ID = Identifier.fromNamespaceAndPath(OneKeyMiner.MOD_ID, "amorphous");

    private ShapeRegistry() {
    }

    public static void register(ChainShape shape) {
        Objects.requireNonNull(shape, "Shape must not be null");
        Objects.requireNonNull(shape.getId(), "Shape ID must not be null");

        Identifier id = shape.getId();
        synchronized (SHAPE_ORDER) {
            ChainShape previous = SHAPES.put(id, shape);
            if (previous == null) {
                SHAPE_ORDER.add(id);
            } else {
                OneKeyMiner.LOGGER.warn("Shape {} was registered more than once; replacing previous instance", id);
            }
        }
    }

    public static ChainShape getShape(Identifier id) {
        return id != null ? SHAPES.get(id) : null;
    }

    public static ChainShape getShapeOrDefault(Identifier id) {
        ChainShape shape = id != null ? SHAPES.get(id) : null;
        if (shape == null) {
            shape = SHAPES.get(DEFAULT_SHAPE_ID);
        }
        if (shape == null) {
            synchronized (SHAPE_ORDER) {
                for (Identifier registeredId : SHAPE_ORDER) {
                    shape = SHAPES.get(registeredId);
                    if (shape != null) {
                        break;
                    }
                }
            }
        }
        return shape;
    }

    public static ChainShape getShapeOrDefault(String idStr) {
        Identifier id = idStr == null ? null : Identifier.tryParse(idStr);
        return getShapeOrDefault(id != null ? id : DEFAULT_SHAPE_ID);
    }

    public static boolean isRegistered(Identifier id) {
        return id != null && SHAPES.containsKey(id);
    }

    public static boolean isValidShapeId(String idStr) {
        Identifier id = idStr == null ? null : Identifier.tryParse(idStr);
        return id != null && SHAPES.containsKey(id);
    }

    public static List<ChainShape> getAllShapes() {
        List<ChainShape> result = new ArrayList<>();
        synchronized (SHAPE_ORDER) {
            for (Identifier id : SHAPE_ORDER) {
                ChainShape shape = SHAPES.get(id);
                if (shape != null) {
                    result.add(shape);
                }
            }
        }
        return List.copyOf(result);
    }

    public static List<Identifier> getAllShapeIds() {
        synchronized (SHAPE_ORDER) {
            return List.copyOf(SHAPE_ORDER);
        }
    }

    public static int getShapeCount() {
        return SHAPES.size();
    }

    public static String getNextShapeId(String currentId) {
        List<Identifier> ids = getAllShapeIds();
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
