-- UTIL.sql
--
-- Authors:     Claus Nagel <nagel@igg.tu-berlin.de>
--
-- Conversion:	Felix Kunde <felix-kunde@gmx.de>
--
-- Copyright:   (c) 2007-2012  Institute for Geodesy and Geoinformation Science,
--                             Technische Universität Berlin, Germany
--                             http://www.igg.tu-berlin.de
--
--              This skript is free software under the LGPL Version 2.1.
--              See the GNU Lesser General Public License at
--              http://www.gnu.org/copyleft/lgpl.html
--              for more details.
-------------------------------------------------------------------------------
-- About:
-- Creates package "geodb_util" containing utility methods for applications
-- and further subpackges. Therefore, "geodb_util" might be a dependency
-- for other packages and/or methods.
-------------------------------------------------------------------------------
--
-- ChangeLog:
--
-- Version | Date       | Description      | Author | Conversion 
-- 1.0.0     2008-09-10   release version    CNag
-- 1.1.0     2012-02-29   update to 2.0.6    CNag	  FKun
--

/*****************************************************************
* versioning_table
*
* @param table_name name of the unversioned table, i.e., omit
*                   suffixes such as _LT
* @RETURN VARCHAR 'ON' for version-enabled, 'OFF' otherwise
******************************************************************/
CREATE OR REPLACE FUNCTION geodb_pkg.util_versioning_table(table_name VARCHAR) RETURNS VARCHAR AS $$
BEGIN
    RETURN 'OFF';
END;
$$
LANGUAGE plpgsql;

/*****************************************************************
* versioning_db
*
* @RETURN VARCHAR 'ON' for version-enabled, 'PARTLY' and 'OFF'
******************************************************************/
CREATE OR REPLACE FUNCTION geodb_pkg.util_versioning_db() RETURNS VARCHAR AS $$
BEGIN
    RETURN 'OFF';
END;
$$
LANGUAGE plpgsql;
 
/*****************************************************************
* db_info
*
* @param srid database srid
* @param srs database srs name
* @param versioning database versioning
******************************************************************/
CREATE OR REPLACE FUNCTION geodb_pkg.util_db_info(OUT srid DATABASE_SRS.SRID%TYPE, OUT srs DATABASE_SRS.GML_SRS_NAME%TYPE) RETURNS SETOF record AS $$
    SELECT srid, gml_srs_name FROM database_srs;
$$ 
LANGUAGE sql;

/******************************************************************
* db_metadata
*
******************************************************************/
CREATE OR REPLACE FUNCTION geodb_pkg.util_db_metadata() 
RETURNS TABLE(srid INTEGER, gml_srs_name VARCHAR(1000), coord_ref_sys_name VARCHAR(2048), coord_ref_sys_kind VARCHAR(2048)) AS $$
BEGIN
    EXECUTE 'SELECT SRID, GML_SRS_NAME FROM DATABASE_SRS' INTO srid, gml_srs_name;
    EXECUTE 'SELECT srtext, srtext FROM spatial_ref_sys WHERE SRID=' || srid || '' INTO coord_ref_sys_name, coord_ref_sys_kind;
    coord_ref_sys_name := split_part(coord_ref_sys_name, '"', 2);
    coord_ref_sys_kind := split_part(coord_ref_sys_kind, '[', 1);
    RETURN NEXT;
END;
$$
LANGUAGE plpgsql;  
  
/******************************************************************
* error_msg
*
* @param err_code PostgreSQL error code, e.g. '06404'
* @RETURN VARCHAR corresponding PostgreSQL error message                 
******************************************************************/
CREATE OR REPLACE FUNCTION geodb_pkg.util_error_msg(err_code VARCHAR) RETURNS VARCHAR AS $$
BEGIN
    BEGIN
        RAISE EXCEPTION USING ERRCODE = err_code;
        RETURN err_code;
    EXCEPTION
        WHEN OTHERS THEN
            RETURN SQLERRM;
   END;
END;
$$
LANGUAGE plpgsql;
   
/******************************************************************
* min
*
* @param a first NUMERIC value
* @param b second NUMERIC value
* @RETURN NUMERIC the smaller of the two input NUMERIC values                
******************************************************************/
CREATE OR REPLACE FUNCTION geodb_pkg.util_min(a NUMERIC, b NUMERIC) RETURNS NUMERIC AS $$
BEGIN
    IF a < b THEN
        RETURN a;
    ELSE
        RETURN b;
    END IF;
END;
$$
LANGUAGE plpgsql;
  
/******************************************************************
* transform_or_null
*
* @param geom the geometry whose representation is to be transformed using another coordinate system 
* @param srid the SRID of the coordinate system to be used for the transformation.
* @RETURN GEOMETRY the transformed geometry representation                
******************************************************************/
CREATE OR REPLACE FUNCTION geodb_pkg.util_transform_or_null(geom GEOMETRY, srid INTEGER) RETURNS geometry AS $$
BEGIN
    IF geom IS NOT NULL THEN
        RETURN ST_Transform(geom, srid);
    ELSE
        RETURN NULL;
    END IF;
END;
$$
LANGUAGE plpgsql;
    
/******************************************************************
* is_coord_ref_sys_3d
*
* no 3D-Coord.-Reference-System defined in the spatial_ref_sys-table of PostGIS 2.0 
*
* @param srid the SRID of the coordinate system to be checked
* @RETURN NUMERIC the boolean result encoded as NUMERIC: 0 = false, 1 = true                
******************************************************************/
/*
CREATE FUNCTION geodb_pkg.util_is_coord_ref_sys_3d(srid INTEGER) RETURNS INTEGER AS $$
  DECLARE
    s ALIAS FOR $1;
	is_3d INTEGER := 0;
  BEGIN
	EXECUTE 'SELECT COUNT(*) from SDO_CRS_COMPOUND where SRID=$1' INTO is_3d USING s;
    IF is_3d = 0 THEN
      EXECUTE 'SELECT COUNT(*) from SDO_CRS_GEOGRAPHIC3D where SRID=$1' INTO is_3d USING s;
    END IF;
    
    RETURN is_3d;
  END;
$$
LANGUAGE plpgsql;
*/  
/******************************************************************
* is_db_coord_ref_sys_3d
*
* @RETURN NUMERIC the boolean result encoded as NUMERIC: 0 = false, 1 = true                
******************************************************************/
/*
CREATE FUNCTION geodb_pkg.util_is_db_coord_ref_sys_3d() RETURNS INTEGER AS $$
  DECLARE
    srid INTEGER;
  BEGIN
    EXECUTE 'SELECT srid from DATABASE_SRS' INTO srid;
    RETURN util_is_coord_ref_sys_3d(srid);
  END;
$$
LANGUAGE plpgsql;
*/
/*******************************************************************
* to_2d - In PostGIS has a function called ST_Force_2d, which cuts the z-values from the geometry.
*
* PL/SQL-code taken from http://forums.oracle.com/forums/thread.jspa?messageID=960492&#960492
*
*******************************************************************/
/*
CREATE FUNCTION geodb_pkg.util_to_2d (geom GEOMETRY, srid INTEGER) RETURNS geometry AS $$
  DECLARE
    geom_2d GEOMETRY;
    geom_poi VARCHAR;
    dim_count INTEGER; -- NUMERIC of dimensions in layer
    n_points INTEGER; -- NUMERIC of points in ordinates array
    n_ordinates INTEGER; -- NUMERIC of ordinates
    i INTEGER;
    j DOUBLE PRECISION;
    k DOUBLE PRECISION;
  BEGIN
    -- If the input geometry is null, just RETURN null
    IF geom IS NULL THEN 
		RETURN null;
    END IF;
    
	SELECT ST_Dimension(geom) INTO dim_count;

    IF dim_count = 2 THEN
      -- Nothing to do, geometry is already 2D
      RETURN geom;
    END IF;
  
    -- Construct and prepare the output geometry
    SELECT ST_GeomFromText((ST_AsText(geom)), srid) INTO geom_2d;

    -- Process the point structure
    IF ST_GeometryType(geom) = 'POINT' THEN
      PERFORM ST_Force_2d(geom);
    ELSE
      -- It is not a point  
      -- Process the ordinates array
  
      -- Prepare the size of the output array
      n_points := ST_NPoints(geom);
      FOR i IN 1..n_points LOOP
		EXECUTE 'SELECT ST_X(ST_PointN('|| geom || ', ' || i || '))' INTO j;
		EXECUTE 'SELECT ST_Y(ST_PointN('|| geom || ', ' || i || '))' INTO k;
		geom_poi := 'POINT(' || k || ' ' || j || ' 0)';
		EXECUTE 'SELECT ST_SetPoint(' || geom || ", " || i || ', ' || geom_poi ||')' INTO geom_2d;
	  END LOOP;
  
    END IF;
  
    RETURN geom_2d;
  
  EXCEPTION
    WHEN OTHERS THEN
      RAISE INFO 'failed to execute to_2d';
      RETURN null;
  END;
$$
LANGUAGE plpgsql;
*/