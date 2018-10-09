package io.renren.modules.test.jmeter;

import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.utils.StressTestUtils;
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

    /**
     * 进行状态，为了和stop配合使用，还是放到了这个对象里。
     */
    private Integer runStatus = StressTestUtils.RUNNING;

    private List<JMeterEngine> engines = new LinkedList<>();

    public void stop() {
        engines.forEach(engine -> {
            if (engine != null) {
                // 本身不是gui方式运行的，没有进程强制结束风险。
                engine.stopTest();
            }
        });

        runStatus = StressTestUtils.RUN_SUCCESS;
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

    public Integer getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(Integer runStatus) {
        this.runStatus = runStatus;
    }
}
