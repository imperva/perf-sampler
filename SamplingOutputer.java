package com.incapsula.sampler.outputer;

public interface SamplingOutputer 
{
	public void printIntervalTitle(String title);
	public void printThreadRecord(String threadName, String record);
	public void flush();
	public void close();
}
