package io.renren.modules.test.service.impl;

import com.jcraft.jsch.JSchException;
import io.renren.common.exception.RRException;
import io.renren.modules.test.dao.StressTestDao;
import io.renren.modules.test.dao.StressTestFileDao;
import io.renren.modules.test.dao.StressTestReportsDao;
import io.renren.modules.test.dao.StressTestSlaveDao;
import io.renren.modules.test.entity.StressTestEntity;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.entity.StressTestSlaveEntity;
import io.renren.modules.test.handler.FileExecuteResultHandler;
import io.renren.modules.test.handler.FileResultHandler;
import io.renren.modules.test.handler.FileStopResultHandler;
import io.renren.modules.test.jmeter.JmeterListenToTest;
import io.renren.modules.test.jmeter.JmeterResultCollector;
import io.renren.modules.test.jmeter.JmeterRunEntity;
import io.renren.modules.test.jmeter.JmeterStatEntity;
import io.renren.modules.test.service.StressTestFileService;
import io.renren.modules.test.utils.SSH2Utils;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.engine.DistributedRunner;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.engine.JMeterEngineException;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service("stressTestFileService")
public class StressTestFileServiceImpl implements StressTestFileService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private StressTestFileDao stressTestFileDao;

    @Autowired
    private StressTestReportsDao stressTestReportsDao;

    @Autowired
    private StressTestSlaveDao stressTestSlaveDao;

    @Autowired
    private StressTestDao stressTestDao;

    @Autowired
    private StressTestUtils stressTestUtils;

    private static final String JAVA_CLASS_PATH = "java.class.path";
    private static final String CLASSPATH_SEPARATOR = File.pathSeparator;

    private static final String OS_NAME = System.getProperty("os.name");// $NON-NLS-1$

    private static final String OS_NAME_LC = OS_NAME.toLowerCase(java.util.Locale.ENGLISH);

    private static final String JMETER_INSTALLATION_DIRECTORY;

    /**
     * 增加了一个static代码块，本身是从Jmeter的NewDriver源码中复制过来的。
     * Jmeter的api中是删掉了这部分代码的，需要从Jmeter源码中才能看到。
     * 由于源码中bug的修复很多，我也就原封保留了。
     *
     * 这段代码块的意义在于，通过Jmeter_Home的地址，找到Jmeter要加载的jar包的目录。
     * 将这些jar包中的方法的class_path，放置到JAVA_CLASS_PATH系统变量中。
     * 而Jmeter在遇到参数化的函数表达式的时候，会从JAVA_CLASS_PATH系统变量中来找到这些对应关系。
     * 而Jmeter的插件也是一个原理，来找到这些对应关系。
     * 其中配置文件还包含了这些插件的过滤配置，默认是.functions. 的必须，.gui.的非必须。
     * 配置key为  classfinder.functions.notContain
     *
     * 带来的影响：
     * 让程序和Jmeter_home外部的联系更加耦合了，这样master必带Jmeter_home才可以。
     * 不仅仅是测试报告的生成了。
     * 同时，需要在pom文件中引用ApacheJMeter_functions，这其中才包含了参数化所用的函数的实现类。
     *
     * 自己修改：
     * 1. 可以将class_path直接拼接字符串的形式添加到系统变量中，不过如果Jmeter改了命名，则这边也要同步修改很麻烦。
     * 2. 修改Jmeter源码，将JAVA_CLASS_PATH系统变量这部分的查找改掉。在CompoundVariable 类的static块中。
     *    ClassFinder.findClassesThatExtend 方法。
     *
     * 写成static代码块，也是因为类加载（第一次请求时），才会初始化并初始化一次。这也是符合逻辑的。
     */
    static {
        final List<URL> jars = new LinkedList<>();
        final String initial_classpath = System.getProperty(JAVA_CLASS_PATH);

        JMETER_INSTALLATION_DIRECTORY = StressTestUtils.getJmeterHome();

        /*
         * Does the system support UNC paths? If so, may need to fix them up
         * later
         */
        boolean usesUNC = OS_NAME_LC.startsWith("windows");// $NON-NLS-1$

        // Add standard jar locations to initial classpath
        StringBuilder classpath = new StringBuilder();
        File[] libDirs = new File[]{new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib"),// $NON-NLS-1$ $NON-NLS-2$
                new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib" + File.separator + "ext"),// $NON-NLS-1$ $NON-NLS-2$
                new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib" + File.separator + "junit")};// $NON-NLS-1$ $NON-NLS-2$
        for (File libDir : libDirs) {
            File[] libJars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (libJars == null) {
                new Throwable("Could not access " + libDir).printStackTrace(); // NOSONAR No logging here
                continue;
            }
            Arrays.sort(libJars); // Bug 50708 Ensure predictable order of jars
            for (File libJar : libJars) {
                try {
                    String s = libJar.getPath();

                    // Fix path to allow the use of UNC URLs
                    if (usesUNC) {
                        if (s.startsWith("\\\\") && !s.startsWith("\\\\\\")) {// $NON-NLS-1$ $NON-NLS-2$
                            s = "\\\\" + s;// $NON-NLS-1$
                        } else if (s.startsWith("//") && !s.startsWith("///")) {// $NON-NLS-1$ $NON-NLS-2$
                            s = "//" + s;// $NON-NLS-1$
                        }
                    } // usesUNC

                    jars.add(new File(s).toURI().toURL());// See Java bug 4496398
                    classpath.append(CLASSPATH_SEPARATOR);
                    classpath.append(s);
                } catch (MalformedURLException e) { // NOSONAR
//                    EXCEPTIONS_IN_INIT.add(new Exception("Error adding jar:"+libJar.getAbsolutePath(), e));
                }
            }
        }

        // ClassFinder needs the classpath
        System.setProperty(JAVA_CLASS_PATH, initial_classpath + classpath.toString());
    }


    @Override
    public StressTestFileEntity queryObject(Long fileId) {
        return stressTestFileDao.queryObject(fileId);
    }

    @Override
    public List<StressTestFileEntity> queryList(Map<String, Object> map) {
        return stressTestFileDao.queryList(map);
    }

    @Override
    public List<StressTestFileEntity> queryList(Long caseId) {
        Map query = new HashMap<>();
        query.put("caseId", caseId.toString());
        return stressTestFileDao.queryList(query);
    }

    @Override
    public int queryTotal(Map<String, Object> map) {
        return stressTestFileDao.queryTotal(map);
    }

    @Override
    public void save(StressTestFileEntity stressTestFile) {
        stressTestFileDao.save(stressTestFile);
    }

    /**
     * 保存用例文件及入库
     */
    @Override
    @Transactional
    public void save(MultipartFile multipartFile, String filePath, StressTestEntity stressCase, StressTestFileEntity stressTestFile) {
        // 保存文件放这里,是因为有事务.
        // 保存数据放在最前,因为当前文件重名校验是根据数据库异常得到
        try {
            String fileMd5 = DigestUtils.md5Hex(multipartFile.getBytes());
            stressTestFile.setFileMd5(fileMd5);
        } catch (IOException e) {
            throw new RRException("获取上传文件的MD5失败！", e);
        }
        if (stressTestFile.getFileId() != null && stressTestFile.getFileId() > 0L) {
            // 替换文件，同时修改添加时间，便于前端显示。
            stressTestFile.setAddTime(new Date());
            update(stressTestFile);
        } else {
            save(stressTestFile);
        }
        // 肯定存在已有的用例信息
        stressTestDao.update(stressCase);
        saveFile(multipartFile, filePath);
    }

    @Override
    public void update(StressTestFileEntity stressTestFile) {
        stressTestFileDao.update(stressTestFile);
    }

    @Override
    @Transactional
    public void update(StressTestFileEntity stressTestFile, StressTestReportsEntity stressTestReports) {
        update(stressTestFile);
        if (stressTestReports != null) {
            stressTestReportsDao.update(stressTestReports);
        }
    }

    /**
     * 更新用例文件及入库
     */
    @Override
    @Transactional
    public void update(MultipartFile multipartFile, String filePath, StressTestEntity stressCase, StressTestFileEntity stressTestFile) {
        try {
            String fileMd5 = DigestUtils.md5Hex(multipartFile.getBytes());
            stressTestFile.setFileMd5(fileMd5);
        } catch (IOException e) {
            throw new RRException("获取上传文件的MD5失败！", e);
        }
        update(stressTestFile);
        stressTestDao.update(stressCase);
        saveFile(multipartFile, filePath);
    }

    /**
     * 批量删除
     * 删除所有缓存 + 方法只要调用即删除所有缓存。
     */
    @Override
    @Transactional
    public void deleteBatch(Object[] fileIds) {
        Arrays.asList(fileIds).stream().forEach(fileId -> {
            StressTestFileEntity stressTestFile = queryObject((Long) fileId);
            String casePath = stressTestUtils.getCasePath();
            String FilePath = casePath + File.separator + stressTestFile.getFileName();

            String jmxDir = FilePath.substring(0, FilePath.lastIndexOf("."));
            File jmxDirFile = new File(jmxDir);
            try {
                FileUtils.forceDelete(new File(FilePath));
            } catch (FileNotFoundException e) {
                logger.error("要删除的文件找不到(删除成功)  " + e.getMessage());
            } catch (IOException e) {
                throw new RRException("删除文件异常失败", e);
            }
            try {
                if (FileUtils.sizeOf(jmxDirFile) == 0L) {
                    FileUtils.forceDelete(jmxDirFile);
                }
            } catch (FileNotFoundException | IllegalArgumentException e) {
                logger.error("要删除的jmx文件夹找不到(删除成功)  " + e.getMessage());
            } catch (IOException e) {
                throw new RRException("删除jmx文件夹异常失败", e);
            }
            //删除远程节点的同步文件，如果远程节点比较多，网络不好，执行时间会比较长。
            deleteSlaveFile((Long) fileId);
        });

        stressTestFileDao.deleteBatch(fileIds);
    }

    /**
     * 没有事务，不允许回滚
     * 因为是遍历执行，每一个执行可以是一个事务。
     * <p>
     * 接口是支持批量运行的，但是强烈不建议这样做。
     */
    @Override
    public void run(Long[] fileIds) {
        Arrays.asList(fileIds).stream().forEach(fileId -> {
            runSingle(fileId);
        });
    }


    /**
     * 脚本的启动都是新的线程，其中的SQL是不和启动是同一个事务的。
     * 同理，也不会回滚这一事务。
     */
    @Transactional
    public void runSingle(Long fileId) {
        StressTestFileEntity stressTestFile = queryObject(fileId);
        if (stressTestFile.getStatus() == 1) {
            throw new RRException("脚本正在运行");
        }

        String casePath = stressTestUtils.getCasePath();
        String fileName = stressTestFile.getFileName();
        String filePath = casePath + File.separator + fileName;

        // 测试结果文件路径
        // jmx用例文件夹对应的相对路径名如20180504172207568\case20180504172207607
        String jmxDir = fileName.substring(0, fileName.lastIndexOf("."));
        // csv文件的名称，如case20180504172207607_4444.csv
        String csvName = jmxDir.substring(jmxDir.lastIndexOf(File.separator) + 1) + StressTestUtils.getSuffix4() + ".csv";
        // csv文件的真实路径，如D:\E\stressTestCases\20180504172207568\case20180504172207607\case20180504172207607_4444.csv
        String csvPath = casePath + File.separator + jmxDir + File.separator + csvName;
        String fileOriginName = stressTestFile.getOriginName();
        String reportOirginName = fileOriginName.substring(0, fileOriginName.lastIndexOf(".")) + "_" + StressTestUtils.getSuffix4();

        File csvFile = new File(csvPath);
        File jmxFile = new File(filePath);

        StressTestReportsEntity stressTestReports = null;
        if (StressTestUtils.NEED_REPORT.equals(stressTestFile.getReportStatus())) {
            stressTestReports = new StressTestReportsEntity();
            //保存测试报告stressTestReportsDao
            stressTestReports.setCaseId(stressTestFile.getCaseId());
            stressTestReports.setFileId(fileId);
            stressTestReports.setOriginName(reportOirginName);
            stressTestReports.setReportName(jmxDir + File.separator + csvName);
            stressTestReports.setFile(csvFile);
        }

        Map map = new HashMap();
        map.put("jmxFile", jmxFile);
        map.put("csvFile", csvFile);

        if (stressTestUtils.isUseJmeterScript()) {
            excuteJmeterRunByScript(stressTestFile, stressTestReports, map);
        } else {
            excuteJmeterRunLocal(stressTestFile, stressTestReports, map);
        }

        //保存文件的执行状态，用于前台提示及后端查看排序。
        //脚本基本执行无异常，才会保存状态入库。
        stressTestFile.setStatus(StressTestUtils.RUNNING);
        update(stressTestFile);

        if (stressTestReports != null) {
            stressTestReportsDao.save(stressTestReports);
        }
    }

    /**
     * 执行Jmeter的脚本文件，采用Apache的commons-exec来执行。
     */
    public void excuteJmeterRunByScript(StressTestFileEntity stressTestFile,
                                        StressTestReportsEntity stressTestReports, Map map) {
        String jmeterHomeBin = stressTestUtils.getJmeterHomeBin();
        String jmeterExc = stressTestUtils.getJmeterExc();
        CommandLine cmdLine = new CommandLine(jmeterHomeBin + File.separator + jmeterExc);
        // 设置参数，-n 命令行模式
        cmdLine.addArgument("-n");
        // -t 设置JMX脚本路径
        cmdLine.addArgument("-t");
        cmdLine.addArgument("${jmxFile}");

        String slaveStr = getSlaveIPPort();
        if (StringUtils.isNotEmpty(slaveStr)) {
            cmdLine.addArgument("-R");
            cmdLine.addArgument(slaveStr);
        }

        if (StressTestUtils.NEED_REPORT.equals(stressTestFile.getReportStatus())) {
            cmdLine.addArgument("-l");
            cmdLine.addArgument("${csvFile}");
        }

        // 指定需要执行的JMX脚本
        cmdLine.setSubstitutionMap(map);

        DefaultExecutor executor = new DefaultExecutor();

        try {
            //非阻塞方式运行脚本命令，不耽误前端的操作。
            //流操作在executor执行源码中已经关闭。
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
            // 设置成功输入及错误输出，用于追查问题，打印日志。
            executor.setStreamHandler(streamHandler);
            // 自定义的钩子程序
            FileExecuteResultHandler resultHandler =
                    new FileExecuteResultHandler(stressTestFile, stressTestReports,
                            this, outputStream, errorStream);
            // 执行脚本命令
            executor.execute(cmdLine, resultHandler);
        } catch (IOException e) {
            //保存状态，执行出现异常
            stressTestFile.setStatus(StressTestUtils.RUN_ERROR);
            update(stressTestFile);
            if (stressTestReports != null) {
                stressTestReportsDao.save(stressTestReports);
            }
            throw new RRException("执行启动脚本异常！", e);
        }
    }

    /**
     * 本地执行Jmeter的脚本文件，采用Apache-Jmeter-Api来执行。
     */
    public void excuteJmeterRunLocal(StressTestFileEntity stressTestFile, StressTestReportsEntity stressTestReports, Map map) {
        File jmxFile = (File) map.get("jmxFile");
        File csvFile = (File) map.get("csvFile");

        // engines是为了分布式节点使用。
        List<JMeterEngine> engines = new LinkedList<>();
        JmeterRunEntity jmeterRunEntity = new JmeterRunEntity();
        jmeterRunEntity.setStressTestFile(stressTestFile);
        jmeterRunEntity.setStressTestReports(stressTestReports);
        jmeterRunEntity.setEngines(engines);
        StressTestUtils.jMeterEntity4file.put(stressTestFile.getFileId(), jmeterRunEntity);

        setJmeterProperties();

        FileServer.getFileServer().setBaseForScript(jmxFile);

        try {
            HashTree jmxTree = SaveService.loadTree(jmxFile);
            JMeter.convertSubTree(jmxTree);

            String slaveStr = getSlaveIPPort();
            // slaveStr用来做脚本是否是分布式执行的判断，不入库。
            stressTestFile.setSlaveStr(slaveStr);

            // 如果不要监控也不要测试报告，则不加自定义的Collector到文件里，让性能最大化。
            if (StressTestUtils.NEED_REPORT.equals(stressTestFile.getReportStatus())
                    || StressTestUtils.NEED_WEB_CHART.equals(stressTestFile.getWebchartStatus())) {
                // 添加收集观察监听程序。
                // 具体情况的区分在其程序内做分别，原因是情况较多，父子类的实现不现实。
                // 使用自定义的Collector，用于前端绘图的数据收集和日志收集等。
                JmeterResultCollector jmeterResultCollector = new JmeterResultCollector(stressTestFile);
                jmeterResultCollector.setFilename(csvFile.getPath());
                jmxTree.add(jmxTree.getArray()[0], jmeterResultCollector);
            }

            // 增加程序执行结束的监控
            // engines 为null停止脚本后不会直接停止远程client的JVM进程。
            // reportGenerator 为null停止后脚本后不会直接生成测试报告。
            jmxTree.add(jmxTree.getArray()[0], new JmeterListenToTest(null,
                    null, this, stressTestFile.getFileId()));

            if (StringUtils.isNotEmpty(slaveStr)) {//分布式的方式启动
                java.util.StringTokenizer st = new java.util.StringTokenizer(slaveStr, ",");//$NON-NLS-1$
                List<String> hosts = new LinkedList<>();
                while (st.hasMoreElements()) {
                    hosts.add((String) st.nextElement());
                }
                DistributedRunner distributedRunner = new DistributedRunner();
                distributedRunner.setStdout(System.out); // NOSONAR
                distributedRunner.setStdErr(System.err); // NOSONAR
                distributedRunner.init(hosts, jmxTree);
                engines.addAll(distributedRunner.getEngines());
                distributedRunner.start();

                // 如果配置了，则将本机节点也增加进去
                // 当前只有本地运行的方式支持本机master节点的添加
                if (checkSlaveLocal()) {
                    // JMeterEngine 本身就是线程，启动即为异步执行，resultCollector会监听保存csv文件。
                    JMeterEngine engine = new StandardJMeterEngine();
                    engine.configure(jmxTree);
                    engine.runTest();
                    engines.add(engine);
                }
            } else {//本机运行
                // JMeterEngine 本身就是线程，启动即为异步执行，resultCollector会监听保存csv文件。
                JMeterEngine engine = new StandardJMeterEngine();
                engine.configure(jmxTree);
                engine.runTest();
                engines.add(engine);
            }

        } catch (IOException e) {
            throw new RRException("本地执行启动脚本文件异常！", e);
        } catch (JMeterEngineException e) {
            throw new RRException("本地执行启动脚本Jmeter程序异常！", e);
        } catch (RuntimeException e) {
            throw new RRException(e.getMessage(), e);
        }
    }

    /**
     * 设置Jmeter运行环境相关的配置，如配置文件的加载，当地语言环境等。
     */
    public void setJmeterProperties() {
        String jmeterHomeBin = stressTestUtils.getJmeterHomeBin();
        JMeterUtils.loadJMeterProperties(jmeterHomeBin + File.separator + "jmeter.properties");
        JMeterUtils.setJMeterHome(StressTestUtils.getJmeterHome());
        JMeterUtils.initLocale();

        Properties jmeterProps = JMeterUtils.getJMeterProperties();

        // Add local JMeter properties, if the file is found
        String userProp = JMeterUtils.getPropDefault("user.properties", ""); //$NON-NLS-1$
        if (userProp.length() > 0) { //$NON-NLS-1$
            File file = JMeterUtils.findFile(userProp);
            if (file.canRead()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    Properties tmp = new Properties();
                    tmp.load(fis);
                    jmeterProps.putAll(tmp);
                } catch (IOException e) {
                }
            }
        }

        // Add local system properties, if the file is found
        String sysProp = JMeterUtils.getPropDefault("system.properties", ""); //$NON-NLS-1$
        if (sysProp.length() > 0) {
            File file = JMeterUtils.findFile(sysProp);
            if (file.canRead()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    System.getProperties().load(fis);
                } catch (IOException e) {
                }
            }
        }

        jmeterProps.put("jmeter.version", JMeterUtils.getJMeterVersion());
    }

    /**
     * 没有事务，不允许回滚
     * 因为是遍历执行，每一个执行可以是一个事务。
     */
    @Override
    public void stop(Long[] fileIds) {
        Arrays.asList(fileIds).stream().forEach(fileId -> {
            stopSingle(fileId);
        });
    }

    /**
     * 脚本的启动都是新的线程，其中的SQL是不和启动是同一个事务的。
     * 同理，也不会回滚这一事务。
     */
    public void stopSingle(Long fileId) {
        if (stressTestUtils.isUseJmeterScript()) {
            throw new RRException("Jmeter脚本启动不支持单独停止，请使用全部停止！");
        } else {
            Map<Long, JmeterRunEntity> jMeterEntity4file = StressTestUtils.jMeterEntity4file;
            if (!jMeterEntity4file.isEmpty()) {
                jMeterEntity4file.forEach((fileIdRunning, jmeterRunEntity) -> {
                    if (fileId == fileIdRunning) {  //找到要停止的脚本文件
                        stopLocal(fileId, jmeterRunEntity);
                    }
                });
            }
        }
    }

    /**
     * 停止内核Jmeter-core方式执行的脚本
     */
    @Override
    @Transactional
    public void stopLocal(Long fileId, JmeterRunEntity jmeterRunEntity) {
        StressTestFileEntity stressTestFile = jmeterRunEntity.getStressTestFile();
        StressTestReportsEntity stressTestReports = jmeterRunEntity.getStressTestReports();

        // 只处理了成功的情况，失败的情况当前捕获不到。
        stressTestFile.setStatus(StressTestUtils.RUN_SUCCESS);
        if (stressTestReports != null && stressTestReports.getFile().exists()) {
            stressTestReports.setFileSize(FileUtils.sizeOf(stressTestReports.getFile()));
        }
        update(stressTestFile, stressTestReports);

        jmeterRunEntity.stop();

        // 需要将结果收集的部分干掉
        StressTestUtils.samplingStatCalculator4File.remove(fileId);

    }

    /**
     * 脚本方式执行，只能全部停止，做不到根据线程名称停止指定执行的用例脚本。
     */
    @Override
    @Transactional
    public void stopAll() {
        String jmeterHomeBin = stressTestUtils.getJmeterHomeBin();
        String jmeterStopExc = stressTestUtils.getJmeterStopExc();

        // 使用脚本执行Jmeter-jmx用例，是另起一个程序进行。
        if (stressTestUtils.isUseJmeterScript()) {
            CommandLine cmdLine = new CommandLine(jmeterHomeBin + File.separator + jmeterStopExc);

            DefaultExecutor executor = new DefaultExecutor();

            try {
                //非阻塞方式运行脚本命令。
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
                PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
                // 设置成功输入及错误输出，用于追查问题，打印日志。
                executor.setStreamHandler(streamHandler);
                // 自定义的钩子程序
                FileResultHandler resultHandler = new FileStopResultHandler(this, outputStream, errorStream);
                // 执行脚本命令
                executor.execute(cmdLine, resultHandler);
            } catch (Exception e) {
                //保存状态，执行出现异常
                throw new RRException("停止所有脚本活动操作出现异常");
            }
        } else {
            // 本机停止脚本，可以做到针对某个脚本jmx文件做停止。
            // 这里是全部停止
            Map<Long, JmeterRunEntity> jMeterEntity4file = StressTestUtils.jMeterEntity4file;
            if (!jMeterEntity4file.isEmpty()) {
                jMeterEntity4file.forEach((fileId, jmeterRunEntity) -> {
                    stopLocal(fileId, jmeterRunEntity);
                });
            }

            // 对于全部停止，再次全部移除统计数据
            StressTestUtils.samplingStatCalculator4File.clear();

            // 如果本地已经没有保存engines了，则将数据库中的状态归位。
            // 本地调试重新启动系统，会出现这种情况。
            if (jMeterEntity4file.isEmpty()) {
                List<StressTestFileEntity> list = queryList(new HashMap<>());
                list.forEach(fileEntity -> {
                    if (StressTestUtils.RUNNING.equals(fileEntity.getStatus())) {
                        fileEntity.setStatus(StressTestUtils.RUN_SUCCESS);
                        update(fileEntity);
                    }
                });
            }
        }
    }

    @Override
    public void stopAllNow() {
    }

    @Override
    public JmeterStatEntity getJmeterStatEntity(Long fileId) {
        // 每次调用都是一个全新的对象。不过这个对象仅用于前端返回，直接可以垃圾回收掉。
        if (StringUtils.isNotEmpty(getSlaveIPPort())) {
            return new JmeterStatEntity(fileId, 0L);
        }
        return new JmeterStatEntity(fileId, null);
    }

    /**
     * 向子节点同步参数化文件
     */
    @Override
    public void synchronizeFile(Long[] fileIds) {
        //当前是向所有的分布式节点推送这个，阻塞操作+轮询，并非多线程，因为本地同步网卡会是瓶颈。
        Map query = new HashMap<>();
        query.put("status", StressTestUtils.ENABLE);
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(query);
        //使用for循环传统写法
        //采用了先给同一个节点机传送多个文件的方式，因为数据库的连接消耗优于节点机的链接消耗
        for (StressTestSlaveEntity slave : stressTestSlaveList) {

            // 不向本地节点传送文件
            if ("127.0.0.1".equals(slave.getIp().trim())) {
                continue;
            }

            SSH2Utils ssh2Util = new SSH2Utils(slave.getIp(), slave.getUserName(),
                    slave.getPasswd(), Integer.parseInt(slave.getSshPort()));
            try {
                ssh2Util.initialSession();

                for (Long fileId : fileIds) {
                    StressTestFileEntity stressTestFile = queryObject(fileId);
                    putFileToSlave(slave, ssh2Util, stressTestFile);
                }
            } catch (JSchException e) {
                throw new RRException(slave.getSlaveName() + "节点机远程链接初始化时失败！请核对节点机信息路径", e);
            } finally {
                try {
                    ssh2Util.close();
                } catch (Exception e) {
                    throw new RRException(slave.getSlaveName() + "节点机远程链接关闭时失败！", e);
                }
            }
        }

    }

    @Override
    public String getFilePath(StressTestFileEntity stressTestFile) {
        String casePath = stressTestUtils.getCasePath();
        String FilePath = casePath + File.separator + stressTestFile.getFileName();
        return FilePath;
    }

    /**
     * 将文件上传到节点机目录上。
     */
    @Transactional
    public void putFileToSlave(StressTestSlaveEntity slave, SSH2Utils ssh2Util, StressTestFileEntity stressTestFile) {
        String casePath = stressTestUtils.getCasePath();
        String fileNameSave = stressTestFile.getFileName();
        String filePath = casePath + File.separator + fileNameSave;
        String fileSaveMD5 = "";
        try {
            fileSaveMD5 = getMd5ByFile(filePath);
        } catch (IOException e) {
            throw new RRException(stressTestFile.getOriginName() + "生成MD5失败！", e);
        }

        // 避免跨系统的问题，远端由于都时linux服务器，则文件分隔符统一为/，不然同步文件会报错。
        String caseFileHome = slave.getHomeDir() + "/bin/stressTestCases";
        try {
            String MD5 = ssh2Util.runCommand("md5sum " + getSlaveFileName(stressTestFile, slave) + "|cut -d ' ' -f1");
            if (fileSaveMD5.equals(MD5)) {//说明目标服务器已经存在相同文件不再重复上传
                return;
            }

            //上传文件
            ssh2Util.scpPutFile(filePath, caseFileHome);
        } catch (JSchException e) {
            throw new RRException(stressTestFile.getOriginName() + "校验节点机文件MD5时失败！", e);
        } catch (IOException e) {
            throw new RRException(stressTestFile.getOriginName() + "IO传输失败！", e);
        }
//        catch (SftpException e) {
//            throw new RRException(stressTestFile.getOriginName() + "上传到节点机文件时失败！", e);
//        }

        stressTestFile.setStatus(StressTestUtils.RUN_SUCCESS);
        //由于事务性，这个地方不好批量更新。
        update(stressTestFile);
        Map fileQuery = new HashMap<>();
        fileQuery.put("originName", stressTestFile.getOriginName() + "_slaveId" + slave.getSlaveId());
        fileQuery.put("slaveId", slave.getSlaveId().toString());
        StressTestFileEntity newStressTestFile = stressTestFileDao.queryObjectForClone(fileQuery);
        if (newStressTestFile == null) {
            newStressTestFile = stressTestFile.clone();
            newStressTestFile.setStatus(-1);
            newStressTestFile.setFileName(getSlaveFileName(stressTestFile, slave));
            newStressTestFile.setOriginName(stressTestFile.getOriginName() + "_slaveId" + slave.getSlaveId());
            newStressTestFile.setFileMd5(fileSaveMD5);
            // 最重要是保存分布式子节点的ID
            newStressTestFile.setSlaveId(slave.getSlaveId());
            save(newStressTestFile);
        } else {
            newStressTestFile.setFileMd5(fileSaveMD5);
            update(newStressTestFile);
        }
    }

    /**
     * 根据fileId 删除对应的slave节点的文件。
     */
    public void deleteSlaveFile(Long fileId) {
        StressTestFileEntity stressTestFile = queryObject(fileId);
        // 获取参数化文件同步到哪些分布式子节点的记录
        Map fileQuery = new HashMap<>();
        fileQuery.put("originName", stressTestFile.getOriginName() + "_slaveId");
        List<StressTestFileEntity> fileDeleteList = stressTestFileDao.queryListForDelete(fileQuery);

        if (fileDeleteList.isEmpty()) {
            return;
        }
        // 将同步过的分布式子节点的ID收集起来，用于查询子节点对象集合。
        String slaveIds = "";
        ArrayList fileDeleteIds = new ArrayList();
        for (StressTestFileEntity stressTestFile4Slave : fileDeleteList) {
            if (stressTestFile4Slave.getSlaveId() == null) {
                continue;
            }
            if (slaveIds.isEmpty()) {
                slaveIds = stressTestFile4Slave.getSlaveId().toString();
            } else {
                slaveIds += " , " + stressTestFile4Slave.getSlaveId().toString();
            }
            fileDeleteIds.add(stressTestFile4Slave.getFileId());
        }

        if (slaveIds.isEmpty()) {
            return;
        }

        // 每一个参数化文件，会对应多个同步子节点slave的记录。
        Map slaveQuery = new HashMap<>();
        slaveQuery.put("slaveIds", slaveIds);
        // 每一个被同步过的记录，都要执行删除操作。
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(slaveQuery);
        for (StressTestSlaveEntity slave : stressTestSlaveList) {
            // 跳过本地节点
            if ("127.0.0.1".equals(slave.getIp().trim())) {
                continue;
            }

            SSH2Utils ssh2Util = new SSH2Utils(slave.getIp(), slave.getUserName(),
                    slave.getPasswd(), Integer.parseInt(slave.getSshPort()));

            try {
                ssh2Util.initialSession();
                ssh2Util.runCommand("rm -f " + getSlaveFileName(stressTestFile, slave));
            } catch (JSchException e) {
                throw new RRException(slave.getSlaveName() + "节点机远程链接初始化时失败！请核对节点机信息路径", e);
            } catch (IOException e) {
                throw new RRException(slave.getSlaveName() + "删除远程文件命令执行失败!", e);
            } finally {
                try {
                    ssh2Util.close();
                } catch (Exception e) {
                    throw new RRException(slave.getSlaveName() + "节点机远程链接关闭时失败！", e);
                }
            }
        }

        stressTestFileDao.deleteBatch(fileDeleteIds.toArray());
    }

    /**
     * 获取slave节点上的参数化文件具体路径
     */
    public String getSlaveFileName(StressTestFileEntity stressTestFile, StressTestSlaveEntity slave) {
        // 避免跨系统的问题，远端由于都时linux服务器，则文件分隔符统一为/，不然同步文件会报错。
        String caseFileHome = slave.getHomeDir() + "/bin/stressTestCases";
        String fileNameUpload = stressTestFile.getOriginName();
        return caseFileHome + "/" + fileNameUpload;
    }

    /**
     * 保存文件
     */
    private void saveFile(MultipartFile multipartFile, String filePath) {
        try {
            File file = new File(filePath);
            FileUtils.forceMkdirParent(file);
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new RRException("保存文件异常失败", e);
        }
    }

    /**
     * 获取上传文件的md5
     */
    public String getMd5(MultipartFile file) throws IOException {
        return DigestUtils.md5Hex(file.getBytes());
    }

    /**
     * 拼装分布式节点，当前还没有遇到分布式节点非常多的情况。
     *
     * @return 分布式节点的IP地址拼装，不包含本地127.0.0.1的IP
     */
    public String getSlaveIPPort() {
        Map query = new HashMap<>();
        query.put("status", StressTestUtils.ENABLE);
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(query);

        StringBuilder stringBuilder = new StringBuilder();
        for (StressTestSlaveEntity slave : stressTestSlaveList) {
            // 本机不包含在内
            if ("127.0.0.1".equals(slave.getIp().trim())) {
                continue;
            }

            if (stringBuilder.length() != 0) {
                stringBuilder.append(",");
            }
            stringBuilder.append(slave.getIp()).append(":").append(slave.getJmeterPort());
        }
        return stringBuilder.toString();
    }

    /**
     * master节点是否被使用为压力节点
     */
    public boolean checkSlaveLocal() {
        Map query = new HashMap<>();
        query.put("status", StressTestUtils.ENABLE);
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(query);

        for (StressTestSlaveEntity slave : stressTestSlaveList) {
            // 本机配置IP为127.0.0.1，没配置localhost
            if ("127.0.0.1".equals(slave.getIp().trim())) {
                return true;
            }
        }

        return false;
    }


    /**
     * 获取文件的MD5值，远程节点机也是通过MD5值来判断文件是否重复及存在，所以就不使用其他算法了。
     */
    public String getMd5ByFile(String filePath) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        return DigestUtils.md5Hex(IOUtils.toByteArray(fis));
    }
}
