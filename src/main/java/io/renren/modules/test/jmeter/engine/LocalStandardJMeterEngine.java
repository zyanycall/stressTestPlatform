package io.renren.modules.test.jmeter.engine;

import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.jmeter.JmeterTestPlan;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.engine.*;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testbeans.TestBeanHelper;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.*;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.apache.jorphan.util.JMeterStopTestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * 本身从Jmeter源码StandardJMeterEngine复制来的，为的是解决平台同时进行多脚本问题。
 * 增加了对JmeterTestPlan的引用和处理。
 *
 * 父类StandardJMeterEngine中都是私有变量，无法仅仅复写configure()方法，所以都复制过来了。
 *
 * Created by zyanycall@gmail.com on 2018/11/5 16:13.
 */
public class LocalStandardJMeterEngine extends StandardJMeterEngine {
    private static final Logger log = LoggerFactory.getLogger(LocalStandardJMeterEngine.class);

    // Should we exit at end of the test? (only applies to server, because host is non-null)
    private static final boolean EXIT_AFTER_TEST =
            JMeterUtils.getPropDefault("server.exitaftertest", false);  // $NON-NLS-1$

    // Allow engine and threads to be stopped from outside a thread
    // e.g. from beanshell server
    // Assumes that there is only one instance of the engine
    // at any one time so it is not guaranteed to work ...
    private static volatile LocalStandardJMeterEngine engine;

    /*
     * Allow functions etc to register for testStopped notification.
     * Only used by the function parser so far.
     * The list is merged with the testListeners and then cleared.
     */
    private static final List<TestStateListener> testList = new ArrayList<>();

    /** Whether to call System.exit(0) in exit after stopping RMI */
    private static final boolean REMOTE_SYSTEM_EXIT = JMeterUtils.getPropDefault("jmeterengine.remote.system.exit", false);

    /** Whether to call System.exit(1) if threads won't stop */
    private static final boolean SYSTEM_EXIT_ON_STOP_FAIL = JMeterUtils.getPropDefault("jmeterengine.stopfail.system.exit", true);

    /** Whether to call System.exit(0) unconditionally at end of non-GUI test */
    private static final boolean SYSTEM_EXIT_FORCED = JMeterUtils.getPropDefault("jmeterengine.force.system.exit", false);

    /** Flag to show whether test is running. Set to false to stop creating more threads. */
    private volatile boolean running = false;

    /** Flag to show whether engine is active. Set to false at end of test. */
    private volatile boolean active = false;

    /** Thread Groups run sequentially */
    private volatile boolean serialized = false;

    /** tearDown Thread Groups run after shutdown of main threads */
    private volatile boolean tearDownOnShutdown = false;

    private HashTree test;

    private final String host;

    // The list of current thread groups; may be setUp, main, or tearDown.
    private final List<AbstractThreadGroup> groups = new CopyOnWriteArrayList<>();

    // 为本地增加的脚本对象。
    private StressTestFileEntity stressTestFile;

    public LocalStandardJMeterEngine() {
        this("");
    }

    public LocalStandardJMeterEngine(StressTestFileEntity stressTestFile) {
        this("");
        this.stressTestFile = stressTestFile;
    }

    public LocalStandardJMeterEngine(String host) {
        // 为保留源代码方式，null可能作为判断的条件所以不变。
        this.host = "".equals(host) ? null : host;
        // Hack to allow external control
        initSingletonEngine(this);
    }
    /**
     * Set the shared engine
     */
    private static void initSingletonEngine(LocalStandardJMeterEngine localStandardJMeterEngine) {
        LocalStandardJMeterEngine.engine = localStandardJMeterEngine;
    }

    /**
     * set the shared engine to null
     */
    private static void resetSingletonEngine() {
        LocalStandardJMeterEngine.engine = null;
    }

    public static void stopEngineNow() {
        if (engine != null) {// May be null if called from Unit test
            engine.stopTest(true);
        }
    }

    public static void stopEngine() {
        if (engine != null) { // May be null if called from Unit test
            engine.stopTest(false);
        }
    }

    public static synchronized void register(TestStateListener tl) {
        testList.add(tl);
    }

    public static boolean stopThread(String threadName) {
        return stopThread(threadName, false);
    }

    public static boolean stopThreadNow(String threadName) {
        return stopThread(threadName, true);
    }

    private static boolean stopThread(String threadName, boolean now) {
        if (engine == null) {
            return false;// e.g. not yet started
        }
        boolean wasStopped = false;
        // ConcurrentHashMap does not need synch. here
        for (AbstractThreadGroup threadGroup : engine.groups) {
            wasStopped = wasStopped || threadGroup.stopThread(threadName, now);
        }
        return wasStopped;
    }

    // End of code to allow engine to be controlled remotely

    /**
     * 是为了修改的这个方法，将TestPlan替换成JmeterTestPlan
     * @param testTree
     */
    @Override
    public void configure(HashTree testTree) {
        // Is testplan serialised?
        SearchByClass jmeterTestPlan = new SearchByClass(JmeterTestPlan.class);
        JmeterTestPlan tpTemp = new JmeterTestPlan();
        // testPlan对应的是测试计划，每一个测试脚本之中只有一个测试计划，所以直接取第一个即可。
        // 交换key值，让我们的自实现的子类jmeterTestPlan进入。
        testTree.replaceKey(testTree.keySet().toArray()[0], tpTemp);
        testTree.traverse(jmeterTestPlan);
        Object[] plan = jmeterTestPlan.getSearchResults().toArray();
        if (plan.length == 0) {
            throw new RuntimeException("Could not find the TestPlan class!");
        }
        JmeterTestPlan tp = (JmeterTestPlan) plan[0];
        // 设置我们平台自己的变量
        tp.setStressTestFile(stressTestFile);

        serialized = tp.isSerialized();
        tearDownOnShutdown = tp.isTearDownOnShutdown();
        active = true;
        test = testTree;
    }

    @Override
    public void runTest() throws JMeterEngineException {
        if (host != null){
            long now=System.currentTimeMillis();
            System.out.println("Starting the test on host " + host + " @ "+new Date(now)+" ("+now+")"); // NOSONAR Intentional
        }
        try {
            Thread runningThread = new Thread(this, "LocalStandardJMeterEngine");
            runningThread.start();
        } catch (Exception err) {
            stopTest();
            throw new JMeterEngineException(err);
        }
    }

    private void removeThreadGroups(List<?> elements) {
        Iterator<?> iter = elements.iterator();
        while (iter.hasNext()) { // Can't use for loop here because we remove elements
            Object item = iter.next();
            if (item instanceof AbstractThreadGroup) {
                iter.remove();
            } else if (!(item instanceof TestElement)) {
                iter.remove();
            }
        }
    }

    private void notifyTestListenersOfStart(SearchByClass<TestStateListener> testListeners) {
        for (TestStateListener tl : testListeners.getSearchResults()) {
            if (tl instanceof TestBean) {
                TestBeanHelper.prepare((TestElement) tl);
            }
            if (host == null) {
                tl.testStarted();
            } else {
                tl.testStarted(host);
            }
        }
    }

    private void notifyTestListenersOfEnd(SearchByClass<TestStateListener> testListeners) {
        log.info("Notifying test listeners of end of test");
        for (TestStateListener tl : testListeners.getSearchResults()) {
            try {
                if (host == null) {
                    tl.testEnded();
                } else {
                    tl.testEnded(host);
                }
            } catch (Exception e) {
                log.warn("Error encountered during shutdown of "+tl.toString(),e);
            }
        }
        if (host != null) {
            log.info("Test has ended on host {} ", host);
            long now=System.currentTimeMillis();
            System.out.println("Finished the test on host " + host + " @ "+new Date(now)+" ("+now+")" // NOSONAR Intentional
                    +(EXIT_AFTER_TEST ? " - exit requested." : ""));
            if (EXIT_AFTER_TEST){
                exit();
            }
        }
        active=false;
    }

    @Override
    public void reset() {
        if (running) {
            stopTest();
        }
    }

    /**
     * Stop Test Now
     */
    @Override
    public synchronized void stopTest() {
        stopTest(true);
    }

    @Override
    public synchronized void stopTest(boolean now) {
        Thread stopThread = new Thread(new LocalStandardJMeterEngine.StopTest(now));
        stopThread.start();
    }

    private class StopTest implements Runnable {
        private final boolean now;

        private StopTest(boolean b) {
            now = b;
        }

        /**
         * For each current thread group, invoke:
         * <ul>
         * <li>{@link AbstractThreadGroup#stop()} - set stop flag</li>
         * </ul>
         */
        private void stopAllThreadGroups() {
            // ConcurrentHashMap does not need synch. here
            for (AbstractThreadGroup threadGroup : groups) {
                threadGroup.stop();
            }
        }

        /**
         * For each thread group, invoke {@link AbstractThreadGroup#tellThreadsToStop()}
         */
        private void tellThreadGroupsToStop() {
            // ConcurrentHashMap does not need protecting
            for (AbstractThreadGroup threadGroup : groups) {
                threadGroup.tellThreadsToStop();
            }
        }

        /**
         * @return boolean true if all threads of all Thread Groups stopped
         */
        private boolean verifyThreadsStopped() {
            boolean stoppedAll = true;
            // ConcurrentHashMap does not need synch. here
            for (AbstractThreadGroup threadGroup : groups) {
                stoppedAll = stoppedAll && threadGroup.verifyThreadsStopped();
            }
            return stoppedAll;
        }

        /**
         * @return total of active threads in all Thread Groups
         */
        private int countStillActiveThreads() {
            int reminingThreads= 0;
            for (AbstractThreadGroup threadGroup : groups) {
                reminingThreads += threadGroup.numberOfActiveThreads();
            }
            return reminingThreads;
        }

        @Override
        public void run() {
            running = false;
            resetSingletonEngine();
            if (now) {
                tellThreadGroupsToStop();
                pause(10L * countStillActiveThreads());
                boolean stopped = verifyThreadsStopped();
                if (!stopped) {  // we totally failed to stop the test
                    if (JMeter.isNonGUI()) {
                        // TODO should we call test listeners? That might hang too ...
                        log.error(JMeterUtils.getResString("stopping_test_failed")); //$NON-NLS-1$
                        if (SYSTEM_EXIT_ON_STOP_FAIL) { // default is true
                            log.error("Exiting");
                            System.out.println("Fatal error, could not stop test, exiting"); // NOSONAR Intentional
                            System.exit(1); // NOSONAR Intentional
                        } else {
                            System.out.println("Fatal error, could not stop test"); // NOSONAR Intentional
                        }
                    } else {
                        JMeterUtils.reportErrorToUser(
                                JMeterUtils.getResString("stopping_test_failed"), //$NON-NLS-1$
                                JMeterUtils.getResString("stopping_test_title")); //$NON-NLS-1$
                    }
                } // else will be done by threadFinished()
            } else {
                stopAllThreadGroups();
            }
        }
    }

    @Override
    public void run() {
        log.info("Running the test!");
        running = true;

        /*
         * Ensure that the sample variables are correctly initialised for each run.
         */
        SampleEvent.initSampleVariables();

        JMeterContextService.startTest();
        try {
            PreCompiler compiler = new PreCompiler();
            test.traverse(compiler);
        } catch (RuntimeException e) {
            log.error("Error occurred compiling the tree:",e);
            JMeterUtils.reportErrorToUser("Error occurred compiling the tree: - see log file", e);
            return; // no point continuing
        }
        /**
         * Notification of test listeners needs to happen after function
         * replacement, but before setting RunningVersion to true.
         */
        SearchByClass<TestStateListener> testListeners = new SearchByClass<>(TestStateListener.class); // TL - S&E
        test.traverse(testListeners);

        // Merge in any additional test listeners
        // currently only used by the function parser
        testListeners.getSearchResults().addAll(testList);
        testList.clear(); // no longer needed

        test.traverse(new TurnElementsOn());
        notifyTestListenersOfStart(testListeners);

        List<?> testLevelElements = new LinkedList<>(test.list(test.getArray()[0]));
        removeThreadGroups(testLevelElements);

        SearchByClass<SetupThreadGroup> setupSearcher = new SearchByClass<>(SetupThreadGroup.class);
        SearchByClass<AbstractThreadGroup> searcher = new SearchByClass<>(AbstractThreadGroup.class);
        SearchByClass<PostThreadGroup> postSearcher = new SearchByClass<>(PostThreadGroup.class);

        test.traverse(setupSearcher);
        test.traverse(searcher);
        test.traverse(postSearcher);

        TestCompiler.initialize();
        // for each thread group, generate threads
        // hand each thread the sampler controller
        // and the listeners, and the timer
        Iterator<SetupThreadGroup> setupIter = setupSearcher.getSearchResults().iterator();
        Iterator<AbstractThreadGroup> iter = searcher.getSearchResults().iterator();
        Iterator<PostThreadGroup> postIter = postSearcher.getSearchResults().iterator();

        ListenerNotifier notifier = new ListenerNotifier();

        int groupCount = 0;
        JMeterContextService.clearTotalThreads();

        if (setupIter.hasNext()) {
            log.info("Starting setUp thread groups");
            while (running && setupIter.hasNext()) {//for each setup thread group
                AbstractThreadGroup group = setupIter.next();
                groupCount++;
                String groupName = group.getName();
                log.info("Starting setUp ThreadGroup: {} : {} ", groupCount, groupName);
                startThreadGroup(group, groupCount, setupSearcher, testLevelElements, notifier);
                if (serialized && setupIter.hasNext()) {
                    log.info("Waiting for setup thread group: {} to finish before starting next setup group",
                            groupName);
                    group.waitThreadsStopped();
                }
            }
            log.info("Waiting for all setup thread groups to exit");
            //wait for all Setup Threads To Exit
            waitThreadsStopped();
            log.info("All Setup Threads have ended");
            groupCount=0;
            JMeterContextService.clearTotalThreads();
        }

        groups.clear(); // The groups have all completed now

        /*
         * Here's where the test really starts. Run a Full GC now: it's no harm
         * at all (just delays test start by a tiny amount) and hitting one too
         * early in the test can impair results for short tests.
         */
        JMeterUtils.helpGC();

        JMeterContextService.getContext().setSamplingStarted(true);
        boolean mainGroups = running; // still running at this point, i.e. setUp was not cancelled
        while (running && iter.hasNext()) {// for each thread group
            AbstractThreadGroup group = iter.next();
            //ignore Setup and Post here.  We could have filtered the searcher. but then
            //future Thread Group objects wouldn't execute.
            if (group instanceof SetupThreadGroup ||
                    group instanceof PostThreadGroup) {
                continue;
            }
            groupCount++;
            String groupName = group.getName();
            log.info("Starting ThreadGroup: {} : {}", groupCount, groupName);
            startThreadGroup(group, groupCount, searcher, testLevelElements, notifier);
            if (serialized && iter.hasNext()) {
                log.info("Waiting for thread group: {} to finish before starting next group", groupName);
                group.waitThreadsStopped();
            }
        } // end of thread groups
        if (groupCount == 0){ // No TGs found
            log.info("No enabled thread groups found");
        } else {
            if (running) {
                log.info("All thread groups have been started");
            } else {
                log.info("Test stopped - no more thread groups will be started");
            }
        }

        //wait for all Test Threads To Exit
        waitThreadsStopped();
        groups.clear(); // The groups have all completed now

        if (postIter.hasNext()){
            groupCount = 0;
            JMeterContextService.clearTotalThreads();
            log.info("Starting tearDown thread groups");
            if (mainGroups && !running) { // i.e. shutdown/stopped during main thread groups
                running = tearDownOnShutdown; // re-enable for tearDown if necessary
            }
            while (running && postIter.hasNext()) {//for each setup thread group
                AbstractThreadGroup group = postIter.next();
                groupCount++;
                String groupName = group.getName();
                log.info("Starting tearDown ThreadGroup: {} : {}", groupCount, groupName);
                startThreadGroup(group, groupCount, postSearcher, testLevelElements, notifier);
                if (serialized && postIter.hasNext()) {
                    log.info("Waiting for post thread group: {} to finish before starting next post group", groupName);
                    group.waitThreadsStopped();
                }
            }
            waitThreadsStopped(); // wait for Post threads to stop
        }

        notifyTestListenersOfEnd(testListeners);
        JMeterContextService.endTest();
        if (JMeter.isNonGUI() && SYSTEM_EXIT_FORCED) {
            log.info("Forced JVM shutdown requested at end of test");
            System.exit(0); // NOSONAR Intentional
        }
    }

    private void startThreadGroup(AbstractThreadGroup group, int groupCount, SearchByClass<?> searcher, List<?> testLevelElements, ListenerNotifier notifier)
    {
        try {
            int numThreads = group.getNumThreads();
            JMeterContextService.addTotalThreads(numThreads);
            boolean onErrorStopTest = group.getOnErrorStopTest();
            boolean onErrorStopTestNow = group.getOnErrorStopTestNow();
            boolean onErrorStopThread = group.getOnErrorStopThread();
            boolean onErrorStartNextLoop = group.getOnErrorStartNextLoop();
            String groupName = group.getName();
            log.info("Starting {} threads for group {}.", numThreads, groupName);
            if (onErrorStopTest) {
                log.info("Test will stop on error");
            } else if (onErrorStopTestNow) {
                log.info("Test will stop abruptly on error");
            } else if (onErrorStopThread) {
                log.info("Thread will stop on error");
            } else if (onErrorStartNextLoop) {
                log.info("Thread will start next loop on error");
            } else {
                log.info("Thread will continue on error");
            }
            ListedHashTree threadGroupTree = (ListedHashTree) searcher.getSubTree(group);
            threadGroupTree.add(group, testLevelElements);

            groups.add(group);
            group.start(groupCount, notifier, threadGroupTree, this);
        } catch (JMeterStopTestException ex) { // NOSONAR Reported by log
            JMeterUtils.reportErrorToUser("Error occurred starting thread group :" + group.getName()+ ", error message:"+ex.getMessage()
                    +", \r\nsee log file for more details", ex);
            return; // no point continuing
        }
    }

    /**
     * Wait for Group Threads to stop
     */
    private void waitThreadsStopped() {
        // ConcurrentHashMap does not need synch. here
        for (AbstractThreadGroup threadGroup : groups) {
            threadGroup.waitThreadsStopped();
        }
    }

    /**
     * Clean shutdown ie, wait for end of current running samplers
     */
    public void askThreadsToStop() {
        if (engine != null) { // Will be null if StopTest thread has started
            engine.stopTest(false);
        }
    }

    /**
     * Remote exit
     * Called by RemoteJMeterEngineImpl.rexit()
     * and by notifyTestListenersOfEnd() iff exitAfterTest is true;
     * in turn that is called by the run() method and the StopTest class
     * also called
     */
    @Override
    public void exit() {
        ClientJMeterEngine.tidyRMI(log); // This should be enough to allow server to exit.
        if (REMOTE_SYSTEM_EXIT) { // default is false
            log.warn("About to run System.exit(0) on {}", host);
            // Needs to be run in a separate thread to allow RMI call to return OK
            Thread t = new Thread() {
                @Override
                public void run() {
                    pause(1000); // Allow RMI to complete
                    log.info("Bye from {}", host);
                    System.out.println("Bye from "+host); // NOSONAR Intentional
                    System.exit(0); // NOSONAR Intentional
                }
            };
            t.start();
        }
    }

    private void pause(long ms){
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void setProperties(Properties p) {
        log.info("Applying properties {}", p);
        JMeterUtils.getJMeterProperties().putAll(p);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public List<AbstractThreadGroup> getGroups() {
        return groups;
    }
}
