package io.renren.modules.test.jmeter;

import io.renren.modules.test.utils.StressTestUtils;
import org.apache.jmeter.visualizers.SamplingStatCalculator;

import java.util.HashMap;
import java.util.Map;

/**
 * 为前端展示监控数据返回的对象，其中包含各种监控数据。
 * 不包含任何查询数据库请求，全部缓存操作。
 * Created by zyanycall@gmail.com on 14:49.
 */
public class JmeterStatEntity {

    private Long fileId;

    /**
     * key值是label，即每个请求的名称。
     * value值是计算值的对象，里面包含每个label所对应的监控计算数据。
     * statMap是在回调时，填充数据
     */
    private Map<String, SamplingStatCalculator> statMap;

    /**
     * 响应时间相关的监控数据。
     */
    private Map<String, String> responseTimesMap = new HashMap<>();

    /**
     * 每秒通过数（RPS）相关的监控数据。
     */
    private Map<String, String> throughputMap = new HashMap<>();

    /**
     * 吞吐量请求相关的监控数据。
     */
    private Map<String, String> networkSentMap = new HashMap<>();

    /**
     * 吞吐量接收相关的监控数据。
     */
    private Map<String, String> networkReceiveMap = new HashMap<>();

    /**
     * 正确率相关的监控数据。是总的请求数的正确率的占比，这个图在单个请求或者
     * 每个请求的请求数量相差不大的时候，比较直观，但是除此之外，不能显现问题严重性。
     */
    private Map<String, String> successPercentageMap = new HashMap<>();

    /**
     * 每个label的错误率，double类型。和successPercentageMap不同，successPercentageMap是所有
     * 请求中，成功失败的占比。
     */
    private Map<String, String> errorPercentageMap = new HashMap<>();

    /**
     * 虚拟用户数相关的监控数据，没有根据label/slave的名称区分。
     * slave分布式节点的线程数，master默认统计不到。
     */
    private Map<String, String> threadCountsMap = new HashMap<>();

    /**
     * 当前是否正在运行
     */
    private Integer runStatus = StressTestUtils.RUNNING;

    private JmeterRunEntity jmeterRunEntity;

    /**
     * 对于分布式场景，取到的statMap是总的，即包含了所有脚本执行的label的数据。
     */
    public JmeterStatEntity(Long fileId, Long fileIdZero) {
        if (fileIdZero != null) {// 分布式情况下
            this.fileId = fileIdZero;
            statMap = StressTestUtils.samplingStatCalculator4File.get(fileIdZero);
        } else {// 单机模式下
            this.fileId = fileId;
            statMap = StressTestUtils.samplingStatCalculator4File.get(fileId);
        }

        // StressTestUtils.jMeterEntity4file 中保存的都是真实的脚本文件信息
        jmeterRunEntity = StressTestUtils.jMeterEntity4file.get(fileId);
        if (jmeterRunEntity != null) {
            runStatus = jmeterRunEntity.getRunStatus();
        }
    }

    public Map<String, String> getResponseTimesMap() {
        if (statMap != null) {
            statMap.forEach((k, v) -> {
                /**
                 * 平均响应时间算法是当前请求总共花费的时间/响应了多少请求。
                 * 这个时间是正确的。
                 */
                responseTimesMap.put(k + "_Avg(ms)", String.format("%.2f", v.getMean()));
//                responseTimesMap.put(k + "_Max(ms)", String.valueOf(v.getMax()));
//                responseTimesMap.put(k + "_Min(ms)", String.valueOf(v.getMin()));
            });
        }
        return responseTimesMap;
    }

    public void setResponseTimesMap(Map<String, String> responseTimesMap) {
        this.responseTimesMap = responseTimesMap;
    }

    public Map<String, String> getThroughputMap() {
        if (statMap != null) {
            statMap.forEach((k, v) -> {
                throughputMap.put(k + "_Rps(OK)", String.format("%.2f", v.getRate()));

                double howLongRunning = 0.0;
                if (v.getRate() > -1e-6) {//double大于0
                    howLongRunning = (1000.0 * v.getCount()) / v.getRate();
                }
                double errorRps = ((double) v.getErrorCount() / howLongRunning) * 1000.0;
                throughputMap.put(k + "_Rps(KO)", String.format("%.2f", errorRps));
            });
        }
        return throughputMap;
    }

    public void setThroughputMap(Map<String, String> throughputMap) {
        this.throughputMap = throughputMap;
    }

    public Map<String, String> getNetworkSentMap() {
        if (statMap != null) {
            for (String key : statMap.keySet()) {
                SamplingStatCalculator calculator = statMap.get(key);
                networkSentMap.put(key + "(Sent)", String.format("%.2f", calculator.getSentKBPerSecond()));
            }
        }
        return networkSentMap;
    }

    public void setNetworkSentMap(Map<String, String> networkSentMap) {
        this.networkSentMap = networkSentMap;
    }

    public Map<String, String> getNetworkReceiveMap() {
        if (statMap != null) {
            for (String key : statMap.keySet()) {
                SamplingStatCalculator calculator = statMap.get(key);
                networkReceiveMap.put(key + "(Received)", String.format("%.2f", calculator.getKBPerSecond()));
            }
        }
        return networkReceiveMap;
    }

    public void setNetworkReceiveMap(Map<String, String> networkReceiveMap) {
        this.networkReceiveMap = networkReceiveMap;
    }

    /**
     * 每个label对应的错误率是个近似值。
     * 并不是在一个循环内求得。
     */
    public Map<String, String> getSuccessPercentageMap() {
        long totalCount = 0L;
        double successPercent = 1.0;
        if (statMap != null) {
            for (String key : statMap.keySet()) {
                SamplingStatCalculator calculator = statMap.get(key);
                totalCount += calculator.getCount();
            }
            for (String key : statMap.keySet()) {
                SamplingStatCalculator calculator = statMap.get(key);
                long errorCount = calculator.getErrorCount();
                double errorPercent = Double.parseDouble(String.format("%.2f", ((double) errorCount / (double) totalCount)));
                successPercentageMap.put(key + "_ErrorPercent", String.valueOf(errorPercent));
                successPercent = successPercent - errorPercent;
            }
        }
        successPercentageMap.put("SuccessPercent", String.format("%.2f", successPercent));
        return successPercentageMap;
    }

    public void setSuccessPercentageMap(Map<String, String> successPercentageMap) {
        this.successPercentageMap = successPercentageMap;
    }

    public Map<String, String> getErrorPercentageMap() {
        if (statMap != null) {
            for (String key : statMap.keySet()) {
                SamplingStatCalculator calculator = statMap.get(key);
                errorPercentageMap.put(key + "_ErrorPercent", String.format("%.2f", calculator.getErrorPercentage()));
            }
        }
        return errorPercentageMap;
    }

    public void setErrorPercentageMap(Map<String, String> errorPercentageMap) {
        this.errorPercentageMap = errorPercentageMap;
    }

    public Map<String, String> getThreadCountsMap() {
        if (jmeterRunEntity != null) {
            threadCountsMap.put("Active", String.valueOf(jmeterRunEntity.getNumberOfActiveThreads()));
        }
        return threadCountsMap;
    }

    public void setThreadCountsMap(Map<String, String> threadCountsMap) {
        this.threadCountsMap = threadCountsMap;
    }

    public Integer getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(Integer runStatus) {
        this.runStatus = runStatus;
    }
}
