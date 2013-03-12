package com.ning.pummel;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Fight implements Callable<DescriptiveStatistics>
{
    private final ThreadPoolExecutor exec;
    private final int          concurrency;
    private final List<String> urls;

    public Fight(ThreadPoolExecutor exec, int concurrency, List<String> urls)
    {
        this.exec = exec;
        this.concurrency = concurrency;
        this.urls = urls;

        exec.setCorePoolSize(concurrency);
        exec.prestartAllCoreThreads();
    }

    public DescriptiveStatistics call() throws Exception
    {
        ExecutorCompletionService<Poll> ecs = new ExecutorCompletionService<Poll>(exec);

        for (String url : urls) {
            ecs.submit(new Fist(url));
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = urls.size(); i != 0; i--) {
            Poll poll = ecs.take().get();
            stats.addValue(poll.getTime());
        }
        return stats;
    }

    public static ThreadPoolExecutor threadPoolExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, /* will be resized by Fight.class */
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        return executor;
    }
}
