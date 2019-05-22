package io.renren.modules.test.jmeter;

import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.testelement.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 复写TestPlan的方法。
 * 为了让脚本停止的时候根据需要关闭文件流，为有参数化的多脚本的同时启动进行服务。
 *
 * Created by zyanycall@gmail.com on 2018/11/5 16:14.
 */
public class JmeterTestPlan extends TestPlan {
    Logger logger = LoggerFactory.getLogger(getClass());

    // 为本地增加的脚本对象。
    private StressTestFileEntity stressTestFile;

    public JmeterTestPlan() {
        super();
    }

    public JmeterTestPlan(String name) {
        super.setName(name);
    }

    /**
     * 原有方法是执行的  closeFiles(); 这会导致整个环境的文件流都关闭，会造成问题。
     * 所以覆盖了这个方法，让其有选择的关闭files。
     *
     * {@inheritDoc}
     */
    @Override
    public void testEnded() {
        try {
            if (stressTestFile == null || !StressTestUtils.checkExistRunningScript()) {
                FileServer.getFileServer().closeFiles();
            } else {
                Long fileId = stressTestFile.getFileId();
                JmeterRunEntity jmeterRunEntity = StressTestUtils.jMeterEntity4file.get(fileId);
                for (String filename : jmeterRunEntity.getFileAliaList()) {
                    FileServer.getFileServer().closeFile(filename);
                }
            }
        } catch (IOException e) {
            logger.error("Problem closing files at end of test", e);
        }
    }

    public StressTestFileEntity getStressTestFile() {
        return stressTestFile;
    }

    public void setStressTestFile(StressTestFileEntity stressTestFile) {
        this.stressTestFile = stressTestFile;
    }
}
