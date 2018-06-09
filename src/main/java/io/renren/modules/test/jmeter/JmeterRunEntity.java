package io.renren.modules.test.jmeter;

import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import org.apache.jmeter.engine.JMeterEngine;

import java.util.LinkedList;
import java.util.List;

/**
 * 为了执行Jmeter用例而设计的类，每一个脚本文件对应一批engine
 * Created by zyanycall@gmail.com on 13:59.
 */
public class JmeterRunEntity {

    private StressTestFileEntity stressTestFile;
    private StressTestReportsEntity stressTestReports;

    private List<JMeterEngine> engines = new LinkedList<>();

    public void stop() {
        engines.forEach(engine -> {
            if (engine != null) {
                // 本身不是gui方式运行的，没有进程强制结束风险。
                engine.stopTest();
            }
        });
    }

    public StressTestFileEntity getStressTestFile() {
        return stressTestFile;
    }

    public void setStressTestFile(StressTestFileEntity stressTestFile) {
        this.stressTestFile = stressTestFile;
    }

    public StressTestReportsEntity getStressTestReports() {
        return stressTestReports;
    }

    public void setStressTestReports(StressTestReportsEntity stressTestReports) {
        this.stressTestReports = stressTestReports;
    }

    public List<JMeterEngine> getEngines() {
        return engines;
    }

    public void setEngines(List<JMeterEngine> engines) {
        this.engines = engines;
    }
}
