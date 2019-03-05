package io.renren.modules.test.service.impl;

import io.renren.common.exception.RRException;
import io.renren.modules.test.dao.StressTestReportsDao;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.handler.ReportCreateResultHandler;
import io.renren.modules.test.jmeter.report.LocalReportGenerator;
import io.renren.modules.test.service.StressTestReportsService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.jmeter.JMeter;
import org.apache.jmeter.report.config.ConfigurationException;
import org.apache.jmeter.report.dashboard.GenerationException;
import org.apache.jmeter.report.dashboard.ReportGenerator;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service("stressTestReportsService")
public class StressTestReportsServiceImpl implements StressTestReportsService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private StressTestReportsDao stressTestReportsDao;

    @Autowired
    private StressTestUtils stressTestUtils;

    @Override
    public StressTestReportsEntity queryObject(Long reportId) {
        return stressTestReportsDao.queryObject(reportId);
    }

    @Override
    public List<StressTestReportsEntity> queryList(Map<String, Object> map) {
        return stressTestReportsDao.queryList(map);
    }

    @Override
    public int queryTotal(Map<String, Object> map) {
        return stressTestReportsDao.queryTotal(map);
    }

    @Override
    public void save(StressTestReportsEntity stressCaseReports) {
        stressTestReportsDao.save(stressCaseReports);
    }

    @Override
    public void update(StressTestReportsEntity stressCaseReports) {
        stressTestReportsDao.update(stressCaseReports);
    }

    @Override
    @Transactional
    public void deleteBatch(Long[] reportIds) {
        Arrays.asList(reportIds).stream().forEach(reportId -> {
            StressTestReportsEntity stressTestReport = queryObject(reportId);
            String casePath = stressTestUtils.getCasePath();
            String reportName = stressTestReport.getReportName();
            //csv结果文件路径
            String csvPath = casePath + File.separator + reportName;
            //测试报告文件目录
            String reportPath = csvPath.substring(0, csvPath.lastIndexOf("."));
            try {
                FileUtils.forceDelete(new File(reportPath));
            } catch (FileNotFoundException e) {
                logger.error("要删除的测试报告文件夹找不到(删除成功)  " + e.getMessage());
            } catch (IOException e) {
                throw new RRException("删除测试报告文件夹异常失败", e);
            }
            deleteReportCSV(stressTestReport);
            deleteReportZip(stressTestReport);
            stressTestUtils.deleteJmxDir(reportPath);
        });
        stressTestReportsDao.deleteBatch(reportIds);
    }

    @Override
    public void deleteBatchCsv(Long[] reportIds) {
        Arrays.asList(reportIds).stream().forEach(reportId -> {
            StressTestReportsEntity stressTestReport = queryObject(reportId);
            deleteReportCSV(stressTestReport);

            // 更新数据库，目的是不允许生成测试报告
            // 删除了CSV文件，同时报告没有生成过，那这个压测其实已经废了。
            stressTestReport.setFileSize(0L);
            update(stressTestReport);
        });
    }

    @Override
    public void deleteReportCSV(StressTestReportsEntity stressCaseReports) {
        String casePath = stressTestUtils.getCasePath();
        String reportName = stressCaseReports.getReportName();
        //csv结果文件路径
        String csvPath = casePath + File.separator + reportName;

        // 为了FileNotFoundException，找不到说明已经删除
        try {
            FileUtils.forceDelete(new File(csvPath));
        } catch (FileNotFoundException e) {
            logger.error("要删除的测试报告来源文件找不到(删除成功)  " + e.getMessage());
        } catch (IOException e) {
            throw new RRException("删除测试报告来源文件异常失败", e);
        }
    }

    public void deleteReportZip(StressTestReportsEntity stressCaseReports) {
        String casePath = stressTestUtils.getCasePath();
        String reportName = stressCaseReports.getReportName();
        //csv结果文件路径
        String csvPath = casePath + File.separator + reportName;
        //测试报告文件目录
        String reportPathDir = csvPath.substring(0, csvPath.lastIndexOf("."));
        //zip文件名
        String reportZipPath = reportPathDir + ".zip";

        // 为了FileNotFoundException，找不到说明已经删除
        try {
            FileUtils.forceDelete(new File(reportZipPath));
        } catch (FileNotFoundException e) {
            logger.error("要删除的测试报告zip文件找不到(删除成功)  " + e.getMessage());
        } catch (IOException e) {
            throw new RRException("删除测试报告zip文件异常失败", e);
        }
    }

    /**
     * 获取zip文件
     */
    @Override
    public FileSystemResource getZipFile(StressTestReportsEntity reportsEntity) throws IOException {
        String casePath = stressTestUtils.getCasePath();
        String reportName = reportsEntity.getReportName();
        //csv结果文件路径
        String csvPath = casePath + File.separator + reportName;
        //测试报告文件目录
        String reportPathDir = csvPath.substring(0, csvPath.lastIndexOf("."));

        //如果测试报告文件目录不存在，直接打断
        File reportDir = new File(reportPathDir);
        if (!reportDir.exists()) {
            throw new RRException("请先生成测试报告！");
        }

        //zip文件名
        String reportZipPath = reportPathDir + ".zip";

        FileSystemResource zipFileResource = new FileSystemResource(reportZipPath);

        if (!zipFileResource.exists()) {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(reportZipPath), Charset.forName("GBK"));
            try {
                File zipFile = new File(reportPathDir);
                zip(out, zipFile, zipFile.getName());
            } finally {
                out.close();
            }
        }

        return zipFileResource;
    }

    /**
     * 递归打包文件夹/文件
     */
    public void zip(ZipOutputStream zOut, File file, String name) throws IOException {
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            name += "/";
            zOut.putNextEntry(new ZipEntry(name));
            for (File listFile : listFiles) {
                zip(zOut, listFile, name + listFile.getName());
            }
        } else {
            FileInputStream in = new FileInputStream(file);
            try {
                zOut.putNextEntry(new ZipEntry(name));
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zOut.write(buffer, 0, len);
                    zOut.flush();
                }
            } finally {
                in.close();
            }
        }
    }

    /**
     * 实际上Jmeter自身的生成测试报告无法批量进行，命令行会报错，跟这里是否异步执行没关系。
     * 默认是自己本进程内实现生成测试报告。
     */
    @Override
    @Transactional
    @Async("asyncServiceExecutor")
    public void createReport(Long[] reportIds) {
        for (Long reportId : reportIds) {
            StressTestReportsEntity stressTestReport = queryObject(reportId);
            createReport(stressTestReport);
        }
    }

    /**
     * 生成测试报告
     */
    @Override
    @Async("asyncServiceExecutor")
    public void createReport(StressTestReportsEntity stressTestReport) {
        String casePath = stressTestUtils.getCasePath();
        String reportName = stressTestReport.getReportName();

        //csv结果文件路径
        String csvPath = casePath + File.separator + reportName;
        //测试报告文件目录
        String reportPathDir = csvPath.substring(0, csvPath.lastIndexOf("."));

        //修复csv文件
        fixReportFile(csvPath);

        //设置开始执行命令生成报告
        stressTestReport.setStatus(StressTestUtils.RUNNING);
        update(stressTestReport);

        if (stressTestUtils.isMasterGenerateReport()) {
            generateReportLocal(stressTestReport, csvPath, reportPathDir);
        } else {
            generateReportByScript(stressTestReport, csvPath, reportPathDir);
        }
    }

    /**
     * 使用本进程多线程生成测试报告。
     */
    public void generateReportLocal(StressTestReportsEntity stressTestReport, String csvPath, String reportPathDir) {
        stressTestUtils.setJmeterProperties();
        LocalReportGenerator generator = null;
        try {
            generator = new LocalReportGenerator(csvPath, null);
            generator.generate(reportPathDir);
            stressTestReport.setStatus(StressTestUtils.RUN_SUCCESS);
            update(stressTestReport);
        } catch (GenerationException | ConfigurationException e) {
            //保存状态，执行出现异常
            stressTestReport.setStatus(StressTestUtils.RUN_ERROR);
            update(stressTestReport);
            throw new RRException("执行生成测试报告脚本异常！", e);
        }
    }

    /**
     * 使用Jmeter_home中的命令生成测试报告。
     */
    public void generateReportByScript(StressTestReportsEntity stressTestReport, String csvPath, String reportPathDir) {
        //开始执行命令行
        String jmeterHomeBin = stressTestUtils.getJmeterHomeBin();
        String jmeterExc = stressTestUtils.getJmeterExc();

        CommandLine cmdLine = new CommandLine(jmeterHomeBin + File.separator + jmeterExc);
        // 设置参数，-g 指定测试结果文件路径，仅用于生成测试报告
        cmdLine.addArgument("-g");
        Map map = new HashMap();
        map.put("csvFile", new File(csvPath));
        File reportDir = new File(reportPathDir);
        map.put("reportDir", reportDir);
        cmdLine.addArgument("${csvFile}");
        // -o 指定测试报告生成文件夹必须为空或者不存在，这里必须为不存在。
        cmdLine.addArgument("-o");
        cmdLine.addArgument("${reportDir}");
        // 指定需要执行的JMX脚本
        cmdLine.setSubstitutionMap(map);

        DefaultExecutor executor = new DefaultExecutor();
        //非阻塞方式运行脚本命令，不耽误前端的操作。
        //流操作在executor执行源码中已经关闭。
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        // 设置成功输入及错误输出，用于追查问题，打印日志。
        executor.setStreamHandler(streamHandler);

        try {
            // 自定义的钩子程序
            ReportCreateResultHandler resultHandler =
                    new ReportCreateResultHandler(stressTestReport,
                            this, outputStream, errorStream);
            // 执行脚本命令
            executor.execute(cmdLine, resultHandler);
        } catch (IOException e) {
            //保存状态，执行出现异常
            stressTestReport.setStatus(StressTestUtils.RUN_ERROR);
            update(stressTestReport);
            throw new RRException("执行生成测试报告脚本异常！", e);
        }
    }

    /**
     * 测试报告文件如果最后一行不完整，会报生成报告的错误。
     * 所以每次生成报告之前，如果不完整则删除最后一行记录，让测试报告生成没有这类文件不完整的错误。
     *
     * @param fileName csv 文件
     */
    public void fixReportFile(String fileName) {
        // 需要增加判断，如果是不完整的最后一行，属于脏数据，则删除这条数据。
        // 如果是完整的，则直接跳出不执行删除操作。
        try {
            RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
            if (raf.length() == 0L) {
                logger.error("测试报告原始csv文件为空，可以删除！");
                throw new RRException("测试报告原始文件找不到，请删除！");
            }
            // 获取倒数第一行的数据
            long pos = raf.length() - 1;
            pos = getPos(raf, pos);
            String lastLine = getLineStr(raf, raf.length(), pos);

            // 获取倒数第二行的数据
            long posSec = pos - 1;
            posSec = getPos(raf, posSec);
            String lastSecLine = getLineStr(raf, pos, posSec);

            // 是否删除最后一行，可以通过最后一行的数据结构是否和倒数第二行相同来判断。
            // csv文件都是英文逗号, 做分割
            String[] theLast = lastLine.split(",");
            String[] theLastSec = lastSecLine.split(",");
            if (theLast.length != theLastSec.length) {
                // 这会删除最后一行
                if (pos <= 0) {
                    raf.setLength(pos);
                } else {
                    raf.setLength(pos + 1);
                }
            }

            //关闭回收
            raf.close();
        } catch (FileNotFoundException e) {
            logger.error("测试报告原始csv文件找不到！", e);
            throw new RRException("测试报告原始文件找不到！");
        } catch (IOException e) {
            logger.error("测试报告原始文件修复时，IO错误！", e);
            throw new RRException("测试报告原始文件修复时出错！");
        }
    }

    /**
     * 获取文件中的某一段数据，这里是以\n做分割的。
     * 本身文件是从后向前做循环，所以多次调用并不会增加过多的性能时间损耗。
     * 默认是UTF-8的编码
     *
     * @param raf      原始文件
     * @param posEnd   要截取数据行的在文件中结束的位置
     * @param posStart 要截取数据行的开始的位置
     * @return 返回一行数据
     * @throws IOException
     */
    public String getLineStr(RandomAccessFile raf, long posEnd, long posStart) throws IOException {
        byte[] bytes = new byte[(int) (posEnd - posStart)];
        raf.read(bytes);
        return new String(bytes, Charset.forName("UTF-8"));
    }

    /**
     * 获取要截取数据行的开始的位置
     * 是从文本的最后开始向前找，找到换行符\n之后即停止，返回位置。
     * 该位置其他调用方法会遇到，会作为起点。
     * <p>
     * 同时由于是从文本的最后向前寻找，所以csv文本是脏数据，最后一行不包含\n，也会找到最后一行。
     */
    public long getPos(RandomAccessFile raf, long posStart) throws IOException {
        while (posStart > 0) {
            posStart--;
            raf.seek(posStart);
            if (raf.readByte() == '\n') {
                break;
            }
        }
        return posStart;
    }
}
