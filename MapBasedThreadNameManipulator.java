package com.incapsula.sampler.manipulate;

import java.util.concurrent.ConcurrentHashMap;

public class MapBasedThreadNameManipulator implements ThreadNameManipulator {

	private ConcurrentHashMap<Thread, String> threadsMap = new ConcurrentHashMap<>(100);

	@Override
	public String getManipulatedThreadName(Thread thread) 
	{
		return threadsMap.get(thread);
	}

	@Override
	public void init() 
	{
	}
	
	public void mapCurrentThread(String mappedString) {
		threadsMap.put(Thread.currentThread(), mappedString);
	}
	
	public void unmapCurrentThread() {
		threadsMap.remove(Thread.currentThread());
	}
	
	public int getMappingsCount() {
		return threadsMap.size();
	}
}
