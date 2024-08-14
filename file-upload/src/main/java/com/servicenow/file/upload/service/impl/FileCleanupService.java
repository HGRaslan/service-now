package com.servicenow.file.upload.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.servicenow.file.upload.config.FileServiceConfig;
import com.servicenow.file.upload.constants.Constants;

import jakarta.annotation.PreDestroy;

@Service
public class FileCleanupService {

	private static final Logger LOGGER = Logger.getLogger("FileCleanupService");

	private ScheduledExecutorService cleanupJobExecutorService;
	private LeaderElectionService leaderElectionService;
	private FileServiceConfig configs;

	public FileCleanupService(LeaderElectionService leaderElectionService, FileServiceConfig configs) {
		this.leaderElectionService = leaderElectionService;
		this.configs = configs;
		this.cleanupJobExecutorService = Executors.newScheduledThreadPool(1);
		this.cleanupJobExecutorService.scheduleAtFixedRate(this::cleanup, 0, configs.getDeleteCycle(), TimeUnit.MINUTES);
	}

	private void cleanup() {
		
		leaderElectionService.electLeader();
		
		if (leaderElectionService.isLeader()) {

			LOGGER.log(Level.FINE, "start cleanup job");

			File destinationDir = new File(configs.getFileSavePath());
			if (!destinationDir.exists() && !destinationDir.isDirectory()) {
				LOGGER.log(Level.WARNING, "Destination directory not exists , will skip cleanup");
				return;
			}

			cleanup(destinationDir);

			leaderElectionService.release();

			LOGGER.log(Level.FINE, "finish cleanup job");
		} else {
			LOGGER.log(Level.INFO, "Skip cleanup job not the leader node.");

		}
	}

	private void cleanup(File destinationDir) {
		for (String fileName : destinationDir.list((File, name) -> !name.equals(configs.getLockFileName()))) {
			try {
				Path filePath = Path.of(destinationDir.getAbsolutePath(), fileName);
				if (isFileExpired(filePath)) {
					Files.delete(filePath);
				}
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, "Error while clean up expired file [" + fileName + "].", ex);
			}
		}
	}

	private boolean isFileExpired(Path filePath) throws IOException {
		byte[] expTimeBytes = (byte[]) Files.getAttribute(filePath, Constants.FILE_ATT_EXPIRATION_TIME);
		return System.currentTimeMillis() >= Long.parseLong(new String(expTimeBytes));
	}

	@PreDestroy
	private void shutdown() {
		if (cleanupJobExecutorService != null) {
			cleanupJobExecutorService.shutdown();
		}
	}

}
