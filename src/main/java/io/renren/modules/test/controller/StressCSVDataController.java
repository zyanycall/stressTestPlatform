package io.renren.modules.test.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.renren.common.utils.R;
import io.renren.modules.test.entity.StressTestFileEntity;
import io.renren.modules.test.service.StressTestFileService;

/**
 * 压力测试脚本文件
 *
 */
@RestController
@RequestMapping("/test/stressFile")
public class StressCSVDataController {
    @Autowired
    private StressTestFileService stressTestFileService;
    
    public static final Integer csvdataisture = 1;

    @RequestMapping("/querycsvdata")
    @RequiresPermissions("test:stress:list")
    public R querycsvdata(){
    	
        List<StressTestFileEntity> stressTestFile = stressTestFileService.querycsvdata(csvdataisture); 
        int total = stressTestFileService.querycsvdataTotal();
        
        //区分主文件和salve文件
        List<StressTestFileEntity> master = new ArrayList<>();
        List<StressTestFileEntity> slave = new ArrayList<>();        
        for (int i = 0; i < stressTestFile.size(); i++) {
        	if (stressTestFile.get(i).getMembershipfileid().longValue() == 0l) {
        		master.add(stressTestFile.get(i));
        	} else {
        		StressTestFileEntity aa = stressTestFile.get(i);
        		//填入参数化文件名
        		aa.setRealname(aa.getFileName().replaceAll(".+?stressTestCases/", ""));
        		slave.add(aa);
        	}
		}

        List<StressTestFileEntity> finalstressCSVDataList = new ArrayList<>();

        //循环master
        for (int j = 0; j < master.size(); j++) {
        	
        	//循环slave匹配master变为其子集
        	List<StressTestFileEntity> stressSalveCSVDataList = new ArrayList<>();
        	for (int k = 0; k < slave.size(); k++) {
				if (master.get(j).getFileId().longValue() == slave.get(k).getMembershipfileid().longValue()) {
					stressSalveCSVDataList.add(slave.get(k));
				}
			}
        	//slave子集塞入对象
        	master.get(j).setchildren(stressSalveCSVDataList);
        	//对象整体塞入List
        	finalstressCSVDataList.add(master.get(j));
		}
        
        return R.ok().put("CSVDataFile", finalstressCSVDataList);
    }
    

    /**
     * 删除参数化文件主文件及所有从文件
     * @param fileId
     * @return
     */
    @RequestMapping("/deleteMasterFile")
    @RequiresPermissions("test:stress:fileDelete")
    public R deleteMasterFile(@RequestBody Long fileId){
    	Long[] fileIds = {fileId};
        stressTestFileService.deleteBatch(fileIds);
        return R.ok();
    }

    /**
     * 单独删除slave服务器的参数化文件
     * @param fileId
     * @return
     */
    @RequestMapping("/deleteSlaveFile")
    @RequiresPermissions("test:stress:fileDelete")
    public R deleteSlaveFile(@RequestBody Long fileId){ 
    	
    	StressTestFileEntity stressTestFile = stressTestFileService.queryObject(fileId);
    	List<StressTestFileEntity> fileDeleteList = new ArrayList<>();
    	fileDeleteList.add(stressTestFile);
    	
        stressTestFileService.deleteSlaveFile(fileDeleteList);
        return R.ok();
    }
    
    
    /**
     * 修改slave中的参数化文件名
     * 目的：给各个slave分发的参数化文件可能不同，但需要保证文件名相同
     * @param params
     * @return
     */
    @RequestMapping("/updateSlaveFileName")
    @RequiresPermissions("test:stress:fileUpdate")
    public R updateSlaveFileName(@RequestBody Map<String,String> params){ 
    	
    	StressTestFileEntity stressTestFile = stressTestFileService.queryObject(Long.valueOf(params.get("fileId").toString()));

        stressTestFileService.updateSlaveFileName(stressTestFile, params.get("realname").toString());
        return R.ok();
    }
    
    
}