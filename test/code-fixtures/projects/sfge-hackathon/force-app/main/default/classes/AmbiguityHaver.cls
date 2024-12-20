public class AmbiguityHaver {
	public boolean callAmbiguousMethod() {
		boolean b1 = AmbiguityHaver.getStaticBoolean();
		SelfContainedParent p = new SelfContainedGrandchild(true);
		return p.publicParentOverrideableMethod() > 3;
	}

	public static boolean getStaticBoolean() {
		return false;
	}
}
