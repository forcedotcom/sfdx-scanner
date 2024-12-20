public class SelfContainedClass1 {
	private boolean b {
		get;
		set;
	}

	private boolean b2 {
		get;
	}

	public SelfContainedClass1(boolean b) {
		this.b = b;
	}

	public boolean getNotB() {
		return !b;
	}

	public boolean overloadedMethod(integer i) {
		return i > 5;
	}

	public boolean overloadedMethod(boolean b) {
		return !b;
	}

	public boolean getNotNotB() {
		return !this.getNotB();
	}

	public boolean callOverloadedMethod(integer i, boolean b) {
		boolean b1 = overloadedMethod(i);
		boolean b2 = overloadedMethod(b);
		//boolean b3 = overloadedMethod(null);
		return b1 || b2;
	}
}
