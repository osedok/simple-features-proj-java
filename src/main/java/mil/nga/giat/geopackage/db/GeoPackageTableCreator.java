package mil.nga.giat.geopackage.db;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import mil.nga.giat.geopackage.GeoPackageException;
import mil.nga.giat.geopackage.property.GeoPackageProperties;
import mil.nga.giat.geopackage.property.PropertyConstants;
import mil.nga.giat.geopackage.user.UserColumn;
import mil.nga.giat.geopackage.user.UserTable;
import mil.nga.giat.geopackage.user.UserUniqueConstraint;

/**
 * Executes database scripts to create GeoPackage tables
 * 
 * @author osbornb
 */
public class GeoPackageTableCreator {

	/**
	 * SQLite database
	 */
	private final GeoPackageCoreConnection db;

	/**
	 * Constructor
	 * 
	 * @param db
	 */
	public GeoPackageTableCreator(GeoPackageCoreConnection db) {
		this.db = db;
	}

	/**
	 * Create Spatial Reference System table and views
	 * 
	 * @return
	 */
	public int createSpatialReferenceSystem() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "spatial_reference_system"));
	}

	/**
	 * Create Contents table
	 * 
	 * @return
	 */
	public int createContents() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "contents"));
	}

	/**
	 * Create Geometry Columns table
	 * 
	 * @return executed statements
	 */
	public int createGeometryColumns() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "geometry_columns"));
	}

	/**
	 * Create Tile Matrix Set table
	 * 
	 * @return executed statements
	 */
	public int createTileMatrixSet() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "tile_matrix_set"));
	}

	/**
	 * Create Tile Matrix table
	 * 
	 * @return executed statements
	 */
	public int createTileMatrix() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "tile_matrix"));
	}

	/**
	 * Create Data Columns table
	 * 
	 * @return executed statements
	 */
	public int createDataColumns() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "data_columns"));
	}

	/**
	 * Create Data Column Constraints table
	 * 
	 * @return executed statements
	 */
	public int createDataColumnConstraints() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "data_column_constraints"));
	}

	/**
	 * Create Metadata table
	 * 
	 * @return executed statements
	 */
	public int createMetadata() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "metadata"));
	}

	/**
	 * Create Metadata Reference table
	 * 
	 * @return executed statements
	 */
	public int createMetadataReference() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "metadata_reference"));
	}

	/**
	 * Create Extensions table
	 * 
	 * @return executed statements
	 */
	public int createExtensions() {
		return createTable(GeoPackageProperties.getProperty(
				PropertyConstants.SQL, "extensions"));
	}

	/**
	 * Create a table using the table script
	 * 
	 * @param tableScript
	 * @return
	 */
	private int createTable(String tableScript) {
		InputStream scriptStream = Thread
				.currentThread()
				.getContextClassLoader()
				.getResourceAsStream(
						GeoPackageProperties.getProperty(PropertyConstants.SQL,
								"directory") + File.separatorChar + tableScript);
		int statements = runScript(scriptStream);
		return statements;
	}

	/**
	 * Run the script input stream
	 * 
	 * @param stream
	 * @return executed statements
	 */
	private int runScript(final InputStream stream) {
		int count = 0;

		// Use multiple newlines as the delimiter
		Scanner s = new Scanner(stream);
		try {
			s.useDelimiter(Pattern.compile("\\n\\s*\\n"));
			// Execute each statement
			while (s.hasNext()) {
				String statement = s.next().trim();
				db.execSQL(statement);
				count++;
			}
		} finally {
			s.close();
		}
		return count;
	}

	/**
	 * Create the user defined table
	 * 
	 * @param table
	 * @param <TColumn>
	 */
	public <TColumn extends UserColumn> void createTable(
			UserTable<TColumn> table) {

		// Verify the table does not already exist
		if (db.tableExists(table.getTableName())) {
			throw new GeoPackageException(
					"Table already exists and can not be created: "
							+ table.getTableName());
		}

		// Build the create table sql
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE ").append(table.getTableName()).append(" (");

		// Add each column to the sql
		List<? extends UserColumn> columns = table.getColumns();
		for (int i = 0; i < columns.size(); i++) {
			UserColumn column = columns.get(i);
			if (i > 0) {
				sql.append(",");
			}
			sql.append("\n  ").append(column.getName()).append(" ")
					.append(column.getTypeName());
			if (column.getMax() != null) {
				sql.append("(").append(column.getMax()).append(")");
			}
			if (column.isNotNull()) {
				sql.append(" NOT NULL");
			}
			if (column.isPrimaryKey()) {
				sql.append(" PRIMARY KEY AUTOINCREMENT");
			}
		}

		// Add unique constraints
		List<UserUniqueConstraint<TColumn>> uniqueConstraints = table
				.getUniqueConstraints();
		for (int i = 0; i < uniqueConstraints.size(); i++) {
			UserUniqueConstraint<TColumn> uniqueConstraint = uniqueConstraints
					.get(i);
			sql.append(",\n  UNIQUE (");
			List<TColumn> uniqueColumns = uniqueConstraint.getColumns();
			for (int j = 0; j < uniqueColumns.size(); j++) {
				TColumn uniqueColumn = uniqueColumns.get(j);
				if (j > 0) {
					sql.append(", ");
				}
				sql.append(uniqueColumn.getName());
			}
			sql.append(")");
		}

		sql.append("\n);");

		// Create the table
		db.execSQL(sql.toString());
	}

}
