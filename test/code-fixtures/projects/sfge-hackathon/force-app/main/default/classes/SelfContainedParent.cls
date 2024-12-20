public abstract class SelfContainedParent {
	private boolean privateParentBoolean1() {
		return true;
	}

	protected integer protectedParentInteger1(integer i) {
		if (i > 7) {
			return i * 4;
		} else {
			return i * 12;
		}
	}

	protected virtual boolean protectedParentBoolean1() {
		return false;
	}

	public integer publicParentInteger1() {
		return 52;
	}

	public virtual integer publicParentOverrideableMethod() {
		return 78;
	}

	public virtual integer publicParentOverrideableMethod2() {
		return 1;
	}

	public abstract boolean abstractBoolean();
}
