package io.renren.modules.job.task;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.renren.modules.sys.entity.SysUserEntity;
import io.renren.modules.sys.service.SysUserService;
import io.renren.modules.test.service.impl.StressTestFileServiceImpl;

/**
 * 测试定时任务(演示Demo，可删除)
 * 
 * testTask为spring bean的名称
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2016年11月30日 下午1:34:24
 */
@Component("testTask")
public class TestTask {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private SysUserService sysUserService;
	
	private static StressTestFileServiceImpl stressTestFileServiceImpl;
	
    @Autowired
    public TestTask(StressTestFileServiceImpl stressTestFileServiceImpl) {
    	TestTask.stressTestFileServiceImpl = stressTestFileServiceImpl;
    }

	//定时任务只能接受一个参数；如果有多个参数，使用json数据即可
	public void test(String params){
		logger.info("我是带参数的test方法，正在被执行，参数为：" + params);
		
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		SysUserEntity user = sysUserService.queryObject(1L);
		System.out.println(ToStringBuilder.reflectionToString(user));
		
	}
	
	
	public void test2(){
		logger.info("我是不带参数的test2方法，正在被执行");
	}
	
	/**
	 * 定时执行测试脚本入口方法
	 * @param fileId
	 */
	public void scheduleJMX(String fileId){
		Long[] fileIds = {Long.valueOf(fileId)};
		stressTestFileServiceImpl.run(fileIds);
	}
	
}
