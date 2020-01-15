package io.renren.modules.test.jmeter.calculator;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.Sample;
import org.apache.jorphan.math.StatCalculatorLong;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by zyanycall@gmail.com on 2020/1/9 5:50 下午.
 * 目的是，干掉一些不必要的计算，同时主要是修改TPS的算法，原有Jmeter的算法对定位问题不理想，和测试报告中的也不一样。
 * <p>
 * Aggregate sample data container. Just instantiate a new instance of this
 * class, and then call {@link #addSample(SampleResult)} a few times, and pull
 * the stats out with whatever methods you prefer.
 */
public class LocalSamplingStatCalculator {
    private final StatCalculatorLong calculator = new StatCalculatorLong();

//    private double maxThroughput;

    private long firstTime;

    private String label;

    private volatile Sample currentSample;

    /**
     * 针对每一秒，存储一个成功请求数的集合。
     * 使用google的缓存技术，把数据的清理交给缓存。
     */
    private Cache<Long, Long> successCountMap;

    /**
     * 针对每一秒，存储一个错误请求数的集合。
     * 使用google的缓存技术，把数据的清理交给缓存。
     */
    private Cache<Long, Long> errorCountMap;


    public LocalSamplingStatCalculator() { // Only for use by test code
        this("");
    }

    public LocalSamplingStatCalculator(String label) {
        this.label = label;
        init();
    }

    private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
        firstTime = Long.MAX_VALUE;
        calculator.clear();
        successCountMap = CacheBuilder.newBuilder()
                .maximumSize(50) // 设置缓存的最大容量
                .expireAfterAccess(30, TimeUnit.SECONDS) // 设置缓存在写入一分钟后失效
                .concurrencyLevel(10) // 设置并发级别为10
//            .recordStats() // 开启缓存统计
                .build();
        errorCountMap = CacheBuilder.newBuilder()
                .maximumSize(50) // 设置缓存的最大容量
                .expireAfterAccess(30, TimeUnit.SECONDS) // 设置缓存在写入一分钟后失效
                .concurrencyLevel(10) // 设置并发级别为10
//            .recordStats() // 开启缓存统计
                .build();
//        maxThroughput = Double.MIN_VALUE;
        currentSample = new Sample();
    }

    /**
     * Clear the counters (useful for differential stats)
     */
    public synchronized void clear() {
        init();
    }

    public Sample getCurrentSample() {
        return currentSample;
    }

    /**
     * Get the elapsed time for the samples
     *
     * @return how long the samples took
     */
    public long getElapsed() {
        if (getCurrentSample().getEndTime() == 0) {
            return 0;// No samples collected ...
        }
        return getCurrentSample().getEndTime() - firstTime;
    }

    public long getFirstTime() {
        return firstTime;
    }

    /**
     * Returns the throughput associated to this sampler in requests per second.
     * May be slightly skewed because it takes the timestamps of the first and
     * last samples as the total time passed, and the test may actually have
     * started before that start time and ended after that end time.
     *
     * @return throughput associated with this sampler per second
     */
    public double getRate() {
        if (calculator.getCount() == 0) {
            return 0.0; // Better behaviour when howLong=0 or lastTime=0
        }

        return getCurrentSample().getThroughput();
    }

    /**
     * Throughput in bytes / second
     *
     * @return throughput in bytes/second
     */
    public double getBytesPerSecond() {
        return getRatePerSecond(calculator.getTotalBytes());
    }

    /**
     * Throughput in kilobytes / second
     *
     * @return Throughput in kilobytes / second
     */
    public double getKBPerSecond() {
        return getBytesPerSecond() / 1024; // 1024=bytes per kb
    }

    /**
     * Sent Throughput in bytes / second
     *
     * @return sent throughput in bytes/second
     */
    public double getSentBytesPerSecond() {
        return getRatePerSecond(calculator.getTotalSentBytes());
    }

    /**
     * @param value long
     * @return rate per second
     */
    private double getRatePerSecond(long value) {
        double rate = 0;
        if (this.getElapsed() > 0 && value > 0) {
            rate = value / ((double) this.getElapsed() / 1000);
        }
        if (rate < 0) {
            rate = 0;
        }
        return rate;
    }

    /**
     * Sent Throughput in kilobytes / second
     *
     * @return Sent Throughput in kilobytes / second
     */
    public double getSentKBPerSecond() {
        return getSentBytesPerSecond() / 1024; // 1024=bytes per kb
    }

    /**
     * calculates the average page size, which means divide the bytes by number
     * of samples.
     *
     * @return average page size in bytes (0 if sample count is zero)
     */
    public double getAvgPageBytes() {
        long count = calculator.getCount();
        if (count == 0) {
            return 0;
        }
        return calculator.getTotalBytes() / (double) count;
    }

    /**
     * @return the label of this component
     */
    public String getLabel() {
        return label;
    }

    /**
     * Records a sample.
     * 修改这个方法，并不会影响csv的保存
     *
     * @param res the sample to record
     * @return newly created sample with current statistics
     */
    public Sample addSample(SampleResult res) {
//        long rtime;
        long cmean;
//        long cstdv;
//        long cmedian;
//        long cpercent;
        long eCount;
        long endTime;
        double throughput;
//        boolean rbool;
        synchronized (calculator) {
            calculator.addValue(res.getTime(), res.getSampleCount());
            calculator.addBytes(res.getBytesAsLong());
            calculator.addSentBytes(res.getSentBytes());
            setStartTime(res);
            eCount = getCurrentSample().getErrorCount();
            eCount += res.getErrorCount();
            endTime = getEndTime(res);
//            howLongRunning = endTime - firstTime;
//            throughput = ((double) calculator.getCount() / (double) howLongRunning) * 1000.0;
            throughput = 0D;

            // zyanycall add
            if (endTime > 0L) {
                long endTimeSec = endTime / 1000;
                Long successCountThisSec = successCountMap.getIfPresent(endTimeSec);
                if (Objects.isNull(successCountThisSec)) {
                    successCountThisSec = 0L;
                }
                if (res.isSuccessful()) {
                    successCountThisSec = successCountThisSec + 1L;
                }

                Long errorCountThisSec = errorCountMap.getIfPresent(endTimeSec);
                if (Objects.isNull(errorCountThisSec)) {
                    errorCountThisSec = 0L;
                }
                if (!res.isSuccessful()) {
                    errorCountThisSec = errorCountThisSec + 1L;
                }
                successCountMap.put(endTimeSec, successCountThisSec);
                errorCountMap.put(endTimeSec, errorCountThisSec);
            }
            if (endTime == 0L) {//异常情况，label会直接打出异常内容，这里就不再计算统计。
            }
            // zyanycall add end

//            if (throughput > maxThroughput) {
//                maxThroughput = throughput;
//            }

//            rtime = res.getTime();
            cmean = (long) calculator.getMean();
//            cstdv = (long)calculator.getStandardDeviation();
            // 注释掉没有用到的，浪费计算力的方法
//            cmedian = calculator.getMedian().longValue();
//            cpercent = calculator.getPercentPoint( 0.500 ).longValue();
//            rbool = res.isSuccessful();
        }

        long count = calculator.getCount();
        Sample s =
                new Sample(null, 0L, cmean, 0L, 0L,
                        0L, throughput, eCount, true, count, endTime);
        currentSample = s;
        return s;
    }

    public long getCountPerSecond() {
        // 平均下来TPS的取值并非就延迟一秒，这里保证的是取的值是TPS。
        long timeSec = (System.currentTimeMillis() / 1000) - 1;
        Long successCountPerSec = successCountMap.getIfPresent(timeSec);
        if (Objects.isNull(successCountPerSec)) {
            return 0L;
        }
        return successCountPerSec;
    }

    public long getErrorCountPerSecond() {
        long timeSec = (System.currentTimeMillis() / 1000) - 1;
        Long errorCountPerSec = errorCountMap.getIfPresent(timeSec);
        if (Objects.isNull(errorCountPerSec)) {
            return 0L;
        }
        return errorCountPerSec;
    }

    private long getEndTime(SampleResult res) {
        long endTime = res.getEndTime();
        long lastTime = getCurrentSample().getEndTime();
        if (lastTime < endTime) {
            lastTime = endTime;
        }
        return lastTime;
    }

    /**
     * @param res
     */
    private void setStartTime(SampleResult res) {
        long startTime = res.getStartTime();
        if (firstTime > startTime) {
            // this is our first sample, set the start time to current timestamp
            firstTime = startTime;
        }
    }

    /**
     * Returns the raw double value of the percentage of samples with errors
     * that were recorded. (Between 0.0 and 1.0)
     *
     * @return the raw double value of the percentage of samples with errors
     * that were recorded.
     */
    public double getErrorPercentage() {
        double rval = 0.0;

        if (calculator.getCount() == 0) {
            return rval;
        }
        rval = (double) getCurrentSample().getErrorCount() / (double) calculator.getCount();
        return rval;
    }

    /**
     * For debugging purposes, only.
     */
    @Override
    public String toString() {
//        StringBuilder mySB = new StringBuilder();
//
//        mySB.append("Samples: " + this.getCount() + "  ");
//        mySB.append("Avg: " + this.getMean() + "  ");
//        mySB.append("Min: " + this.getMin() + "  ");
//        mySB.append("Max: " + this.getMax() + "  ");
//        mySB.append("Error Rate: " + this.getErrorPercentage() + "  ");
//        mySB.append("Sample Rate: " + this.getRate());
//        return mySB.toString();
        return "";
    }

    /**
     * @return errorCount
     */
    public long getErrorCount() {
        return getCurrentSample().getErrorCount();
    }

    /**
     * @return Returns the maxThroughput.
     */
//    public double getMaxThroughput() {
//        return maxThroughput;
//    }

//    public Map<Number, Number[]> getDistribution() {
//        return calculator.getDistribution();
//    }

//    public Number getPercentPoint(double percent) {
//        return calculator.getPercentPoint(percent);
//    }
    public long getCount() {
        return calculator.getCount();
    }

//    public Number getMax() {
//        return calculator.getMax();
//    }

    public double getMean() {
        return calculator.getMean();
    }

//    public Number getMeanAsNumber() {
//        return Long.valueOf((long) calculator.getMean());
//    }

//    public Number getMedian() {
//        return calculator.getMedian();
//    }

//    public Number getMin() {
//        if (calculator.getMin().longValue() < 0) {
//            return Long.valueOf(0);
//        }
//        return calculator.getMin();
//    }

//    public Number getPercentPoint(float percent) {
//        return calculator.getPercentPoint(percent);
//    }

//    public double getStandardDeviation() {
//        return calculator.getStandardDeviation();
//    }

}
