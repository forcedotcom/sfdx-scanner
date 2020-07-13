public class UnsafeSOQL {

        public static Id getId(String name) {
            List<Account> acc = Database.query('SELECT Id FROM Account WHERE Name LIKE \'%' + name + '%\''); //sfdc:sink3 source9
            return acc[0].Id;
        }
}