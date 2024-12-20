public virtual class SelfContainedChild1 extends SelfContainedParent {

	public override boolean protectedParentBoolean1() {
		return true;
	}

	public integer publicChildInteger1() {
		return 82;
	}

	public virtual integer publicChildInteger2() {
		return 2;
	}

	public virtual override integer publicParentOverrideableMethod() {
		return 2;
	}

//	public boolean queryRunner() {
//		[SELECT Name from Account LIMIT 1];
//		return false;
//	}

	public override boolean abstractBoolean() {
		return false;
	}
}
