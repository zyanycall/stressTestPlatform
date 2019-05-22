package io.renren.modules.test.service;

import io.renren.modules.test.entity.StressTestReportsEntity;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 性能测试报告
 */
public interface StressTestReportsService {

    /**
     * 根据ID，查询文件
     */
    StressTestReportsEntity queryObject(Long reportId);

    /**
     * 查询文件列表
     */
    List<StressTestReportsEntity> queryList(Map<String, Object> map);

    /**
     * 查询总数
     */
    int queryTotal(Map<String, Object> map);

    /**
     * 保存性能测试用例文件
     */
    void save(StressTestReportsEntity stressCaseReports);

    /**
     * 更新性能测试用例信息
     */
    void update(StressTestReportsEntity stressCaseReports);

    /**
     * 批量删除
     */
    void deleteBatch(Long[] reportIds);

    /**
     * 批量删除测试报告的来源CSV文件
     */
    void deleteBatchCsv(Long[] reportIds);

    /**
     * 生成测试报告
     */
    void createReport(Long[] reportIds);

    /**
     * 生成测试报告
     */
    void createReport(StressTestReportsEntity reportsEntity);

    /**
     * 批量删除测试报告的来源CSV文件
     */
    void deleteReportCSV(StressTestReportsEntity stressCaseReports);


    FileSystemResource getZipFile(StressTestReportsEntity reportsEntity) throws IOException;

//    /**
//     * 批量更新性能测试用例信息
//     */
//    int updateBatch(Long[] caseIds, int status);

}
