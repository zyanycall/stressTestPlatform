package io.renren.modules.test.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.exception.RRException;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.test.entity.StressTestReportsEntity;
import io.renren.modules.test.service.StressTestReportsService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 压力测试报告
 */
@RestController
@RequestMapping("/test/stressReports")
public class StressTestReportsController {
    @Autowired
    private StressTestReportsService stressTestReportsService;
    @Autowired
    private StressTestUtils stressTestUtils;

    /**
     * 测试报告列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("test:stress:reportsList")
    public R list(@RequestParam Map<String, Object> params) {
        //查询列表数据
        Query query = new Query(StressTestUtils.filterParms(params));
        List<StressTestReportsEntity> reportList = stressTestReportsService.queryList(query);
        int total = stressTestReportsService.queryTotal(query);

        PageUtils pageUtil = new PageUtils(reportList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 查询具体测试报告信息
     */
    @RequestMapping("/info/{reportId}")
    @RequiresPermissions("test:stress:reportInfo")
    public R info(@PathVariable("reportId") Long reportId) {
        StressTestReportsEntity reportsEntity = stressTestReportsService.queryObject(reportId);
        return R.ok().put("stressCaseReport", reportsEntity);
    }

    /**
     * 修改性能测试用例报告文件
     */
    @SysLog("修改性能测试用例报告文件")
    @RequestMapping("/update")
    @RequiresPermissions("test:stress:reportUpdate")
    public R update(@RequestBody StressTestReportsEntity stressCaseReport) {
        ValidatorUtils.validateEntity(stressCaseReport);
        stressTestReportsService.update(stressCaseReport);
        return R.ok();
    }

    /**
     * 删除指定测试报告及文件
     */
    @SysLog("删除性能测试报告")
    @RequestMapping("/delete")
    @RequiresPermissions("test:stress:reportDelete")
    public R delete(@RequestBody Long[] reportIds) {
        stressTestReportsService.deleteBatch(reportIds);
        return R.ok();
    }

    /**
     * 删除指定测试报告的测试结果文件，目的是避免占用空间太大
     */
    @SysLog("删除性能测试报告结果文件")
    @RequestMapping("/deleteCsv")
    @RequiresPermissions("test:stress:reportDeleteCsv")
    public R deleteCsv(@RequestBody Long[] reportIds) {
        stressTestReportsService.deleteBatchCsv(reportIds);
        return R.ok();
    }

    /**
     * 生成测试报告及文件
     */
    @SysLog("生成性能测试报告")
    @RequestMapping("/createReport")
    @RequiresPermissions("test:stress:reportCreate")
    public R createReport(@RequestBody Long[] reportIds) {
        for (Long reportId : reportIds) {
            StressTestReportsEntity stressTestReport = stressTestReportsService.queryObject(reportId);

            //首先判断，如果file_size为0或者空，说明没有结果文件，直接报错打断。
            if (stressTestReport.getFileSize() == 0L || stressTestReport.getFileSize() == null) {
                throw new RRException("找不到测试结果文件，无法生成测试报告！");
            }
            //如果测试报告文件目录已经存在，说明生成过测试报告，直接打断
            if (StressTestUtils.RUN_SUCCESS.equals(stressTestReport.getStatus())) {
                throw new RRException("已经存在测试报告不要重复创建！");
            }
            stressTestReportsService.createReport(stressTestReport);
        }
        return R.ok();
    }


    /**
     * 下载测试报告zip包
     */
    @SysLog("下载测试报告zip包")
    @RequestMapping("/downloadReport/{reportId}")
    @RequiresPermissions("test:stress:reportDownLoad")
    public ResponseEntity<InputStreamResource> downloadReport(@PathVariable("reportId") Long reportId) throws IOException {
        StressTestReportsEntity reportsEntity = stressTestReportsService.queryObject(reportId);
        FileSystemResource zipFile = stressTestReportsService.getZipFile(reportsEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache,no-store,must-revalidate");
        headers.add("Content-Disposition",
                "attachment;filename=" + reportsEntity.getOriginName() + ".zip");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.setContentType(MediaType.parseMediaType("application/octet-stream"));

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(zipFile.contentLength())
                .body(new InputStreamResource(zipFile.getInputStream()));
    }
}