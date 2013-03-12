package com.ning.pummel.cli;

import clojure.lang.IFn;
import clojure.lang.RT;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.ning.pummel.Fight;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.mvel.MVEL;
import org.skife.cli.Arguments;
import org.skife.cli.Command;
import org.skife.cli.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Iterables.cycle;
import static com.ning.pummel.cli.Benchmark.nsToMs;
import static java.util.concurrent.TimeUnit.SECONDS;

@Command(name = "step", description = "Step through concurrency levels and report statistics")
public class Step implements Callable<Void>
{
    @Option(name = {"-c", "--cycle"}, title = "cycle/loop", description = "Cycle or loop through url file?")
    public boolean cycle = false;

    @Option(name = {"-m", "--max"}, title = "max-requests", description = "Maximum number of requests to execute")
    public int maxRequests = -1;

    @Option(name = {"-s", "--start"}, description = "initial concurrency level, defaults to 1")
    public int start = 1;

    @Option(name = "--step", title = "step-function", description = "clojure function to apply to generate next step, default is 'c + 1'")
    public String stepFunction = "c + 1";

    @Option(name = {"-l", "--labels"}, description = "Show column labels")
    public boolean labels = false;

    @Option(name = {"-L", "--limit"}, description = "concurrency limit to stop at, default is " + Integer.MAX_VALUE)
    public int limit = Integer.MAX_VALUE;

    @Option(name = {"-p", "--percentile"}, description = "Percentile to report, default is 99th percentile")
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

        for (String line = in.readLine(); line != null && maxRequests != 0; line = in.readLine(), maxRequests--) {
            urls.add(line);
        }

        if (cycle && maxRequests > 0) {
            List<String> urlsCopy = Lists.newArrayList(urls);
            addAll(urls, limit(cycle(urlsCopy), maxRequests));
        }

        Optional<StepFunction> of = mvel(stepFunction);
        if (!of.isPresent()) {
            of = clojure(stepFunction);
        }

        if (!of.isPresent()) {
            System.err.printf("'%s' is an invalid step function\n", stepFunction);
            return null;
        }

        StepFunction step = of.get();

        if (labels) {System.out.printf("clients\ttp%.1f\tmean\tstddev\tmax\tcount\ttime\treqs/sec\n", percentile);}
        int concurrency = start;
        ThreadPoolExecutor exec = Fight.threadPoolExecutor();

        do {
            Fight fight = new Fight(exec, concurrency, urls);

            long start = System.nanoTime();
            DescriptiveStatistics stats = fight.call();
            long stop = System.nanoTime();
            long duration_nanos = stop - start;

            System.out.printf("%d\t%.2f\t%.2f\t%.2f\t%.2f\t%d\t%.2f\t%.2f\n",
                              concurrency,
                              nsToMs(stats.getPercentile(percentile)),
                              nsToMs(stats.getMean()),
                              nsToMs(stats.getStandardDeviation()),
                              nsToMs(stats.getMax()),
                              stats.getN(),
                              ((double) duration_nanos) / SECONDS.toNanos(1),
                              ((double) stats.getN()) * SECONDS.toNanos(1) / duration_nanos);
            concurrency = step.step(concurrency);
        }
        while (concurrency < limit);

        exec.shutdownNow();

        return null;
    }

    private static interface StepFunction
    {
        public int step(int from);
    }


    private static Optional<StepFunction> clojure(String src) throws ClassNotFoundException
    {
        Class.forName(RT.class.getName());
        try {
            final IFn fn = (IFn) clojure.lang.Compiler.load(new StringReader(src));
            StepFunction step = new StepFunction()
            {
                public int step(int from)
                {
                    return ((Long)fn.invoke(from)).intValue();
                }
            };
            return Optional.of(step);

        }
        catch (Exception e) {
            return Optional.absent();
        }

    }

    private static Optional<StepFunction> mvel(String source)
    {
        try {
            final Serializable mvel = MVEL.compileExpression(source);
            MVEL.executeExpression(mvel, ImmutableMap.of("c", 1));
            StepFunction step = new StepFunction()
            {
                public int step(int from)
                {
                    return (Integer) MVEL.executeExpression(mvel, ImmutableMap.of("c", from));
                }
            };
            return Optional.of(step);
        }
        catch (Exception e) {
            return Optional.absent();
        }
    }
}
