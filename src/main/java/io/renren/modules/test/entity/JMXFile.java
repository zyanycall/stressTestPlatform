package io.renren.modules.test.entity;

import java.io.Serializable;
import java.util.List;

public class JMXFile implements Serializable {

    /**
     * 主键id
     */
    private Long fileId;
    
    private List<JMXThreadGroup> jmxThreadGroup;

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public List<JMXThreadGroup> getJmxThreadGroup() {
		return jmxThreadGroup;
	}

	public void setJmxThreadGroup(List<JMXThreadGroup> jmxThreadGroup) {
		this.jmxThreadGroup = jmxThreadGroup;
	}
    
}