public class SelfContainedGrandchild extends SelfContainedChild1 {

	public SelfContainedGrandchild() {
		this(true);
	}

	public SelfContainedGrandchild(boolean b) {
		super();
	}

	public override integer publicChildInteger2() {
		return 7;
	}

	public boolean publicGrandchildBoolean1() {
		return true;
	}

	public virtual override integer publicParentOverrideableMethod() {
		return super.publicParentOverrideableMethod();
	}

	public virtual override integer publicParentOverrideableMethod2() {
		return 11;
	}
}
