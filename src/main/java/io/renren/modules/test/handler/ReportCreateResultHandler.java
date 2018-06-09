package io.renren.modules.test.handler;

import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.service.StressTestReportsService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.commons.exec.ExecuteException;

import java.io.ByteArrayOutputStream;

/**
 * 测试报告生成的脚本钩子程序
 * Created by zyanycall@gmail.com on 17:09.
 */
public class ReportCreateResultHandler extends FileResultHandler {

    // report对象
    private StressTestReportsEntity stressTestReports;

    private StressTestReportsService stressTestReportsService;

    public ReportCreateResultHandler(StressTestReportsEntity stressTestReports,
                                     StressTestReportsService stressTestReportsService,
                                     ByteArrayOutputStream outputStream, ByteArrayOutputStream errorStream) {
        super(outputStream, errorStream);
        this.stressTestReports = stressTestReports;
        this.stressTestReportsService = stressTestReportsService;
    }

    /**
     * jmx脚本执行成功会走到这里
     * 重写父类方法，增加入库及日志打印
     * 生成报告文件后没有删除测试报告原始文件。
     * 原因是命令行虽然成功，但是结果是失败的，也会走到这里。
     */
    @Override
    public void onProcessComplete(final int exitValue) {
        stressTestReports.setStatus(StressTestUtils.RUN_SUCCESS);
        stressTestReportsService.update(stressTestReports);
        super.onProcessComplete(exitValue);
        //保存状态，执行完毕
    }

    /**
     * jmx脚本执行失败会走到这里
     * 重写父类方法，增加入库及日志打印
     */
    @Override
    public void onProcessFailed(final ExecuteException e) {
        stressTestReports.setStatus(StressTestUtils.RUN_ERROR);
        stressTestReportsService.update(stressTestReports);
        super.onProcessFailed(e);
    }
}