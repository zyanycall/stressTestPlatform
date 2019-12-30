package io.renren.modules.test.jmeter;

import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.jmeter.engine.LocalStandardJMeterEngine;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContextService;

import java.util.*;

/**
 * 为了执行Jmeter用例而设计的类，每一个脚本文件对应一个JmeterRunEntity对象
 * Created by zyanycall@gmail.com on 13:59.
 */
public class JmeterRunEntity {

    private StressTestFileEntity stressTestFile;
    private StressTestReportsEntity stressTestReports;
    /**
     * 用于测试报告文件流的flush
     */
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

    /**
     * 用来统计运行脚本的线程数量的数据
     */
    private HashMap<String, Integer> threadsCountMap = new HashMap<>();

    /**
     * 活跃线程标识常量标签。
     */
    public final static String ACTIVE_THREADS = "Active";

    /**
     * 已经启动线程标识常量标签。
     */
    public final static String STARTED_THREADS = "Started";

    /**
     * 已经停止线程标识常量标签。
     */
    public final static String FINISHED_THREADS = "Finished";


    /**
     * 停止当前脚本的压力引擎
     */
    public void stop(boolean now) {
        engines.forEach(engine -> {
            if (engine != null) {
//                if (engine instanceof StandardJMeterEngine) {
//                    // 本身不是gui方式运行的，没有进程强制结束风险。
//                    // 如果使用字节码修改技术，则必须使用反射的方法调用。
//                    try {
//                        Method stopTestM = engine.getClass().getMethod("stopTest", new Class[]{Boolean.class});
//                        stopTestM.invoke(engine, new Object[]{now});
//                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
//                        throw new RRException(e.getMessage(), e);
//                    }
//                } else {
//                    // 不强制停止
//                    engine.stopTest(now);
//                }
                // 不强制停止
                engine.stopTest(now);
            }
        });
        // 缓存中变更状态为成功执行
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

    /**
     * 返回当前脚本所有的engine的正在活跃的或者已经启动的线程数量
     */
    public Map getNumberOfActiveThreads() {

        // 当前脚本正在执行的active状态的线程数，是以脚本为单位，脚本内如果包含多个请求，则统计整体数量。
        int numberOfActiveThreads = 0;
        int numberOfStartedThreads = 0;
        int numberOfFinishedThreads = 0;
        for (JMeterEngine engine : engines) {
            if (engine != null) {
                if (engine instanceof LocalStandardJMeterEngine) {
                    List<AbstractThreadGroup> groups = ((LocalStandardJMeterEngine) engine).getGroups();
                    for (AbstractThreadGroup group : groups) {
                        numberOfActiveThreads += group.getNumberOfThreads();
                    }
                } else { // 分布式情况下，活跃的线程数和已经启动的线程数可能不一致。
                    // 原因是每次启动脚本时，started 和 finish 的线程数都会清零，后再启动。但是active不会这样。
                    // 如果我们强制关闭脚本，会让分布式节点的active有残留值。这并非bug。
                    numberOfActiveThreads = JMeterContextService.getThreadCounts().activeThreads;
                    numberOfStartedThreads = JMeterContextService.getThreadCounts().startedThreads;
                    numberOfFinishedThreads = JMeterContextService.getThreadCounts().finishedThreads;
                    break;
                }
            }
        }
        threadsCountMap.put(ACTIVE_THREADS, numberOfActiveThreads);
        // 主机启动的情况下，已经启动和已经停止的先不统计（没找到太合适的入口来统计）
        threadsCountMap.put(STARTED_THREADS, numberOfStartedThreads);
        threadsCountMap.put(FINISHED_THREADS, numberOfFinishedThreads);

        return threadsCountMap;
    }
}
