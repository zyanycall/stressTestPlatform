package io.renren.modules.test.service;

import io.renren.modules.test.entity.StressTestSlaveEntity;

import java.util.List;
import java.util.Map;

/**
 * 性能测试用例
 * 
 */
public interface StressTestSlaveService {

	/**
	 * 根据ID，查询子节点信息
	 */
	StressTestSlaveEntity queryObject(Long slaveId);
	
	/**
	 * 查询子节点列表
	 */
	List<StressTestSlaveEntity> queryList(Map<String, Object> map);
	
	/**
	 * 查询总数
	 */
	int queryTotal(Map<String, Object> map);
	
	/**
	 * 保存
	 */
	void save(StressTestSlaveEntity stressTestSlave);

	/**
	 * 更新
	 */
	void update(StressTestSlaveEntity stressTestSlave);
	
	/**
	 * 批量删除
	 */
	void deleteBatch(Long[] slaveIds);
	
	/**
	 * 批量更新状态
	 */
	void updateBatchStatus(List<Long> slaveIds, Integer status);
}
