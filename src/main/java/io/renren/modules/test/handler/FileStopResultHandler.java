package io.renren.modules.test.handler;

import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.service.StressTestFileService;
import io.renren.modules.test.utils.StressTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Created by zyanycall@gmail.com on 16:49.
 */
public class FileStopResultHandler extends FileResultHandler {

    private StressTestFileService stressTestFileService;

    public FileStopResultHandler(StressTestFileService stressTestFileService,
                                 ByteArrayOutputStream outputStream, ByteArrayOutputStream errorStream) {
        super(outputStream, errorStream);
        this.stressTestFileService = stressTestFileService;
    }

    /**
     * jmx脚本执行成功会走到这里
     * 重写父类方法，增加入库及日志打印
     * 如果是正常的全部停止，会走到这里，及原脚本执行的正常Execute的onProcessComplete。
     * 那么至少会执行一次update及一次全量查询。
     * 这里的修改状态是为了系统重启后，仍然存在的未停止的running状态的脚本，来停止使用。
     * 虽然存在数据库的额外查询甚至update代价，对于全部停止功能来说，重复执行可以接受。
     */
    @Override
    public void onProcessComplete(final int exitValue) {
        List<StressTestFileEntity> list = stressTestFileService.queryList(new HashMap<>());
        list.forEach(fileEntity -> {
            if (StressTestUtils.RUNNING.equals(fileEntity.getStatus())) {
                fileEntity.setStatus(StressTestUtils.RUN_SUCCESS);
                stressTestFileService.update(fileEntity);
            }
        });

        super.onProcessComplete(exitValue);
    }
}
