package io.renren.modules.test.service.impl;

import io.renren.common.exception.RRException;
import io.renren.modules.test.dao.StressTestSlaveDao;
import io.renren.modules.test.entity.StressTestSlaveEntity;
import io.renren.modules.test.service.StressTestSlaveService;
import io.renren.modules.test.utils.SSH2Utils;
import io.renren.modules.test.utils.StressTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service("stressTestSlaveService")
public class StressTestSlaveServiceImpl implements StressTestSlaveService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private StressTestSlaveDao stressTestSlaveDao;

    @Autowired
    private StressTestUtils stressTestUtils;

    @Override
    public StressTestSlaveEntity queryObject(Long slaveId) {
        return stressTestSlaveDao.queryObject(slaveId);
    }

    @Override
    public List<StressTestSlaveEntity> queryList(Map<String, Object> map) {
        return stressTestSlaveDao.queryList(map);
    }

    @Override
    public int queryTotal(Map<String, Object> map) {
        return stressTestSlaveDao.queryTotal(map);
    }

    @Override
    public void save(StressTestSlaveEntity stressTestSlave) {
        stressTestSlaveDao.save(stressTestSlave);
    }

    @Override
    public void update(StressTestSlaveEntity stressTestSlave) {
        stressTestSlaveDao.update(stressTestSlave);
    }

    @Override
    public void deleteBatch(Long[] slaveIds) {
        stressTestSlaveDao.deleteBatch(slaveIds);
    }

    /**
     * 重启节点
     */
    @Override
    @Async("asyncServiceExecutor")
    public void restartSingle(Long slaveId) {
        StressTestSlaveEntity slave = queryObject(slaveId);

        // 跳过本机节点 和 已经禁用的节点
        if (!"127.0.0.1".equals(slave.getIp().trim())
                && !StressTestUtils.DISABLE.equals(slave.getStatus())) {
            //更新数据库为进行中
            slave.setStatus(StressTestUtils.PROGRESSING);
            update(slave);

            try {
                SSH2Utils ssh2Util = new SSH2Utils(slave.getIp(), slave.getUserName(),
                        slave.getPasswd(), Integer.parseInt(slave.getSshPort()));

                startSlave(ssh2Util, slave, StressTestUtils.DISABLE);

                stopSlave(ssh2Util, slave, StressTestUtils.ENABLE);

            } catch (RRException e) {
                slave.setStatus(StressTestUtils.RUN_ERROR);
                update(slave);
                throw e;
            }
        }
    }

    /**
     * 批量强制切换节点的状态：仅更新数据库字段
     */
    @Override
    public void updateBatchStatusForce(List<Long> slaveIds, Integer status) {
        //使用for循环传统写法，直接更新数据库。
        for (Long slaveId : slaveIds) {
            StressTestSlaveEntity slave = queryObject(slaveId);
            //更新数据库
            slave.setStatus(status);
            update(slave);
        }
    }

    /**
     * 批量切换节点的状态：真正去执行Slave节点机启动脚本。
     */
    @Override
    @Async("asyncServiceExecutor")
    public void updateBatchStatus(Long slaveId, Integer status) {
        //当前是向所有的分布式节点推送这个，阻塞操作+轮询，并非多线程，因为本地同步网卡会是瓶颈。
        //采用了先给同一个节点机传送多个文件的方式，因为数据库的连接消耗优于节点机的链接消耗
        StressTestSlaveEntity slave = queryObject(slaveId);

        // 跳过本机节点
        if (!"127.0.0.1".equals(slave.getIp().trim())) {
            //更新数据库为进行中
            slave.setStatus(StressTestUtils.PROGRESSING);
            update(slave);

            try {
                SSH2Utils ssh2Util = new SSH2Utils(slave.getIp(), slave.getUserName(),
                        slave.getPasswd(), Integer.parseInt(slave.getSshPort()));
                if (StressTestUtils.ENABLE.equals(status)) {
                    startSlave(ssh2Util, slave, status);
                }
                if (StressTestUtils.DISABLE.equals(status)) {
                    stopSlave(ssh2Util, slave, status);
                }
            } catch (RRException e) {
                slave.setStatus(StressTestUtils.RUN_ERROR);
                update(slave);
                throw e;
            }
        }
    }

    /**
     * 启动单节点。
     * 由于设置了 -Dserver_port=port 参数，支持了单机可以根据不同的端口启动多个slave进程来触发集群测试。
     *
     * @param slave 节点对象
     */
    private void startSlave(SSH2Utils ssh2Util, StressTestSlaveEntity slave, Integer status) {
        if (StressTestUtils.ENABLE.equals(slave.getStatus())) {
            //本身已经是启用状态
            throw new RRException(slave.getSlaveName() + " 已经启动不要重复启动！");
        }

        // 避免跨系统的问题，远端由于都时linux服务器，则文件分隔符统一为/，不然同步文件会报错。
        String jmeterServer = slave.getHomeDir() + "/bin/jmeter-server";
        String md5Str = ssh2Util.runCommand("md5sum " + jmeterServer + " | cut -d ' ' -f1");
        if (!checkMD5(md5Str)) {
            throw new RRException(slave.getSlaveName() + " 执行遇到问题！找不到jmeter-server启动文件！");
        }
        //首先创建目录，会遇到重复创建
        ssh2Util.runCommand("mkdir " + slave.getHomeDir() + "/bin/stressTestCases");
        //让JAVA_HOME生效
        ssh2Util.runCommand("source /etc/bashrc");
        //启动节点
        String enableResult = ssh2Util.runCommand(
                "cd " + slave.getHomeDir() + "/bin/stressTestCases/" + "\n" +
                        "sh " + "../jmeter-server -Djava.rmi.server.hostname=" + slave.getIp()
                        + " -Dserver_port=" + slave.getJmeterPort());

        logger.error("启动节点" + slave.getIp() + "执行结果:" + enableResult);

        if (!enableResult.contains("remote")) {
            throw new RRException(slave.getSlaveName() + " jmeter-server启动节点失败！请先尝试在节点机命令执行");
        }

        //更新数据库
        slave.setStatus(status);
        update(slave);
    }

    /**
     * 停止单节点
     * 拆出来停止单节点，是由于单机可能存在多个slave进程，kill操作需要把相同IP的状态都设置为禁用。
     * 相同IP，根据不同的端口来区分杀掉进程也可以，但这样需要slave端部署的时候匹配，将jmeter-server中 -Dserver_port=${SERVER_PORT:1099}
     * 删除掉，或者避免设置为1099端口。但是这样会增加维护的负担，不如全杀掉来得稳妥简单。
     * 后续slave启停，程序和docker集群来做适配，那么这个问题将会得到解决。（但是docker环境会带来其他问题不在这里展开）
     *
     * @param slave 节点对象
     */
    private void stopSlave(SSH2Utils ssh2Util, StressTestSlaveEntity slave, Integer status) {
        //禁用远程节点，当前是直接kill掉
        //kill掉就不用判断结果了，不抛异常即OK
        //考虑到网络的操作容易失败，执行2次kill
        ssh2Util.runCommand("ps -efww|grep -w 'jmeter-server'|grep -v grep|cut -c 9-18|xargs kill -9");
        stressTestUtils.pause(2000);
        ssh2Util.runCommand("ps -efww|grep -w 'jmeter-server'|grep -v grep|cut -c 9-18|xargs kill -9");

        //更新数据库
        //存在单机多Jmeter进程情况，所以要根据IP，相同IP的slave都禁用掉。
        Map ipMap = new HashMap();
        ipMap.put("ip", slave.getIp());
        List<StressTestSlaveEntity> list = queryList(ipMap);
        for (StressTestSlaveEntity slaveEntity : list) {
            slaveEntity.setStatus(status);
            update(slaveEntity);
        }
    }

    /**
     * 使用正则表达式校验MD5合法性
     */
    public boolean checkMD5(String md5Str) {
        return Pattern.matches("^([a-fA-F0-9]{32})$", md5Str);
    }
}
