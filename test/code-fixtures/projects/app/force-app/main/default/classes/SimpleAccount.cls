public class SimpleAccount {
    //used to serialize data passed back to aura client
    @AuraEnabled public String Id { get; set; }
    @AuraEnabled public String Name { get; set; }
    public String Phone { get; set; }
    
    // Trivial constructor, for server-side Apex -> client-side JavaScript
    public SimpleAccount(String id, String name, String phone) {
        this.Id = id;
        this.Name = name;
        this.Phone = phone;
    }
    
    // Default, no-arg constructor, for client-side -> server-side
    public SimpleAccount() {}
    
}