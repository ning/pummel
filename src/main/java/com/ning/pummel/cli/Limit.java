package com.ning.pummel.cli;

import com.google.common.collect.Lists;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.skife.cli.Arguments;
import org.skife.cli.Command;
import org.skife.cli.Option;
import com.ning.pummel.Fight;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

@Command(name = "limit", description = "find the lowest concurrency level at which response times stay below a threshold at a certain percentile")
public class Limit implements Callable<Void>
{
    @Option(name = {"-m", "--max"}, title = "max-requests", description = "Maximum number of requests to execute")
    public int maxRequests = -1;

    @Option(name = {"-s", "--start"}, description = "initial concurrency level, defaults to 100")
    public int start = 1;

    @Option(name = {"-l", "--labels"}, description = "Show column labels")
    public boolean labels = false;

    @Option(name = {"-t", "--target"}, description = "target 99th percentile threshold, default is 100")
    public long target = 100L;

    @Option(name = {"-p", "--percentile"}, description = "Percentile to try to target, default is 99th percentile")
    public double percentile = 99.0;

    @Arguments(title = "url file", description = "input file to pull urls from, otherwise will use stdin")
    public File urlFile;

    public Void call() throws Exception
    {
        List<String> urls = Lists.newArrayList();
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


        if (labels) {System.out.printf("clients\ttp%.1f\tmean\treqs/sec\n", percentile);}

        ThreadPoolExecutor exec = Fight.threadPoolExecutor();

        int best_concurency = start;
        int concurrency = start;
        DescriptiveStatistics result;
        DescriptiveStatistics best_result = null;
        double reqs_per_sec;
        double res = 1;
        while ((result = new Fight(exec, concurrency, urls).call()).getPercentile(percentile) < target) {
            res = result.getPercentile(percentile);
            reqs_per_sec = ((1000 / result.getMean()) * concurrency);
            System.out.printf("%d\t%.2f\t%.2f\t%.2f\n", concurrency, res, result.getMean(), reqs_per_sec);
            best_concurency = concurrency;
            best_result = result;
            concurrency = concurrency * 2;
        }
        reqs_per_sec = ((1000 / result.getMean()) * concurrency);
        System.out.printf("%d\t%.2f\t%.2f\t%.2f\n", concurrency, result.getPercentile(percentile), result.getMean(), reqs_per_sec);

        int increment = (int) Math.sqrt((concurrency));
        concurrency = concurrency / 2;
        while ((result = new Fight(exec, concurrency, urls).call()).getPercentile(percentile) < target) {
            res = result.getPercentile(percentile);
            reqs_per_sec = ((1000 / result.getMean()) * concurrency);
            System.out.printf("%d\t%.2f\t%.2f\t%.2f\n", concurrency, res, result.getMean(), reqs_per_sec);
            best_concurency = concurrency;
            best_result = result;
            concurrency += increment;
        }
        reqs_per_sec = ((1000 / result.getMean()) * concurrency);
        System.out.printf("%d\t%.2f\t%.2f\t%.2f\n", concurrency, result.getPercentile(percentile), result.getMean(), reqs_per_sec);

        increment = (int) Math.sqrt(Math.sqrt(concurrency));
        concurrency = concurrency - (2 * increment);
        while ((result = new Fight(exec, concurrency, urls).call()).getPercentile(percentile) < target) {
            res = result.getPercentile(percentile);
            reqs_per_sec = ((1000 / result.getMean()) * concurrency);
            System.out.printf("%d\t%.2f\t%.2f\t%.2f\n", concurrency, res, result.getMean(), reqs_per_sec);
            best_concurency = concurrency;
            best_result = result;
            concurrency += increment;
        }
        reqs_per_sec = ((1000 / result.getMean()) * concurrency);
        System.out.printf("%d\t%.2f\t%.2f\t%.2f\n", concurrency, result.getPercentile(percentile), result.getMean(), reqs_per_sec);


        assert best_result != null;
        reqs_per_sec = ((1000 / best_result.getMean()) * best_concurency);
        System.out.printf("%d\t%.2f\t%.2f\t%.2f\n", best_concurency, best_result.getPercentile(percentile), best_result.getMean(), reqs_per_sec);

        exec.shutdownNow();

        return null;
    }
}
