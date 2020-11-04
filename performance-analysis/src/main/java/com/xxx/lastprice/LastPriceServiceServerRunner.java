package com.xxx.lastprice;

import com.xxx.lastprice.server.LastPriceServer;

import java.io.IOException;

/**
 * @author Evgeny Stankevich {@literal <stankevich.evg@gmail.com>}.
 */
public class LastPriceServiceServerRunner {

    public static void main(String[] args) throws IOException, InterruptedException {
        final LastPriceServer server = new LastPriceServer();
        server.start();
    }

}
