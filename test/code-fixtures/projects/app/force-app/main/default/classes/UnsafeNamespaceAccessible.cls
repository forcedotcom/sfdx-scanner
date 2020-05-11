@namespaceAccessible
public class UnsafeNamespaceAccessible {

    public String unsafeName; //sfdc:sink

    @namespaceAccessible
    public static Id getName(String name) { //sfdc:source25
        return UnsafeSOQL.getId(name);
    }

    @namespaceAccessible
    public static Id name2(String name) { //sfdc:source26
        return UnsafeSOQL.getId(name);
    }

    @namespaceAccessible
    public  UnsafeGlobal(String name) { //sfdc:source27
        this.unsafeName = name;

    }

    @namespaceAccessible
    public Id name3(String name) { //sfdc:source28
        return UnsafeSOQL.getId(name);
    }
}