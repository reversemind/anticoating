package com.reversemind.nicobar.container.watcher;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.nicobar.core.archive.ModuleId;
import com.reversemind.nicobar.container.IContainerListener;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Adaptation of a directory watcherService (or tree) for changes to files.
 *
 * // TODO turn on - if directory is created, and watching recursively
 * // TODO - refactor it
 */
public class PathWatcher {

    private volatile WatcherThread pathWatcherThread;

    private WatchService watcherService;
    private Map<WatchKey, Path> keys;
    private boolean trace = false;

    private Map<Path, ModuleId> pathModuleIdMap;
    private Set<ModuleId> triggeredModules;

    private IContainerListener containerListener;

    // TODO it's not optimal solution -- ScheduledThreadPoolExecutor
    private ScheduledExecutorService scheduledThreadPool;

    boolean isTriggered;

    private long watchPeriod;
    private long notifyPeriod;

    public PathWatcher(IContainerListener containerListener,
                       long watchPeriod,
                       long notifyPeriod) throws IOException {

        this.isTriggered = false;
        this.triggeredModules = Collections.synchronizedSet(new HashSet<ModuleId>());

        // https://bugs.openjdk.java.net/browse/JDK-8081063 - WatchService.take() ignores pathWatcherThread interrupt status if a WatchKey is signalled
        this.scheduledThreadPool = new ScheduledThreadPoolExecutor(1, new ThreadFactoryBuilder().setNameFormat("pool-notifier-pathWatcherThread-%d").build());
        this.pathModuleIdMap = new ConcurrentHashMap<Path, ModuleId>();

        this.watcherService = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.containerListener = containerListener;

        this.watchPeriod = watchPeriod <= 0 ? 100 : watchPeriod;
        this.notifyPeriod = notifyPeriod <= 0 ? 5000 : notifyPeriod;

        // enable trace after initial registration
        this.trace = true;
        this.pathWatcherThread = new WatcherThread(false);
        this.pathWatcherThread.start();

        // TODO it's not optimal solution - what about JGit - use API for git
        this.scheduledThreadPool.scheduleAtFixedRate(new NotifierThread(), notifyPeriod, notifyPeriod, TimeUnit.MILLISECONDS);
    }

    public PathWatcher stop() {
        this.pathWatcherThread.setIsWatch(false);
        return this;
    }

    public PathWatcher start() {
        this.pathWatcherThread.setIsWatch(true);
        return this;
    }

    public void destroy() throws InterruptedException, IOException {
        this.containerListener = null;

        this.stop();
        this.pathWatcherThread.setIsWatch(false);

        try{
            if(this.pathWatcherThread != null){
                System.out.println("watcher pathWatcherThread:" + pathWatcherThread.getName());
                this.pathWatcherThread.interrupt();
            }
        }catch (Exception ignore){
            ignore.printStackTrace();
        }


        System.out.println("scheduledThreadPool list:" + this.scheduledThreadPool.shutdownNow());
        this.scheduledThreadPool.awaitTermination(1, TimeUnit.SECONDS);

        this.watcherService.close();
        this.watcherService = null;

        this.triggeredModules.clear();
        this.triggeredModules = null;

        this.keys.clear();
        this.keys = null;

        this.scheduledThreadPool = null;
        Runtime.getRuntime().gc();
    }

    /**
     * Add the given directory with the PathWatcher
     */
    public PathWatcher register(final ModuleId moduleId, Path directory) throws IOException {
        WatchKey key = directory.register(watcherService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("PathWatcher register: %s\n", directory);
            } else {
                if (!directory.equals(prev)) {
                    System.out.format("PathWatcher update: %s -> %s\n", prev, directory);
                }
            }
        }
        keys.put(key, directory);
        pathModuleIdMap.put(directory, moduleId);

        return this;
    }

    /**
     * Register the given directory, and all its sub-directories, with the PathWatcher.
     */
    public PathWatcher register(
            final ModuleId moduleId, final Path baseDirectory, boolean isRecursively) throws IOException {
        // register directory and sub-directories
        if (!isRecursively) {
            register(moduleId, baseDirectory);
        } else {
            Files.walkFileTree(baseDirectory, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs)
                                throws IOException {
                            register(moduleId, directory);
                            return FileVisitResult.CONTINUE;
                        }
                    }
            );
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public class WatcherThread extends Thread implements Runnable {

        private boolean _isWatch;

        public WatcherThread(boolean isWatch) {
            this._isWatch = isWatch;
            this.setName("PATH-WATCHER-THREAD");
        }

        public void setIsWatch(boolean isWatch) {
            this._isWatch = isWatch;
        }

        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                while (this._isWatch) {

                    // wait for key to be signalled
                    WatchKey key;
                    try {
                        key = watcherService.take();
                    } catch (InterruptedException x) {
                        return;
                    }

                    Path directory = keys.get(key);
                    if (directory == null) {
//                        log.error "WatchKey ${key} not recognized";
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind kind = event.kind();

                        // TBD - provide example of how OVERFLOW event is handled
                        if (kind == OVERFLOW) {
                            continue;
                        }

                        // Context for directory entry event is the file name of entry
                        WatchEvent<Path> ev = cast(event);
                        Path name = ev.context();
                        Path child = directory.resolve(name);

//                        log.info "${new Date().getTime()} | ${event.kind().name()} ${child}\n";
                        System.out.println(" " + new Date().getTime() + "|" + event.kind().name() + " " + child);

                        // TODO collapse a bunch of short events into single per predefinder period of time 100 ms

                        isTriggered = true;

                        // TODO what about new elements?! - need to detect sub path inclusion
                        if (pathModuleIdMap.containsKey(directory)) {
                            triggeredModules.add(pathModuleIdMap.get(directory));
                        }

                        /*

                            !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

                         */

                        // if directory is created, and watching recursively, then
                        // register it and its sub-directories

                        // TODO figure out for what moduleId it's sub directory was created
                        // if no any moduleId is detected then do not
//                    if (recursive && (kind == ENTRY_CREATE)) {
//                        try {
//                            if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
//                                // TODO it's a DIRTY & TEMP solution - need to figure out a correct moduleId via
//                                registerAll(moduleId, child);
//                            }
//                        } catch (IOException x) {
//                            // ignore to keep sample readable
//                        }
//                    }
                    }

                    // reset key and remove from set if directory no longer accessible
                    boolean valid = key.reset();
                    if (!valid) {
                        keys.remove(key);

                        // all directories are inaccessible
                        if (keys.isEmpty()) {
                            break;
                        }
                    }

                    try {
                        Thread.sleep(watchPeriod);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public class NotifierThread implements Runnable {
        @Override
        public void run() {
            if (isTriggered) {
                if (!triggeredModules.isEmpty()) {
                    Set<ModuleId> _set = new HashSet<>(triggeredModules.size());
                    _set.addAll(triggeredModules);
                    containerListener.changed(_set);
                    triggeredModules.clear();
                    _set.clear();
                }
                isTriggered = false;
            }
        }
    }

}
