package com.company.test.httpclient.rest;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 *
 */
public class RestClientByHttpClientTest {

    private static final Mapper beanMapper = new DozerBeanMapper(Collections.singletonList("dozer-mapping.xml"));

    @Test
    public void testHttpClient() throws IOException {

        final String billingRestApiInternal = "http://msdp-spms-web:50002/billing/api/internal/balance/12345678901";

        HttpClient httpClient = new DefaultHttpClient();

        HttpGet httpGet = new HttpGet(billingRestApiInternal);
        httpGet.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        HttpResponse resp = httpClient.execute(httpGet);
        HttpEntity ent = resp.getEntity();

        StatusLine statusLine = resp.getStatusLine();
        System.out.println("\n\n - statusLine:" + statusLine + "  // code:" + statusLine.getStatusCode() + "  // message:" + statusLine.getReasonPhrase());

        if (ent != null) {
            try {
                InputStream is = ent.getContent();
                if (is != null) {
                    try {
                        String response = IOUtils.toString(is);
                        System.out.println("\n\n RESP:" + response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        is.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                EntityUtils.consume(ent);
            }
        }

    }

    @Test
    public void testParseJSON() {
        String JSON = "{\"amount\":1,\"currency\":{\"symbol\":\"currencySymbol\",\"code\":\"currencyCode\",\"name\":\"currencyName\"},\"balanceDate\":\"2015-07-08T10:22:37.800+03:00\",\"dateOfExpire\":\"2015-07-08T10:22:37.800+03:00\"}";

        ObjectMapper objectMapper = new ObjectMapper();

        BalanceResponse balanceResponse = null;
        try {
            balanceResponse = objectMapper.readValue(JSON, BalanceResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("balanceResponse:" + balanceResponse);
        System.out.println("Date:" + balanceResponse);


        String JSON_WRONG = "{\"amounts\":1,\"currencies\":{\"symbol\":\"currencySymbol\",\"code\":\"currencyCode\",\"name\":\"currencyName\"},\"balanceDate\":\"2015-07-08T10:22:37.800+03:00\",\"dateOfExpire\":\"2015-07-08T10:22:37.800+03:00\"}";
        try {
            balanceResponse = objectMapper.readValue(JSON_WRONG, BalanceResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testBalanceResponseToBalanceDataMapping() {

        String JSON = "{\"amount\":1,\"currency\":{\"symbol\":\"currencySymbol\",\"code\":\"currencyCode\",\"name\":\"currencyName\"},\"balanceDate\":\"2015-07-08T10:22:37.800+03:00\",\"dateOfExpire\":\"2015-07-08T10:22:37.800+03:00\"}";

        ObjectMapper objectMapper = new ObjectMapper();

        BalanceResponse balanceResponse = null;
        try {
            balanceResponse = objectMapper.readValue(JSON, BalanceResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BalanceData balanceData = beanMapper.map(balanceResponse, BalanceData.class);

        System.out.println("balanceData:" + balanceData);
    }
}
