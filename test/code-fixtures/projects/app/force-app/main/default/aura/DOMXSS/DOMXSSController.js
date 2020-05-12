({
	testHref : function(cmp, evt, hlp) {
        cmp.set("v.href","javascript:alert(document.cookie)");
	},
    testFrameSrc : function(cmp, evt, hlp) {
		cmp.set("v.frameSrc","javascript:alert(document.cookie)");
	},
    testLocation: function(cmp,evt,hlp) {
        var taint = cmp.get("v.taint");
        document.location = taint;
    },
    testInnerHTMLController: function(cmp,evt,hlp) {
        var taint = cmp.get("v.taint");
        var el = cmp.find("target_div").getElement();
        el.innerHTML = taint; //sfdc:sink13
    },
    testAttributeController: function(cmp,evt,hlp) {
        var taint = cmp.get("v.taint");
        var el = cmp.find("target_div").getElement();
        var el2 = document.createElement("iframe");
        el2.src=taint; //sfdc:sink8
        el.appendChild(el2);
    },
    testAttr2AttrFlow: function(cmp,evt,hlp) {
        var a = cmp.get('v.href');
        cmp.set('v.taint2',a);
    },
    handleEvent: function(cmp, evt, hlp) {
        cmp.set("v.evtMessage", "received component with name: " + evt.getName());
        cmp.set("v.evtHref", evt.getParam("message"))
    }
})