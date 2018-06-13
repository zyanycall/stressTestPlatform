package io.renren.modules.test.utils;

import io.renren.modules.test.jmeter.JmeterRunEntity;
import org.apache.jmeter.visualizers.SamplingStatCalculator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static io.renren.common.utils.ConfigConstant.OS_NAME_LC;

/**
 * 性能测试的工具类，用于读取配置文件。
 */
@ConfigurationProperties(prefix = "test.stress")
@Component
public class StressTestUtils {

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


    private String jmeterHome;

    private String casePath;

    private boolean useJmeterScript;

    public String getJmeterHome() {
        return jmeterHome;
    }

    public void setJmeterHome(String jmeterHome) {
        this.jmeterHome = jmeterHome;
    }

    public String getCasePath() {
        return casePath;
    }

    public void setCasePath(String casePath) {
        this.casePath = casePath;
    }

    public boolean isUseJmeterScript() {
        return useJmeterScript;
    }

    public void setUseJmeterScript(boolean useJmeterScript) {
        this.useJmeterScript = useJmeterScript;
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
}
