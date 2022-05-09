package com.salesforce;

import com.salesforce.graph.ApexPath;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * This interface allows collections to store null keys and values without the need to wrap it in an
 * {@link Optional}. Storing Optionals in collections is considered bad practice because they can't
 * be serialized and it requires all objects to be wrapped in an additional class. Any classes that
 * are stored in collections and may be null should do the following.
 *
 * <ol>
 *   <li>Implement this interface, returning {@code this} from the {@link #getCollectible()} method.
 *   <li>Declare a public static variable {@code NULL_KEY} that is an instance of {@link
 *       NullCollectible} specific to the class that supports null. See {@link ApexPath#NULL_VALUE}
 * </ol>
 */
public interface Collectible<T> {
    @Nullable
    /** @return the value stored in the collection. May return null. */
    T getCollectible();
}
