package com.reversemind.jgit

import groovy.util.logging.Slf4j
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.Status
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import spock.lang.Specification

import java.nio.file.Path
import java.nio.file.Paths

/**
 */
@Slf4j
class JGitTest extends Specification {

    // https://github.com/centic9/jgit-cookbook/blob/master/build.gradle

    def 'get info from git'() {
        setup:
        log.info ""

        File repoDir = new File("/opt/dev/github/reversemind/anticoating/.git");

        // now open the resulting repository with a FileRepositoryBuilder
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        when:
        log.info ""

        Ref _head = repository.getRef("refs/heads/master");
        System.out.println("Ref of refs/heads/master: " + _head);


        println repository.getRemoteNames()

        println "\n\n"

        Status status = new Git(repository).status().call();
        System.out.println("Added: " + status.getAdded());
        System.out.println("Changed: " + status.getChanged());
        System.out.println("Conflicting: " + status.getConflicting());
        System.out.println("ConflictingStageState: " + status.getConflictingStageState());
        System.out.println("IgnoredNotInIndex: " + status.getIgnoredNotInIndex());
        System.out.println("Missing: " + status.getMissing());
        System.out.println("Modified: " + status.getModified());
        System.out.println("Removed: " + status.getRemoved());
        System.out.println("Untracked: " + status.getUntracked());
        System.out.println("UntrackedFolders: " + status.getUntrackedFolders());


        println "\n\nShowChangedFilesBetweenCommits\n:"

        // The {tree} will return the underlying tree-id instead of the commit-id itself!
        // For a description of what the carets do see e.g. http://www.paulboxley.com/blog/2011/06/git-caret-and-tilde
        // This means we are selecting the parent of the parent of the parent of the parent of current HEAD and
        // take the tree-ish of it
        ObjectId oldHead = repository.resolve("HEAD^^^^{tree}");
        ObjectId head = repository.resolve("HEAD^{tree}");

        System.out.println("Printing diff between tree: " + oldHead + " and " + head);

        // prepare the two iterators to compute the diff between
        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        oldTreeIter.reset(reader, oldHead);
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        newTreeIter.reset(reader, head);

        // finally get the list of changed files
        List<DiffEntry> diffs = new Git(repository).diff()
                .setNewTree(newTreeIter)
                .setOldTree(oldTreeIter)
                .call();
        for (DiffEntry entry : diffs) {
            System.out.println("Entry: " + entry + "|" + entry.getPath(DiffEntry.Side.NEW));
        }
        System.out.println("Done");


        then:
        log.info ""

        repository.close();
    }


    def 'get changed files from git'() {
        setup:
        log.info ""

        File repoDir = new File("/opt/dev/github/reversemind/anticoating/.git");

        // now open the resulting repository with a FileRepositoryBuilder
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = builder.setGitDir(repoDir)
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        when:
        log.info ""

        println "\n\nShowChangedFilesBetweenCommits\n:"

        // The {tree} will return the underlying tree-id instead of the commit-id itself!
        // For a description of what the carets do see e.g. http://www.paulboxley.com/blog/2011/06/git-caret-and-tilde
        // This means we are selecting the parent of the parent of the parent of the parent of current HEAD and
        // take the tree-ish of it
        ObjectId oldHead = repository.resolve("HEAD^^^^{tree}");
        ObjectId head = repository.resolve("HEAD^{tree}");

        System.out.println("Printing diff between tree: " + oldHead + " and " + head);

        // prepare the two iterators to compute the diff between
        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        oldTreeIter.reset(reader, oldHead);
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        newTreeIter.reset(reader, head);

        // TODO what about a tracking by commit hash also ?!
        // finally get the list of changed files
        List<DiffEntry> diffs = new Git(repository).diff()
                .setNewTree(newTreeIter)
                .setOldTree(oldTreeIter)
                .call();
        for (DiffEntry entry : diffs) {
            System.out.println("\nEntry: " + entry + "|\npath:" + getPath(entry).toString() + "\n");
            entry.getChangeType()
        }
        System.out.println("Done");


        then:
        log.info ""

        repository.close();
    }

    public static Path getPath(DiffEntry entry){
        if(entry.getChangeType().equals(DiffEntry.ChangeType.ADD)){
            return Paths.get(entry.getPath(DiffEntry.Side.NEW)).toAbsolutePath();
        }

        if(entry.getChangeType().equals(DiffEntry.ChangeType.DELETE)){
            return Paths.get(entry.getPath(DiffEntry.Side.OLD)).toAbsolutePath();
        }

        return Paths.get(entry.getPath(DiffEntry.Side.NEW)).toAbsolutePath();
    }
}
