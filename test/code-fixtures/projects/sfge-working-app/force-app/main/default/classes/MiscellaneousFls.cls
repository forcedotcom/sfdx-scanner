public with sharing class MiscellaneousFls {
	public String flsNoEnforcementAttempted() {
		// Because this method isn't aura-enabled, doesn't return a PageReference, and isn't a public method on a known
		// VF controller, this method is not considered a valid entrypoint. It will only be picked up by the analyzer if
		// it's part of a path from a valid entrypoint.
		Account[] accs = [SELECT Phone FROM Account];
		return accs.get(0).Phone;
	}
}
