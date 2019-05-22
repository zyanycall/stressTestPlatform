package io.renren.modules.test.entity;

public class JMXThreadGroup {

	/**
	 * 线程组名称
	 */

	private String testname;
	/**
	 * 线程组是否开启
	 */

	private boolean enabled;
	/**
	 * 线程数量
	 */

	private String num_threads;
	/**
	 * 多少秒内启动全部线程
	 */

	private String ramp_time;
	/**
	 * 是否开启自动停止
	 */

	private boolean scheduler;
	/**
	 * 运行多少时间（秒）
	 */
	private String duration;

		
	public String getTestname() {
		return testname;
	}

	public void setTestname(String testname) {
		this.testname = testname;
	}

	public boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getNum_threads() {
		return num_threads;
	}

	public void setNum_threads(String num_threads) {
		this.num_threads = num_threads;
	}

	public String getRamp_time() {
		return ramp_time;
	}

	public void setRamp_time(String ramp_time) {
		this.ramp_time = ramp_time;
	}

	public boolean getScheduler() {
		return scheduler;
	}

	public void setScheduler(boolean scheduler) {
		this.scheduler = scheduler;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}
}