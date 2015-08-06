package com.reversemind.nicobar.container.watcher

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.IContainerListener
import groovy.util.logging.Slf4j

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.*

import static java.nio.file.StandardWatchEventKinds.*

/**
 * Adaptation of a directory watcherService (or tree) for changes to files.
 */
@Slf4j
public class PathWatcher {

    private final WatchService watcherService;
    private final Map<WatchKey, Path> keys;
    private boolean trace = false;

    private Map<Path, ModuleId> pathModuleIdMap;
    private final Set<ModuleId> triggeredModules = new HashSet<>();

    private IContainerListener containerListener;

    // TODO it's not optimal solution
    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
    private ExecutorService watcherThread = Executors.newSingleThreadExecutor();

    private boolean isWatch = false;
    boolean isTriggered = false;

    private long watchPeriod
    private long notifyPeriod

    public PathWatcher(IContainerListener containerListener,
                       long watchPeriod,
                       long notifyPeriod) throws IOException {

        this.pathModuleIdMap = new ConcurrentHashMap<Path, ModuleId>();

        this.watcherService = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.containerListener = containerListener;

        this.watchPeriod = watchPeriod <= 0 ? 100 : watchPeriod;
        this.notifyPeriod = notifyPeriod <= 0 ? 5000 : notifyPeriod;

        log.info "watchPeriod:${watchPeriod} ms";
        log.info "notifyPeriod:${notifyPeriod} ms";

        // enable trace after initial registration
        this.trace = true;
        this.watcherThread.submit(new WatcherThread());

        // TODO it's not optimal solution - what about JGit - use API for git
        this.scheduledThreadPool.scheduleAtFixedRate(new NotifierThread(), notifyPeriod, notifyPeriod, TimeUnit.MILLISECONDS);
    }

    public PathWatcher stop() {
        isWatch = false;
        return this;
    }

    public PathWatcher start() {
        isWatch = true;
        return this;
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
            } as FileVisitor<? super Path>);
        }

        return this;
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public class WatcherThread implements Runnable {
        public void run() {
            while (true) {
                while (isWatch) {

                    // wait for key to be signalled
                    WatchKey key;
                    try {
                        key = watcherService.take();
                    } catch (InterruptedException x) {
                        return;
                    }

                    Path directory = keys.get(key);
                    if (directory == null) {
                        log.error "WatchKey ${key} not recognized";
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

                        log.info "${new Date().getTime()} | ${event.kind().name()} ${child}\n"

                        // TODO collapse a bunch of short events into single per predefinder period of time 100 ms

                        isTriggered = true;

                        // TODO what about new elements?! - need to detect sub path inclusion
                        if (pathModuleIdMap.containsKey(directory)) {
                            synchronized (triggeredModules) {
                                triggeredModules.add(pathModuleIdMap.get(directory))
                            }
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

                    Thread.sleep(watchPeriod);
                }
            }
        }
    }

    public class NotifierThread implements Runnable {
        @Override
        public void run() {
            if (isTriggered) {
                log.debug "\n\n////////////////////////////////////////\n" + Thread.currentThread().getName() + " Start. Time = " + new Date();
                synchronized (triggeredModules) {
                    if (!triggeredModules.isEmpty()) {
                        Set<ModuleId> _set = new HashSet<>();
                        _set.addAll(triggeredModules);
                        containerListener.changed(_set);
                        triggeredModules.clear()
                    }
                }
                isTriggered = false;
                log.debug "\n\n\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\n" + Thread.currentThread().getName() + " End. Time = " + new Date();
            }
        }
    }

}
