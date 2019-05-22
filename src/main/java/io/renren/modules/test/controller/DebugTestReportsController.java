package io.renren.modules.test.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.test.entity.DebugTestReportsEntity;
import io.renren.modules.test.service.DebugTestReportsService;
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
@RequestMapping("/test/debugReports")
public class DebugTestReportsController {
    @Autowired
    private DebugTestReportsService debugTestReportsService;

    /**
     * 测试报告列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("test:debug:reportsList")
    public R list(@RequestParam Map<String, Object> params) {
        //查询列表数据
        Query query = new Query(StressTestUtils.filterParms(params));
        List<DebugTestReportsEntity> reportList = debugTestReportsService.queryList(query);
        int total = debugTestReportsService.queryTotal(query);

        PageUtils pageUtil = new PageUtils(reportList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 查询具体测试报告信息
     */
    @RequestMapping("/info/{reportId}")
    @RequiresPermissions("test:debug:reportInfo")
    public R info(@PathVariable("reportId") Long reportId) {
        DebugTestReportsEntity reportsEntity = debugTestReportsService.queryObject(reportId);
        return R.ok().put("debugCaseReport", reportsEntity);
    }

    /**
     * 修改调试报告数据
     */
    @SysLog("修改调试测试报告数据")
    @RequestMapping("/update")
    @RequiresPermissions("test:debug:reportUpdate")
    public R update(@RequestBody DebugTestReportsEntity debugCaseReport) {
        ValidatorUtils.validateEntity(debugCaseReport);
        debugTestReportsService.update(debugCaseReport);
        return R.ok();
    }

    /**
     * 删除指定测试报告及文件
     */
    @SysLog("删除调试测试报告")
    @RequestMapping("/delete")
    @RequiresPermissions("test:debug:reportDelete")
    public R delete(@RequestBody Long[] reportIds) {
        debugTestReportsService.deleteBatch(reportIds);
        return R.ok();
    }

    /**
     * 删除指定测试报告的测试结果文件，目的是避免占用空间太大
     */
    @SysLog("删除调试报告结果文件")
    @RequestMapping("/deleteJtl")
    @RequiresPermissions("test:debug:reportDeleteJtl")
    public R deleteJtl(@RequestBody Long[] reportIds) {
        debugTestReportsService.deleteBatchJtl(reportIds);
        return R.ok();
    }

    /**
     * 生成测试报告及文件
     */
    @SysLog("生成调试测试报告")
    @RequestMapping("/createReport")
    @RequiresPermissions("test:debug:reportCreate")
    public R createReport(@RequestBody Long[] reportIds) {
        for (Long reportId : reportIds) {
            DebugTestReportsEntity debugTestReport = debugTestReportsService.queryObject(reportId);
            debugTestReport.setStatus(StressTestUtils.RUNNING);
            debugTestReportsService.update(debugTestReport);
            // 异步生成调试报告
            debugTestReportsService.createReport(reportId);
        }
        return R.ok();
    }


    /**
     * 下载测试报告
     */
    @SysLog("下载调试测试报告")
    @RequestMapping("/downloadReport/{reportId}")
    @RequiresPermissions("test:debug:reportDownLoad")
    public ResponseEntity<InputStreamResource> downloadReport(@PathVariable("reportId") Long reportId) throws IOException {
        DebugTestReportsEntity reportsEntity = debugTestReportsService.queryObject(reportId);
        FileSystemResource fileResource = debugTestReportsService.getReportFile(reportsEntity);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache,no-store,must-revalidate");
        headers.add("Content-Disposition",
                "attachment;filename=" + reportsEntity.getOriginName() + ".html");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.setContentType(MediaType.parseMediaType("application/octet-stream"));

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(fileResource.contentLength())
                .body(new InputStreamResource(fileResource.getInputStream()));
    }
}