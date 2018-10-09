package io.renren.modules.test.utils;

import io.renren.common.utils.SpringContextUtils;
import io.renren.modules.sys.service.SysConfigService;
import io.renren.modules.test.jmeter.JmeterRunEntity;
import org.apache.jmeter.visualizers.SamplingStatCalculator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.renren.common.utils.ConfigConstant.OS_NAME_LC;

/**
 * 性能测试的工具类，同时用于读取配置文件。
 * 也可以将性能测试参数配置到系统参数配置中去。
 */
@ConfigurationProperties(prefix = "test.stress")
@Component
public class StressTestUtils {

    private static SysConfigService sysConfigService;

    static {
        StressTestUtils.sysConfigService = (SysConfigService) SpringContextUtils.getBean("sysConfigService");
    }


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
     * 是否需要记录前端日志的状态标识
     */
    //0：不需要前端显示日志  1：前端仅显示错误日志
    //2：前端仅显示正确日志   3：前端正确错误日志都显示
    public static final Integer NO_NEED_WEB_LOG = 0;
    public static final Integer JUST_WEB_ERROR_LOG = 1;
    public static final Integer JUST_WEB_RIGHT_LOG = 2;
    public static final Integer ALL_WEB_LOG = 3;

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

    private static String jmeterHome;

    private String casePath;

    private boolean useJmeterScript;

    private boolean replaceFile = true;

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
     * 上传文件时，遇到同名文件是替换还是报错，默认是替换为true
     */
    public final static String MASTER_JMETER_REPLACE_FILE_KEY = "MASTER_JMETER_REPLACE_FILE_KEY";

    public static String getJmeterHome() {
        String value = sysConfigService.getValue(MASTER_JMETER_HOME_KEY);
        return value == null ? jmeterHome : value;
    }

    public void setJmeterHome(String jmeterHome) {
        this.jmeterHome = jmeterHome;
    }

    public String getCasePath() {
        String value = sysConfigService.getValue(MASTER_JMETER_CASES_HOME_KEY);
        return value == null ? casePath : value;
    }

    public void setCasePath(String casePath) {
        this.casePath = casePath;
    }

    public boolean isUseJmeterScript() {
        String value = sysConfigService.getValue(MASTER_JMETER_USE_SCRIPT_KEY);
        return value == null ? useJmeterScript : Boolean.valueOf(value);
    }

    public void setUseJmeterScript(boolean useJmeterScript) {
        this.useJmeterScript = useJmeterScript;
    }

    public boolean isReplaceFile() {
        String value = sysConfigService.getValue(MASTER_JMETER_REPLACE_FILE_KEY);
        return value == null ? replaceFile : Boolean.valueOf(value);
    }

    public void setReplaceFile(boolean replaceFile) {
        this.replaceFile = replaceFile;
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
}
