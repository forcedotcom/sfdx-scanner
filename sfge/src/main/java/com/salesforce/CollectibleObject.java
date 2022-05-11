package com.salesforce;

import com.salesforce.graph.vertex.BaseSFVertex;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * This interface allows collections to store null keys and values without the need to wrap it in an
 * {@link Optional}. Storing Optionals in collections is considered bad practice because they can't
 * be serialized and it requires all objects to be wrapped in an additional class. Any classes that
 * are stored in collections and may be null should do the following.
 *
 * <ol>
 *   <li>Implement this interface, returning {@code this} from the {@link #getCollectibleObject()}
 *       method.
 *   <li>Declare a public static variable {@code NULL_KEY} that is an instance of {@link
 *       NullCollectibleObject} specific to the class that supports null. See {@link
 *       BaseSFVertex#NULL_VALUE}
 * </ol>
 *
 * @deprecated Use {@link Collectible} if possible. This interface is a temporary implementation
 *     that avoids changing BaseSFVertex to a generic class.
 */
@Deprecated
public interface CollectibleObject {
    @Nullable
    /** @return the value stored in the collection. May return null. */
    Object getCollectibleObject();
}
