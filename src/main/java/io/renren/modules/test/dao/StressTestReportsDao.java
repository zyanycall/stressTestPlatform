package io.renren.modules.test.dao;

import io.renren.modules.sys.dao.BaseDao;
import io.renren.modules.test.entity.StressTestReportsEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StressTestReportsDao extends BaseDao<StressTestReportsEntity> {

    int deleteBatchByCaseIds(Object[] id);
}
