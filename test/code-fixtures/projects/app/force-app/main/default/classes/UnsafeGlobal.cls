global class UnsafeGlobal {

    global String unsafeName;

    global static Id getName(String name) {
        return UnsafeSOQL.getId(name);
    }

    public static Id name2(String name) {
        return UnsafeSOQL.getId(name);
    }

    global  UnsafeGlobal(String name) {
        this.unsafeName = name;

    }


    global Id name3(String name) {
        return UnsafeSOQL.getId(name);
    }
}