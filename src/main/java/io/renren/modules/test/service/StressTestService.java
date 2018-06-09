package io.renren.modules.test.service;

import io.renren.modules.test.entity.StressTestEntity;

import java.util.List;
import java.util.Map;

/**
 * 性能测试用例
 * 
 */
public interface StressTestService {

	/**
	 * 根据ID，查询性能测试用例
	 */
	StressTestEntity queryObject(Long caseId);
	
	/**
	 * 查询性能测试用例列表
	 */
	List<StressTestEntity> queryList(Map<String, Object> map);
	
	/**
	 * 查询总数
	 */
	int queryTotal(Map<String, Object> map);
	
	/**
	 * 保存性能测试用例
	 */
	void save(StressTestEntity stressCase);

	/**
	 * 更新性能测试用例信息
	 */
	void update(StressTestEntity stressCase);
	
	/**
	 * 批量删除
	 */
	void deleteBatch(Long[] caseIds);
	
	/**
	 * 批量更新性能测试用例信息
	 */
	int updateBatch(Long[] caseIds, int status);
	
}
