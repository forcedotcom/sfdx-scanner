global class UnsafeWebService {

    webservice static Id getName(String name) {
        return UnsafeSOQL.getId(name);
    }
    
}