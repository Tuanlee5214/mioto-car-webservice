/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import util.Config;

/**
 *
 * @author tuanlee
 */
public class SimpleCache<K, V>{
    private final int  _maxSize;
    private final long _ttlMs;
    private final LruMap<K, Entry<V>> _map;
    private final Object _lock = new Object();       // LinkedHashMap is NOT thread-safe

    private final AtomicLong _hit     = new AtomicLong();
    private final AtomicLong _miss    = new AtomicLong();
    private final AtomicLong _expired = new AtomicLong();
    private final AtomicLong _put     = new AtomicLong();
    private final AtomicLong _removed = new AtomicLong();

    private static final class Entry<V> {
        final V value;
        final long expireAtMs;
        Entry(V value, long expireAtMs) {
            this.value = value;
            this.expireAtMs = expireAtMs;
        }
    }

    private static final class LruMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = 1L;
        private final int maxSize;
        LruMap(int maxSize) {
            super(16, 0.75f, true);                  // true = ACCESS order, not insertion order
            this.maxSize = maxSize;
        }
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;                 // evict the least recently USED
        }
    }

    /** Reads [SimpleCache@name]: max-size, ttl-seconds. */
    public SimpleCache(String name) {
        _maxSize = Config.getInt(SimpleCache.class, name, "max-size", 500);
        _ttlMs   = Config.getInt(SimpleCache.class, name, "ttl-seconds", 60) * 1000L;
        _map     = new LruMap<K, Entry<V>>(_maxSize);
    }

    /** @return the cached value, or null on a miss (or an expired entry). */
    public V get(K key) {
        synchronized (_lock) {                       // note: even a READ mutates access order
            Entry<V> e = _map.get(key);
            if (e == null) {
                _miss.incrementAndGet();
                return null;
            }
            if (System.currentTimeMillis() >= e.expireAtMs) {
                _map.remove(key);                    // lazy expiry, on read
                _expired.incrementAndGet();
                _miss.incrementAndGet();
                return null;
            }
            _hit.incrementAndGet();
            return e.value;
        }
    }

    public void put(K key, V value) {
        if (key == null || value == null) {
            return;                                  // this basic cache stores no nulls
        }
        synchronized (_lock) {
            _map.put(key, new Entry<>(value, System.currentTimeMillis() + _ttlMs));
            _put.incrementAndGet();
        }
    }

    public void remove(K key) {
        synchronized (_lock) {
            if (_map.remove(key) != null) {
                _removed.incrementAndGet();
            }
        }
    }

    public void clear() {
        synchronized (_lock) {
            _map.clear();
        }
    }

    public int size() {
        synchronized (_lock) {
            return _map.size();
        }
    }

    public long getHit()     { return _hit.get(); }
    public long getMiss()    { return _miss.get(); }
    public long getExpired() { return _expired.get(); }

    /** Hit rate over all lookups. The number that tells you if the cache earns its memory. */
    public int hitRatePercent() {
        long h = _hit.get();
        long total = h + _miss.get();
        return total == 0 ? 0 : (int) (h * 100 / total);
    }

    public String stats() {
        return "cache{size=" + size() + "/" + _maxSize + ", hit=" + _hit.get()
             + ", miss=" + _miss.get() + ", expired=" + _expired.get()
             + ", put=" + _put.get() + ", removed=" + _removed.get()
             + ", hitRate=" + hitRatePercent() + "%}";
    }
}
