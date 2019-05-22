package io.renren.modules.test.entity;

import java.io.Serializable;

public class SynchronizeFile implements Serializable {

    /**
     * 主键id
     */
    private Long fileId;
    
    private Long slaveId;

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public Long getSlaveId() {
		return slaveId;
	}

	public void setSlaveId(Long slaveId) {
		this.slaveId = slaveId;
	}

}