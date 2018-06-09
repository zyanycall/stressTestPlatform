package io.renren.modules.test.dao;

import io.renren.modules.sys.dao.BaseDao;
import io.renren.modules.test.entity.StressTestSlaveEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
public interface StressTestSlaveDao extends BaseDao<StressTestSlaveEntity> {

    /**
     * 批量更新
     */
    int updateBatch(Map<String, Object> map);

}
