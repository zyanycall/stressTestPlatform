package io.renren.modules.test.service.impl;

import io.renren.common.exception.RRException;
import io.renren.modules.test.dao.DebugTestReportsDao;
import io.renren.modules.test.dao.StressTestDao;
import io.renren.modules.test.dao.StressTestFileDao;
import io.renren.modules.test.dao.StressTestReportsDao;
import io.renren.modules.test.entity.StressTestEntity;
import io.renren.modules.test.service.StressTestService;
import io.renren.modules.test.utils.StressTestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("stressTestService")
public class StressTestServiceImpl implements StressTestService {

    @Autowired
    private StressTestDao stressTestDao;

    @Autowired
    private StressTestFileDao stressTestFileDao;

    @Autowired
    private StressTestReportsDao stressTestReportsDao;

    @Autowired
    private DebugTestReportsDao debugTestReportsDao;

    @Autowired
    private StressTestUtils stressTestUtils;

    @Override
    public StressTestEntity queryObject(Long caseId) {
        return stressTestDao.queryObject(caseId);
    }

    @Override
    public List<StressTestEntity> queryList(Map<String, Object> map) {
        return stressTestDao.queryList(map);
    }

    @Override
    public int queryTotal(Map<String, Object> map) {
        return stressTestDao.queryTotal(map);
    }

    @Override
    public void save(StressTestEntity stressCase) {
        stressTestDao.save(stressCase);
    }

    @Override
    public void update(StressTestEntity stressCase) {
        stressTestDao.update(stressCase);
    }

    @Override
    @Transactional
    public void deleteBatch(Long[] caseIds) {
        for (Long caseId : caseIds) {
            // 先删除所属文件
            StressTestEntity stressCase = queryObject(caseId);
            String casePath = stressTestUtils.getCasePath();
            String caseFilePath = casePath + File.separator + stressCase.getCaseDir();
            try {
                FileUtils.forceDelete(new File(caseFilePath));
            } catch (FileNotFoundException e) {
                //doNothing
            } catch (IOException e) {
                throw new RRException("删除文件异常失败", e);
            }
        }
        // 删除数据库内容
        // 脚本文件的删除调用file的自身方法，在controller中调用。因为file包含了分布式节点的数据。
        // 测试报告的内容都在master服务器端，所以直接删除case文件夹即可。
        stressTestReportsDao.deleteBatchByCaseIds(caseIds);
        debugTestReportsDao.deleteBatchByCaseIds(caseIds);
        stressTestDao.deleteBatch(caseIds);
    }

    @Override
    public int updateBatch(Long[] caseIds, int status) {
        Map<String, Object> map = new HashMap<>();
        map.put("list", caseIds);
        map.put("status", status);
        return stressTestDao.updateBatch(map);
    }
}
