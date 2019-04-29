package com.incapsula.sampler;

public class SampleCount {

    private long m_cummulativeTime = 0;
    private long m_methodTime = 0;
    private final int m_depth;
    private final String m_packageClassMethodLine;

    public SampleCount(String packageClassMethodLine, int depth)
    {
        m_depth = depth;
        m_packageClassMethodLine = packageClassMethodLine;
    }

    public void increment(long millis, boolean isCurrentMethod)
    {
        m_cummulativeTime += millis;
        if (isCurrentMethod) {
            m_methodTime += millis;
        }
    }

    public long getCummulativeTime()
    {
        return m_cummulativeTime;
    }

    public long getMethodTime()
    {
        return m_methodTime;
    }

    public int getDepth()
    {
        return m_depth;
    }

    public String getPackageClassMethodLine() { return m_packageClassMethodLine; }

}
