package com.company.deep

import spock.lang.Specification

/**
 *
 */
class FindExceptionsDeepTest extends Specification {

    def 'deel search'(){
        setup:

        String[] names = [
            'SpmsFavoriteRegionCapacityException',
            'SpmsPackageAutoRulesIntersectException',
            'SpmsRegionNotFoundException',
            'SpmsSubscriberInMigrationException',
            'SpmsMuiaQuotaLimitExceededException',
            'SpmsPackageCounterFormulasIntersectException',
            'SpmsSpecialObjectNameUsedException',
            'SpmsSubscriberNoPackageException',
            'SpmsNewRegionIsHomeException',
            'SpmsPackageNotAllowedToBeAddedException',
            'SpmsSubscriberAlreadyExistsException',
            'SpmsSubscriberNotFoundException',
            'SpmsObjectNotFoundException',
            'SpmsPackageRegionalPriceException',
            'SpmsSubscriberExistsWithinAnotherRegionException'
        ]

        def pathV3 = "/version3.x"
        def pathOriginal = "/master"

        names.each { name ->
            def command = "find ${pathOriginal} -name \"*.java\" ! -name \"${name}.java\"| xargs grep -R -i -n '${name}'"

            println "\n---------------------------------------------"
            println "oName:${name}\n"
            RunBash.bash(command)
        }

    }
}
