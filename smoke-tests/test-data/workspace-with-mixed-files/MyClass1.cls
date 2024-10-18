public class MyClass1 {
	public static void doSomeQueries(string[] accNames) {
		for (string accName : accNames) {
			Account acc = [SELECT Name FROM Account LIMIT 1];
		}
	}
}
