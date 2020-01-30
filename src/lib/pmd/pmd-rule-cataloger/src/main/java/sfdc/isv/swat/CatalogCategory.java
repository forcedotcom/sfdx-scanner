package sfdc.isv.swat;

class CatalogCategory {
  private String name;
  private String path;

  CatalogCategory(String name, String path) {
    this.name = name;
    this.path = path;
  }


  String getName() {
    return name;
  }

  String getPath() {
    return path;
  }
}
