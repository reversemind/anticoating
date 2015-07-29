package com.reversemind.nicobar.container.watcher

import com.netflix.nicobar.core.archive.ModuleId
import com.reversemind.nicobar.container.IScriptContainerListener
import groovy.util.logging.Slf4j

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
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

    private IScriptContainerListener scriptContainerListener;
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
                       IScriptContainerListener scriptContainerListener,
                       Path directory,
                       boolean recursive,
                       long watchPeriod,
                       long notifyPeriod) throws IOException {

        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = recursive;
        this.scriptContainerListener = scriptContainerListener;
        this.moduleId = moduleId;

        this.watchPeriod = watchPeriod <= 0 ? 100 : watchPeriod;
        this.notifyPeriod = notifyPeriod <= 0 ? 5000 : notifyPeriod;

        log.info "watchPeriod:${watchPeriod} ms";
        log.info "notifyPeriod:${notifyPeriod} ms";

        if (recursive) {
            log.info "Scanning ${directory} ...\n"
            registerAll(directory);
            log.info "Done"
        } else {
            register(directory);
        }

        // TODO it's not optimal solution - what about JGit - use API for git
        scheduledThreadPool.scheduleAtFixedRate(new NotifierThread(), notifyPeriod, notifyPeriod, TimeUnit.MILLISECONDS);

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path directory) throws IOException {
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
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs)
                    throws IOException {
                register(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Process all events for keys queued to the watcher
     */
    def processEvents = { ->

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

                        // if directory is created, and watching recursively, then
                        // register it and its sub-directories
                        if (recursive && (kind == ENTRY_CREATE)) {
                            try {
                                if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                                    registerAll(child);
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

    static void usage() {
        System.err.println("usage: java PathWatcher [-r] directory");
        System.exit(-1);
    }

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public static void main(String[] args) throws IOException {
        // parse arguments
        if (args.length == 0 || args.length > 2)
            usage();
        boolean recursive = false;
        int dirArg = 0;
        if (args[0].equals("-r")) {
            if (args.length < 2)
                usage();
            recursive = true;
            dirArg++;
        }

        // register directory and process its events
        Path directory = Paths.get(args[dirArg]);
        new PathWatcher(ModuleId.create("moduleName", "moduleVersion"),
                new IScriptContainerListener() {
                    @Override
                    void changed(ModuleId moduleId) {
                        log.info "Just changed module:" + moduleId;
                    }
                },
                directory,
                recursive, 0, 0).processEvents();
    }

    public class NotifierThread implements Runnable {
        @Override
        public void run() {
            if (isTriggered) {
                System.out.println("\n\n////////////////////////////////////////\n" + Thread.currentThread().getName() + " Start. Time = " + new Date());
                scriptContainerListener.changed(moduleId)
                isTriggered = false;
                System.out.println("\n\n\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\n" + Thread.currentThread().getName() + " End. Time = " + new Date());
            }
        }
    }
}
