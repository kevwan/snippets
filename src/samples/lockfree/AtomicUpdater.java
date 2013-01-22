package samples.lockfree;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AtomicUpdater
{
    public static abstract class AtomicMapUpdater<K, V>
    {
        private final AtomicReference<Map<K, V>> ref = new AtomicReference<Map<K, V>>();

        public AtomicMapUpdater(Map<K, V> map)
        {
            ref.set(map);
        }

        public Map<K, V> getMap()
        {
            return ref.get();
        }

        public V lookup(K key)
        {
            return ref.get().get(key);
        }

        public void update(K key, V value)
        {
            Map<K, V> newDict = null;
            Map<K, V> oldDict = null;
            do
            {
                oldDict = ref.get();
                newDict = newInstance();
                newDict.putAll(oldDict);
                newDict.put(key, value);
            } while (!ref.compareAndSet(oldDict, newDict));
        }

        protected abstract Map<K, V> newInstance();
    }

    public static class AtomicTreeMapUpdater<K, V> extends AtomicMapUpdater<K, V>
    {
        public AtomicTreeMapUpdater(Map<K, V> map)
        {
            super(map);
        }

        @Override
        protected Map<K, V> newInstance()
        {
            return new TreeMap<K, V>();
        }
    }

    public static class RWMap<K, V>
    {
        private final Map<K, V> map;
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public RWMap(Map<K, V> map)
        {
            this.map = map;
        }

        public V lookup(K key)
        {
            lock.readLock().lock();
            try
            {
                return map.get(key);
            }
            finally
            {
                lock.readLock().unlock();
            }
        }

        public void update(K key, V value)
        {
            lock.writeLock().lock();
            try
            {
                map.put(key, value);
            }
            finally
            {
                lock.writeLock().unlock();
            }
        }
    }

    class QueryTask implements Runnable
    {
        public void run()
        {
            int i = index.getAndIncrement();
            while (i < TOTAL_QUERIES)
            {
                if (i % INTERVAL == 0)
                {
                    updater.update(new Integer(i), Integer.toString(i));
                }
                else
                {
                    String v = updater.lookup(new Integer(-1));
                    if (!v.equals("-1"))
                    {
                        System.out.println("failed");
                    }
                }
                i = index.getAndIncrement();
            }
        }
    }

    class RWQueryTask implements Runnable
    {
        public void run()
        {
            int i = index.getAndIncrement();
            while (i < TOTAL_QUERIES)
            {
                if (i % INTERVAL == 0)
                {
                    rwmap.update(new Integer(i), Integer.toString(i));
                }
                else
                {
                    String v = rwmap.lookup(new Integer(-1));
                    if (!v.equals("-1"))
                    {
                        System.out.println("failed");
                    }
                }
                i = index.getAndIncrement();
            }
        }
    }

    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicMapUpdater<Integer, String> updater =
        new AtomicTreeMapUpdater<Integer, String>(new TreeMap<Integer, String>());
    private final RWMap<Integer, String> rwmap =
        new RWMap<Integer, String>(new TreeMap<Integer, String>());
    private final int TOTAL_THREADS = 50;
    private final int TOTAL_QUERIES = 1000000;
    private final int INTERVAL = 1000;

    public static void main(String[] args)
    {
        new AtomicUpdater().doTest();
    }

    public void doTest()
    {
        long start = System.currentTimeMillis();
        updater.update(new Integer(-1), "-1");

        Thread[] threads = new Thread[TOTAL_THREADS];
        for (int i = 0; i < TOTAL_THREADS; ++i)
        {
            threads[i] = new Thread(new QueryTask());
            threads[i].start();
        }

        for (int i = 0; i < TOTAL_THREADS; ++i)
        {
            try
            {
                threads[i].join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        System.out.println("Time ellapsed: " + (System.currentTimeMillis() - start) + "ms");

        Map<Integer, String> m = updater.getMap();
        if (m.size() != TOTAL_QUERIES / INTERVAL + 1)
        {
            System.out.println("wrong size");
        }

        System.out.println("================================================");
        index.set(0);
        start = System.currentTimeMillis();
        rwmap.update(new Integer(-1), "-1");

        threads = new Thread[TOTAL_THREADS];
        for (int i = 0; i < TOTAL_THREADS; ++i)
        {
            threads[i] = new Thread(new RWQueryTask());
            threads[i].start();
        }

        for (int i = 0; i < TOTAL_THREADS; ++i)
        {
            try
            {
                threads[i].join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        System.out.println("Time ellapsed: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void printMap()
    {
        Map<Integer, String> m = updater.getMap();
        System.out.println(m.toString());
    }
}