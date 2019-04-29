package com.incapsula.sampler.outputer;

import java.io.File;
import java.io.PrintStream;

public class PrintStreamSamplingOutputer implements SamplingOutputer 
{
	private PrintStream ps = System.out;
	private boolean isFile = false;
	
	public PrintStreamSamplingOutputer() {
	}
	
	public PrintStreamSamplingOutputer(String fileName) 
	{
		try 
		{
			File parentDir = new File(fileName).getParentFile();
			if (parentDir != null && parentDir.exists() == false) {
				if(!parentDir.mkdirs()){
					throw new Exception("PrintStreamSamplingOutputer: failed to make dirs");
				}
			}
			ps = new PrintStream(fileName, "UTF-8");
			isFile = true;
		}
		catch(Exception e) 
		{
			System.out.println("com.imperva.sampler.PrintStreamSamplingOutputer - Failed to open file " + fileName + ". Will log sampler output to stdout instead.");
			e.printStackTrace(System.out);
		}
	}	
	
	@Override
	public void printIntervalTitle(String title) {
		ps.println("\n" + title);
	}

	@Override
	public void printThreadRecord(String threadName, String record) {
		ps.println(threadName);
		ps.println(record);
	}

	@Override
	public void flush() {
		ps.flush();
	}

	@Override
	public void close() 
	{
		if (isFile) {
			ps.close();
		}
	}

	@Override
	public void printError(String message, Throwable t) {
		ps.println(message);
		t.printStackTrace(ps);
	}

}
