package com.imperva.sampler;

import com.imperva.sampler.manipulate.RegexThreadNameManipulator;
import com.imperva.sampler.outputer.PrintStreamSamplingOutputer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadsSamplerTest
{
    @Test
    public void testSnapshotSampler() throws IOException {
        ThreadsSampler ts = new ThreadsSampler();
        ts.setActive(false);
        ts.setMonitoredPackages("com.imperva");
        ts.setReportFrequencySeconds(0);
        ts.setSamplingFrequencyMillis(30L);
        ts.setSkipDaemonThreads(true);
        ts.setThreadToBeSampled(Thread.currentThread());
        ts.setReportZeroTimePackages(true);
        ts.setSamplingOutputer(new PrintStreamSamplingOutputer());
        ts.init();
        ts.sampleOnceAndPrint();
        ts.close();
    }

    @Test
    public void testAggregationSampler() throws InterruptedException, IOException
    {
        try (ThreadsSampler ts = new ThreadsSampler())
        {
            ts.setMonitoredPackages("com.imperva");
            ts.setReportFrequencySeconds(0);
            ts.setSamplingFrequencyMillis(20L);
            ts.setSkipDaemonThreads(true);
            ts.setActive(true);
            ts.setSampleTheseThreadsOnly(Collections.singletonList(Thread.currentThread()));
            ts.setThreadNameManipulator(new RegexThreadNameManipulator());
            ts.init();
            Thread.sleep(40L);
        }
    }

    public static class CpuBoundTask implements Callable<Integer> {
        @Override
        public Integer call() {
            int rc = 0;
            for (int i = 0; i < 1000000; i++) {
                rc = (int) (Math.random() * i);
                if (i % 10 == 0) {
                    rc = calcEveryTenth();

                }
             }

            return rc;
        }

        private int calcEveryTenth() {
            return (int) (Math.random() * 4);
        }
    }

    @Test
    public void testOverhead() throws InterruptedException, IOException
    {
        ExecutorService es = Executors.newFixedThreadPool(3);
        ArrayList<CpuBoundTask> tasks = new ArrayList<>(120);
        CpuBoundTask cpuBoundTask  = new CpuBoundTask();
        for (int i = 0; i < 120; i++) {
            tasks.add(new CpuBoundTask());
        }

        System.out.println("Start warming up");
        es.invokeAll(tasks.subList(0, 3));
        System.out.println("Start perf test");
        long startTime = System.currentTimeMillis();
        es.invokeAll(tasks);
        long durationNoSampling = System.currentTimeMillis() - startTime;
        System.out.println("No sampling duration: " + durationNoSampling);
        try (ThreadsSampler ts = new ThreadsSampler())
        {
            ts.setMonitoredPackages("com.imperva");
            ts.setReportFrequencySeconds(0);
            ts.setSamplingFrequencyMillis(20L);
            ts.setSkipDaemonThreads(true);
            ts.setActive(true);
            ts.init();
            startTime = System.currentTimeMillis();
            es.invokeAll(tasks);
            long durationSamplingAll = System.currentTimeMillis() - startTime;
            System.out.println("Sampling all duration: " + durationSamplingAll +
                    ". Overhead: " + ((durationSamplingAll - durationNoSampling) * 100 / durationNoSampling) + "%");

            Assert.assertTrue("Sampling overhead should be below 10%. Duration without sampling: " +
                    durationNoSampling + ", Duration with sampling all: " + durationSamplingAll,
                    ((durationSamplingAll - durationNoSampling) * 100 / durationNoSampling) < 10);
        }

        try (ThreadsSampler ts = new ThreadsSampler())
        {
            ts.setMonitoredPackages("com.imperva");
            ts.setReportFrequencySeconds(0);
            ts.setSamplingFrequencyMillis(20L);
            ts.setSkipDaemonThreads(true);
            ts.setActive(true);
            ts.setThreadToBeSampled(Thread.currentThread());
            ts.init();
            startTime = System.currentTimeMillis();
            es.invokeAll(tasks);
            long durationSamplingSingle = System.currentTimeMillis() - startTime;
            System.out.println("Sampling one duration: " + durationSamplingSingle +
                    ". Overhead: " + ((durationSamplingSingle - durationNoSampling) * 100 / durationNoSampling) + "%");

            Assert.assertTrue("Sampling overhead should be below 4%. Duration without sampling: " +
                            durationNoSampling + ", Duration with sampling single: " + durationSamplingSingle,
                    ((durationSamplingSingle - durationNoSampling) * 100 / durationNoSampling) < 4);
        }
    }

}
