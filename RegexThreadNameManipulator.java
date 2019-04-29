package com.incapsula.sampler.manipulate;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class RegexThreadNameManipulator implements ThreadNameManipulator {

	private Pattern[] threadNameRemovalPatterns = null;

	@Override
	public String getManipulatedThreadName(Thread thread) 
	{
		String aggregatedThreadName = thread.getName();
		for (Pattern textRemovalPattern : threadNameRemovalPatterns) {
			aggregatedThreadName = textRemovalPattern.matcher(aggregatedThreadName).replaceAll("");
		}
		
		return aggregatedThreadName.length() == 0 ? null : aggregatedThreadName;
	}

	public void setThreadNameRemovalPatterns(List<String> patterns)
	{
		threadNameRemovalPatterns = new Pattern[patterns.size()];
		for (int i = 0; i < threadNameRemovalPatterns.length; i++) {
			threadNameRemovalPatterns[i] = Pattern.compile(patterns.get(i));
		}
	}

	@Override
	public void init() 
	{
		if (threadNameRemovalPatterns == null) {
			setThreadNameRemovalPatterns(Arrays.asList(new String[]{"[0-9a-f]{16,}", "[\\.:\\d]+"}));
		}
	}
}
