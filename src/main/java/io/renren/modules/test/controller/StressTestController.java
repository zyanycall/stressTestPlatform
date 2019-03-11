package io.renren.modules.test.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.DateUtils;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.test.entity.StressTestEntity;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.service.StressTestFileService;
import io.renren.modules.test.service.StressTestService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.File;
import java.util.*;

/**
 * 性能测试
 */
@RestController
@RequestMapping("/test/stress")
public class StressTestController {
    @Autowired
    private StressTestService stressTestService;

    @Autowired
    private StressTestFileService stressTestFileService;

    @Autowired
    private StressTestUtils stressTestUtils;

    /**
     * 性能测试用例列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("test:stress:list")
    public R list(@RequestParam Map<String, Object> params) {
        //查询列表数据
        Query query = new Query(StressTestUtils.filterParms(params));
        List<StressTestEntity> stressTestList = stressTestService.queryList(query);
        int total = stressTestService.queryTotal(query);

        PageUtils pageUtil = new PageUtils(stressTestList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 性能测试用例信息
     */
    @RequestMapping("/info/{caseId}")
    @RequiresPermissions("test:stress:info")
    public R info(@PathVariable("caseId") Long caseId) {
        StressTestEntity stressCase = stressTestService.queryObject(caseId);

        return R.ok().put("stressCase", stressCase);
    }


    /**
     * 上传文件
     * fileInput组件无论是同步上传还是异步上传，
     * 都是一个线程处理一个文件，所以接收的文件类型为 MultipartFile, 用MultipartFile[] 也可以
     * 但接收到的也仅是一个文件，
     * List<MultipartFile>则不可以。
     */
    @RequestMapping("/upload")
    @RequiresPermissions("test:stress:upload")
    public R upload(@RequestParam("files") MultipartFile multipartFile, MultipartHttpServletRequest request) {

        if (multipartFile.isEmpty()) {
            // 为了前端fileinput组件提示使用。
            return R.ok().put("error","上传文件不能为空");
//            throw new RRException("上传文件不能为空");
        }

        String originName = multipartFile.getOriginalFilename();
        //用例文件名可以是汉字毕竟是唯一标识用例内容的.
        //用例的参数化文件不允许包含汉字,避免Linux系统读取文件报错.
        String suffix = originName.substring(originName.lastIndexOf("."));
        if (!".jmx".equalsIgnoreCase(suffix) && originName.length() != originName.getBytes().length) {
            return R.ok().put("error","非脚本文件名不能包含汉字");
//            throw new RRException("非脚本文件名不能包含汉字");
        }

        String caseId = request.getParameter("caseIds");
        //允许文件名不同但是文件内容相同,因为不同的文件名对应不同的用例.
        StressTestEntity stressCase = stressTestService.queryObject(Long.valueOf(caseId));
        //主节点master的用于保存Jmeter用例及文件的地址
        String casePath = stressTestUtils.getCasePath();

        Map<String, Object> query = new HashMap<String, Object>();
        query.put("originName", originName);
        // fileList中最多有一条记录
        List<StressTestFileEntity> fileList = stressTestFileService.queryList(query);
        //数据库中已经存在同名文件
        if (!fileList.isEmpty()) {
            // 不允许上传同名文件
            if (!stressTestUtils.isReplaceFile()) {
                return R.ok().put("error","系统中已经存在此文件记录！不允许上传同名文件！");
//                throw new RRException("系统中已经存在此文件记录！不允许上传同名文件！");
            } else {// 允许上传同名文件方式是覆盖。
                for (StressTestFileEntity stressCaseFile : fileList) {
                    // 如果是不同用例，但是要上传同名文件，是不允许的，这是数据库的唯一索引要求的。
                    if (Long.valueOf(caseId) != stressCaseFile.getCaseId()) {
                        return R.ok().put("error","其他用例已经包含此同名文件！");
//                        throw new RRException("其他用例已经包含此同名文件！");
                    }
                    // 目的是从名称上严格区分脚本。而同名脚本不同项目模块甚至标签
                    String filePath = casePath + File.separator + stressCaseFile.getFileName();
                    stressTestFileService.save(multipartFile, filePath, stressCase, stressCaseFile);
                }
            }
        } else {// 新上传文件
            StressTestFileEntity stressCaseFile = new StressTestFileEntity();
            stressCaseFile.setOriginName(originName);

            //主节点master文件夹名称
            //主节点master会根据stressCase的添加时间及随机数生成唯一的文件夹,用来保存用例文件及参数化文件.
            //从节点slave会默认使用$JMETER_HOME/bin/stressTest 来存储参数化文件
            //master的文件分开放(web页面操作无感知),slave的参数化文件统一放.
            Date caseAddTime = stressCase.getAddTime();
            String caseAddTimeStr = DateUtils.format(caseAddTime, DateUtils.DATE_TIME_PATTERN_4DIR);
            String caseFilePath;
            if (StringUtils.isEmpty(stressCase.getCaseDir())) {
                //random使用时间种子的随机数,避免了轻度并发造成文件夹重名.
                caseFilePath = caseAddTimeStr + new Random(System.nanoTime()).nextInt(1000);
                stressCase.setCaseDir(caseFilePath);
            } else {
                caseFilePath = stressCase.getCaseDir();
            }

            String filePath;
            if (".jmx".equalsIgnoreCase(suffix)) {
                String jmxRealName = "case" + caseAddTimeStr +
                        new Random(System.nanoTime()).nextInt(1000) + suffix;
                stressCaseFile.setFileName(caseFilePath + File.separator + jmxRealName);
                filePath = casePath + File.separator + caseFilePath + File.separator + jmxRealName;
            } else {
                stressCaseFile.setFileName(caseFilePath + File.separator + originName);
                filePath = casePath + File.separator + caseFilePath + File.separator + originName;
            }

            //保存文件信息
            stressCaseFile.setCaseId(Long.valueOf(caseId));
            stressTestFileService.save(multipartFile, filePath, stressCase, stressCaseFile);
        }

        return R.ok();
    }


    /**
     * 保存性能测试用例
     */
    @SysLog("保存性能测试用例")
    @RequestMapping("/save")
    @RequiresPermissions("test:stress:save")
    public R save(@RequestBody StressTestEntity stressTestCase) {
        ValidatorUtils.validateEntity(stressTestCase);
        // 生成用例时即生成用例的文件夹名，上传附件时才会将此名称落地成为文件夹。
        if (StringUtils.isEmpty(stressTestCase.getCaseDir())) {
            Date caseAddTime = new Date();
            String caseAddTimeStr = DateUtils.format(caseAddTime, DateUtils.DATE_TIME_PATTERN_4DIR);
            //random使用时间种子的随机数,避免了轻度并发造成文件夹重名.
            String caseFilePath = caseAddTimeStr + new Random(System.nanoTime()).nextInt(1000);
            stressTestCase.setCaseDir(caseFilePath);
        }

        stressTestService.save(stressTestCase);

        return R.ok();
    }

    /**
     * 修改性能测试用例
     */
    @SysLog("修改性能测试用例")
    @RequestMapping("/update")
    @RequiresPermissions("test:stress:update")
    public R update(@RequestBody StressTestEntity stressTestCase) {
        ValidatorUtils.validateEntity(stressTestCase);

        stressTestService.update(stressTestCase);

        return R.ok();
    }

    /**
     * 删除性能测试用例
     */
    @SysLog("删除性能测试用例")
    @RequestMapping("/delete")
    @RequiresPermissions("test:stress:delete")
    public R delete(@RequestBody Long[] caseIds) {
        // 先删除其下的脚本文件。
        for (Long caseId : caseIds) {
            ArrayList fileIdList = new ArrayList();
            List<StressTestFileEntity> fileList = stressTestFileService.queryList(caseId);
            for (StressTestFileEntity stressTestFile : fileList) {
                fileIdList.add(stressTestFile.getFileId());
            }
            if (!fileIdList.isEmpty()) {
                stressTestFileService.deleteBatch(fileIdList.toArray());
            }
        }
        // 后删除用例
        if (caseIds.length > 0) {
            stressTestService.deleteBatch(caseIds);
        }
        return R.ok();
    }
}
