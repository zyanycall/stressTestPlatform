package io.renren.modules.test.jmeter;

import io.renren.common.exception.RRException;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.engine.StandardJMeterEngine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 为了执行Jmeter用例而设计的类，每一个脚本文件对应一批engine
 * Created by zyanycall@gmail.com on 13:59.
 */
public class JmeterRunEntity {

    private StressTestFileEntity stressTestFile;
    private StressTestReportsEntity stressTestReports;
    private JmeterResultCollector jmeterResultCollector;

    /**
     * 进行状态，为了和stop配合使用，还是放到了这个对象里。
     */
    private Integer runStatus = StressTestUtils.INITIAL;

    private List<JMeterEngine> engines = new LinkedList<>();

    /**
     * 脚本文件所使用的文件名的集合，例如"classinfo.txt"
     */
    private ArrayList<String> fileAliaList;

    public void stop() {
        engines.forEach(engine -> {
            if (engine != null) {
                if (engine instanceof StandardJMeterEngine) {
                    // 本身不是gui方式运行的，没有进程强制结束风险。
                    // 反射的类使用反射的方法。
                    try {
                        Method stopTestM = engine.getClass().getMethod("stopTest", new Class[]{});
                        stopTestM.invoke(engine, new Object[]{});
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RRException(e.getMessage(), e);
                    }
                } else {
                    engine.stopTest();
                }
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

    public ArrayList<String> getFileAliaList() {
        return fileAliaList;
    }

    public void setFileAliaList(ArrayList<String> fileAliaList) {
        this.fileAliaList = fileAliaList;
    }

    public JmeterResultCollector getJmeterResultCollector() {
        return jmeterResultCollector;
    }

    public void setJmeterResultCollector(JmeterResultCollector jmeterResultCollector) {
        this.jmeterResultCollector = jmeterResultCollector;
    }
}
