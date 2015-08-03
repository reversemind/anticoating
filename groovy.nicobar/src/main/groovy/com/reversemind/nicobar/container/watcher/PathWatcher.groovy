package com.reversemind.nicobar.container.watcher

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.IContainerListener
import groovy.util.logging.Slf4j

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import static java.nio.file.LinkOption.NOFOLLOW_LINKS
import static java.nio.file.StandardWatchEventKinds.*

/**
 * Adaptation of a directory watcher (or tree) for changes to files.
 *
 * // TODO need listener from ModuleBuilder - when it's possible to watch for directory - means after compilation and so on
 */
@Slf4j
public class PathWatcher {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;
    private boolean trace = false;

    private Map<Path, ModuleId> pathModuleIdMap;
    private final Set<ModuleId> triggeredModules = new HashSet<>();

    private IContainerListener containerListener;
    private final ModuleId moduleId;

    // TODO it's not optimal solution
    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
    boolean isTriggered = false;

    private final long watchPeriod
    private final long notifyPeriod

    /**
     * Creates a WatchService and registers the given directory
     */
    public PathWatcher(ModuleId moduleId,
                       IContainerListener containerListener,
                       Path directory,
                       boolean recursive,
                       long watchPeriod,
                       long notifyPeriod) throws IOException {

        this.pathModuleIdMap = new ConcurrentHashMap<Path, ModuleId>();

        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = recursive;
        this.containerListener = containerListener;
        this.moduleId = moduleId;

        this.watchPeriod = watchPeriod <= 0 ? 100 : watchPeriod;
        this.notifyPeriod = notifyPeriod <= 0 ? 5000 : notifyPeriod;

        log.info "watchPeriod:${watchPeriod} ms";
        log.info "notifyPeriod:${notifyPeriod} ms";

        if (recursive) {
            log.info "Scanning ${directory} ...\n"
            registerAll(moduleId, directory);
            log.info "Done"
        } else {
            register(moduleId, directory);
        }

        // TODO it's not optimal solution - what about JGit - use API for git
        scheduledThreadPool.scheduleAtFixedRate(new NotifierThread(), notifyPeriod, notifyPeriod, TimeUnit.MILLISECONDS);

        // enable trace after initial registration
        this.trace = true;

        pathModuleIdMap.put(directory, moduleId);
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(final ModuleId moduleId, Path directory) throws IOException {
        WatchKey key = directory.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", directory);
            } else {
                if (!directory.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, directory);
                }
            }
        }
        keys.put(key, directory);
        pathModuleIdMap.put(directory, moduleId);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final ModuleId moduleId, final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs)
                    throws IOException {
                register(moduleId, directory);
                return FileVisitResult.CONTINUE;
            }
        } as FileVisitor<? super Path>);
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents() {
        new Thread() {
            @Override
            public void run() {
                for (; ;) {

                    // wait for key to be signalled
                    WatchKey key;
                    try {
                        key = watcher.take();
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

                        // if directory is created, and watching recursively, then
                        // register it and its sub-directories
                        if (recursive && (kind == ENTRY_CREATE)) {
                            try {
                                if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                                    // TODO it's a DIRTY & TEMP solution - need to figure out a correct moduleId via
                                    registerAll(moduleId, child);
                                }
                            } catch (IOException x) {
                                // ignore to keep sample readbale
                            }
                        }
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
        }.start();
    }

    public class NotifierThread implements Runnable {
        @Override
        public void run() {
            if (isTriggered) {
                log.debug "\n\n////////////////////////////////////////\n" + Thread.currentThread().getName() + " Start. Time = " + new Date();
                if(!triggeredModules.isEmpty()){
                    synchronized (triggeredModules) {
                        Set<ModuleId> _set = new HashSet<>();
                        _set.addAll(triggeredModules);
                        containerListener.changed(_set);
                        isTriggered = false;
                        triggeredModules.clear()
                    }
                }
                log.debug "\n\n\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\n" + Thread.currentThread().getName() + " End. Time = " + new Date();
            }
        }
    }
}
