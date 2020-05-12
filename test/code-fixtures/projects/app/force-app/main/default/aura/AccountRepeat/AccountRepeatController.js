({
	get_SimpleAccts : function(cmp) {
        var action = cmp.get("c.getSimpleAccounts");
        action.setCallback(this, function(response) {
            var state = response.getState();
            if (state === "SUCCESS") {
                cmp.set("v.saccounts", response.getReturnValue());
            }
        });
        $A.enqueueAction(action);	
	},
	get_objs : function(cmp) {
        var action = cmp.get("c.getObjs");
        action.setCallback(this, function(response) {
            var state = response.getState();
            if (state === "SUCCESS") {
                cmp.set("v.customobjects", response.getReturnValue());
            }
        });
        $A.enqueueAction(action);	
	},
    get_accts : function(cmp) {
        var action = cmp.get("c.getAccts");
        action.setCallback(this, function(response) {
            var state = response.getState();
            if (state === "SUCCESS") {
                cmp.set("v.accounts", response.getReturnValue());
            }
        });
        $A.enqueueAction(action);	
	}
    
})