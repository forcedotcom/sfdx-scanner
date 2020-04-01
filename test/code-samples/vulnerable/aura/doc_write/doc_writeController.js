({
	myAction : function(component, event, helper) {
        var url = $A.get('$Resource.test');
        document.write("<script src='" + url + "'></script>");
        var xyz = new XMLHttpRequest();
		console.log("Protocol in Locker: " + xyz.__proto__);
	}
})