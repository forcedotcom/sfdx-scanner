public virtual class SelfContainedChild2 extends SelfContainedParent {

	public override boolean protectedParentBoolean1() {
		return true;
	}

	public integer publicChildInteger1() {
		return 82;
	}

	public virtual integer publicChildInteger2() {
		return 2;
	}

	public integer publicChildComplexMethod(boolean b, boolean b2) {
		if (b) {
			if (b2) {
				return this.publicChildInteger1();
			} else {
				return this.publicChildInteger2();
			}
		} else {
			if (b2) {
				return 12;
			} else {
				return 7;
			}
		}
	}

	public virtual override integer publicParentOverrideableMethod() {
		return 2;
	}

	public override boolean abstractBoolean() {
		return true;
	}
}
