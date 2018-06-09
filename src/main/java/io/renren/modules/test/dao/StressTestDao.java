package io.renren.modules.test.dao;

import io.renren.modules.sys.dao.BaseDao;
import io.renren.modules.test.entity.StressTestEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

/**
 * 性能测试
 * 
 */
@Mapper
public interface StressTestDao extends BaseDao<StressTestEntity> {
	
	/**
	 * 批量更新状态
	 */
	int updateBatch(Map<String, Object> map);
}
