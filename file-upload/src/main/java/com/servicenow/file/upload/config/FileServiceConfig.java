package com.servicenow.file.upload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileServiceConfig {

	@Value("${file.save.path}")
	private String fileSavePath;

	@Value("${file.delete.cycle}")
	private int deleteCycle;

	@Value("${file.leader.timeout}")
	private long leaderTimeout;

	@Value("${file.heartbeat.interval}")
	private long heartBeatInterval;

	@Value("${file.lock.name}")
	private String lockFileName;

	public String getFileSavePath() {
		return fileSavePath;
	}

	public int getDeleteCycle() {
		return deleteCycle;
	}

	public long getLeaderTimeout() {
		return leaderTimeout;
	}

	public long getHeartBeatInterval() {
		return heartBeatInterval;
	}

	public String getLockFileName() {
		return lockFileName;
	}

}