package sfdc.sfdx.scanner.pmd.catalog;

public class PmdCatalogCategory {
	/**
	 * The name of the category, e.g. "error prone", "performance", etc.
	 */
	private String name;
	/**
	 * The path to the definition file for this category.
	 */
	private String path;
	/**
	 * The JAR containing this category's definition file.
	 */
	private String sourceJar;

	/**
	 * @param name - The name for this category.
	 * @param path - The path to the category's definition file.
	 * @param sourceJar - The JAR containing the category's definition file.
	 */
	public PmdCatalogCategory(String name, String path, String sourceJar) {
		this.name = name;
		this.path = path;
		this.sourceJar = sourceJar;
	}

	/**
	 * Getter for 'name' property.
	 *
	 * @return - Category name.
	 */
	String getName() {
		return name;
	}

	/**
	 * Getter for 'path' property.
	 *
	 * @return - Category definition path.
	 */
	String getPath() {
		return path;
	}

	/**
	 * Getter for 'sourceJar' property.
	 * @return - JAR containing definition file.
	 */
	String getSourceJar() {
		return sourceJar;
	}
}
