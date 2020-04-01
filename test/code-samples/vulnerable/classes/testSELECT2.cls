public with sharing class testSELECT2 {
    public String id { get; set; }

    public testSELECT2() {
         this.id = ApexPages.currentPage().getParameters().get('id');
        
    }



    public string getName() {

        return [SELECT Name FROM Account 
                    WHERE Id=:id ][0].Name;
    }
}