package com.queomedia.di;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CollectionUtils {

    public static <T> boolean containsDuplicates(Collection<T> collection) {
        Set<T> set = new HashSet<>(collection);
        return set.size() < collection.size();
    }

}
