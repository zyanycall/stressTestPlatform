package io.renren.modules.test.controller;

import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.R;
import io.renren.modules.test.entity.JMXFile;
import io.renren.modules.test.entity.JMXThreadGroup;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.entity.SynchronizeFile;
import io.renren.modules.test.service.StressJMXService;
import io.renren.modules.test.service.StressTestFileService;

/**
 * 压力测试用例文件
 *
 */
@RestController
@RequestMapping("/test/stressFile")
public class StressJMXController {
    @Autowired
    private StressTestFileService stressTestFileService;
    
    @Autowired
    private StressJMXService stressJMXService;
    
    
    /**
     * 查询jmx脚本文件内【线程组】组件的信息
     */
    @RequestMapping("/queryJMXFile/{fileId}")
    @RequiresPermissions("test:stress:fileUpdate")
    public R info(@PathVariable("fileId") Long fileId){
        StressTestFileEntity stressTestFile = stressTestFileService.queryObject(fileId);
        String jmxpath = stressTestFileService.getFilePath(stressTestFile);
        
        List<JMXThreadGroup> jmxThreadGroupList = stressJMXService.queryJMXFile(jmxpath);

        return R.ok().put("jmxFile", jmxThreadGroupList);
    }
    

    /**
     * 修改jmx脚本文件内【线程组】组件的信息
     */
    @SysLog("修改jmx脚本文件内【线程组】组件的信息")
    @RequestMapping("/updateJMXFile")
    @RequiresPermissions("test:stress:fileUpdate")
    public R updateJMX(@RequestBody JMXFile JMXFile) {
//      ValidatorUtils.validateEntity(jmxThreadGroupList);
    	
        StressTestFileEntity stressTestFile = stressTestFileService.queryObject(JMXFile.getFileId());
        String jmxpath = stressTestFileService.getFilePath(stressTestFile);
        stressJMXService.update(jmxpath, JMXFile.getJmxThreadGroup());

        return R.ok();
    }

 
    
}