package com.composum.ai.backend.slingbase;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.Resource;

public class AIResourceUtil {
    /**
     * Returns a stream that goes through all descendants of a resource, parents come before
     * their children.
     *
     * @param resource a resource or null
     * @return a stream running through the resource and it's the descendants, not null
     */
    @Nonnull
    public static Stream<Resource> descendantsStream(@Nullable Resource resource) {
        if (resource == null) {
            return Stream.empty();
        }
        return Stream.concat(Stream.of(resource),
                StreamSupport.stream(resource.getChildren().spliterator(), false)
                        .flatMap(AIResourceUtil::descendantsStream));
    }

    /**
     * Checks whether a resource is of a certain node type - be that jcr:primaryType or a mixin.
     *
     * @param resource a resource or null
     * @param nodetype a node type or null ; if null we return false
     * @return true if the resource is of the node type
     */
    public static boolean isOfNodeType(@Nullable Resource resource, @Nullable String nodetype) {
        if (resource == null || nodetype == null) {
            return false;
        }
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            try {
                return node.isNodeType(nodetype);
            } catch (RepositoryException e) { // can't happen
                throw new SlingException("cannot check node type for " + resource.getPath(), e);
            }
        }
        // Not quite complete fallback
        return nodetype.equals(resource.getValueMap().get("jcr:primaryType", String.class))
                || Stream.of(resource.getValueMap().get("jcr:mixinTypes", String[].class))
                .anyMatch(nodetype::equals);
    }

}
