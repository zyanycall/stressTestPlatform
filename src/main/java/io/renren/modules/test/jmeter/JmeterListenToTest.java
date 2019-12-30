package io.renren.modules.test.jmeter;

import io.renren.modules.test.service.StressTestFileService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.jmeter.engine.ClientJMeterEngine;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.report.dashboard.GenerationException;
import org.apache.jmeter.report.dashboard.ReportGenerator;
import org.apache.jmeter.samplers.Remoteable;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 核心代价借鉴了Jmeter源码JMeter.java，没有继承覆盖是因为源码是私有子类。
 * 所以其中部分注释和基本的代码内容保留，以免造成bug。
 * 作用是创建一个listener，用来监控测试执行结束，用其来执行回调后的操作。
 * 如修改当前脚本运行的状态。
 * <p>
 * Created by zyanycall@gmail.com on 2018/10/8 14:28.
 */
public class JmeterListenToTest implements TestStateListener, Runnable, Remoteable {

    Logger log = LoggerFactory.getLogger(getClass());

    private final AtomicInteger started = new AtomicInteger(0); // keep track of remote tests

    private final List<JMeterEngine> engines;

    private final ReportGenerator reportGenerator;

    private final StressTestFileService stressTestFileService;

    private final Long fileId;

    /**
     * @param engines         List<JMeterEngine>
     * @param reportGenerator {@link ReportGenerator}
     */
    public JmeterListenToTest(List<JMeterEngine> engines, ReportGenerator reportGenerator,
                              StressTestFileService stressTestFileService, Long fileId) {
        this.engines = engines;
        this.reportGenerator = reportGenerator;
        this.stressTestFileService = stressTestFileService;
        this.fileId = fileId;
    }

    @Override
    // N.B. this is called by a daemon RMI thread from the remote host
    public void testEnded(String host) {
        final long now = System.currentTimeMillis();
        log.info("Finished remote host: {} ({})", host, now);
        if (started.decrementAndGet() <= 0) {
            Thread stopSoon = new Thread(this);
            // the calling thread is a daemon; this thread must not be
            // see Bug 59391
            stopSoon.setDaemon(false);
            stopSoon.start();
        }
        updateEndStatus();
    }

    @Override
    public void testEnded() {
        long now = System.currentTimeMillis();
        log.error("Tidying up ...    @ " + new Date(now) + " (" + now + ")");
        try {
            generateReport();
        } catch (Exception e) {
            log.error("Error generating the report", e);
        }
        checkForRemainingThreads();
        updateEndStatus();
        log.error("... end of run");
    }

    @Override
    public void testStarted(String host) {
        started.incrementAndGet();
        final long now = System.currentTimeMillis();
        log.info("Started remote host:  {} ({})", host, now);
    }

    @Override
    public void testStarted() {
        if (log.isInfoEnabled()) {
            final long now = System.currentTimeMillis();
            log.info("{} ({})", JMeterUtils.getResString("running_test"), now);//$NON-NLS-1$
        }
    }

    /**
     * This is a hack to allow listeners a chance to close their files. Must
     * implement a queue for sample responses tied to the engine, and the
     * engine won't deliver testEnded signal till all sample responses have
     * been delivered. Should also improve performance of remote JMeter
     * testing.
     */
    @Override
    public void run() {
        long now = System.currentTimeMillis();
        log.error("Tidying up remote @ " + new Date(now) + " (" + now + ")");
        if (engines != null) { // it will be null unless remoteStop = true
            log.error("Exiting remote servers");
            for (JMeterEngine e : engines) {
                e.exit();
            }
        }
        try {
            TimeUnit.SECONDS.sleep(5); // Allow listeners to close files
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        ClientJMeterEngine.tidyRMI(log);
        try {
            generateReport();
        } catch (Exception e) {
            System.err.println("Error generating the report: " + e);//NOSONAR
            log.error("Error generating the report", e);
        }
        checkForRemainingThreads();
        log.error("... end of run");
    }

    /**
     * Generate report
     * 当前程序没有测试完成后直接生成测试报告的要求。
     * 所以此方法仅作为保留。
     */
    private void generateReport() {
        if (reportGenerator != null) {
            try {
                log.info("Generating Dashboard");
                reportGenerator.generate();
                log.info("Dashboard generated");
            } catch (GenerationException ex) {
                log.error("Error generating dashboard: {}", ex, ex);
            }
        }
    }

    /**
     * Runs daemon thread which waits a short while;
     * if JVM does not exit, lists remaining non-daemon threads on stdout.
     */
    private void checkForRemainingThreads() {
        // This cannot be a JMeter class variable, because properties
        // are not initialised until later.
        // 由于系统集成了Jmeter的配置文件架构，所以此处可以这么引用。
        // 未来如果引用新的配置文件，此处需要修改。
        final int pauseToCheckForRemainingThreads =
                JMeterUtils.getPropDefault("jmeter.exit.check.pause", 2000); // $NON-NLS-1$

        if (pauseToCheckForRemainingThreads > 0) {
            Thread daemon = new Thread(() -> {
                try {
                    TimeUnit.MILLISECONDS.sleep(pauseToCheckForRemainingThreads); // Allow enough time for JVM to exit
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                // This is a daemon thread, which should only reach here if there are other
                // non-daemon threads still active
//                log.debug("The JVM should have exited but did not.");//NOSONAR
//                log.debug("The following non-daemon threads are still running (DestroyJavaVM is OK):");//NOSONAR
//                JOrphanUtils.displayThreads(false);
            });
            daemon.setDaemon(true);
            daemon.start();
        } else {
            log.debug("jmeter.exit.check.pause is <= 0, JMeter won't check for unterminated non-daemon threads");
        }
    }

    /**
     * 更新状态
     * 程序到这里engine已经停止结束了。
     * 分布式处理会复杂，先考虑单机
     */
    private void updateEndStatus() {
        // 延时两秒，是为了给前端监控返回完整的数据。
        // 要不然直接停止设置停止状态后，前端监控就会立即停止更新
        // 有可能丢掉一次内容数据
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.error("Thread.sleep meet error!", e);
        }

        JmeterRunEntity jmeterRunEntity = StressTestUtils.jMeterEntity4file.get(fileId);

        //实际上已经完全停止，则使用立即停止的方式，会打断Jmeter执行的线程
        stressTestFileService.stopLocal(fileId, jmeterRunEntity, true);
    }
}
