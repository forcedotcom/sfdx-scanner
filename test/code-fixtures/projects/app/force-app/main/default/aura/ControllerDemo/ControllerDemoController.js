({
	getMyId : function(cmp, evt, hlp) {
		var action = cmp.get("c.getId");
        action.setParams({
            "name" : cmp.get("v.Name")
        });
        action.setCallback(this, function(response) {
            if (response.getState() == "SUCCESS") {
                cmp.set("v.Id", response.getReturnValue());
            } else {
                window.alert("error!");
            }
        });
        $A.enqueueAction(action);
	},
    setMyName : function(cmp, evt, hlp) {
        var action = cmp.get("c.setName");
        action.setParams({
            "name" : cmp.get("v.toUpdate"),
            "id" : cmp.get("v.Id")    
        });
        action.setCallback(this, function(response) {
            if (response.getState() == "SUCCESS") {
                window.alert("SUCCESS!");
            } else {
                window.alert("ERROR!");
            }
        });
        $A.enqueueAction(action);
    }
 
})