package com.ning.pummel.cli;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.skife.cli.Arguments;
import org.skife.cli.Command;
import org.skife.cli.Option;
import com.ning.pummel.Fist;
import com.ning.pummel.Poll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Command(name = {"benchmark", "go"}, description = "Go beat up a server")
public class Benchmark implements Callable<Void>
{
    @Option(name = {"-c", "--concurrency"}, description = "concurrency -- how many requests to keep in flight at once")
    public int concurrency = 10;

    @Option(name = {"-m", "--max"}, description = "Maximum number of requests to execute")
    public int maxRequests = -1;

    @Option(name = {"-r", "--report"}, description = "report basic stats on stderr when finished")
    public boolean report = false;

    @Arguments(title="url file", description = "input file to pull urls from, otherwise will use stdin")
    public File urlFile;

    public Void call() throws Exception
    {
        LinkedBlockingQueue<String> urls = new LinkedBlockingQueue<String>();
        final BufferedReader in;
        if (urlFile != null) {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(urlFile)));
        }
        else {
            in = new BufferedReader(new InputStreamReader(System.in));
        }
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            if (maxRequests >= 0) {
                if (maxRequests == 0) {
                    break;
                }
                else if (maxRequests > 0) {
                    maxRequests--;
                }
            }

            urls.add(line);
        }

        ExecutorService exec = Executors.newFixedThreadPool(concurrency);
        ExecutorCompletionService<Poll> ecs = new ExecutorCompletionService<Poll>(exec);

        long start = System.nanoTime();
        for (String url : urls) {
            ecs.submit(new Fist(url));
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = urls.size(); i != 0; i--) {
            Poll poll = ecs.take().get();
            System.out.println(poll.getTime());
            stats.addValue(poll.getTime());
        }

        long finish = System.nanoTime();

        if (report) {
            System.err.printf("time\t%d\n", nsToMs(finish - start));
            System.err.printf("n\t%d\n", stats.getN());
            System.err.printf("max\t%f\n", nsToMs(stats.getMax()));
            System.err.printf("mean\t%f\n", nsToMs(stats.getMean()));
            System.err.printf("99.9%%\t%f\n", nsToMs(stats.getPercentile(99.9)));
            System.err.printf("99%%\t%f\n", nsToMs(stats.getPercentile(99)));
            System.err.printf("90%%\t%f\n", nsToMs(stats.getPercentile(90)));
            System.err.printf("80%%\t%f\n", nsToMs(stats.getPercentile(80)));
            System.err.printf("50%%\t%f\n", nsToMs(stats.getPercentile(50)));
        }

        exec.shutdown();

        return null;
    }

    static double nsToMs(double duration_nanos) {
        return duration_nanos / MILLISECONDS.toNanos(1);
    }
}
