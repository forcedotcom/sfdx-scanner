Global without sharing class SafeNoSharing { //sfdc:source17 sink14
    global String myvalue;
    global SafeNoSharing(String value) {
        this.myvalue = value;
    }

    global String getValue() {
        return this.myvalue;
    }
    //not a sharing problem because there are no DB operations
    
}