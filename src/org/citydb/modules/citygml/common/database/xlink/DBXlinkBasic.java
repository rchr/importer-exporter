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
package org.citydb.modules.citygml.common.database.xlink;

import org.citydb.database.TableEnum;

public class DBXlinkBasic implements DBXlink {
	private long id;
	private TableEnum fromTable;
	private String gmlId;
	private TableEnum toTable;
	private String attrName;

	public DBXlinkBasic(long id, TableEnum fromTable, String gmlId, TableEnum toTable) {
		this.id = id;
		this.fromTable = fromTable;
		this.gmlId = gmlId;
		this.toTable = toTable;
	}

	public long getId() {
		return id;
	}

	public TableEnum getFromTable() {
		return fromTable;
	}

	public void setFromTable(TableEnum fromTable) {
		this.fromTable = fromTable;
	}

	public TableEnum getToTable() {
		return toTable;
	}

	public void setToTable(TableEnum toTable) {
		this.toTable = toTable;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getGmlId() {
		return gmlId;
	}

	public void setGmlId(String gmlId) {
		this.gmlId = gmlId;
	}

	public String getAttrName() {
		return attrName;
	}

	public void setAttrName(String attrName) {
		this.attrName = attrName;
	}

	@Override
	public DBXlinkEnum getXlinkType() {
		return DBXlinkEnum.BASIC;
	}
}
