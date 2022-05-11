package com.salesforce.graph.ops;

import com.salesforce.exception.UnexpectedException;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.Pair;

/** Utilities to make reflective access easier */
public final class ReflectionUtil {
    private static final ConcurrentHashMap<String, Class<?>> CLASS_FOR_NAME =
            new ConcurrentHashMap<>();

    /** Cache the lookup to improve performance */
    private static final ConcurrentHashMap<Pair<Class<?>, String>, Field> CLASS_FIELDS =
            new ConcurrentHashMap();

    /**
     * Find the java class using {@link Class#forName(String)}
     *
     * @return the class if found, else Optional.empty
     */
    public static Optional<Class<?>> getClass(String className) {
        final Class<?> clazz =
                CLASS_FOR_NAME.computeIfAbsent(
                        className,
                        k -> {
                            try {
                                return Class.forName(className);
                            } catch (ClassNotFoundException ex) {
                                // Use this as a placeholder since ConcurrentHashMap doesn't support
                                // nulls
                                return ReflectionUtil.class;
                            }
                        });

        if (clazz.equals(ReflectionUtil.class)) {
            return Optional.empty();
        } else {
            return Optional.of(clazz);
        }
    }

    /**
     * Retrieves the value of the field using reflection
     *
     * @throws UnexpectedException if any exceptions occur
     */
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration") // TODO: revisit
    public static <T> T getFieldValue(Object object, String fieldName) {
        try {
            final Pair<Class<?>, String> key = Pair.of(object.getClass(), fieldName);
            final Field field =
                    CLASS_FIELDS.computeIfAbsent(
                            key,
                            k -> {
                                try {
                                    final Field f = object.getClass().getDeclaredField(fieldName);
                                    f.setAccessible(true);
                                    return f;
                                } catch (NoSuchFieldException ex) {
                                    throw new UnexpectedException(ex);
                                }
                            });
            return (T) field.get(object);
        } catch (IllegalAccessException ex) {
            throw new UnexpectedException(ex);
        }
    }

    private ReflectionUtil() {}
}
