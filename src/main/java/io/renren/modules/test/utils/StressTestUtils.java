package io.renren.modules.test.utils;

import io.renren.common.exception.RRException;
import io.renren.common.utils.SpringContextUtils;
import io.renren.modules.sys.service.SysConfigService;
import io.renren.modules.test.jmeter.JmeterRunEntity;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.SamplingStatCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.renren.common.utils.ConfigConstant.OS_NAME_LC;

/**
 * 性能测试的工具类，同时用于读取配置文件。
 * 也可以将性能测试参数配置到系统参数配置中去。
 */
//@ConfigurationProperties(prefix = "test.stress")
@Component
public class StressTestUtils {

    Logger logger = LoggerFactory.getLogger(getClass());

    private static SysConfigService sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    public static String xslFilePath = "classpath:config/jmeter.results.zyanycall.xsl";

    //0：初始状态  1：正在运行  2：成功执行  3：运行出现异常
    public static final Integer INITIAL = 0;
    public static final Integer RUNNING = 1;
    public static final Integer RUN_SUCCESS = 2;
    public static final Integer RUN_ERROR = 3;

    /**
     * 是否需要测试报告的状态标识
     */
    //0：保存测试报告原始文件  1：不需要测试报告
    public static final Integer NEED_REPORT = 0;
    public static final Integer NO_NEED_REPORT = 1;

    /**
     * 是否需要前端Chart监控的状态标识
     */
    //0：需要前端监控  1：不需要前端监控
    public static final Integer NEED_WEB_CHART = 0;
    public static final Integer NO_NEED_WEB_CHART = 1;

    /**
     * 是否开启调试的状态标识
     */
    //0：默认关闭调试  1：开启调试
    public static final Integer NO_NEED_DEBUG = 0;
    public static final Integer NEED_DEBUG = 1;

    //0：禁用  1：启用
    public static final Integer DISABLE = 0;
    public static final Integer ENABLE = 1;

    /**
     * 针对每一个fileId，存储一份
     * 用于存储每一个用例的计算结果集合。
     */
    public static Map<Long, Map<String, SamplingStatCalculator>> samplingStatCalculator4File = new HashMap<>();

    /**
     * 针对每一个fileId，存储一份Jmeter的Engines，用于指定的用例启动和停止。
     * 如果不使用分布式节点，则Engines仅包含master主节点。
     * 默认是使用分布式的，则Engines会包含所有有效的分布式节点的Engine。
     */
    public static Map<Long, JmeterRunEntity> jMeterEntity4file = new HashMap<>();

    /**
     * 主进程Master内保存的一些状态，主要用于分布式的压测操作服务。
     */
    public static Map<String, String> jMeterStatuses = new HashMap<>();

//    private static String jmeterHome;

//    private String casePath;

//    private boolean useJmeterScript;

//    private boolean replaceFile = true;

    /**
     * Jmeter在Master节点的绝对路径
     */
    public final static String MASTER_JMETER_HOME_KEY = "MASTER_JMETER_HOME_KEY";

    /**
     * Jmeter在Master节点存储用例信息的绝对路径
     * 存放用例的总目录，里面会细分文件存放用例及用例文件
     * Jmeter节点机需要在/etc/bashrc中配置JAVA_HOME，同时source /etc/bashrc生效
     */
    public final static String MASTER_JMETER_CASES_HOME_KEY = "MASTER_JMETER_CASES_HOME_KEY";

    /**
     * 如果配置了Jmeter脚本启动，则额外开启Jmeter进程运行测试用例脚本及分布式程序。
     * 分布式程序可以取消ssl校验。
     * 同时仅支持Jmeter+InfluxDB+Grafana的实时监控。
     * 如果没有配置Jmeter脚本启动，则使用web本身自带的Jmeter功能。
     * 支持自带的ECharts实时监控。
     * 默认是false，即使用web程序进程来启动Jmeter-master程序。
     */
    public final static String MASTER_JMETER_USE_SCRIPT_KEY = "MASTER_JMETER_USE_SCRIPT_KEY";

    /**
     * 如果配置了本地生成测试报告（不包括调试报告），则使用web程序进程生成测试报告。
     * 默认是true，即配置为本地web程序进程，好处是可以多线程并发生成测试报告。
     * 对应的，如果为false则使用Jmeter_home的脚本生成测试报告，无法同时生成多个测试报告。
     */
    public final static String MASTER_JMETER_GENERATE_REPORT_KEY = "MASTER_JMETER_GENERATE_REPORT_KEY";

    /**
     * 上传文件时，遇到同名文件是替换还是报错，默认是替换为true
     */
    public final static String MASTER_JMETER_REPLACE_FILE_KEY = "MASTER_JMETER_REPLACE_FILE_KEY";

    public static String getJmeterHome() {
        return sysConfigService.getValue(MASTER_JMETER_HOME_KEY);
    }

    public String getCasePath() {
        return sysConfigService.getValue(MASTER_JMETER_CASES_HOME_KEY);
    }

    public boolean isUseJmeterScript() {
        return Boolean.valueOf(sysConfigService.getValue(MASTER_JMETER_USE_SCRIPT_KEY));
    }

    public boolean isReplaceFile() {
        return Boolean.valueOf(sysConfigService.getValue(MASTER_JMETER_REPLACE_FILE_KEY));
    }

    public boolean isMasterGenerateReport() {
        return Boolean.valueOf(sysConfigService.getValue(MASTER_JMETER_GENERATE_REPORT_KEY));
    }

    public static String getSuffix4() {
        String currentTimeStr = System.currentTimeMillis() + "";
        return currentTimeStr.substring(currentTimeStr.length() - 4);
    }

    /**
     * 获取Jmeter的bin目录
     */
    public String getJmeterHomeBin() {
        return getJmeterHome() + File.separator + "bin";
    }

    /**
     * 根据操作系统信息获取可以执行的jmeter主程序
     */
    public String getJmeterExc() {
        String jmeterExc = "jmeter";
        if (OS_NAME_LC.startsWith("windows")) {
            jmeterExc = "jmeter.bat";
        }
        return jmeterExc;
    }

    /**
     * 根据操作系统信息获取可以停止的jmeter主程序
     */
    public String getJmeterStopExc() {
        String jmeterExc = "shutdown.sh";
        if (OS_NAME_LC.startsWith("windows")) {
            jmeterExc = "shutdown.cmd";
        }
        return jmeterExc;
    }

    /**
     * 为前台的排序和数据之间做适配
     */
    public static Map<String, Object> filterParms(Map<String, Object> params) {
        if (params.containsKey("sidx") && params.get("sidx") != null) {
            String sidxValue = params.get("sidx").toString();

            if ("caseid".equalsIgnoreCase(sidxValue)) {
                params.put("sidx", "case_id");
            } else if ("addTime".equalsIgnoreCase(sidxValue)) {
                params.put("sidx", "add_time");
            } else if ("updateTime".equalsIgnoreCase(sidxValue)) {
                params.put("sidx", "update_time");
            } else if ("fileId".equalsIgnoreCase(sidxValue)) {
                params.put("sidx", "file_id");
            } else if ("reportId".equalsIgnoreCase(sidxValue)) {
                params.put("sidx", "report_id");
            } else if ("slaveId".equalsIgnoreCase(sidxValue)) {
                params.put("sidx", "slave_id");
            } else if ("fileSize".equalsIgnoreCase(sidxValue)) {
                params.put("sidx", "file_size");
            }
        }
        return params;
    }

    /**
     * 获取上传文件的md5
     */
    public String getMd5(MultipartFile file) throws IOException {
        return DigestUtils.md5Hex(file.getBytes());
    }

    /**
     * 获取文件的MD5值，远程节点机也是通过MD5值来判断文件是否重复及存在，所以就不使用其他算法了。
     */
    public String getMd5ByFile(String filePath) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        return DigestUtils.md5Hex(IOUtils.toByteArray(fis));
    }

    /**
     * 保存文件
     */
    public void saveFile(MultipartFile multipartFile, String filePath) {
        try {
            File file = new File(filePath);
            FileUtils.forceMkdirParent(file);
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new RRException("保存文件异常失败", e);
        }
    }

    /**
     * 判断当前是否存在正在执行的脚本
     */
    public static boolean checkExistRunningScript() {
        for (JmeterRunEntity jmeterRunEntity : jMeterEntity4file.values()) {
            if (jmeterRunEntity.getRunStatus().equals(RUNNING)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 设置Jmeter运行环境相关的配置，如配置文件的加载，当地语言环境等。
     */
    public void setJmeterProperties() {
        String jmeterHomeBin = getJmeterHomeBin();
        JMeterUtils.loadJMeterProperties(jmeterHomeBin + File.separator + "jmeter.properties");
        JMeterUtils.setJMeterHome(getJmeterHome());
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
     * 为调试模式动态设置Jmeter的结果文件格式，让jtl包含必要的调试信息。
     * 这些信息会显著影响压力机性能，所以仅供调试使用。
     * 同时这些配置仅对当前进程即master节点生效。
     */
    public void setJmeterOutputFormat() {
        Properties jmeterProps = JMeterUtils.getJMeterProperties();
        jmeterProps.put("jmeter.save.saveservice.label", "true");
        jmeterProps.put("jmeter.save.saveservice.response_data", "true");
        jmeterProps.put("jmeter.save.saveservice.response_data.on_error", "true");
        jmeterProps.put("jmeter.save.saveservice.response_message", "true");
        jmeterProps.put("jmeter.save.saveservice.successful", "true");
        jmeterProps.put("jmeter.save.saveservice.thread_name", "true");
        jmeterProps.put("jmeter.save.saveservice.time", "true");
        jmeterProps.put("jmeter.save.saveservice.subresults", "true");
        jmeterProps.put("jmeter.save.saveservice.assertions", "true");
        jmeterProps.put("jmeter.save.saveservice.latency", "true");
        jmeterProps.put("jmeter.save.saveservice.connect_time", "true");
        jmeterProps.put("jmeter.save.saveservice.samplerData", "true");
        jmeterProps.put("jmeter.save.saveservice.responseHeaders", "true");
        jmeterProps.put("jmeter.save.saveservice.requestHeaders", "true");
        jmeterProps.put("jmeter.save.saveservice.encoding", "true");
        jmeterProps.put("jmeter.save.saveservice.bytes", "true");
        jmeterProps.put("jmeter.save.saveservice.url", "true");
        jmeterProps.put("jmeter.save.saveservice.filename", "true");
        jmeterProps.put("jmeter.save.saveservice.hostname", "true");
        jmeterProps.put("jmeter.save.saveservice.thread_counts", "true");
        jmeterProps.put("jmeter.save.saveservice.sample_count", "true");
        jmeterProps.put("jmeter.save.saveservice.idle_time", "true");
    }

    /**
     * 为测试报告和调试报告提供的删除jmx的生成目录方法。
     * 如果删除的测试报告是测试脚本唯一的测试报告，则将目录也一并删除。
     */
    public void deleteJmxDir(String reportPath) {
        try {
            String jmxDir = reportPath.substring(0, reportPath.lastIndexOf(File.separator));
            File jmxDirFile = new File(jmxDir);
            if (FileUtils.sizeOf(jmxDirFile) == 0L) {
                FileUtils.forceDelete(jmxDirFile);
            }
        } catch (FileNotFoundException | IllegalArgumentException e) {
            logger.error("要删除的测试报告上级文件夹找不到(删除成功)  " + e.getMessage());
        } catch (IOException e) {
            throw new RRException("删除测试报告上级文件夹异常失败", e);
        }
    }
}