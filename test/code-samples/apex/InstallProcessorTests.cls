@isTest
private class InstallProcessorTests {

	/**
	 * Test for creating integration user perm
	 */
	testMethod static void testInstallProcessorBatchAssignPS() {
		InstallProcessorBatchAssignPS installProcessorBatchAssignPS = new InstallProcessorBatchAssignPS();
		try {
			installProcessorBatchAssignPS.createPermissionSet();
		} catch (Exception e) {
			System.assert(false, e);
		}
	}

	/**
	 * Default test for all non-setup, normal data-related installation code
	 */
	private class DataTests implements InstallHandler {
		public void onInstall(InstallContext context) {
			new InstallProcessor(context.previousVersion().major(), context.previousVersion().minor()).onInstallData();
		}
	}

	testMethod static void testInstallData() {
		InstallProcessorLogDAO dao = new InstallProcessorLogDAO();
		InstallProcessorLog__c[] logs = dao.loadByMethodName('createCustomActionsOnQLE');
		System.assert(logs.size() == 0);

		Test.testInstall(new DataTests(), new Version(21, 0), false);

		logs = dao.loadByMethodName('createCustomActionsOnQLE');
		System.assert(logs.size() == 1);
	}

	testMethod static void testEnableLegacyGroupTotalBehavior() {
		System.assertEquals(false, new SettingsVO().shouldExcludeHiddenLinesInTotals());
		Test.testInstall(new DataTests(), new Version(26, 1), false);
		System.assertEquals(false, new SettingsVO().shouldExcludeHiddenLinesInTotals());
		Test.testInstall(new DataTests(), new Version(26, 0), false);
		System.assertEquals(true, new SettingsVO().shouldExcludeHiddenLinesInTotals());
	}

	testMethod static void upgradeFromPre212_ExpectIsLegacyCustomerChecked() {
		// Ensure that the field is false by default.
		System.assertEquals(false, new SettingsVO().generalSettings.IsLegacyCustomer__c);
		// Simulate an upgrade from Version 208, and assert that the field is checked.
		Test.testInstall(new DataTests(), new Version(208, 0), false);
		System.assertEquals(true, new SettingsVO().generalSettings.IsLegacyCustomer__c);
	}

	// Disable until we go live with PSL enforcement
	/*testMethod static void upgradeFromPost212_ExpectIsLegacyCustomerUnchecked() {
		  // Simulate an upgrade from Version 212, and assert that the field false.
		  Test.testInstall(new DataTests(), new Version(212,0), false);
		  System.assertEquals(false, new SettingsVO().generalSettings.IsLegacyCustomer__c);
	  }*/

	testMethod static void upgradeFromPre214_ExpectEnableIntegrationUserChecked() {
		// Ensure that the field is false by default.
		System.assertEquals(false, new SettingsVO().generalSettings.SteelBrickIntegrationUserEnabled__c);
		// Simulate an upgrade from Version 212, and assert that the field is checked.
		Test.testInstall(new DataTests(), new Version(212, 0), false);
//		System.assertEquals(true, new SettingsVO().generalSettings.SteelBrickIntegrationUserEnabled__c);
		// TODO: switch back to asserting true after we re-enable the integration user
		System.assertEquals(false, new SettingsVO().generalSettings.SteelBrickIntegrationUserEnabled__c);
	}

	testMethod static void upgradeFromPre216_ExpectEnableAsyncCalculateUnchecked() {
		System.assertEquals(false, new SettingsVO().generalSettings.AsyncCalculate__c);

		// Simulate an upgrade from Version 214, and assert that the field remains unchecked.
		Test.testInstall(new DataTests(), new Version(214, 0), false);
		System.assertEquals(false, new SettingsVO().generalSettings.AsyncCalculate__c);
	}

	testMethod static void upgradeFromPre216_ExpectEnableAsyncCalculateDisabled() {
		// Set AsyncCalculate__c to true
		GeneralSettings__c userSettings = GeneralSettings__c.getOrgDefaults();
		userSettings.AsyncCalculate__c = true;
		upsert userSettings;

		// Run Post Install and expect AsyncCalculate__c to be false
		Test.testInstall(new DataTests(), new Version(214, 0), false);
		System.assertEquals(false, new SettingsVO().generalSettings.AsyncCalculate__c);
	}

	testMethod static void testMigrateOpps() {
		// Setup
		Opportunity opp = new Opportunity(Name = 'Test', CloseDate = System.today(), StageName = 'Prospecting');
		insert opp;

		TriggerControl.getInstance().setDisabled();
		Quote__c quote = new Quote__c(ShippingCity__c = opp.Id);
		insert quote;
		TriggerControl.getInstance().reset();
		System.assertEquals(null, quote.Opportunity2__c);

		Test.startTest();

		// Test field migration
		new InstallProcessorBatchOpportunity2('ShippingCity__c', null).queue();

		Test.stopTest();

		// Assert we are in Gogurt Country
		quote = [select Opportunity2__c from Quote__c where Id = :quote.Id][0];
		System.assertEquals(opp.Id, quote.Opportunity2__c);

		String status = new InstallProcessor().getExecutionStatus();
		System.assert(status.contains('Completed'));
	}

	testMethod static void testMigrateLookupQueriesFromMD() {
		// Setup
		PriceRule__c pr = new PriceRule__c(Name = 'Test');
		insert pr;

		LookupQuery__c lq = new LookupQuery__c(MatchType__c = 'Static Value', TestedValue__c = String.valueOf(pr.Id), Operator__c = 'equals', LookupField__c = String.valueOf(LookupData__c.Category__c));
		insert lq;

		Test.startTest();

		// Test field migration
		new InstallProcessor().migrateToPriceRuleLookup('TestedValue__c');

		Test.stopTest();

		lq = [SELECT PriceRule2__c FROM LookupQuery__c WHERE Id = :lq.Id][0];
		System.assertEquals(pr.Id, lq.PriceRule2__c);
	}

	testMethod static void testMigrateAdvancedConditions() {
		TriggerControl.getInstance().setDisabled();

		PriceRule__c pr = new PriceRule__c(Name = 'Test1');
		insert pr;

		ProductRule__c pdr = new ProductRule__c(Name = 'Test2', Scope__c = 'Product', EvaluationEvent__c = 'Save');
		insert pdr;

		QuoteTerm__c qt = new QuoteTerm__c(PrintOrder__c = 1);
		insert qt;

		TriggerControl.getInstance().reset();

		Test.startTest();
		new InstallProcessor().migrateAdvancedConditions('Name');
		Test.stopTest();

		pr = [SELECT AdvancedCondition__c FROM PriceRule__c WHERE Id = :pr.Id LIMIT 1];
		pdr = [SELECT AdvancedCondition__c FROM ProductRule__c WHERE Id = :pdr.Id LIMIT 1];
		qt = [SELECT AdvancedCondition__c FROM QuoteTerm__c WHERE Id = :qt.Id LIMIT 1];

		System.assertEquals(pr.AdvancedCondition__c, 'Test1');
		System.assertEquals(pdr.AdvancedCondition__c, 'Test2');
		System.assertNotEquals(qt.AdvancedCondition__c, null);
	}

	testMethod static void testMigrateConditionNumbers() {
		TriggerControl.getInstance().setDisabled();

		PriceRule__c pr = new PriceRule__c(Name = 'Test1', AdvancedCondition__c = '(0000001 AND 0000004) OR (0000001 AND 0000005)');
		insert pr;
		PriceCondition__c pc = new PriceCondition__c(Rule__c = pr.Id, Object__c = 'Quote Line', Field__c = 'Product Code', Operator__c = 'not equals', FilterType__c = 'Value', Value__c = 'FAKE');
		insert pc;

		ProductRule__c pdr = new ProductRule__c(Name = 'Test2', AdvancedCondition__c = '(0000001 AND 0000004) OR (0000001 AND 0000005)', Scope__c = 'Quote', EvaluationEvent__c = 'Save');
		insert pdr;
		ErrorCondition__c ec = new ErrorCondition__c(Rule__c = pdr.Id, TestedObject__c = 'Quote', TestedField__c = 'Group Line Items', Operator__c = 'equals', FilterType__c = 'Value', FilterValue__c = 'false');
		insert ec;

		QuoteTerm__c qt = new QuoteTerm__c(PrintOrder__c = 1, AdvancedCondition__c = '(0000001 AND 0000004) OR (0000001 AND 0000005)');
		insert qt;
		TermCondition__c tc = new TermCondition__c(QuoteTerm__c = qt.Id, TestedField__c = 'List Amount', Operator__c = 'equals', Value__c = '0');
		insert tc;

		TriggerControl.getInstance().reset();

		Test.startTest();
		new InstallProcessor().migrateConditionNumbers('Name');
		Test.stopTest();

		pc = [SELECT Name, Index__c FROM PriceCondition__c WHERE Rule__c = :pr.Id LIMIT 1];
		ec = [SELECT Name, Index__c FROM ErrorCondition__c WHERE Rule__c = :pdr.Id LIMIT 1];
		tc = [SELECT Name, Index__c FROM TermCondition__c WHERE QuoteTerm__c = :qt.Id LIMIT 1];

		pr = [SELECT AdvancedCondition__c FROM PriceRule__c WHERE Id = :pr.Id];
		pdr = [SELECT AdvancedCondition__c FROM ProductRule__c WHERE Id = :pdr.Id];
		qt = [SELECT AdvancedCondition__c FROM QuoteTerm__c WHERE Id = :qt.Id];

		System.assertEquals(Integer.valueOf(pc.Name), pc.Index__c);
		System.assertEquals(Integer.valueOf(ec.Name), ec.Index__c);
		System.assertEquals(Integer.valueOf(tc.Name), tc.Index__c);

		System.assertEquals(pr.AdvancedCondition__c, '(1 AND 4) OR (1 AND 5)');
		System.assertEquals(pdr.AdvancedCondition__c, '(1 AND 4) OR (1 AND 5)');
		System.assertEquals(qt.AdvancedCondition__c, '(1 AND 4) OR (1 AND 5)');
	}

	testMethod static void testPopulateHasConfigurationAttributes() {
		Product2 p = new Product2(Name = 'Blowfish');
		insert p;

		ConfigurationAttribute__c ca = new ConfigurationAttribute__c(Product__c = p.Id, DisplayOrder__c = 10);
		insert ca;

		p.HasConfigurationAttributes__c = false;
		update p;

		p = [SELECT HasConfigurationAttributes__c FROM Product2 WHERE Id = :p.Id][0];
		System.assert(!p.HasConfigurationAttributes__c);

		new InstallProcessor().populateHasConfigurationAttributesFlag();

		p = [SELECT HasConfigurationAttributes__c FROM Product2 WHERE Id = :p.Id][0];
		System.assert(p.HasConfigurationAttributes__c);
	}

	testMethod static void testPopulateSubscriptionRegularPrice() {
		Product2 p = new Product2(Name = 'Test');
		insert p;

		ProductVO pvo = new ProductVO(p);

		QuoteTestsBase.setUp();
		QuoteTestsBase.setUpPrices(new Product2[]{
			p
		});

		QuoteLine__c line = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, RegularPrice__c = 100);
		insert line;

		Subscription__c sub = new Subscription__c(Product__c = p.Id, QuoteLine__c = line.Id, Quantity__c = 1);
		insert sub;
		Test.startTest();
		new InstallProcessorBatchRegularPrice(null).queue();
		Test.stopTest();
		sub = [SELECT RegularPrice__c FROM Subscription__c WHERE Id = :sub.Id LIMIT 1];
		System.assertEquals(100, sub.RegularPrice__c);
	}

	testMethod static void testMigrateRequiredByAsset() {
		Account act = new Account(Name = 'Account');
		insert act;

		Product2 p = new Product2(Name = 'Test');
		insert p;

		Asset ast1 = new Asset(Name = 'Child Asset of Asset', AccountId = act.Id);
		Asset ast2 = new Asset(Name = 'Child Asset of Subscription', AccountId = act.Id);
		Asset ast3 = new Asset(Name = 'Child Asset of Asset Too', AccountId = act.Id);
		Asset parentAst = new Asset(Name = 'Parent Asset', AccountId = act.Id);
		insert new Asset[]{
			ast1, ast2, ast3, parentAst
		};

		Subscription__c parentSub = new Subscription__c(Product__c = p.Id, Quantity__c = 1);
		insert parentSub;

		ast1.RequiredById__c = parentAst.Id;
		ast2.RequiredById__c = parentSub.Id;
		ast3.RequiredById__c = parentAst.Id;
		ast3.RequiredByAsset__c = parentAst.Id;
		update new Asset[]{
			ast1, ast2, ast3
		};

		Test.startTest();
		new InstallProcessorBatchRequiredByAsset(null).queue();
		Test.stopTest();
		ast1 = [SELECT RequiredByAsset__c, RootAsset__c FROM Asset WHERE Id = :ast1.Id LIMIT 1];
		System.assertEquals(parentAst.Id, ast1.RequiredByAsset__c);
		System.assertEquals(parentAst.Id, ast1.RootAsset__c);
		Id ast2Id = ast2.Id;
		ast2 = Database.query('SELECT ' + ServiceCloud.getAssetRequiredByLineItemField() + ' FROM Asset WHERE Id = :ast2Id LIMIT 1');
		System.assertEquals(parentSub.Id, ast2.get(ServiceCloud.getAssetRequiredByLineItemField()));
		ast3 = [SELECT RequiredByAsset__c, RootAsset__c FROM Asset WHERE Id = :ast3.Id LIMIT 1];
		System.assertEquals(parentAst.Id, ast3.RootAsset__c);
	}

	testMethod static void testCreateCustomActionsOnQLE() {
		CustomAction__c action = new CustomAction__c(Name = 'Add Bundles', Label__c = 'Add Products', Page__c = 'Quote Line Editor', DisplayOrder__c = 15);
		insert action;

		Test.testInstall(new DataTests(), new Version(21, 0), false);

		CustomAction__c[] customActions = [SELECT Id, Action__c, Type__c, ParentCustomAction__c FROM CustomAction__c WHERE Active__c = true AND Page__c = 'Quote Line Editor' ORDER BY DisplayOrder__c];
		// 31 new custom actions are created
		System.assertEquals(32, customActions.size());
		// Existing custom action is updated for the new fields on the Custom Action object
		// First action is the existing action since it has the smallest display order. The custom actions created via the install script has display order >= 900.
		System.assertEquals('Menu', customActions[0].Type__c);
		System.assertEquals('Add Products', customActions[0].Action__c);
		System.assertEquals(customActions[0].Id, customActions[1].ParentCustomAction__c);
	}

	testMethod static void testCreateAmendSubscriptionsOnQLE() {
		// Create Amend Subscriptions Custom Action to mimic existing action
		CustomAction__c[] actions = new CustomAction__c[]{
			new CustomAction__c(Name = 'Add Bundles', Label__c = 'Add Products', Page__c = 'Quote Line Editor', DisplayOrder__c = 15),
			new CustomAction__c(Action__c = 'Amend Subscriptions', Active__c = true, Label__c = 'Amend Subscriptions', Name = 'Amend Subscriptions', Location__c = 'Quote/Group', Type__c = 'Button', Page__c = 'Quote Line Editor', DisplayOrder__c = 999)
		};
		insert actions;

		// Test upgrading from 216
		Test.testInstall(new DataTests(), new Version(216, 0), false);

		CustomAction__c[] customActions = [SELECT Id FROM CustomAction__c WHERE Active__c = true AND Page__c = 'Quote Line Editor' AND Action__c = 'Amend Subscriptions'];
		// Should only have one Amend Subscriptions action
		System.assertEquals(1, customActions.size());
	}

	testMethod static void testPopulateAssetCombineKeyValues() {
		Account act = new Account(Name = 'PopulateAssetCombineKeyValues');
		insert act;

		Product2[] products = new Product2[]{
			new Product2(Name = 'Product 1', ProductCode = 'P1', Component__c = false, IsActive = true),
			new Product2(Name = 'Product 2', ProductCode = 'P2', Component__c = false, IsActive = true),
			new Product2(Name = 'Product 3', ProductCode = 'P3', Component__c = false, IsActive = true)
		};
		insert products;

		//ProductVO pvo = new ProductVO(p);

		QuoteTestsBase.setUp();
		QuoteTestsBase.setUpPrices(products);

		QuoteLine__c[] lines = new QuoteLine__c[]{
			new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = products[0].Id, RegularPrice__c = 100),
			new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = products[1].Id, RegularPrice__c = 100),
			new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = products[2].Id, RegularPrice__c = 100)
		};
		insert lines;

		Asset[] assets = new Asset[]{
			new Asset(Name = 'QuoteLine and LatestQuoteLine', AccountId = act.Id, QuoteLine__c = lines[0].Id, LatestQuoteLine__c = lines[1].Id),
			new Asset(Name = 'QuoteLine only', AccountId = act.Id, QuoteLine__c = lines[2].Id),
			new Asset(Name = 'No Original Line', AccountId = act.Id)
		};
		insert assets;

		Set<Id> assetIds = new Set<Id>();
		for (Asset eachAsset : assets)
			assetIds.add(eachAsset.Id);

		Test.startTest();
		new InstallProcessorBatchAssetCombineKey(null).queue();
		Test.stopTest();

		List<Asset> testedAssets = [SELECT Name, CombineKey__c FROM Asset WHERE Id IN :assetIds];

		for (Asset testedAsset : testedAssets) {
			if (testedAsset.Name == 'QuoteLine and LatestQuoteLine')
				System.assertEquals(lines[1].Id, testedAsset.CombineKey__c); else if (testedAsset.Name == 'QuoteLine only')
				System.assertEquals(lines[2].Id, testedAsset.CombineKey__c); else
				System.assertEquals(null, testedAsset.CombineKey__c);
		}
	}

	testMethod static void testUpdateConsumptionFlag() {
		TriggerControl.getInstance().setDisabled();

		List<Product2> products = new Product2[]{
			new Product2(Name = 'Product 1', ProductCode = 'P1', Component__c = false, IsActive = true),
			new Product2(Name = 'Product 2', ProductCode = 'P2', Component__c = false, IsActive = true)
		};
		insert products;
		ConsumptionSchedule consumptionScheduleA = new ConsumptionSchedule(Name = 'CS 1', IsActive = false, RatingMethod = 'tier', Type = 'Range', BillingTerm = 12, BillingTermUnit = 'Month');
		ConsumptionSchedule consumptionScheduleB = new ConsumptionSchedule(Name = 'CS 2', IsActive = false, RatingMethod = 'tier', Type = 'Range', BillingTerm = 12, BillingTermUnit = 'Month');
		List<ConsumptionSchedule> schedules = new List<ConsumptionSchedule>{
			consumptionScheduleA, consumptionScheduleB
		};
		insert schedules;

		ConsumptionRate rateA = new ConsumptionRate(ConsumptionScheduleId = consumptionScheduleA.Id, ProcessingOrder = 1, LowerBound = 0, UpperBound = 10, Price = 10, PricingMethod = 'PerUnit');
		ConsumptionRate rateB = new ConsumptionRate(ConsumptionScheduleId = consumptionScheduleB.Id, ProcessingOrder = 1, LowerBound = 0, UpperBound = 10, Price = 10, PricingMethod = 'PerUnit');
		List<ConsumptionRate> rates = new List<ConsumptionRate>{
			rateA, rateB
		};
		insert rates;

		consumptionScheduleA.IsActive = true;
		consumptionScheduleB.IsActive = true;
		List<ConsumptionSchedule> updateSchedules = new List<ConsumptionSchedule>{
			consumptionScheduleA, consumptionScheduleB
		};
		update updateSchedules;


		List<ProductConsumptionSchedule> junctions = new List<ProductConsumptionSchedule>{
			new ProductConsumptionSchedule(ProductId = products[0].Id, ConsumptionScheduleId = schedules[0].Id),
			new ProductConsumptionSchedule(ProductId = products[1].Id, ConsumptionScheduleId = schedules[1].Id)
		};
		insert junctions;

		Test.startTest();
		new InstallProcessorBatchHasCS(null).queue();
		Test.stopTest();

		List<Product2> testedProducts = [
			SELECT Id, HasConsumptionSchedule__c
			FROM Product2
			WHERE Name IN :new Set<String>{
				'Product 1', 'Product 2'
			}
		];
		for (Product2 product : testedProducts) {
			System.assertEquals(true, product.HasConsumptionSchedule__c);
		}

	}

	testMethod static void createSearchIndexRecords() {
		Product2[] products = new Product2[]{
			new Product2(Name = 'Product 1', ProductCode = 'P1', Component__c = false, IsActive = true),
			new Product2(Name = 'Product 2', ProductCode = 'P2', Component__c = false, IsActive = true),
			new Product2(Name = 'Product 3', ProductCode = 'P3', Component__c = false, IsActive = true),
			new Product2(Name = 'Product 4', ProductCode = 'P4', Component__c = false, IsActive = true)
		};
		insert products;

		ProductOption__c option = new ProductOption__c(Number__c = 1, ConfiguredSKU__c = products.get(0).Id, OptionalSKU__c = products.get(1).Id);
		insert option;
		TriggerControl.getInstance().setDisabled();
		Localization__c[] localizations = new Localization__c[]{
			new Localization__c(APIName__c = 'field_1__c', Language__c = 'en', Text__c = '123', Product__c = products.get(0).Id),
			new Localization__c(APIName__c = 'field_2__c', Language__c = 'en', TextArea__c = '456', Product__c = products.get(1).Id),
			new Localization__c(APIName__c = 'field_3__c', Language__c = 'en', LongTextArea__c = '789', Product__c = products.get(2).Id),
			new Localization__c(APIName__c = 'field_4__c', Language__c = 'en', RichTextArea__c = '101', Product__c = products.get(3).Id),
			new Localization__c(APIName__c = 'field_5__c', Language__c = 'en', Text__c = '112', ProductOption__c = option.Id)
		};
		insert localizations;

		Test.startTest();
		new InstallProcessorBatchLocalizationIndex(null).queue();
		Test.stopTest();

		List<SearchIndex__c> indexResults = [SELECT Id, Value__c, Localization__c FROM SearchIndex__c];
		System.assert(indexResults.size() == 4);

		for (SearchIndex__c indexRecord : indexResults) {
			System.assert(indexRecord.Localization__c != null);
			System.assert(indexRecord.Value__c != null);
		}
	}

	testMethod static void newThemeDocumentWithDefaultValuesForNewThemePropertiesGeneratedOnUpgrade() {
		List<Document> documents = new List<Document>();
		List<Theme__c> themes = new List<Theme__c>();

		documents.add(new Document(AuthorId = UserInfo.getUserId(), FolderId = UserInfo.getUserId(), Name = 'Document1'));
		documents.add(new Document(AuthorId = UserInfo.getUserId(), FolderId = UserInfo.getUserId(), Name = 'Document2'));
		documents.add(new Document(AuthorId = UserInfo.getUserId(), FolderId = UserInfo.getUserId(), Name = 'Document3'));
		insert documents;

		themes.add(new Theme__c(DocumentId__c = documents[0].Id));
		themes.add(new Theme__c(DocumentId__c = documents[1].Id));
		themes.add(new Theme__c(DocumentId__c = documents[2].Id));
		insert themes;

		Test.startTest();
		new InstallProcessorBatchThemeGenerator(null, 100).queue();
		Test.stopTest();

		Theme__c[] themesWithoutDocumentId = [SELECT Id FROM Theme__c WHERE UI2DocumentId__c = null];
		System.assert(themesWithoutDocumentId.isEmpty());
	}

	testMethod static void setThemeDocumentIdIfDocumentGeneratedForThemeSetInUserSettings() {
		Document document = new Document(AuthorId = UserInfo.getUserId(), FolderId = UserInfo.getUserId(), Name = 'Document1');
		insert document;

		Theme__c theme = new Theme__c(DocumentId__c = document.Id);
		insert theme;

		GeneralSettings__c userSettings = GeneralSettings__c.getInstance();
		userSettings.Theme__c = theme.Id;
		insert userSettings;

		new InstallProcessorBatchThemeGenerator(null, 100).setThemeDocumentIdIfDocumentGeneratedForSetTheme(GeneralSettings__c.getInstance(), theme.Id, document.Id);

		System.assert(GeneralSettings__c.getInstance().ThemeDocumentId__c == document.Id);
	}

	testMethod static void setThemeDocumentIdIfDocumentGeneratedForThemeSetInOrgSettings() {
		Document document = new Document(AuthorId = UserInfo.getUserId(), FolderId = UserInfo.getUserId(), Name = 'Document1');
		insert document;

		Theme__c theme = new Theme__c(DocumentId__c = document.Id);
		insert theme;

		GeneralSettings__c orgSettings = GeneralSettings__c.getOrgDefaults();
		orgSettings.Theme__c = theme.Id;
		insert orgSettings;

		new InstallProcessorBatchThemeGenerator(null, 100).setThemeDocumentIdIfDocumentGeneratedForSetTheme(GeneralSettings__c.getOrgDefaults(), theme.Id, document.Id);

		System.assert(GeneralSettings__c.getOrgDefaults().ThemeDocumentId__c == document.Id);
	}

	/**
	   *  Test to make sure the setting is set to false for the older org
	   */
	testMethod static void enableRenewalPOTDuringOrderContract() {
		GeneralSettings__c instance = GeneralSettings__c.getOrgDefaults();

		// Make the value to false and do a fresh install
		instance.EnableRenewalPOTOrderContract__c = false;
		upsert instance;

		Test.testInstall(new InstallProcessor(), null, false);
		instance = GeneralSettings__c.getOrgDefaults();
		System.assert(instance.EnableRenewalPOTOrderContract__c == false, 'EnableRenewalPOTOrderContract should be false because install should not change the data');

		// Make the value to false and do a upgrade install
		instance.EnableRenewalPOTOrderContract__c = true;
		upsert instance;

		//Simulate an upgrade from 218 to 220
		Test.testInstall(new InstallProcessor(), new Version(218, 0), false);
		instance = GeneralSettings__c.getOrgDefaults();
		System.assert(instance.EnableRenewalPOTOrderContract__c == true, 'EnableRenewalPOTOrderContract should be true for default ');


	}

	testMethod static void enableLegacyContractServiceShouldEnableLegacyContractService() {
		GeneralSettings__c instance = GeneralSettings__c.getOrgDefaults();
		instance.UseLegacyContractService__c = false;
		upsert instance;

		System.assert(GeneralSettings__c.getOrgDefaults().UseLegacyContractService__c == false);
		Test.testInstall(new DataTests(), new Version(210, 0), false);
		System.assert(GeneralSettings__c.getOrgDefaults().UseLegacyContractService__c == false);
		instance.UseLegacyContractService__c = true;
		update instance;

		System.assert(GeneralSettings__c.getOrgDefaults().UseLegacyContractService__c == true);
		Test.testInstall(new DataTests(), new Version(208, 0), false);
		System.assert(GeneralSettings__c.getOrgDefaults().UseLegacyContractService__c == true);
		instance.UseLegacyContractService__c = false;
		update instance;

		System.assert(GeneralSettings__c.getOrgDefaults().UseLegacyContractService__c == false);
		Test.testInstall(new DataTests(), new Version(208, 0), false);
		System.assert(GeneralSettings__c.getOrgDefaults().UseLegacyContractService__c == true);
	}

	testMethod static void api_salt_gets_set() {
		new InstallProcessor().setPublicAPISalt();
		System.assertNotEquals(null, GeneralSettings__c.getOrgDefaults().PublicAPISalt__c);
		System.assertEquals(50, GeneralSettings__c.getOrgDefaults().PublicAPISalt__c.length());
	}

	testMethod static void api_salt_does_NOT_get_overwritten() {
		String existingValue = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmn';
		GeneralSettings__c instance = GeneralSettings__c.getOrgDefaults();
		instance.PublicAPISalt__c = existingValue;
		upsert instance;
		new InstallProcessor().setPublicAPISalt();

		System.assertNotEquals(null, GeneralSettings__c.getOrgDefaults().PublicAPISalt__c);
		System.assertEquals(existingValue, GeneralSettings__c.getOrgDefaults().PublicAPISalt__c);
	}

	testMethod static void testPopulateOrderItemQuotedListPrice() {
		Product2 p = new Product2(Name = 'Test');
		insert p;

		ProductVO pvo = new ProductVO(p);

		QuoteTestsBase.setUp();
		QuoteTestsBase.setUpPrices(new Product2[]{
			p
		});

		QuoteLine__c line = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100);
		insert line;

		QuoteTestsBase.quote.Primary__c = true;
		update QuoteTestsBase.quote;

		Order order = new Order(AccountId = QuoteTestsBase.account.Id, Quote__c = QuoteTestsBase.quote.Id,
			EffectiveDate = OrderTestBase.getStartDate(), Status = 'Draft',
			Pricebook2Id = QuoteTestsBase.pricebook.Id);
		insert order;

		PricebookEntry pricebookEntry = [
			SELECT Id
			FROM PricebookEntry
			WHERE Pricebook2Id = :order.Pricebook2Id AND
			Product2Id = :p.Id
			LIMIT 1
		];
		OrderItem orderItem = new OrderItem(OrderId = order.Id, QuoteLine__c = line.Id, Quantity = 1,
			PricebookEntryId = pricebookEntry.Id, UnitPrice = 10);
		insert orderItem;
		System.assertEquals(null, orderItem.QuotedListPrice__c);

		Test.startTest();
		Test.testInstall(new InstallProcessor(), new Version(212, 0), false);
		Test.stopTest();

		orderItem = [SELECT QuotedListPrice__c FROM OrderItem WHERE Id = :orderItem.Id LIMIT 1];
		System.assertEquals(100, orderItem.QuotedListPrice__c);
	}

	testMethod static void testPopulateOrderItemProrateMultiplier() {
		Product2 p = new Product2(Name = 'TestOrderItemProrateMultiplier');
		insert p;

		ProductVO pvo = new ProductVO(p);

		QuoteTestsBase.setUp();
		QuoteTestsBase.setUpPrices(new Product2[]{
			p
		});

		QuoteLine__c line = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id,
			ListPrice__c = 100, ProrateMultiplier__c = 1);
		insert line;

		QuoteTestsBase.quote.Primary__c = true;
		update QuoteTestsBase.quote;

		Order order = new Order(AccountId = QuoteTestsBase.account.Id, Quote__c = QuoteTestsBase.quote.Id,
			EffectiveDate = OrderTestBase.getStartDate(), Status = 'Draft',
			Pricebook2Id = QuoteTestsBase.pricebook.Id);
		insert order;

		PricebookEntry pricebookEntry = [
			SELECT Id
			FROM PricebookEntry
			WHERE Pricebook2Id = :order.Pricebook2Id AND
			Product2Id = :p.Id
			LIMIT 1
		];
		OrderItem orderItem = new OrderItem(OrderId = order.Id, QuoteLine__c = line.Id, Quantity = 1,
			PricebookEntryId = pricebookEntry.Id, UnitPrice = 10, QuotedListPrice__c = 100);
		insert orderItem;
		System.assertEquals(null, orderItem.ProrateMultiplier__c);

		Test.startTest();
		Test.testInstall(new InstallProcessor(), new Version(212, 0), false);
		Test.stopTest();

		orderItem = [SELECT ProrateMultiplier__c FROM OrderItem WHERE Id = :orderItem.Id LIMIT 1];
		System.assertEquals(1, orderItem.ProrateMultiplier__c);
	}

	testMethod static void testPopulateQuoteLinePriceBookEntry() {

		//Data Setup
		Account account = new Account(Name = 'PopulateQuoteLinePriceBookEntry');
		insert account;

		//Create Pricebooks
		Pricebook2[] pricebooks = new Pricebook2[]{
			new Pricebook2(Name = 'TestPricebook 1', IsActive = true),
			new Pricebook2(Name = 'TestPricebook 2', IsActive = true),
			new Pricebook2(Name = 'TestPricebook 3', IsActive = true)
		};
		insert pricebooks;

		OpportunityStage os = [SELECT MasterLabel FROM OpportunityStage WHERE IsClosed = false AND IsActive = true LIMIT 1];
		opportunity[] opportunities = new Opportunity[]{
			new Opportunity(Name = 'Test 1', StageName = os.MasterLabel, CloseDate = System.today(), AccountId = account.Id, Pricebook2Id = pricebooks[0].Id),
			new Opportunity(Name = 'Test 2', StageName = os.MasterLabel, CloseDate = System.today(), AccountId = account.Id, Pricebook2Id = pricebooks[1].Id),
			new Opportunity(Name = 'Test 3', StageName = os.MasterLabel, CloseDate = System.today(), AccountId = account.Id, Pricebook2Id = pricebooks[2].Id),
			new Opportunity(Name = 'Test 4', StageName = os.MasterLabel, CloseDate = System.today(), AccountId = account.Id, Pricebook2Id = pricebooks[2].Id)
		};
		insert opportunities;

		Product2[] products = new Product2[]{
			new Product2(Name = 'Product 1', ProductCode = 'P1', IsActive = true),
			new Product2(Name = 'Product 2', ProductCode = 'P2', IsActive = true),
			new Product2(Name = 'Product 3', ProductCode = 'P3', IsActive = true),
			new Product2(Name = 'Product 4', ProductCode = 'P4', IsActive = true)
		};
		insert products;

		QuoteTestsBase.insertStandardPricebookEntries(products);

		// Insert pricebook prices
		PricebookEntry[] entries = new PricebookEntry[]{
			//pb1
			new PricebookEntry(Product2Id = products[0].Id, Pricebook2Id = pricebooks[0].Id, Product2 = products[0], UnitPrice = 100, IsActive = true),
			new PricebookEntry(Product2Id = products[1].Id, Pricebook2Id = pricebooks[0].Id, Product2 = products[1], UnitPrice = 200, IsActive = true),
			new PricebookEntry(Product2Id = products[2].Id, Pricebook2Id = pricebooks[0].Id, Product2 = products[1], UnitPrice = 300, IsActive = true),
			new PricebookEntry(Product2Id = products[3].Id, Pricebook2Id = pricebooks[0].Id, Product2 = products[1], UnitPrice = 400, IsActive = true),

			//pb2
			new PricebookEntry(Product2Id = products[0].Id, Pricebook2Id = pricebooks[1].Id, Product2 = products[0], UnitPrice = 100, IsActive = true),
			new PricebookEntry(Product2Id = products[1].Id, Pricebook2Id = pricebooks[1].Id, Product2 = products[1], UnitPrice = 200, IsActive = true),
			new PricebookEntry(Product2Id = products[2].Id, Pricebook2Id = pricebooks[1].Id, Product2 = products[2], UnitPrice = 300, IsActive = true),
			new PricebookEntry(Product2Id = products[3].Id, Pricebook2Id = pricebooks[1].Id, Product2 = products[1], UnitPrice = 400, IsActive = true),

			//pb3
			new PricebookEntry(Product2Id = products[2].Id, Pricebook2Id = pricebooks[2].Id, Product2 = products[0], UnitPrice = 100, IsActive = true),
			new PricebookEntry(Product2Id = products[3].Id, Pricebook2Id = pricebooks[2].Id, Product2 = products[1], UnitPrice = 200, IsActive = true)
		};
		insert entries;

		//Create Quotes
		Quote__c[] quotes = new Quote__c[]{
			new Quote__c(Opportunity2__c = opportunities[0].Id, LineItemsGrouped__c = false, PaperSize__c = 'Letter', Primary__c = true),
			new Quote__c(Opportunity2__c = opportunities[1].Id, LineItemsGrouped__c = false, PaperSize__c = 'Letter', Primary__c = true),
			new Quote__c(Opportunity2__c = opportunities[2].Id, LineItemsGrouped__c = false, PaperSize__c = 'Letter', Primary__c = true),
			new Quote__c(Opportunity2__c = opportunities[3].Id, LineItemsGrouped__c = false, PaperSize__c = 'Letter', Primary__c = true),
			new Quote__c(Opportunity2__c = opportunities[3].Id, LineItemsGrouped__c = false, PaperSize__c = 'Letter', Primary__c = false)
		};
		insert quotes;

		//insert Quotelines
		QuoteLine__c[] lines = new QuoteLine__c[]{
			//opp1 Pb1
			new QuoteLine__c(Quote__c = quotes[0].Id, Product__c = products[0].Id, RegularPrice__c = 100),
			new QuoteLine__c(Quote__c = quotes[0].Id, Product__c = products[1].Id, RegularPrice__c = 200),
			new QuoteLine__c(Quote__c = quotes[0].Id, Product__c = products[2].Id, RegularPrice__c = 300),
			new QuoteLine__c(Quote__c = quotes[0].Id, Product__c = products[3].Id, RegularPrice__c = 400),

			//opp2 pb2
			new QuoteLine__c(Quote__c = quotes[1].Id, Product__c = products[0].Id, RegularPrice__c = 100),
			new QuoteLine__c(Quote__c = quotes[1].Id, Product__c = products[1].Id, RegularPrice__c = 200),
			new QuoteLine__c(Quote__c = quotes[1].Id, Product__c = products[2].Id, RegularPrice__c = 300),
			new QuoteLine__c(Quote__c = quotes[1].Id, Product__c = products[3].Id, RegularPrice__c = 400),

			//opp3 pb3
			new QuoteLine__c(Quote__c = quotes[2].Id, Product__c = products[2].Id, RegularPrice__c = 300),
			new QuoteLine__c(Quote__c = quotes[2].Id, Product__c = products[3].Id, RegularPrice__c = 400),

			//opp4 pb3
			new QuoteLine__c(Quote__c = quotes[3].Id, Product__c = products[2].Id, RegularPrice__c = 300),
			new QuoteLine__c(Quote__c = quotes[3].Id, Product__c = products[3].Id, RegularPrice__c = 400),

			//opp4 pb3 non-primary quote
			new QuoteLine__c(Quote__c = quotes[4].Id, Product__c = products[2].Id, RegularPrice__c = 300),
			new QuoteLine__c(Quote__c = quotes[4].Id, Product__c = products[3].Id, RegularPrice__c = 400)
		};
		insert lines;

		Test.startTest();

		//Simulate an upgrade from 212 to 214 all the Quote line should be populated with PBE
		Test.testInstall(new DataTests(), new Version(212, 0), false);

		Test.stopTest();

		QuoteLine__c[] updatedlines = [
			SELECT Id, Product__c, Quote__c, PriceBookEntryId__c
			from QuoteLine__c
			where Quote__c IN :new List<String>{
				quotes[0].Id, quotes[1].Id, quotes[2].Id, quotes[3].Id
			}
		];

		//Assert
		for (QuoteLine__c line : updatedlines) {

			//Quote1
			if (line.Quote__c == quotes[0].Id && line.Product__c == products[0].Id)
				System.assertEquals(entries[0].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[0].Id && line.Product__c == products[1].Id)
				System.assertEquals(entries[1].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[0].Id && line.Product__c == products[2].Id)
				System.assertEquals(entries[2].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[0].Id && line.Product__c == products[3].Id)
				System.assertEquals(entries[3].Id, line.PriceBookEntryId__c);

			//Quote2
			if (line.Quote__c == quotes[1].Id && line.Product__c == products[0].Id)
				System.assertEquals(entries[4].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[1].Id && line.Product__c == products[1].Id)
				System.assertEquals(entries[5].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[1].Id && line.Product__c == products[2].Id)
				System.assertEquals(entries[6].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[1].Id && line.Product__c == products[3].Id)
				System.assertEquals(entries[7].Id, line.PriceBookEntryId__c);

			//Quote3
			if (line.Quote__c == quotes[2].Id && line.Product__c == products[2].Id)
				System.assertEquals(entries[8].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[2].Id && line.Product__c == products[3].Id)
				System.assertEquals(entries[9].Id, line.PriceBookEntryId__c);

			//Quote4
			if (line.Quote__c == quotes[3].Id && line.Product__c == products[2].Id)
				System.assertEquals(entries[8].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[3].Id && line.Product__c == products[3].Id)
				System.assertEquals(entries[9].Id, line.priceBookEntryId__c);

			//Non-primary quote
			if (line.Quote__c == quotes[4].Id && line.Product__c == products[2].Id)
				System.assertEquals(entries[8].Id, line.PriceBookEntryId__c);
			if (line.Quote__c == quotes[4].Id && line.Product__c == products[3].Id)
				System.assertEquals(entries[9].Id, line.priceBookEntryId__c);
		}

	}

	testMethod static void testPopulateOrderItemBundleRoot() {
		TriggerControl.getInstance().setDisabled();

		Product2 p = new Product2(Name = 'testPopulateOrderItemBundleRoot');
		insert p;

		ProductVO pvo = new ProductVO(p);

		QuoteTestsBase.setUp();
		QuoteTestsBase.setUpPrices(new Product2[]{
			p
		});

		QuoteLine__c parentQuoteLine = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100, Bundle__c = true);
		QuoteLine__c childComponentQuoteLine1 = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100, Bundle__c = true, OptionType__c = 'Component');
		QuoteLine__c childAccessoryQuoteLine2 = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100, Bundle__c = true, OptionType__c = 'Accessory');
		QuoteLine__c grandChildComponentQuoteLine1 = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100, OptionType__c = 'Component');
		QuoteLine__c grandChildAccessoryQuoteLine1 = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100, OptionType__c = 'Accessory');
		QuoteLine__c grandChildComponentQuoteLine2 = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100, OptionType__c = 'Component');
		QuoteLine__c grandChildAccessoryQuoteLine2 = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100, OptionType__c = 'Accessory');

		QuoteLine__c[] lines = new List<QuoteLine__c>{
			parentQuoteLine, childComponentQuoteLine1, childAccessoryQuoteLine2, grandChildComponentQuoteLine1, grandChildAccessoryQuoteLine1, grandChildComponentQuoteLine2, grandChildAccessoryQuoteLine2
		};
		insert lines;

		childComponentQuoteLine1.RequiredBy__c = parentQuoteLine.Id;
		childAccessoryQuoteLine2.RequiredBy__c = parentQuoteLine.Id;
		grandChildComponentQuoteLine1.RequiredBy__c = childComponentQuoteLine1.Id;
		grandChildAccessoryQuoteLine1.RequiredBy__c = childComponentQuoteLine1.Id;
		grandChildComponentQuoteLine2.RequiredBy__c = childAccessoryQuoteLine2.Id;
		grandChildAccessoryQuoteLine2.RequiredBy__c = childAccessoryQuoteLine2.Id;

		update lines;

		Order order = new Order(AccountId = QuoteTestsBase.account.Id, Quote__c = QuoteTestsBase.quote.Id, EffectiveDate = OrderTestBase.getStartDate(), Status = 'Draft', Pricebook2Id = QuoteTestsBase.pricebook.Id);
		insert order;

		PricebookEntry pricebookEntry = [SELECT Id FROM PricebookEntry WHERE Pricebook2Id = :order.Pricebook2Id AND Product2Id = :p.Id LIMIT 1];
		OrderItem parent = new OrderItem(OrderId = order.Id, QuoteLine__c = parentQuoteLine.Id, Quantity = 1, PricebookEntryId = pricebookEntry.Id, UnitPrice = 10, QuotedListPrice__c = 100);
		OrderItem childComponent1 = new OrderItem(OrderId = order.Id, QuoteLine__c = childComponentQuoteLine1.Id, Quantity = 1, PricebookEntryId = pricebookEntry.Id, UnitPrice = 10, QuotedListPrice__c = 100);
		OrderItem childAccessory2 = new OrderItem(OrderId = order.Id, QuoteLine__c = childAccessoryQuoteLine2.Id, Quantity = 1, PricebookEntryId = pricebookEntry.Id, UnitPrice = 10, QuotedListPrice__c = 100);
		OrderItem grandChildComponent1 = new OrderItem(OrderId = order.Id, QuoteLine__c = grandChildComponentQuoteLine1.Id, Quantity = 1, PricebookEntryId = pricebookEntry.Id, UnitPrice = 10, QuotedListPrice__c = 100);
		OrderItem grandChildAccessory1 = new OrderItem(OrderId = order.Id, QuoteLine__c = grandChildAccessoryQuoteLine1.Id, Quantity = 1, PricebookEntryId = pricebookEntry.Id, UnitPrice = 10, QuotedListPrice__c = 100);
		OrderItem grandChildComponent2 = new OrderItem(OrderId = order.Id, QuoteLine__c = grandChildComponentQuoteLine2.Id, Quantity = 1, PricebookEntryId = pricebookEntry.Id, UnitPrice = 10, QuotedListPrice__c = 100);
		OrderItem grandChildAccessory2 = new OrderItem(OrderId = order.Id, QuoteLine__c = grandChildAccessoryQuoteLine2.Id, Quantity = 1, PricebookEntryId = pricebookEntry.Id, UnitPrice = 10, QuotedListPrice__c = 100);

		OrderItem[] orderItems = new List<OrderItem>{
			parent, childComponent1, childAccessory2, grandChildComponent1, grandChildAccessory1, grandChildComponent2, grandChildAccessory2
		};
		insert orderItems;

		childComponent1.RequiredBy__c = parent.Id;
		childAccessory2.RequiredBy__c = parent.Id;
		grandChildComponent1.RequiredBy__c = childComponent1.Id;
		grandChildAccessory1.RequiredBy__c = childComponent1.Id;
		grandChildComponent2.RequiredBy__c = childAccessory2.Id;
		grandChildAccessory2.RequiredBy__c = childAccessory2.Id;

		update orderItems;

		Test.startTest();

		//Simulate an upgrade from 212 to 214
		Test.testInstall(new DataTests(), new Version(212, 0), false);

		Test.stopTest();

		Map<Id, OrderItem> orderItemMap = new Map<Id, OrderItem>([SELECT Id, BundleRoot__c FROM OrderItem WHERE OrderId = :order.Id]);

		System.assertEquals(orderItemMap.get(parent.Id).BundleRoot__c, null);
		System.assertEquals(orderItemMap.get(childComponent1.Id).BundleRoot__c, parent.Id);
		System.assertEquals(orderItemMap.get(childAccessory2.Id).BundleRoot__c, null);
		System.assertEquals(orderItemMap.get(grandChildComponent1.Id).BundleRoot__c, parent.Id);
		System.assertEquals(orderItemMap.get(grandChildAccessory1.Id).BundleRoot__c, null);
		System.assertEquals(orderItemMap.get(grandChildComponent2.Id).BundleRoot__c, childAccessory2.Id);
		System.assertEquals(orderItemMap.get(grandChildAccessory2.Id).BundleRoot__c, null);
	}

	testMethod static void testPopulateOrderItemRevisedOrderProductAndContractActionAmend() {
		OrderTestBase testHandle = new OrderTestBase();
		QuoteVO amendmentQuoteVO = testHandle.createAmendmentQuote(12, System.Today());

		//Need to run test by disabling the trigger as the insert of Order Item also sets these fields
		TriggerControl.getInstance().setDisabled();

		Test.startTest();

		List<QuoteLineVO> qliList = amendmentQuoteVO.getLineItems();

		List<QuoteLine__c> updateQliList = new List<QuoteLine__c>();

		for (QuoteLineVO qliVO : qliList) {
			QuoteLine__c qli = (QuoteLine__c) qliVO.record;
			if (qli.ProductName__c.contains('Wireless Router')) {
				qli.Quantity__c = 11;
			}
			if (qli.ProductName__c.contains('Percent of Total Item')) {
				qli.Quantity__c = 0;
			}
			if (qli.ProductName__c.contains('LIST')) {
				qli.Quantity__c = 9;
			}
			updateQliList.add(qli);
		}

		update updateQliList;

		Order amendmentOrderRecord = testHandle.quickCreateOrder(amendmentQuoteVO, System.today());

		//Run the batch job
		Test.testInstall(new InstallProcessor(), new Version(214, 0), false);

		Test.stopTest();

		List<OrderItem> updatedOrderItems = [SELECT Id,QuoteLine__r.ProductName__c,RevisedOrderProduct__c,ContractAction__c,Pricebookentry.Product2Id FROM OrderItem WHERE orderId = :amendmentOrderRecord.Id];
		Map<ID, OrderItem> originalOrderItems = new Map<ID, OrderItem>([SELECT Id,RevisedOrderProduct__c,ContractAction__c,Pricebookentry.Product2Id FROM OrderItem WHERE orderId != :amendmentOrderRecord.Id]);
		for (OrderItem rec : updatedOrderItems) {
			System.assertNotEquals(null, rec.RevisedOrderProduct__c);
			System.assertEquals(originalOrderItems.get(rec.RevisedOrderProduct__c).Pricebookentry.Product2Id, rec.Pricebookentry.Product2Id, 'Revised Order Product on Order Product on amendment is not as expected');
			if (rec.QuoteLine__r.ProductName__c.contains('Wireless Router')) {
				System.assertEquals(OrderConstants.CONTRACT_ACTION_V_QUANTITY_INCREASE, rec.ContractAction__c, 'Contract Action is not set right on Amendment.');
			}
			if (rec.QuoteLine__r.ProductName__c.contains('Percent of Total Item')) {
				System.assertEquals(OrderConstants.CONTRACT_ACTION_V_CANCEL, rec.ContractAction__c, 'Contract Action is not set right on Amendment.');
			}
			if (rec.QuoteLine__r.ProductName__c.contains('LIST')) {
				System.assertEquals(OrderConstants.CONTRACT_ACTION_V_QUANTITY_DECREASE, rec.ContractAction__c, 'Contract Action is not set right on Amendment.');
			}
		}

	}

	testMethod static void testPopulateOrderItemRevisedOrderProductAndContractActionRenew() {

		OrderTestBase testHandle = new OrderTestBase();
		QuoteVO renewalQuote = testHandle.createRenewalQuote(12, System.Today());

		//Need to run test by disabling the trigger as the insert of Order Item also sets these fields
		TriggerControl.getInstance().setDisabled();

		Test.startTest();

		Order renewalOrderRecord = testHandle.quickCreateOrder(renewalQuote, System.today());

		//Run the batch job
		Test.testInstall(new InstallProcessor(), new Version(214, 0), false);
		Test.stopTest();

		List<OrderItem> updatedOrderItems = [SELECT Id,RevisedOrderProduct__c,ContractAction__c,Pricebookentry.Product2Id FROM OrderItem WHERE orderId = :renewalOrderRecord.Id];
		Map<ID, OrderItem> originalOrderItems = new Map<ID, OrderItem>([SELECT Id,RevisedOrderProduct__c,ContractAction__c,Pricebookentry.Product2Id FROM OrderItem WHERE orderId != :renewalOrderRecord.Id]);

		for (OrderItem rec : updatedOrderItems) {
			System.assertNotEquals(null, rec.RevisedOrderProduct__c);
			System.assertEquals(originalOrderItems.get(rec.RevisedOrderProduct__c).Pricebookentry.Product2Id, rec.Pricebookentry.Product2Id, 'Revised Order Product on Order Product on renewal is not as expected');
			System.assertEquals(OrderConstants.CONTRACT_ACTION_V_RENEW, rec.ContractAction__c, 'Contract Action is not set right on Renewal.');
		}
	}

	testMethod static void testUpdateOIsWithAssetsContractsSubs() {
		OrderTestBase testHandle = new OrderTestBase();
		QuoteVO renewalQuote = testHandle.createRenewalQuote(12, System.Today());

		TriggerControl.getInstance().setDisabled();

		Test.startTest();

		Order renewalOrderRecord = testHandle.quickCreateOrder(renewalQuote, System.today());
		List<OrderItem> originalOrderItems = [SELECT Id,Subscription__c,Contract__c,Asset__c FROM OrderItem WHERE orderId != :renewalOrderRecord.Id];
		for (OrderItem rec : originalOrderItems) {
			rec.Subscription__c = null;
			rec.Contract__c = null;
			rec.Asset__c = null;
		}
		update originalOrderItems;

		new BatchOrderItemAssetContractSubs('updateOIsWithAssetsContractsSubs', null).queue();

		Test.stopTest();

		List<OrderItem> updatedOrderItems = [SELECT Id,Subscription__c,Contract__c,Asset__c, QuoteLine__r.ProductName__c FROM OrderItem WHERE orderId != :renewalOrderRecord.Id];
		for (OrderItem rec : updatedOrderItems) {
			if (rec.QuoteLine__r.ProductName__c.contains('Wireless Router')) {
				System.assert(!String.isBlank(rec.Asset__c), 'Install Script should populate Subscription on Order Item record');
			} else {
				System.assert(!String.isBlank(rec.Subscription__c), 'Install Script should populate Subscription on Order Item record');
				System.assert(!String.isBlank(rec.Contract__c), 'Install Script should populate Contract on Order Item record');
			}
		}
	}

	testMethod static void testUpdateIntegrationUserInfo() {
		InstallProcessor.updateIntegrationUserInfo();
	}


	testMethod static void testMapPricingMethodFromQuoteLineToOrderItem() {
		OrderTestBase handle = new OrderTestBase();
		QuoteVO quoteVO = handle.createSimpleQuote(12, new List<String>{
			'Non Subscription Percent of Total Item'
		});
		Id orderId = handle.createOrder(quoteVO.getId());

		OrderItem[] orderItems = [SELECT Id, PricingMethod__c FROM OrderItem WHERE OrderId = :orderId AND PricingMethod__c = 'Percent Of Total'];
		orderItems[0].PricingMethod__c = null;
		update orderItems[0];

		Test.startTest();

		//Simulate an upgrade from 214 to 216
		Test.testInstall(new InstallProcessor(), new Version(214, 0), false);

		Test.stopTest();

		orderItems = [SELECT Id, PricingMethod__c FROM OrderItem WHERE OrderId = :orderId AND PricingMethod__c = 'Percent Of Total'];

		System.assert(orderItems.size() == 1);
	}

	testMethod static void testOrderItemUpdatesFromExecuteButton() {
		Product2 p = new Product2(Name = 'Test');
		insert p;

		ProductVO pvo = new ProductVO(p);

		QuoteTestsBase.setUp();
		QuoteTestsBase.setUpPrices(new Product2[]{
			p
		});

		QuoteLine__c line = new QuoteLine__c(Quote__c = QuoteTestsBase.quote.Id, Product__c = p.Id, ListPrice__c = 100);
		insert line;

		QuoteTestsBase.quote.Primary__c = true;
		update QuoteTestsBase.quote;

		Order order = new Order(AccountId = QuoteTestsBase.account.Id, Quote__c = QuoteTestsBase.quote.Id,
			EffectiveDate = OrderTestBase.getStartDate(), Status = 'Draft',
			Pricebook2Id = QuoteTestsBase.pricebook.Id);
		insert order;

		PricebookEntry pricebookEntry = [
			SELECT Id
			FROM PricebookEntry
			WHERE Pricebook2Id = :order.Pricebook2Id AND
			Product2Id = :p.Id
			LIMIT 1
		];
		OrderItem orderItem = new OrderItem(OrderId = order.Id, QuoteLine__c = line.Id, Quantity = 1,
			PricebookEntryId = pricebookEntry.Id, UnitPrice = 10);
		insert orderItem;
		System.assertEquals(null, orderItem.QuotedListPrice__c);
		System.assertEquals(null, orderItem.ProrateMultiplier__c);

		Test.startTest();
		InstallProcessor.execute();
		Test.stopTest();

		orderItem = [SELECT ProrateMultiplier__c, QuotedListPrice__c FROM OrderItem WHERE Id = :orderItem.Id LIMIT 1];
		System.assertEquals(1, orderItem.ProrateMultiplier__c);
		System.assertEquals(100, orderItem.QuotedListPrice__c);

	}

	testMethod static void testBatchMigrateOrderQuantity() {
		OrderTestBase handle = new OrderTestBase();
		QuoteVO quoteVO = handle.createSimpleQuote(12, new List<String>{
			'LIST', 'Percent of Total Item'
		});

		Id orderId = handle.createOrder(quoteVO.getId());

		OrderItem[] orderItems = [SELECT Id, OrderedQuantity__c, Quantity FROM OrderItem WHERE OrderId = :orderId];
		for (OrderItem oi : orderItems) {
			oi.OrderedQuantity__c = null;
		}

		TriggerControl.getInstance().setDisabled();
		update orderItems;
		TriggerControl.getInstance().reset();

		Test.startTest();

		//Simulate an upgrade from 214 to 216
		Test.testInstall(new InstallProcessor(), new Version(26, 1), false);

		Test.stopTest();

		orderItems = [SELECT Id, OrderedQuantity__c, Quantity FROM OrderItem WHERE OrderId = :orderId];

		for (OrderItem oi : orderItems) {
			System.assertEquals(oi.OrderedQuantity__c, oi.Quantity);
		}
	}

	testMethod static void testFilterExistingActions() {
		List<CustomAction__c> actions = new List<CustomAction__c>();
		Integer index = 900;
		actions.add(new CustomAction__c(Action__c = 'Add Products', Active__c = true, Label__c = 'Add Products', Name = 'Add Products', Location__c = 'Quote/Group', Type__c = 'Menu', Page__c = 'Quote Line Editor', DisplayOrder__c = index++));
		actions.add(new CustomAction__c(Active__c = true, Name = 'Separator', Location__c = 'Quote', Type__c = 'Separator', Page__c = 'Quote Line Editor', DisplayOrder__c = index++));
		actions.add(new CustomAction__c(Action__c = 'Add Favorites', Active__c = true, Label__c = 'Add Favorites', Name = 'Add Favorites', Location__c = 'Quote/Group', Type__c = 'Button', Page__c = 'Quote Line Editor', DisplayOrder__c = index++));
		insert actions;

		String parentId = actions[0].Id;

		List<CustomAction__c> newActions = new List<CustomAction__c>();
		newActions.add(new CustomAction__c(Action__c = 'Add Products', Active__c = true, Label__c = 'Add Products', Name = 'Add Products', Location__c = 'Quote/Group', Type__c = 'Menu', Page__c = 'Quote Line Editor', DisplayOrder__c = index++));
		newActions.add(new CustomAction__c(Active__c = true, Name = 'Separator', Location__c = 'Quote', Type__c = 'Separator', Page__c = 'Quote Line Editor', DisplayOrder__c = index++));
		newActions.add(new CustomAction__c(Action__c = 'Add Favorites', Active__c = true, Label__c = 'Add Favorites', Name = 'Add Favorites', Location__c = 'Quote/Group', Type__c = 'Button', Page__c = 'Quote Line Editor', ParentCustomAction__c = parentId, DisplayOrder__c = index++));

		List<CustomAction__c> filtered = new InstallProcessor().filterExistingActions(newActions);
		System.assertEquals(filtered.size(), 1);
		System.assertEquals(filtered[0].Action__c, 'Add Favorites');
		System.assertEquals(filtered[0].ParentCustomAction__c, parentId);
	}
}
