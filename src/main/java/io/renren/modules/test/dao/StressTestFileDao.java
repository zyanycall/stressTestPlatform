package io.renren.modules.test.dao;

import io.renren.modules.sys.dao.BaseDao;
import io.renren.modules.test.entity.StressTestFileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StressTestFileDao extends BaseDao<StressTestFileEntity> {

    int deleteBatchByCaseIds(Object[] id);
}
