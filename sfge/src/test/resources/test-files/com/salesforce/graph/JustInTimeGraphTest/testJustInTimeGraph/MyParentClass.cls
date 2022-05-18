public class MyParentClass implements MyInterface {
    public void doSomething(String s, String sObjectType) {
        Boolean isSearchable = Schema.getGlobalDescribe().get(objectName).getDescribe().isSearchable();
        System.debug(isSearchable);
    }
}
