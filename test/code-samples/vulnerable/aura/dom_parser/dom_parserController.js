({
	onPress : function(component, event, helper) {
        var elem = component.find('attach').getElement();
        
        var svg_string = '<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">';
        svg_string += '<circle cx="100" cy="100" r="100"/> </svg>';        
        var parser = new DOMParser();
        var doc = parser.parseFromString(svg_string, "image/svg+xml");
        var date = new Date();
        doc.__proto__.__proto__.__proto__.__proto__ = date;
        
        console.log(doc instanceof Date);
        
        var my_elem = doc.documentElement;
        
        console.log(my_elem instanceof Date);
        debugger;
        try { 
            elem.appendChild(my_elem);
        } catch (err) {}
        
        my_elem.__proto__.__proto__.__proto__.__proto__ = date;
        elem.appendChild(my_elem);
        
	}
})