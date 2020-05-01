({
	myAction : function(component, event, helper) {
		var otherComponent = component.find('newcomp');
		otherComponent.addValueHandler({
            event: 'change',
            value: 'v.test2',
            method: function(event) {
                var e = event.getSource();
                alert('THE Secret: ' + e.get('v.secret'));
            }
	     });
    }
})