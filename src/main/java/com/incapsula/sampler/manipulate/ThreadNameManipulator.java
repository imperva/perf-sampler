package com.incapsula.sampler.manipulate;

public interface ThreadNameManipulator 
{
	/** 
	 * Manipulate thread name to be used for aggregation. 
	 * @param thread - sampled Thread.
	 * @return manipulated thread name string.<br> If null is returned, this thread will not be aggregated at all.<br>**/
	public String getManipulatedThreadName(Thread thread);
	public void init();
}
