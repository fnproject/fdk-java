package com.fnproject.fn.api;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created on 10/09/2018.
 * <p>
 * (c) 2018 Oracle Corporation
 */
public class HeadersTest {

    @Test
    public void shouldCanonicalizeHeaders(){
        for (String[] v : new String[][] {
          {"",""},
          {"a","A"},
          {"fn-ID-","Fn-Id-"},
          {"myHeader-VaLue","Myheader-Value"},
          {" Not a Header "," Not a Header "},
          {"-","-"},
          {"--","--"},
          {"a-","A-"},
          {"-a","-A"}
        }){
            assertThat(Headers.canonicalKey(v[0])).isEqualTo(v[1]);
        }
    }


}
