package com.ning.pummel;

import com.squareup.okhttp.OkHttpClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Fist implements Callable<Poll>
{
    private static final OkHttpClient http = new OkHttpClient();
    private final URL url;
    private final byte[] body;

    public Fist(String url) throws MalformedURLException
    {
        String[] parts = url.split("\\s+");
        this.url = new URL(parts[0]);

        if (parts.length == 1) {
            body = null;
        }
        else {
            body = parts[1].getBytes();
        }

    }

    public Poll call() throws Exception
    {
        long start = System.nanoTime();
        HttpURLConnection conn = http.open(this.url);
        if (body != null) {
            conn.setRequestMethod("POST");

            OutputStream out = conn.getOutputStream();
            out.write(body);
            out.close();
        }

        InputStream in = conn.getInputStream();
        byte[] buffer = new byte[4096];

        //noinspection StatementWithEmptyBody
        while (in.read(buffer) != -1) {
            // discard
        }
        in.close();

        long stop = System.nanoTime();
        long duration_nanos = stop - start;
        long millis = TimeUnit.MILLISECONDS.convert(duration_nanos, TimeUnit.NANOSECONDS);
        return new Poll(millis);
    }
}
