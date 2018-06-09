package io.renren.modules.test.jmeter;

import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.SamplingStatCalculator;

import java.util.HashMap;
import java.util.Map;

/**
 * Jmeter执行程序的结果收集类，Jmeter每次执行完都会调用到这里。
 * 相当于观察者模式的观察类，钩子程序。
 * Created by zyanycall@gmail.com on 17:51.
 */
public class JmeterResultCollector extends ResultCollector {

    protected static final long serialVersionUID = 240L;

    StressTestFileEntity stressTestFile;

    Map<String, SamplingStatCalculator> samplingStatCalculatorMap;

    public JmeterResultCollector() {
    }

    public JmeterResultCollector(StressTestFileEntity stressTestFile) {
        samplingStatCalculatorMap = new HashMap<>();
        this.stressTestFile = stressTestFile;
        StressTestUtils.samplingStatCalculator4File.put(stressTestFile.getFileId(), samplingStatCalculatorMap);
    }

    /**
     * 每一次jmeter的请求都会走到这里，
     * 包括每个用例文件中每个请求。
     * 再乘以各个分布式节点的请求，所以请求量预计会比较大。
     *
     * @param sampleEvent 监听的事件
     */
    @Override
    public void sampleOccurred(SampleEvent sampleEvent) {
        // 使用父类默认的保存csv/xml结果的方法。未来可能会优化，保存到性能更高的地方。
        // csv最终的实现是来一个结果，使用PrintWriter写一行(有锁)，保证时序性。
        // 本质是将信息保存到操作系统的文件内存里，默认是不实时刷新操作系统的文件buffer（Jmeter源码写的）。
        // 保证时序性+性能交给操作系统，性能应该还OK。毕竟造成的压力和实时锁相比不是一个数量级的。
        if (StressTestUtils.NEED_REPORT.equals(stressTestFile.getReportStatus())) {
            super.sampleOccurred(sampleEvent);
        }

        if (StressTestUtils.NEED_WEB_CHART.equals(stressTestFile.getWebchartStatus())) {
            //获取到请求的label，注意不是jmx脚本文件的label，是其中的请求的label，可能包含汉字。
            SampleResult sampleResult = sampleEvent.getResult();
            String label = sampleResult.getSampleLabel();

            // label不会很多。
            if (samplingStatCalculatorMap.get(label) == null) {
                samplingStatCalculatorMap.put(label, new SamplingStatCalculator(label));
            }
            SamplingStatCalculator samplingStatCalculator = samplingStatCalculatorMap.get(label);

            // 这个计算的过程会消耗CPU，一切为了前端绘图。
            samplingStatCalculator.addSample(sampleResult);
        }
    }
}