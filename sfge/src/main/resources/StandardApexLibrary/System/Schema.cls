global class Schema {
	global static List<Schema.DescribeDataCategoryGroupStructureResult> describeDataCategoryGroupStructures(List<Schema.DataCategoryGroupSobjectTypePair> pairs, Boolean topCategoriesOnly) { }
	global static List<Schema.DescribeDataCategoryGroupResult> describeDataCategoryGroups(List<String> sobjects) { }
	global static List<Schema.DescribeSObjectResult> describeSObjects(List<String> types) { }
	global static List<Schema.DescribeTabSetResult> describeTabs() { }
	global static Map<String,Schema.SObjectType> getAppDescribe(String appName) { }
	global static Map<String,Schema.SObjectType> getGlobalDescribe() { }
	global static Map<String,Schema.SObjectType> getModuleDescribe(String moduleName) { }
	global static Map<String,Schema.SObjectType> getModuleDescribe() { }

}