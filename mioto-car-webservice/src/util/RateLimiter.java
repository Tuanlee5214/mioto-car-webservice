/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import cache.SimpleCache;

/**
 *
 * @author tuanlee
 */
public class RateLimiter {
    private RateLimiter() {}
    
    private static final class Counter
    {
        int count;
        long windowSecondMilis;
        public Counter(int count, long windowSecondMilis)
        {
            this.count = count;
            this.windowSecondMilis = windowSecondMilis;
        }
    }
    
    private static final SimpleCache<String, Counter> cache = new SimpleCache<>("ratelimit");
    private static final Object lock = new Object();
    
    public static boolean allow(String key, int max, int windowSeconds)
    {
        long now = System.currentTimeMillis();
        synchronized(lock)
        {
            Counter c = cache.get(key);
            if(c == null || now > c.windowSecondMilis)
            {
                cache.put(key, new Counter(1, now + windowSeconds * 1000L));
                return true;
            }
            
            if(c.count >= max) return false;
            
            cache.put(key, new Counter(c.count + 1, c.windowSecondMilis));
            return true;
        }
    }
}
