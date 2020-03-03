package com.glyde.mall.scheduler.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class ConfigItem {

	@Id
	String configJob;

	String configParam;

	String configSchedule;

	public ConfigItem() {
	}

	public ConfigItem(String configJob, String configParam, String configSchedule) {
		this.configJob = configJob;
		this.configParam = configParam;
		this.configSchedule = configSchedule;
	}

	public String getConfigJob() {
		return configJob;
	}

	public void setConfigJob(String configJob) {
		this.configJob = configJob;
	}

	public String getConfigParam() {
		return configParam;
	}

	public void setConfigParam(String configParam) {
		this.configParam = configParam;
	}

	public String getConfigSchedule() {
		return configSchedule;
	}

	public void setConfigSchedule(String configSchedule) {
		this.configSchedule = configSchedule;
	}
}
