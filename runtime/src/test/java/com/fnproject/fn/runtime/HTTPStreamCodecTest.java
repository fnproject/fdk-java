package com.fnproject.fn.runtime;


import com.fnproject.fn.api.InputEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.unixsocket.client.HttpClientTransportOverUnixSockets;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

import org.junit.Test;

/**
 * Created on 24/08/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class HTTPStreamCodecTest {

    @Test
    public void shouldAcceptDataOnHttp () throws Exception{
	final String FN_LISTENER="/tmp/fn.sock";

        StdErrLog logger = new StdErrLog();
        logger.setDebugEnabled(true);
        Log.setLog(logger);

        Map<String,String> env = new HashMap<>();
        env.put("FN_APP_NAME","myapp");
        env.put("FN_PATH","mypath");
	env.put("FN_LISTENER",FN_LISTENER);

        CountDownLatch cdl = new CountDownLatch(1);
        new Thread(()->{
            try{
                HttpClient httpClient = new HttpClient(new HttpClientTransportOverUnixSockets(FN_LISTENER), null);
	        httpClient.start();
                cdl.await();
		httpClient.newRequest("http://localhost").send();
            }catch(Exception ignored){}

        }).start();

        try(HTTPStreamCodec codec = new HTTPStreamCodec(env)){
            cdl.countDown();

            Optional<InputEvent> evt = codec.readEvent();
        }

    }
}
