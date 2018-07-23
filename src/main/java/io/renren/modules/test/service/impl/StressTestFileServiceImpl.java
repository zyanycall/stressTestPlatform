package io.renren.modules.test.service.impl;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
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
import java.math.BigInteger;
import java.security.MessageDigest;
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

        String jmeterHomeBin = stressTestUtils.getJmeterHomeBin();
        JMeterUtils.loadJMeterProperties(jmeterHomeBin + File.separator + "jmeter.properties");
        JMeterUtils.setJMeterHome(stressTestUtils.getJmeterHome());
        JMeterUtils.initLocale();
        FileServer.getFileServer().setBaseForScript(jmxFile);

        try {
            HashTree jmxTree = SaveService.loadTree(jmxFile);
            JMeter.convertSubTree(jmxTree);

            String slaveStr = getSlaveIPPort();
            // slaveStr用来做脚本是否是分布式执行的判断，不入库。
            stressTestFile.setSlaveStr(slaveStr);
            // 都会添加收集观察监听程序。
            // 具体情况的区分在其程序内做分别，原因是情况较多，父子类的实现不现实。
            // 使用自定义的Collector，用于前端绘图的数据收集和日志收集等。
            JmeterResultCollector jmeterResultCollector = new JmeterResultCollector(stressTestFile);
            jmeterResultCollector.setFilename(csvFile.getPath());
            jmxTree.add(jmxTree.getArray()[0], jmeterResultCollector);

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

    @Override
    public JmeterStatEntity getJmeterStatEntity(Long fileId) {
        if (StringUtils.isNotEmpty(getSlaveIPPort())) {
            return new JmeterStatEntity(0L);
        }
        return new JmeterStatEntity(fileId);
    }

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
                    putFileToSlave(slave, ssh2Util, fileId);
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
    private void putFileToSlave(StressTestSlaveEntity slave, SSH2Utils ssh2Util, Long fileId) {
        StressTestFileEntity stressTestFile = queryObject(fileId);
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
        String fileNameUpload = stressTestFile.getOriginName();
        try {
            String MD5 = ssh2Util.runCommand("md5sum " + caseFileHome + File.separator +
                    fileNameUpload + "|cut -d ' ' -f1");
            if (fileSaveMD5.equals(MD5)) {//说明目标服务器已经存在相同文件不再重复上传
                return;
            }

            //上传文件
            ssh2Util.putFile(filePath, caseFileHome);
        } catch (JSchException e) {
            throw new RRException(stressTestFile.getOriginName() + "校验节点机文件MD5时失败！", e);
        } catch (IOException e) {
            throw new RRException(stressTestFile.getOriginName() + "IO传输失败！", e);
        } catch (SftpException e) {
            throw new RRException(stressTestFile.getOriginName() + "上传到节点机文件时失败！", e);
        }
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
     *
     */
    public String getMd5(MultipartFile file) throws IOException {
        return DigestUtils.md5Hex(file.getBytes());
    }

    /**
     * 拼装分布式节点，当前还没有遇到分布式节点非常多的情况。
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
    public boolean checkSlaveLocal(){
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
