package io.renren.modules.test.entity;

import org.hibernate.validator.constraints.NotBlank;

import java.io.Serializable;
import java.util.Date;

/**
 * 压力测试
 * 
 */
public class StressTestEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 用例id
	 */
	private Long caseId;

	/**
	 * 用例名称
	 */
	@NotBlank(message="用例名称不能为空")
	private String caseName;
	
	/**
	 * 所属项目
	 */
	@NotBlank(message="项目不能为空")
	private String project;

	/**
	 * 模块
	 */
	@NotBlank(message="模块不能为空")
	private String module;

	/**
	 * 备注
	 */
	private String remark;

	/**
	 * 操作人
	 */
	@NotBlank(message="操作人不能为空")
	private String operator;

	/**
	 * 用例进行的状态  0：未执行(停止状态)  1：正在执行
	 */
	private Integer status = 0;

	/**
	 * 用例优先级
	 */
	private String priority;

	/**
	 * 用例文件夹
	 */
	private String caseDir;

	/**
	 * 提交用例的人
	 */
	private String addBy;

	/**
	 * 修改用例的人
	 */
	private String updateBy;

	/**
	 * 提交的时间
	 */
	private Date addTime;

	/**
	 * 更新的时间
	 */
	private Date updateTime;

	public Long getCaseId() {
		return caseId;
	}

	public void setCaseId(Long caseId) {
		this.caseId = caseId;
	}

	public String getCaseName() {
		return caseName;
	}

	public void setCaseName(String caseName) {
		this.caseName = caseName;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getAddBy() {
		return addBy;
	}

	public void setAddBy(String addBy) {
		this.addBy = addBy;
	}

	public String getUpdateBy() {
		return updateBy;
	}

	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getCaseDir() {
		return caseDir;
	}

	public void setCaseDir(String caseDir) {
		this.caseDir = caseDir;
	}
}
