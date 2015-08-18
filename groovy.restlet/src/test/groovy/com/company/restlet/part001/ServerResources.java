package com.company.restlet.part001;

import groovy.util.logging.Slf4j;
import org.restlet.resource.Get;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Server resource
 */
@Slf4j
public class ServerResources extends org.restlet.resource.ServerResource {

    private final Logger LOG = getLogger();

    @Get("json")
    public String getResult() {
        LOG.info("touch the resource at " + new Date());
        return "response from resource:" + ServerResources.class.getCanonicalName();
    }

}
