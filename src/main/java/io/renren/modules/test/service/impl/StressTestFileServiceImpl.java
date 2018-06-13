package io.renren.modules.test.service.impl;

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
import io.renren.modules.test.jmeter.JmeterResultCollector;
import io.renren.modules.test.jmeter.JmeterRunEntity;
import io.renren.modules.test.service.StressTestFileService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.engine.DistributedRunner;
import org.apache.jmeter.engine.JMeterEngine;
import org.apache.jmeter.engine.JMeterEngineException;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Service("stressTestFileService")
public class StressTestFileServiceImpl implements StressTestFileService {

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
        return stressTestFileDao.queryList(caseId);
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
    public void save(MultipartFile multipartFile, String filePath, StressTestEntity stressCase, StressTestFileEntity stressCaseFile) {
        // 保存文件放这里,是因为有事务.
        // 保存数据放在最前,因为当前文件重名校验是根据数据库异常得到
        save(stressCaseFile);
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
    public void deleteBatch(Long[] fileIds) {
        Arrays.asList(fileIds).stream().forEach(fileId -> {
            StressTestFileEntity stressTestFile = queryObject(fileId);
            String casePath = stressTestUtils.getCasePath();
            String FilePath = casePath + File.separator + stressTestFile.getFileName();

            String jmxDir = FilePath.substring(0, FilePath.lastIndexOf("."));
            File jmxDirFile = new File(jmxDir);
            try {
                FileUtils.forceDelete(new File(FilePath));
            } catch (FileNotFoundException e) {
                //doNothing
            } catch (IOException e) {
                throw new RRException("删除文件异常失败", e);
            }
            try {
                if (FileUtils.sizeOf(jmxDirFile) == 0L) {
                    FileUtils.forceDelete(jmxDirFile);
                }
            } catch (IllegalArgumentException | FileNotFoundException e) {
                //doNothing
            } catch (IOException e) {
                throw new RRException("删除jmx文件夹异常失败", e);
            }
        });

        stressTestFileDao.deleteBatch(fileIds);
    }

    /**
     * JmeterApi调用无法使用到分布式的特性，并且相比脚本本身驱动少了很多校验及参数输入的部分，
     * 少了结果文件的类型及位置，少了分布式相关内容，是非常弱的JmeterApi调用。
     * 本身Api调用就会加载Jmeter所依赖的lib文件，会增加系统体积。
     * 后续Api调用要复写JMeter的相关代码（Mavan中引用核心的Jmeter包）。
     * 同时和当前系统使用同一个classLoader，会执行在同一个进程中。
     * 另开一个进程直接调用Jmeter的脚本来实现，节省空间及跨平台。
     * <p>
     * 没有事务，不允许回滚
     * 因为是遍历执行，每一个执行可以是一个事务。
     * <p>
     * 接口是支持批量运行的，但是强烈不建议这样做。
     */
    @Override
    public void run(Long[] fileIds) {
        Arrays.asList(fileIds).stream().forEach(fileId -> {
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

            //保存文件的执行状态，用于前台提示及后端查看排序
            stressTestFile.setStatus(StressTestUtils.RUNNING);
            update(stressTestFile);
            if (stressTestReports != null) {
                stressTestReportsDao.save(stressTestReports);
            }

            if (stressTestUtils.isUseJmeterScript()) {
                excuteJmeterRunByScript(stressTestFile, stressTestReports, map);
            } else {
                excuteJmeterRunLocal(stressTestFile, stressTestReports, map);
            }
        });
    }

    /**
     * 执行Jmeter的脚本文件，采用Apache的commons-exec来执行。
     */
    @Transactional
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

        cmdLine.addArgument("-l");
        cmdLine.addArgument("${csvFile}");

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
    @Transactional
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

        String jmeterHomeBin = stressTestUtils.getJmeterHomeBin();
        JMeterUtils.loadJMeterProperties(jmeterHomeBin + File.separator + "jmeter.properties");
        JMeterUtils.setJMeterHome(stressTestUtils.getJmeterHome());
        JMeterUtils.initLocale();
        FileServer.getFileServer().setBaseForScript(jmxFile);

        try {
            HashTree jmxTree = SaveService.loadTree(jmxFile);
            JMeter.convertSubTree(jmxTree);

            // 使用自定义的Collector，用于前端绘图的数据收集和日志收集等。
            JmeterResultCollector resultCollector = new JmeterResultCollector(stressTestFile);
            resultCollector.setFilename(csvFile.getPath());

            String slaveStr = getSlaveIPPort();
            if (StringUtils.isNotEmpty(slaveStr)) {//分布式的方式启动
                java.util.StringTokenizer st = new java.util.StringTokenizer(slaveStr, ",");//$NON-NLS-1$
                List<String> hosts = new LinkedList<>();
                while (st.hasMoreElements()) {
                    hosts.add((String) st.nextElement());
                }


                Summariser summariser = new Summariser("JmeterTest");

                jmxTree.add(jmxTree.getArray()[0], summariser);

                DistributedRunner distributedRunner=new DistributedRunner();
                distributedRunner.setStdout(System.out); // NOSONAR
                distributedRunner.setStdErr(System.err); // NOSONAR
                distributedRunner.init(hosts, jmxTree);
                engines.addAll(distributedRunner.getEngines());
                distributedRunner.start();
            } else {//本机运行

                // 都会添加收集观察监听程序。
                // 具体情况的区分在其程序内做分别，原因是情况较多，父子类的实现不现实。
                jmxTree.add(jmxTree.getArray()[0], resultCollector);

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
        }
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
                    StressTestFileEntity stressTestFile = jmeterRunEntity.getStressTestFile();
                    StressTestReportsEntity stressTestReports = jmeterRunEntity.getStressTestReports();

                    // 只处理了成功的情况，失败的情况当前捕获不到。
                    stressTestFile.setStatus(StressTestUtils.RUN_SUCCESS);
                    if (stressTestReports != null && stressTestReports.getFile().exists()) {
                        stressTestReports.setFileSize(FileUtils.sizeOf(stressTestReports.getFile()));
                    }
                    update(stressTestFile, stressTestReports);

                    jmeterRunEntity.stop();
                });
            }

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

    public String getSlaveIPPort() {
        Map query = new HashMap<>();
        query.put("status", StressTestUtils.ENABLE);
        List<StressTestSlaveEntity> stressTestSlaveList = stressTestSlaveDao.queryList(query);

        StringBuilder sb = new StringBuilder();
        stressTestSlaveList.forEach(slave -> {
            if (sb.length() != 0) {
                sb.append(",");
            }
            sb.append(slave.getIp()).append(":").append(slave.getPort());
        });

        return sb.toString();
    }


    public static void main(String[] args) {
    }
}
