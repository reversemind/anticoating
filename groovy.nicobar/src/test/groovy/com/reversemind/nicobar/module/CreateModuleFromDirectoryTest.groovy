package com.reversemind.nicobar.module

import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.hash.Hasher
import com.google.common.hash.Hashing
import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 *
 */
@Slf4j
class CreateModuleFromDirectoryTest extends Specification {

    def 'create module form directory'() {
        setup:
        log.info "GO"


        // #1 Watch directory

        // #2 Create module for directory
        // 2.1 - select directory to compile - all structure
        // 2.2 Compile it - if compilation is succesfull that ready to create jar file


        // #3 is it really need to compile fileda


        // Guava sha1 - git file hash
        HashFunction hashFunction = Hashing.sha1();
        Hasher hasher = hashFunction.newHasher()
        hasher.putBytes("".getBytes("UTF-8"))

        HashCode hashCode = hasher.hash()
        println "sha1:" + hashCode.toString()


        // http://stackoverflow.com/questions/7225313/how-does-git-compute-file-hashes

        /*
        How does GIT compute its commit hashes

        Commit Hash (SHA1) = SHA1("blob" + <size_of_file> + "\0" + <contents_of_file>)
         */
    }
}
