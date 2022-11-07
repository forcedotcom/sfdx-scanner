package com.salesforce;

import java.util.Objects;
import javax.annotation.Nullable;

/** Represents a null value that can be stored in a collection. See {@link CollectibleObject} */
public final class NullCollectibleObject<T> implements CollectibleObject {
    private final Class<?> clazz;

    /** Create a new null value that represents {@code clazz} when it is stored in a collection. */
    public NullCollectibleObject(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * @return null, since this value represents a null instance
     */
    @Nullable
    @Override
    public T getCollectibleObject() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NullCollectibleObject<?> that = (NullCollectibleObject<?>) o;
        return Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz);
    }

    @Override
    public String toString() {
        return "NullCollectibleObject{" + "clazz=" + clazz + '}';
    }
}
