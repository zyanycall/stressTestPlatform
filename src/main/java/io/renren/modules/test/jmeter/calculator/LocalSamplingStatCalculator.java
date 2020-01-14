package io.renren.modules.test.jmeter.calculator;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.Sample;
import org.apache.jorphan.math.StatCalculatorLong;

import java.util.HashMap;

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
     * 并非公共变量，每个对象/每个sample/每条线，都会有一个自己的全新的值。
     * 正常的通过有返回的通过的数量的计算，每一秒都会计算一个数量总数
     */
    private long countPerSecond;

    /**
     * 正常的通过有返回的断言失败的，但是是有通过的数量的计算，每一秒都会计算一个数量总数
     */
    private long errorCountPerSecond;

    /**
     * 保存成功/失败通过数量的保存，真正的TPS是从这里提取的。
     * 原因如果直接提取数量的目前的值，那不叫TPS，叫T。
     * 保存好的，即每一秒会保存一次，更新一次的，才是TPS。
     */
    private HashMap<String, Long> countMap = new HashMap<>();

    private long countPerSecondTime;

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
        errorCountPerSecond = 0L;
        countPerSecond = 0L;
        countPerSecondTime = 0L;
        countMap.put("countPerSec", 0L);
        countMap.put("errorCountPerSec", 0L);
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
            long howLongRunning = endTime - firstTime;
            throughput = ((double) calculator.getCount() / (double) howLongRunning) * 1000.0;

            // zyanycall add
            // 如果是报错的请求，时间都是0.这样可以单独处理报错的请求。

            // 实际上，只有三种情况考虑：
            // 1. 全部是正确的请求，只有正确的TPS。
            // 2. 全部是错误的请求，只有错误的TPS。
            // 3. 每一秒，都有正确的TPS和错误的TPS。
            if (endTime > 0L) {
                long endTimeSec = endTime / 1000;
                String endTimeSecStr = endTimeSec + "";
                if (endTimeSec > countPerSecondTime) {
                    countPerSecondTime = endTimeSec;
                    if (res.isSuccessful()) { // 断言成功
                        // 实际上，这里取到的数字，是上一秒的全量的一秒内的请求的次数。
                        // 但是并不是说TPS就是延迟一秒，平均下来，是延迟半秒。
                        // 计算完之后，这个值就会被刷新。如果不是下一秒，则不会刷新，这样计算的TPS才是准确的。
                        countMap.put("countPerSec", countPerSecond);
                        countPerSecond = 1;

                        // 清理错误的TPS。
                        if (!(errorCountPerSecond + "").startsWith(endTimeSecStr)) {
                            errorCountPerSecond = 0L;
                            countMap.put("errorCountPerSec", 0L);
                        }
                    } else {// 断言失败
                        countMap.put("errorCountPerSec", errorCountPerSecond);
                        errorCountPerSecond = 1L;

                        // 清理正确的TPS。
                        if (!(countPerSecond + "").startsWith(endTimeSecStr)) {
                            countPerSecond = 0L;
                            countMap.put("countPerSec", 0L);
                        }
                    }
                } else if (endTimeSec == countPerSecondTime) {
                    if (res.isSuccessful()) {// 断言成功
                        countPerSecond = countPerSecond + 1L;
                    } else {// 断言失败
                        errorCountPerSecond = errorCountPerSecond + 1L;
                    }
                }
            }
            if (endTime == 0L) {//异常情况，label会直接打出异常内容，这里就不再计算统计。
                countPerSecond = 0L;
                errorCountPerSecond = 0L;
                countMap.put("countPerSec", 0L);
                countMap.put("errorCountPerSec", 0L);
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
        return countMap.get("countPerSec");
    }

    public long getErrorCountPerSecond() {
        return countMap.get("errorCountPerSec");
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
