package com.imperva.sampler.outputer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerSamplingOutputer implements SamplingOutputer 
{
    private static Logger logger = LoggerFactory.getLogger(LoggerSamplingOutputer.class);

	public LoggerSamplingOutputer() {
	}
		
	@Override
	public void printIntervalTitle(String title) {
		logger.info(title);
	}

	@Override
	public void printThreadRecord(String threadName, String record) {
		logger.info(threadName + "\n" + record);
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() 
	{
	}

	@Override
	public void printError(String message, Throwable t) {
		logger.error(message, t);
		
	}

}
