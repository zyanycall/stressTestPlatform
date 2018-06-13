package io.renren.modules.test.handler;

import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.service.StressTestFileService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;

/**
 * 执行的钩子程序。
 * Created by zyanycall@gmail.com on 15:50.
 */
public class FileExecuteResultHandler extends FileResultHandler {

    // file对象
    private StressTestFileEntity stressTestFile;

    // report对象
    private StressTestReportsEntity stressTestReports;

    private StressTestFileService stressTestFileService;

    public FileExecuteResultHandler(StressTestFileEntity stressTestFile, StressTestReportsEntity stressTestReports,
                                    StressTestFileService stressTestFileService,
                                    ByteArrayOutputStream outputStream, ByteArrayOutputStream errorStream) {
        super(outputStream, errorStream);
        this.stressTestFile = stressTestFile;
        this.stressTestReports = stressTestReports;
        this.stressTestFileService = stressTestFileService;
    }

    /**
     * jmx脚本执行成功会走到这里
     * 重写父类方法，增加入库及日志打印
     */
    @Override
    public void onProcessComplete(final int exitValue) {
        stressTestFile.setStatus(StressTestUtils.RUN_SUCCESS);
        if (stressTestReports != null && stressTestReports.getFile().exists()) {
            stressTestReports.setFileSize(FileUtils.sizeOf(stressTestReports.getFile()));
        }
        stressTestFileService.update(stressTestFile, stressTestReports);
        super.onProcessComplete(exitValue);
        //保存状态，执行完毕
    }

    /**
     * jmx脚本执行失败会走到这里
     * 重写父类方法，增加入库及日志打印
     */
    @Override
    public void onProcessFailed(final ExecuteException e) {
        if (stressTestFile != null) {
            stressTestFile.setStatus(StressTestUtils.RUN_ERROR);
            stressTestFileService.update(stressTestFile);
        }
        super.onProcessFailed(e);
    }
}
