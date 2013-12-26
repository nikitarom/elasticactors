package org.elasticsoftware.elasticactors.cluster;

import com.google.common.hash.HashFunction;
import org.elasticsoftware.elasticactors.base.util.Charsets;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

public final class ConsistentHash<T> {

    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final SortedMap<Long, T> circle = new TreeMap<>();

    public ConsistentHash(HashFunction hashFunction, int numberOfReplicas,
                          Collection<T> nodes) {
        this.hashFunction = hashFunction;
        this.numberOfReplicas = numberOfReplicas;

        for (T node : nodes) {
            add(node);
        }
    }

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.put(hashFunction.hashString(node.toString() + i, Charsets.UTF_8).asLong(), node);
        }
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            circle.remove(hashFunction.hashString(node.toString() + i, Charsets.UTF_8).asLong());
        }
    }

    public T get(String key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = hashFunction.hashString(key, Charsets.UTF_8).asLong();
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

}
