public with sharing class testSELECT {

    public String id { get; set; }

    public testSELECT() {
         String id = ApexPages.currentPage().getParameters().get('id');
         if (id == null) {
            id = '0016A000002rGJ9';
         }
         this.id = id;
        
    }



    public string getName() {

        return [SELECT Name FROM Account 
                    WHERE Id=:id ].Name;
    }

}