package com.company.restlet.part001;

import org.restlet.resource.ClientResource;

/**
 *
 */
public class Client {
    public static void main(String[] args) throws Exception {
        ClientResource clientResource = new ClientResource("http://localhost:" + Config.PORT_NUMBER + "/");
        clientResource.get().write(System.out);
    }
}
