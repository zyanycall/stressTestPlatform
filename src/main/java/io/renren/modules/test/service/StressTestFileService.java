package io.renren.modules.test.service;

import io.renren.modules.test.entity.StressTestEntity;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.StressTestReportsEntity;
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
     * 批量删除
     */
    void deleteBatch(Long[] fileIds);

    /**
     * 立即执行
     */
    void run(Long[] fileIds);

    /**
     * 停止运行
     */
    void stopAll();

    /**
     * 立即停止运行
     */
    void stopAllNow();


//    /**
//     * 批量更新性能测试用例信息
//     */
//    int updateBatch(Long[] caseIds, int status);


}
