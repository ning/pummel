package com.ning.pummel;

import com.google.common.io.ByteStreams;
import com.google.common.io.NullOutputStream;
import com.google.common.io.Resources;

import java.net.URL;
import java.util.concurrent.Callable;

public class Fist implements Callable<Poll>
{
    private final String     url;

    public Fist(String url)
    {
        this.url = url;
    }

    public Poll call() throws Exception
    {
        long start = System.nanoTime();
        ByteStreams.copy(Resources.newInputStreamSupplier(new URL(this.url)), new NullOutputStream());

        long stop = System.nanoTime();
        long duration_nanos = stop - start;
        return new Poll(url, duration_nanos);
    }
}
