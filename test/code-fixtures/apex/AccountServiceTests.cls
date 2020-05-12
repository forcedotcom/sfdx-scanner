/**
 * Unit tests for <code>AccountService</code> class.
 *
 * @author Max Rudman
 * @since 5/20/2012
 */
@isTest
private class AccountServiceTests {
	private static Account acct;
	private static Product2[] products;
	private static String prefix = Utils.getDeveloperPrefix();
	private static String ACCOUNT = prefix + 'Account__c';
	private static String PRODUCT = prefix + 'Product__c';

	testMethod static void testFindAccountWithSummaries() {
		setUp();
		Set<Id> prodIds = new Map<Id, Product2>(products).keySet();
		AccountService target = new AccountService();
		AccountVO avo = target.findAccountWithSummaries(acct.Id, prodIds);
		System.assert(avo != null);
		System.assertEquals(3, avo.getAssetSummaries(prodIds)[0].quantity);
		System.assertEquals(5, avo.getSubscriptionSummaries(prodIds)[0].quantity);

		System.assert(!avo.isRenewalPricingList());
		System.assert(!avo.isPriceHoldInEffect());
		System.assert(!avo.isAssetQuantitiesCombined());
		System.assert(avo.isCoTerminationNever());
		System.assert(!avo.isCoTerminationPrompt());
	}

	testMethod static void testFindAccountWithSummariesWithConstraints() {
		setUp();

		Set<Id> prodIds = new Map<Id, Product2>(products).keySet();

		Date startDate = System.today().addDays(-4); // Different start date from our original contract
		// Add another contract and subscription
		Contract c = new Contract(AccountId = acct.Id, StartDate = startDate, ContractTerm = 12);
		insert c;

		c.Status = [SELECT MasterLabel FROM ContractStatus WHERE StatusCode = 'Activated' LIMIT 1].MasterLabel;
		update c;

		Subscription__c sub = new Subscription__c(Contract__c = c.Id, Account__c = acct.Id, Product__c = products[1].Id, Quantity__c = 6);
		insert sub;
		String name = sub.Name;

		AccountService target = new AccountService();
		AccountVO avo = target.findAccountWithSummaries(acct.Id, prodIds, new Set<String>{
			String.valueOf(Subscription__c.StartDate__c)
		});
		System.assert(avo != null);

		AccountVO.Summary subscriptionSummary = avo.getSubscriptionSummary(products[1].Id);
		// First, the total quantity should be 10, which is sub1 plus sub2, 5 + 5
		System.assertEquals(11, subscriptionSummary.quantity);

		Decimal constraintQuantity = subscriptionSummary.getConstraintQuantity(String.valueOf(Subscription__c.StartDate__c), startDate);
		// Now, assert that the individual constraint quantity where StartDate__c == our start date only
		// matches the second subscription, with quantity 6
		System.assertEquals(constraintQuantity, 6, subscriptionSummary);
	}

	testMethod static void LIMIT_COUNT_equals_2000() {
		System.assertEquals(2000, AccountService.LIMIT_COUNT);
	}

	testMethod static void getAssetSummaryResults_returns_asset_AggregateResult_with_three_asset_when_LIMIT_COUNT_is_set_to_2000() {
		testAssetSummaryResults(2000);
	}

	testMethod static void getAssetSummaryResults_returns_asset_AggregateResult_with_three_asset_when_LIMIT_COUNT_is_set_to_2() {
		testAssetSummaryResults(2);
	}

	testMethod static void getSubscriptionSummaryResults_returns_subscription_AggregateResult_with_three_subscriptions_when_LIMIT_COUNT_is_set_to_2000() {
		testSubscriptionSummaryResults(2000);
	}

	testMethod static void getSubscriptionSummaryResults_returns_subscription_AggregateResult_with_three_subscriptions_when_LIMIT_COUNT_is_set_to_2() {
		testSubscriptionSummaryResults(2);
	}

	private static void testAssetSummaryResults(Integer queryLimit) {
		setUp();
		Integer originalLimit = AccountService.LIMIT_COUNT;
		AccountService.LIMIT_COUNT = queryLimit;
		try {
			AggregateResult[] results = AccountService.getInstance().getAssetSummaryResults(new Set<Id>{
				acct.Id
			}, new Set<Id>(), new List<Schema.SObjectField>(), 0);
			System.assertEquals(3, results.size());
			System.assertEquals(acct.Id, results[0].get('AccountId'));
			System.assertEquals(products[0].Id, results[0].get('Product2Id'));
			System.assertEquals(3.0, results[0].get('expr0'));
			System.assertEquals(acct.Id, results[1].get('AccountId'));
			System.assertEquals(products[1].Id, results[1].get('Product2Id'));
			System.assertEquals(6.0, results[1].get('expr0'));
			System.assertEquals(acct.Id, results[2].get('AccountId'));
			System.assertEquals(products[2].Id, results[2].get('Product2Id'));
			System.assertEquals(9.0, results[2].get('expr0'));
		} finally {
			AccountService.LIMIT_COUNT = originalLimit;
		}
	}

	private static void testSubscriptionSummaryResults(Integer queryLimit) {
		setUp();
		Integer originalLimit = AccountService.LIMIT_COUNT;
		AccountService.LIMIT_COUNT = queryLimit;
		try {
			AggregateResult[] results = AccountService.getInstance().getSubscriptionSummaryResults(new Set<Id>{
				acct.Id
			}, new Set<Id>(), new List<Schema.SObjectField>(), 0);
			System.assertEquals(3, results.size());
			System.assertEquals(acct.Id, results[0].get(ACCOUNT));
			System.assertEquals(products[0].Id, results[0].get(PRODUCT));
			System.assertEquals(5.0, results[0].get('expr0'));
			System.assertEquals(acct.Id, results[1].get(ACCOUNT));
			System.assertEquals(products[1].Id, results[1].get(PRODUCT));
			System.assertEquals(5.0, results[1].get('expr0'));
			System.assertEquals(acct.Id, results[2].get(ACCOUNT));
			System.assertEquals(products[2].Id, results[2].get(PRODUCT));
			System.assertEquals(5.0, results[2].get('expr0'));
		} finally {
			AccountService.LIMIT_COUNT = originalLimit;
		}
	}

	private static void setUp() {
		acct = new Account(Name = 'Test');
		insert acct;

		products = new Product2[3];
		products[0] = new Product2(Name = 'Asset', IsActive = true);
		products[1] = new Product2(Name = 'Sub', IsActive = true);
		products[2] = new Product2(Name = 'Test', IsActive = true);
		insert products;

		Asset[] assets = new Asset[3];
		assets[0] = new Asset(Name = 'Test 1', AccountId = acct.Id, Product2Id = products[0].Id, Quantity = 3);
		assets[1] = new Asset(Name = 'Test 2', AccountId = acct.Id, Product2Id = products[1].Id, Quantity = 6);
		assets[2] = new Asset(Name = 'Test 3', AccountId = acct.Id, Product2Id = products[2].Id, Quantity = 9);
		insert assets;


		Date startDate = System.today().addDays(-5);
		Contract c = new Contract(AccountId = acct.Id, StartDate = startDate, ContractTerm = 12);
		insert c;

		c.Status = [SELECT MasterLabel FROM ContractStatus WHERE StatusCode = 'Activated' LIMIT 1].MasterLabel;
		update c;

		Subscription__c[] subs = new Subscription__c[3];
		subs[0] = new Subscription__c(Contract__c = c.Id, Account__c = acct.Id, Product__c = products[0].Id, Quantity__c = 5);
		subs[1] = new Subscription__c(Contract__c = c.Id, Account__c = acct.Id, Product__c = products[1].Id, Quantity__c = 5);
		subs[2] = new Subscription__c(Contract__c = c.Id, Account__c = acct.Id, Product__c = products[2].Id, Quantity__c = 5);
		insert subs;
	}
}
