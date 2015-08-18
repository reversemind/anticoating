package com.company.restlet.part001;

import org.restlet.data.Protocol;

/**
 *
 */
public class Server {

    public static void main(String[] args) throws Exception {
        org.restlet.Server server = new org.restlet.Server(Protocol.HTTP, Config.PORT_NUMBER, ServerResources.class);
        server.start();
    }
}
