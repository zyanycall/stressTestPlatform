package io.renren.modules.test.service;

import java.util.List;

import io.renren.modules.test.entity.JMXThreadGroup;

/**
 * 性能测试压测脚本jmx文件 
 */
public interface StressJMXService {

    /**
     * 查询jmx脚本文件内容
     * @param path jmx文件文件路径
     * @return
     */
	List<JMXThreadGroup> queryJMXFile(String path);

    /**
     * 
     * 编辑jmx脚本文件内容
     * @param path
     * @param threadGroupList 需要修改的参数
     */
    void update(String path, List<JMXThreadGroup> threadGroupList);

}
