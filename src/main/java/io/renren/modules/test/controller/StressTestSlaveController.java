package io.renren.modules.test.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.test.entity.StressTestSlaveEntity;
import io.renren.modules.test.service.StressTestSlaveService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分布式节点管理
 */
@RestController
@RequestMapping("/test/stressSlave")
public class StressTestSlaveController {
    @Autowired
    private StressTestSlaveService stressTestSlaveService;

    @Autowired
    private StressTestUtils stressTestUtils;

    /**
     * 分布式节点列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("test:stress:slaveList")
    public R list(@RequestParam Map<String, Object> params) {
        //查询列表数据
        Query query = new Query(params);
        List<StressTestSlaveEntity> stressTestList = stressTestSlaveService.queryList(query);
        int total = stressTestSlaveService.queryTotal(query);

        PageUtils pageUtil = new PageUtils(stressTestList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 性能测试用例信息
     */
    @RequestMapping("/info/{slaveId}")
    @RequiresPermissions("test:stress:slaveInfo")
    public R info(@PathVariable("slaveId") Long slaveId) {
        StressTestSlaveEntity stressTestSlave = stressTestSlaveService.queryObject(slaveId);

        return R.ok().put("stressTestSlave", stressTestSlave);
    }

    /**
     * 保存性能测试用例
     */
    @SysLog("保存性能测试用例")
    @RequestMapping("/save")
    @RequiresPermissions("test:stress:slaveSave")
    public R save(@RequestBody StressTestSlaveEntity stressTestSlave) {
        ValidatorUtils.validateEntity(stressTestSlave);

        stressTestSlaveService.save(stressTestSlave);

        return R.ok();
    }

    /**
     * 修改性能测试用例
     */
    @SysLog("修改性能测试用例")
    @RequestMapping("/update")
    @RequiresPermissions("test:stress:slaveUpdate")
    public R update(@RequestBody StressTestSlaveEntity stressTestSlave) {
        ValidatorUtils.validateEntity(stressTestSlave);

        stressTestSlaveService.update(stressTestSlave);

        return R.ok();
    }

    /**
     * 删除性能测试用例
     */
    @SysLog("删除性能测试用例")
    @RequestMapping("/delete")
    @RequiresPermissions("test:stress:slaveDelete")
    public R delete(@RequestBody Long[] slaveIds) {
        stressTestSlaveService.deleteBatch(slaveIds);

        return R.ok();
    }

}
