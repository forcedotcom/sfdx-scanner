global with sharing class SharingInnerClass {
    global SharingInnerClass() {
        
    }

    global class InnerClass { //sfdc:source19
        //Unsafe because it does not inherit sharing policy
        public Account acc;
        global InnerClass(Id id) {
            this.acc = [SELECT Id, Name FROM Account WHERE Id =:id][0];  
        }

        global Account getAccount() {
            return this.acc; //sfdc:sink15
        }
    }
}