package com.imperva.sampler.outputer;

public interface SamplingOutputer 
{
	public void printIntervalTitle(String title);
	public void printThreadRecord(String threadName, String record);
	public void printError(String message, Throwable t);
	public void flush();
	public void close();
}
