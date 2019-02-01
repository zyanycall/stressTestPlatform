package io.renren.modules.test.service;

import io.renren.modules.test.entity.StressTestEntity;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.jmeter.JmeterRunEntity;
import io.renren.modules.test.jmeter.JmeterStatEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 性能测试压测文件
 */
public interface StressTestFileService {

    /**
     * 根据ID，查询文件
     */
    StressTestFileEntity queryObject(Long fileId);

    /**
     * 查询文件列表
     */
    List<StressTestFileEntity> queryList(Map<String, Object> map);

    /**
     * 查询文件列表
     */
    List<StressTestFileEntity> queryList(Long caseId);

    /**
     * 查询总数
     */
    int queryTotal(Map<String, Object> map);

    /**
     * 保存性能测试用例文件
     */
    void save(StressTestFileEntity stressCaseFile);

    /**
     * 保存性能测试用例文件
     */
    void save(MultipartFile file, String filePath, StressTestEntity stressCase, StressTestFileEntity stressCaseFile);

    /**
     * 更新性能测试用例信息
     */
    void update(StressTestFileEntity stressTestFile);

    /**
     * 更新性能测试用例信息
     */
    void update(StressTestFileEntity stressTestFile, StressTestReportsEntity stressTestReports);

    /**
     * 更新性能测试用例信息
     */
    void update(MultipartFile file, String filePath, StressTestEntity stressCase, StressTestFileEntity stressCaseFile);

    /**
     * 批量更新性能测试用例状态
     */
    void updateStatusBatch(StressTestFileEntity stressTestFile);

    /**
     * 批量删除
     */
    void deleteBatch(Object[] fileIds);

    /**
     * 立即执行
     */
    String run(Long[] fileIds);

    /**
     * 立即停止
     */
    void stop(Long[] fileIds);

    /**
     * 停止运行
     */
    void stopAll();

    /**
     * 立即停止运行
     */
    void stopAllNow();

    /**
     * 获取轮询监控结果
     */
    JmeterStatEntity getJmeterStatEntity(Long fileId);

    /**
     * 同步参数化文件到节点机
     */
    void synchronizeFile(Long[] fileIds);

    /**
     * 获取文件路径，是文件的真实绝对路径
     */
    String getFilePath(StressTestFileEntity stressTestFile);

    /**
     * 相同进程内执行的脚本，可以使用这个方法停止
     */
    void stopLocal(Long fileId, JmeterRunEntity jmeterRunEntity);

}
