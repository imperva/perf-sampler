package com.imperva.sampler;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.imperva.sampler.manipulate.RegexThreadNameManipulator;
import com.imperva.sampler.manipulate.ThreadNameManipulator;
import com.imperva.sampler.outputer.PrintStreamSamplingOutputer;
import com.imperva.sampler.outputer.SamplingOutputer;


public class ThreadsSampler implements Runnable, AutoCloseable
{
	private volatile boolean m_continue = true;
	private volatile boolean m_printRequested = false;
	private long m_sleepBetweenSamplesInMillis = 100L;
	private String[] m_packagePrefixes = {"com.imperva.", "com.mprv."};
	private final HashMap<String, ThreadGroupSamples> m_samplingMap = new HashMap<>();
	private long m_reportIntervalMillis = 900000L;
	private volatile long lastReportAt = System.currentTimeMillis();
	private ThreadNameManipulator threadNameManipulator = null;
	private SamplingOutputer outputer = null;
	private volatile boolean isActive = true, isStarted = false;
	private volatile boolean isSampleOnceAndPrint = false;
	private static final Pattern commaSplitter = Pattern.compile(",");
	private boolean skipDaemonThreads = true;
	private Thread samplerThread = null;
	private final HashSet<Thread> sampleTheseThreadOnly = new HashSet<>();
	private boolean isReportZeroTimePackages = false;
	private volatile boolean isEmptySamplingMap = true;

	private long prevTime;
	private long sampleDuration;
	
	private static final Pattern digitsRemover = Pattern.compile("\\d+");
	
	public void setSamplingFrequencyMillis(long frequencyInMillis) {
		m_sleepBetweenSamplesInMillis = frequencyInMillis;
	}
	
	public void setReportFrequencySeconds(long frequencyInSeconds) {
		m_reportIntervalMillis = 1000L * frequencyInSeconds;
	}

	public void setSampleTheseThreadsOnly(List<Thread> threads) {
		if (threads.size() > 0) {
			sampleTheseThreadOnly.addAll(threads);
		}
		else {
			sampleTheseThreadOnly.clear();
		}
	}

	public void setReportZeroTimePackages(boolean isReportZeroTimePackages) {
	    this.isReportZeroTimePackages = isReportZeroTimePackages;
    }

	public void setThreadToBeSampled(Thread thread) {
		sampleTheseThreadOnly.add(thread);
	}

	public void setSkipDaemonThreads(boolean shouldSkip) { skipDaemonThreads = shouldSkip; }
	
	public void setMonitoredPackages(String commaDelimitedPackageNames) {
		m_packagePrefixes = commaDelimitedPackageNames.split(",");
	}
	
	public void setSamplingOutputer(SamplingOutputer outputer) 
	{
		this.outputer = outputer;
	}
	
	public void setThreadNameManipulator(ThreadNameManipulator tnm) {
		threadNameManipulator = tnm;
	}
		
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	public void init() 
	{
		if (threadNameManipulator == null) {
			threadNameManipulator = new RegexThreadNameManipulator();
		}
		
		threadNameManipulator.init();
		
		if (outputer == null) {
			outputer = new PrintStreamSamplingOutputer();
		}
		
		Thread t = new Thread(this);
		t.setDaemon(true);
		t.setName("Performance Sampler");
		t.start();

		try {
			for (int i = 0; i < 50 && ! isStarted; i++) {
				Thread.sleep(1L);
			}
		} catch (InterruptedException e) {}
	}
		
	@Override
	public void run() 
	{
		isStarted = true;
		samplerThread = Thread.currentThread();
		prevTime = System.currentTimeMillis();
		sampleDuration = 0;
		while (m_continue)
		{
			try {
				processLoop();
			} catch (Exception e) {
				outputer.printError("ThreadsSampler -  failed", e);
			}

		}

		printReportNow();
	}

	private void processLoop() {

		if (m_sleepBetweenSamplesInMillis > sampleDuration)
		{
			try {
				Thread.sleep(m_sleepBetweenSamplesInMillis - sampleDuration);
			}
			catch (InterruptedException ie) {
				Thread.interrupted();
			}
		}

		if (m_reportIntervalMillis > 0  && System.currentTimeMillis() > m_reportIntervalMillis + lastReportAt) {
			m_printRequested = true;
		}

		if (m_printRequested) {
			printReportNow();
			m_printRequested = false;
			lastReportAt = System.currentTimeMillis();
		}

		long smapleStart = System.currentTimeMillis();
		if (isActive) {
			sampleOnce(System.currentTimeMillis() - prevTime + sampleDuration);
		}


		if (isSampleOnceAndPrint) {
			sampleOnce(m_sleepBetweenSamplesInMillis);
			lastReportAt = System.currentTimeMillis();
			printReportNow();
			isSampleOnceAndPrint = false;
		}

		sampleDuration = System.currentTimeMillis() - smapleStart;
		prevTime = System.currentTimeMillis();
	}
	
	public void pauseSampling() {
		isActive = false;
	}
	
	public void resumeSampling() {
		isActive = true;
		lastReportAt = System.currentTimeMillis();
	}
	
	public void sampleOnceAndPrint()
	{
		if (isActive || samplerThread == null) {
			throw new RuntimeException("Sequence error. " +
					"Start the sampler and invoke pauseSampling() first. Only then, invoke sampleOnceAndPrint().");
		}

		isSampleOnceAndPrint = true;
		samplerThread.interrupt();
	}
	
	private void sampleOnce(long duration)
	{
		HashMap<Thread, StackTraceElement[]> sampledThreadsMap = null;
		if (sampleTheseThreadOnly.size() > 0) {
			sampledThreadsMap = new HashMap<>(sampleTheseThreadOnly.size() * 3);
			for (Thread toBeSampled : sampleTheseThreadOnly) {
				sampledThreadsMap.put(toBeSampled, toBeSampled.getStackTrace());
			}
		}
		Collection<Map.Entry<Thread, StackTraceElement[]>> threadEntries = sampleTheseThreadOnly.size() > 0 ?
				sampledThreadsMap.entrySet() : Thread.getAllStackTraces().entrySet();
		for (Map.Entry<Thread, StackTraceElement[]> threadEntry : threadEntries)
		{
			Thread sampledThread = threadEntry.getKey();
			if (sampleTheseThreadOnly.size() > 0 && ! sampleTheseThreadOnly.contains(sampledThread)) {
				continue;
			}

			String aggregatedThreadName = threadNameManipulator.getManipulatedThreadName(sampledThread);
			if (aggregatedThreadName == null || (skipDaemonThreads && sampledThread.isDaemon())) {
				continue;
			}

			ThreadGroupSamples threadGroupSamples = m_samplingMap.get(aggregatedThreadName);
			if (threadGroupSamples == null)
			{
				threadGroupSamples = new ThreadGroupSamples();
				m_samplingMap.put(aggregatedThreadName, threadGroupSamples);
			}

			StackTraceElement[] stackTrace = threadEntry.getValue();
			StringBuilder sb = new StringBuilder();
			threadGroupSamples.threadNamesMap.put(sampledThread.getId(), sampledThread.getName());
			if (stackTrace.length > threadGroupSamples.maxDepth) {
				threadGroupSamples.maxDepth = stackTrace.length;
			}

			int deepestIndex = 0;
			if (m_packagePrefixes.length > 0 && stackTrace.length > 0)
			{
				deepestIndex = (stackTrace.length < 2) ? stackTrace.length : stackTrace.length - 1;
				for (int i = stackTrace.length - 1; i >= 1; i--)
				{
					if (isMonitoredPackageName(stackTrace[i].getClassName()))
					{
						if (i < 2) {
							deepestIndex = 0;
						} else {
							deepestIndex = i - 1;
						}
					}
				}
			}

			for (int i = stackTrace.length - 1; i >= deepestIndex; i--)
			{
				if (i < stackTrace.length - 1) {
					sb.append(",");
				}
				
				String packageClassMethodLine = stackTrace[i].toString();
				if (stackTrace[i].getClassName().startsWith("sun.reflect.GeneratedMethodAccessor")) {
					packageClassMethodLine = digitsRemover.matcher(packageClassMethodLine).replaceAll("");
				}
				sb.append(packageClassMethodLine);
				
				String methodKey = sb.toString();
				SampleCount count = threadGroupSamples.samplingMap.get(methodKey);
				if (count == null)
				{
					count = new SampleCount(packageClassMethodLine, stackTrace.length - i - 1);
					threadGroupSamples.samplingMap.put(methodKey, count);
				}

				count.increment(duration, i == deepestIndex);
			}
		}

		isEmptySamplingMap = m_samplingMap.size() == 0;
	}
	
	private boolean isMonitoredPackageName(String fullPackageName) 
	{
		for (String monitoredPackagePrefix : m_packagePrefixes) {
			if (fullPackageName.startsWith(monitoredPackagePrefix)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void shutdown()
	{
		if (samplerThread != null && samplerThread.isAlive())
		{
			try
			{
				m_continue = false;
				isActive = false;

				if (m_samplingMap.size() > 0) {
					printReport();
				}
				samplerThread.interrupt();
				for (int i = 0; i < 50 && samplerThread.isAlive(); i++) {
					Thread.sleep(1L);
				}
			} catch (InterruptedException e) {}
		}
		else {
			printReportNow();
		}
	}
	
	public void printReport()
	{
		m_printRequested = true;
	}

	private static final Pattern packagePattern = Pattern.compile("([a-z]+\\.([a-z]+|)).*");

	private synchronized void printReportNow()
	{
	    if (isEmptySamplingMap) {
	    	return;
		}
		String spaces = "                                                                                                                                                      ";
		long[] depthMethodTime = new long[1000];
		long[] depthCummulativeTime = new long[1000];
		String[] depthPackage = new String[1000];
		outputer.printIntervalTitle("Report interval: from " + new Date(lastReportAt) + " till " + new Date());
		for (Map.Entry<String, ThreadGroupSamples> aggregatedThread : m_samplingMap.entrySet())
		{
			ThreadGroupSamples  tgs = aggregatedThread.getValue();
			String threadTitle = "Aggregated thread: " + aggregatedThread.getKey() + ". Max depth: " + tgs.maxDepth +
					". Distinct threads: " + tgs.threadNamesMap.size() + " " + tgs.threadNamesMap.values();
			StringBuilder sb = new StringBuilder();
			
			TreeMap<String, SampleCount> methodsMap = tgs.samplingMap;
			for (Map.Entry<String, SampleCount> method: methodsMap.entrySet())
			{
				SampleCount sc = method.getValue();
				if (sc.getDepth() < depthMethodTime.length) {
					depthMethodTime[sc.getDepth()] = sc.getMethodTime();
					depthCummulativeTime[sc.getDepth()] = sc.getCummulativeTime();
					if (isReportZeroTimePackages) {
                        Matcher packageMatcher = packagePattern.matcher(sc.getPackageClassMethodLine());
                        depthPackage[sc.getDepth()] = packageMatcher.matches() ? packageMatcher.group(1) : sc.getPackageClassMethodLine();
                    } else {
                        depthPackage[sc.getDepth()] = "";
                    }
				}
				
				if (sc.getDepth() >= depthMethodTime.length || (sc.getMethodTime() == 0L &&
						sc.getDepth() > 0 && sc.getCummulativeTime() == depthCummulativeTime[sc.getDepth() - 1] &&
						depthPackage[sc.getDepth()].equals(depthPackage[sc.getDepth() - 1]))) {
					continue;
				}
				
				int collapsedDepth = 0;
				String[] tokens = commaSplitter.split(method.getKey());
				for (int i = 0; i < sc.getDepth(); i++)
				{
					if (depthCummulativeTime[i] != depthCummulativeTime[i + 1] || depthMethodTime[i + 1] > 0L ||
							! depthPackage[i +1].equals(depthPackage[i])) {
						collapsedDepth++;
						sb.append("  ");
					}
				}
				
				int methodLength = 2 * collapsedDepth + tokens[sc.getDepth()].length();
				
				sb.append(tokens[sc.getDepth()]).append(" ").append(spaces.substring(0, methodLength < spaces.length() ? spaces.length() - methodLength : 0)).
					append(" Cumulative time(ms): ").append(sc.getCummulativeTime()).append(", Method time(ms): ").append(sc.getMethodTime()).append("\n");
			}
			
			outputer.printThreadRecord(threadTitle, sb.toString());
		}
		
		outputer.flush();
		m_samplingMap.clear();
		isEmptySamplingMap = true;
	}

	@Override
	public void close() throws IOException 
	{
		shutdown();

		if (outputer != null) {
			outputer.close();
		}
	}
}
