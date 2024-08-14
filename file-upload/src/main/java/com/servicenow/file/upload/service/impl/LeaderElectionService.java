package com.servicenow.file.upload.service.impl;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.servicenow.file.upload.config.FileServiceConfig;

import jakarta.annotation.PreDestroy;

@Service
public class LeaderElectionService {

	private static final Logger LOGGER = Logger.getLogger("LeaderElectionService");

	private FileServiceConfig configs;
	private AtomicBoolean isLeader;
	private ScheduledExecutorService heartBeatExecutorService;

	public LeaderElectionService(FileServiceConfig configs) {
		this.configs = configs;
		this.isLeader = new AtomicBoolean(false);
		this.heartBeatExecutorService = new ScheduledThreadPoolExecutor(1);
	}

	public void electLeader() {

		// try to become leader by creating new lock file it will fail if file already
		// exists
		attemptToBecomeLeader();

		// if failed to be leader, then it will check if the existing lock file is stale
		// to be deleted or other node is acquiring the leadership
		// there is room for race condition between checking file stale and deleting lock file
		//The most robust solution is to use database locking or a service coordinator such as Redis or Zookeeper.		
		if (!isLeader() && deleteIfLockFileStale()) {
			// Lock file is stale and deleted , try to become the new leader
			attemptToBecomeLeader();
		}
	}

	private void attemptToBecomeLeader() {
		try {
			// write lock file , will fail if file exists or created by another node
			Files.write(getLockFile().toPath(), getHostName().getBytes(), StandardOpenOption.CREATE_NEW);

			isLeader.set(true);

			// run heart beat thread to update lock file last modification time
			heartBeatExecutorService.scheduleAtFixedRate(this::performHeartBeat, configs.getHeartBeatInterval(),
					configs.getHeartBeatInterval(), TimeUnit.SECONDS);

			LOGGER.info("This node has become the leader.");

		} catch (IOException e) {
			isLeader.set(false);
			LOGGER.info("Failed to become the leader. Another node may have taken over.");
		}
	}

	public void release() {
		if (isLeader()) {
			deleteLockFile();
			isLeader.set(false);
		}
	}

	private void performHeartBeat() {
		File lockFile = getLockFile();
		while (isLeader.get()) {
			if (lockFile.exists()) {
				lockFile.setLastModified(System.currentTimeMillis());
			} else {
				isLeader.set(false);
			}
		}
	}

	/*
	 * the operation of checking if the file is stale 
	 * return true if the file not exists or deleted successfully ,
	 * otherwise return false
	 */
	private boolean deleteIfLockFileStale() {
		File lockFile = getLockFile();

		// Check if the file still exists
		if (!lockFile.exists()){
			return true;
		}
		
		if(isStaleLockFile(lockFile.toPath())) {
			deleteLockFile();
			return lockFile.delete();
		}
		
		return false;
	}

	private boolean isStaleLockFile(Path filePath) {
		try {
			FileTime lastModifiedTime = Files.getLastModifiedTime(filePath);
			long leaderTimeoutMillis = TimeUnit.SECONDS.toMillis(configs.getLeaderTimeout());
			return (System.currentTimeMillis() - lastModifiedTime.toMillis()) >= leaderTimeoutMillis;
		} catch (IOException e) {
			return true;
		}
	}

	public boolean isLeader() {
		return isLeader.get();
	}

	@PreDestroy
	private void shutdown() {
		if (heartBeatExecutorService != null) {
			heartBeatExecutorService.shutdown();
		}
	}

	private File getLockFile() {
		return new File(configs.getFileSavePath(), configs.getLockFileName());
	}

	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			return "default";
		}
	}

	private void deleteLockFile() {
		try {
			Files.deleteIfExists(getLockFile().toPath());
		} catch (Exception ex) {
			LOGGER.log(Level.WARNING, "Failed to delete lock file", ex);
		}
	}
}
