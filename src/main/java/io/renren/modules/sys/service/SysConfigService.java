package io.renren.modules.sys.service;

import io.renren.modules.sys.entity.SysConfigEntity;

import java.util.List;
import java.util.Map;

/**
 * 系统配置信息
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2016年12月4日 下午6:49:01
 */
public interface SysConfigService {
	
	/**
	 * 保存配置信息
	 */
	void save(SysConfigEntity config);
	
	/**
	 * 更新配置信息
	 */
	void update(SysConfigEntity config);
	
	/**
	 * 根据key，更新value
	 */
	void updateValueByKey(String key, String value);
	
	/**
	 * 删除配置信息
	 */
	void deleteBatch(Long[] ids);
	
	/**
	 * 获取List列表
	 */
	List<SysConfigEntity> queryList(Map<String, Object> map);
	/**
	 * 获取总记录数
	 */
	int queryTotal(Map<String, Object> map);
	
	SysConfigEntity queryObject(Long id);
	
	/**
	 * 根据key，获取配置的value值
	 * 
	 * @param key           key
	 */
	String getValue(String key);
	
	/**
	 * 根据key，获取value的Object对象
	 * @param key    key
	 * @param clazz  Object对象
	 */
	<T> T getConfigObject(String key, Class<T> clazz);
	
}
