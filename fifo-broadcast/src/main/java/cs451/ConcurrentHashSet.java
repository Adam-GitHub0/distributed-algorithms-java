package cs451;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class ConcurrentHashSet<T> {
    private final Set<T> set;

    public ConcurrentHashSet() {
        this.set = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    // Adds an element to the set
    public boolean add(T item) {
        return set.add(item);
    }

    // Checks if the set contains the element
    public boolean contains(T item) {
        return set.contains(item);
    }

    // Removes an element from the set
    public boolean remove(T item) {
        return set.remove(item);
    }

    // Checks if the set is empty
    public boolean isEmpty() {
        return set.isEmpty();
    }

    // Clears all elements from the set
    public void clear() {
        set.clear();
    }

    // Provides an iterable view
    public Iterable<T> iterable() {
        return set;
    }
}

