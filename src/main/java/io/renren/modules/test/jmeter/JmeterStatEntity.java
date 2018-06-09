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
     * 正确率相关的监控数据。
     */
    private Map<String, String> successPercentageMap = new HashMap<>();

    public JmeterStatEntity(Long fileId) {
        this.fileId = fileId;
        statMap = StressTestUtils.samplingStatCalculator4File.get(fileId);
    }

    public Map<String, String> getResponseTimesMap() {
        if (statMap != null) {
            statMap.forEach((k, v) -> {
                /**
                 * 平均响应时间并非真正的一个请求响应的时间，而是一段时间内响应了多少请求而计算出的平均响应时间。
                 * 所以，如果是被测试的服务器满负荷，这个响应时间才是接近于一个请求的真正的响应时间。
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
                successPercentageMap.put(key + "_errorPercent", String.valueOf(errorPercent));
                successPercent = successPercent - errorPercent;
            }
        }
        successPercentageMap.put("successPercent", String.format("%.2f", successPercent));
        return successPercentageMap;
    }

    public void setSuccessPercentageMap(Map<String, String> successPercentageMap) {
        this.successPercentageMap = successPercentageMap;
    }
}
