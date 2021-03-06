/*
 * 3D City Database - The Open Source CityGML Database
 * http://www.3dcitydb.org/
 * 
 * (C) 2013 - 2015,
 * Chair of Geoinformatics,
 * Technische Universitaet Muenchen, Germany
 * http://www.gis.bgu.tum.de/
 * 
 * The 3D City Database is jointly developed with the following
 * cooperation partners:
 * 
 * virtualcitySYSTEMS GmbH, Berlin <http://www.virtualcitysystems.de/>
 * M.O.S.S. Computer Grafik Systeme GmbH, Muenchen <http://www.moss.de/>
 * 
 * The 3D City Database Importer/Exporter program is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 */
package org.citydb.modules.citygml.importer.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import org.citydb.config.project.database.DBConnection;
import org.citygml4j.model.citygml.CityGMLClass;

public class ImportLogger {
	private static int counter;
	private final ReentrantLock lock = new ReentrantLock();
	
	private Path logFile;
	private BufferedWriter writer;
	private Date date;

	public ImportLogger(String logDir, File importFile, DBConnection connection) throws IOException {
		Path path = Paths.get(logDir, connection.getDescription().replaceAll("[^a-zA-Z0-9\\.\\-]", "_"));
		if (!Files.exists(path))
			Files.createDirectories(path);

		date = Calendar.getInstance().getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS");

		logFile = Paths.get(path.toString(), "imported_features-" + dateFormat.format(date) + "_" + (++counter) + ".log");
		if (Files.exists(logFile))
			throw new IOException("The log file '" + logFile.getFileName() + "' for imported-top level features already exists.");

		writer = Files.newBufferedWriter(logFile, Charset.defaultCharset());
		writeHeader(importFile, connection);
	}

	public Path getLogFilePath() {
		return logFile;
	}

	private void writeHeader(File fileName, DBConnection connection) throws IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

		writer.write('#' + this.getClass().getPackage().getImplementationTitle() + ", version \"" +
				this.getClass().getPackage().getImplementationVersion() + "\"");
		writer.newLine();		
		writer.write("#Imported top-level features from file: ");
		writer.write(fileName.getAbsolutePath());
		writer.newLine();
		writer.write("#Database connection string: ");
		writer.write(connection.toConnectString());
		writer.newLine();
		writer.write("#Timestamp: ");
		writer.write(dateFormat.format(date));
		writer.newLine();
		writer.write("FEATURE_TYPE,CITYOBJECT_ID,GMLID_IN_FILE");
		writer.newLine();
	}

	private void writeFooter(boolean success) throws IOException {
		if (success)
			writer.write("#Import successfully finished.");
		else
			writer.write("#Import aborted.");
	}
	
	public void write(ImportLogEntry entry) throws IOException {
		final ReentrantLock lock = this.lock;
		lock.lock();
		
		try {
			writer.write(entry.type.toString());
			writer.write(',');
			writer.write(String.valueOf(entry.id));
			writer.write(',');
			writer.write(entry.gmlId);
			writer.newLine();
		} finally {
			lock.unlock();
		}
	}

	public void close(boolean success) throws IOException {
		writeFooter(success);
		writer.close();
	}

	public static class ImportLogEntry {
		private final CityGMLClass type;
		private final long id;
		private final String gmlId;

		public ImportLogEntry(CityGMLClass type, long id, String gmlId) {
			this.type = type;
			this.id = id;
			this.gmlId = gmlId != null && !gmlId.isEmpty() ? gmlId : "";
		}
	}
}
