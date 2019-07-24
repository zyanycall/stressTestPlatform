package io.renren.modules.test.jmeter.runner;

/**
 * Created by zyanycall@gmail.com on 2019-06-24 18:06.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import org.apache.jmeter.engine.ClientJMeterEngine;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.engine.JMeterEngineException;
import org.apache.jmeter.engine.TreeCloner;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * 这个类来自Jmeter4.0，所以如果未来高版本要求需要修改此类。
 * 此类是为了增加一个需求：按照分布式的节点机的权重来配比压力负载。
 * 因为默认的Jmeter是每一个负载机都使用相同的脚本，相同的压力，这和实际情况并不相符。
 * 当前是压测平台需要使用这个功能，如果未来Jmeter已经提供类似方案，可以另行借鉴。
 * <p>
 * This class serves all responsibility of starting and stopping distributed tests.
 * It was refactored from JMeter and RemoteStart classes to unify retry behavior.
 *
 * @see org.apache.jmeter.JMeter
 * @see org.apache.jmeter.gui.action.RemoteStart
 */
public class LocalDistributedRunner {
    private static final Logger log = LoggerFactory.getLogger(org.apache.jmeter.engine.DistributedRunner.class);

    public static final String RETRIES_NUMBER = "client.tries"; // $NON-NLS-1$
    public static final String RETRIES_DELAY = "client.retries_delay"; // $NON-NLS-1$
    public static final String CONTINUE_ON_FAIL = "client.continue_on_fail"; // $NON-NLS-1$

    private final Properties remoteProps;
    private final boolean continueOnFail;
    private final int retriesDelay;
    private final int retriesNumber;
    private PrintStream stdout = new PrintStream(new LocalDistributedRunner.SilentOutputStream());
    private PrintStream stderr = new PrintStream(new LocalDistributedRunner.SilentOutputStream());
    private final Map<String, JMeterEngine> engines = new HashMap<>();


    public LocalDistributedRunner() {
        this(new Properties());
    }

    public LocalDistributedRunner(Properties props) {
        remoteProps = props;
        retriesNumber = JMeterUtils.getPropDefault(RETRIES_NUMBER, 1);
        continueOnFail = JMeterUtils.getPropDefault(CONTINUE_ON_FAIL, false);
        retriesDelay = JMeterUtils.getPropDefault(RETRIES_DELAY, 5000);
    }

    /**
     * 为了不破坏源生的代码，仅做增量修改。
     */
    public void init(List<String> addresses, HashTree tree, Map<String, Integer> addrWeight) {
        // converting list into mutable version
        List<String> addrs = new LinkedList<>(addresses);

        for (int tryNo = 0; tryNo < retriesNumber; tryNo++) {
            if (tryNo > 0) {
                println("Following remote engines will retry configuring: " + addrs);
                println("Pausing before retry for " + retriesDelay + "ms");
                try {
                    Thread.sleep(retriesDelay);
                } catch (InterruptedException e) {  // NOSONAR
                    throw new RuntimeException("Interrupted while initializing remote", e);
                }
            }

            int idx = 0;
            while (idx < addrs.size()) {
                String address = addrs.get(idx);
                println("Configuring remote engine: " + address);

                // zyanycall add
                // 在进程内存内把HashTree即脚本文件的内容修改，主要改两部分，线程数和加载虚拟用户数用时，都是按照比例增加或者缩减。
                // 将脚本内所有的线程组都要修改。
                Integer weight = addrWeight.get(address);
                // 需要将原始的tree clone，因为可能会存在多个分布式节点权重不一样的情况。
                HashTree treeClone = null;

                if (weight != null && weight > 0 && weight != 100) {

                    // 需要时再clone，clone的消耗会稍微多一些
                    // false 是如果 test Element 的类型node ，也会clone。
                    TreeCloner cloner = new TreeCloner(false);
                    tree.traverse(cloner);
                    treeClone = cloner.getClonedTree();

                    for (HashTree item : treeClone.values()) {
                        Set treeKeys = item.keySet();
                        for (Object key : treeKeys) {
                            if (key instanceof ThreadGroup) {
                                int originThreadNum = Integer.parseInt(((ThreadGroup) key).getPropertyAsString(ThreadGroup.NUM_THREADS));
                                // 假如脚本内原来虚拟用户数是100，权重是90，则脚本的虚拟用户数要修改为90，加载用时要修改为原来的90%。
                                // 修改的数值向上取整（避免为0的情况）。
                                int threadNumFix = (int) Math.ceil(originThreadNum * weight / 100d);
                                ((ThreadGroup) key).setProperty(ThreadGroup.NUM_THREADS, "" + threadNumFix);

                                // 修改加载用户数的用时
                                int originRampTime = Integer.parseInt(((ThreadGroup) key).getPropertyAsString(ThreadGroup.RAMP_TIME));
                                int rampTimeFix = (int) Math.ceil(originRampTime * weight / 100d);
                                ((ThreadGroup) key).setProperty(ThreadGroup.RAMP_TIME, "" + rampTimeFix);
                            }
                        }
                    }
                }
                JMeterEngine engine = getClientEngine(address.trim(), treeClone == null ? tree : treeClone);
                // zyanycall fix end

                if (engine != null) {
                    engines.put(address, engine);
                    addrs.remove(address);
                } else {
                    println("Failed to configure " + address);
                    idx++;
                }
            }

            if (addrs.isEmpty()) {
                break;
            }
        }

        if (!addrs.isEmpty()) {
            String msg = "Following remote engines could not be configured:" + addrs;
            if (!continueOnFail || engines.size() == 0) {
                stop();
                throw new RuntimeException(msg); // NOSONAR
            } else {
                println(msg);
                println("Continuing without failed engines...");
            }
        }
    }

    /**
     * Starts a remote testing engines
     *
     * @param addresses list of the DNS names or IP addresses of the remote testing engines
     */
    public void start(List<String> addresses) {
        println("Starting remote engines");
        long now = System.currentTimeMillis();
        println("Starting the test @ " + new Date(now) + " (" + now + ")");
        for (String address : addresses) {
            try {
                if (engines.containsKey(address)) {
                    engines.get(address).runTest();
                } else {
                    log.warn("Host not found in list of active engines: {}", address);
                }
            } catch (IllegalStateException | JMeterEngineException e) { // NOSONAR already reported to user
                JMeterUtils.reportErrorToUser(e.getMessage(), JMeterUtils.getResString("remote_error_starting")); // $NON-NLS-1$
            }
        }
        println("Remote engines have been started");
    }

    /**
     * Start all engines that were previously initiated
     */
    public void start() {
        List<String> addresses = new LinkedList<>();
        addresses.addAll(engines.keySet());
        start(addresses);
    }

    public void stop(List<String> addresses) {
        println("Stopping remote engines");
        for (String address : addresses) {
            try {
                if (engines.containsKey(address)) {
                    engines.get(address).stopTest(true);
                } else {
                    log.warn("Host not found in list of active engines: {}", address);
                }
            } catch (RuntimeException e) {
                errln("Failed to stop test on " + address, e);
            }
        }
        println("Remote engines have been stopped");
    }

    /**
     * Stop all engines that were previously initiated
     */
    public void stop() {
        List<String> addresses = new LinkedList<>();
        addresses.addAll(engines.keySet());
        stop(addresses);
    }

    public void shutdown(List<String> addresses) {
        println("Shutting down remote engines");
        for (String address : addresses) {
            try {
                if (engines.containsKey(address)) {
                    engines.get(address).stopTest(false);
                } else {
                    log.warn("Host not found in list of active engines: {}", address);
                }

            } catch (RuntimeException e) {
                errln("Failed to shutdown test on " + address, e);
            }
        }
        println("Remote engines have been shut down");
    }

    public void exit(List<String> addresses) {
        println("Exiting remote engines");
        for (String address : addresses) {
            try {
                if (engines.containsKey(address)) {
                    engines.get(address).exit();
                } else {
                    log.warn("Host not found in list of active engines: {}", address);
                }
            } catch (RuntimeException e) {
                errln("Failed to exit on " + address, e);
            }
        }
        println("Remote engines have been exited");
    }

    private JMeterEngine getClientEngine(String address, HashTree testTree) {
        JMeterEngine engine;
        try {
            engine = createEngine(address);
            engine.configure(testTree);
            if (!remoteProps.isEmpty()) {
                engine.setProperties(remoteProps);
            }
            return engine;
        } catch (Exception ex) {
            log.error("Failed to create engine at {}", address, ex);
            JMeterUtils.reportErrorToUser(ex.getMessage(),
                    JMeterUtils.getResString("remote_error_init") + ": " + address); // $NON-NLS-1$ $NON-NLS-2$
            return null;
        }
    }

    /**
     * A factory method that might be overridden for unit testing
     *
     * @param address address for engine
     * @return engine instance
     * @throws RemoteException       if registry can't be contacted
     * @throws NotBoundException     when name for address can't be found
     * @throws MalformedURLException when address can't be converted to valid URL
     */
    protected JMeterEngine createEngine(String address) throws RemoteException, NotBoundException, MalformedURLException {
        return new ClientJMeterEngine(address);
    }

    private void println(String s) {
        log.info(s);
        stdout.println(s);
    }

    private void errln(String s, Exception e) {
        log.error(s, e);
        stderr.println(s + ": ");
        e.printStackTrace(stderr); // NOSONAR
    }

    public void setStdout(PrintStream stdout) {
        this.stdout = stdout;
    }

    public void setStdErr(PrintStream stdErr) {
        this.stderr = stdErr;
    }

    private static class SilentOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
            // enjoy the silence
        }
    }

    /**
     * @return {@link Collection} of {@link JMeterEngine}
     */
    public Collection<? extends JMeterEngine> getEngines() {
        return engines.values();
    }
}
