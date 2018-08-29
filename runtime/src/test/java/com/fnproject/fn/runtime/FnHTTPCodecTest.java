package com.fnproject.fn.runtime;


import com.fnproject.fn.api.InputEvent;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * Created on 24/08/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class FnHTTPCodecTest {


    @Test
    public void shoouldAcceptDataOnHttp () throws Exception{

        Map<String,String> env = new HashMap<>();
        env.put("FN_APP","myapp");
        env.put("FN_PATH","mypath");

        CountDownLatch cdl = new CountDownLatch(1);
        new Thread(()->{
            try{
                cdl.await();

            }catch(InterruptedException ignored){}

        });

        try(FnHTTPCodec codec = new FnHTTPCodec(env)){
            cdl.countDown();

            Optional<InputEvent> evt = codec.readEvent();
        }



    }
}
