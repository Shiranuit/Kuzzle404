package fr.shiranuit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class UniqueQueue <T> extends LinkedBlockingQueue {
    private Map<T, Boolean> unique = Collections.synchronizedMap(new HashMap<>());

    public boolean push(T o) {
        if (!unique.containsKey(o)) {
            unique.put(o, true);
            return super.add(o);
        }
        return false;
    }

    public T pop() {
        return (T)super.poll();
    }
}
