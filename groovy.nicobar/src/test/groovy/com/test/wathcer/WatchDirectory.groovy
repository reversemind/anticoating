package com.test.wathcer

import groovy.util.logging.Slf4j

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.LinkOption.NOFOLLOW_LINKS
import static java.nio.file.StandardWatchEventKinds.*

/**
 * Example to watch a directory (or tree) for changes to files.
 */
@Slf4j
public class WatchDirectory {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;
    private boolean trace = false;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
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
     * Creates a WatchService and registers the given directory
     */
    WatchDirectory(Path diectoryr, boolean recursive) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = recursive;

        if (recursive) {
            log.info "Scanning ${diectoryr} ...\n"
            registerAll(diectoryr);
            log.info "Done."
        } else {
            register(diectoryr);
        }

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
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

                // print out event
                log.info "${new Date().getTime()} | ${event.kind().name()} ${child}\n"

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
        }
    }

    static void usage() {
        System.err.println("usage: java WatchDirectory [-r] directory");
        System.exit(-1);
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
        new WatchDirectory(directory, recursive).processEvents();
    }
}
