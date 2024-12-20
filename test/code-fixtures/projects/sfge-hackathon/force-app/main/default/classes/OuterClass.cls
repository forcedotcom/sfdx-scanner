global class OuterClass {
    public boolean causeCollision() {

    	ChainedClass1 c1 = new ChainedClass1(true);
    	boolean b1 = c1.s2.s3.s4.b;
    	boolean b2 = c1.s2.s3.s4.getNotB();
        return new CollidingClass().getBoolean();
    }

    public class CollidingClass {
        public boolean getBoolean() {
            return true;
        }
    }
    public class SiblingClass {
        public boolean causeCollision() {
            return new CollidingClass().getBoolean();
        }
    }

	public class ChainedClass1 {
		public ChainedClass2 s2;

		public ChainedClass1(boolean b) {
			this.s2 = new ChainedClass2(b);
		}
	}

    public class ChainedClass2 {
    	public ChainedClass3 s3;

    	public ChainedClass2(boolean b) {
    		this.s3 = new ChainedClass3(b);
    	}
    }

    public class ChainedClass3 {
    	public ChainedClass4 s4;

    	public ChainedClass3(boolean b) {
    		this.s4 = new ChainedClass4(b);
    	}
    }

    public class ChainedClass4 {
    	public boolean b;

    	public ChainedClass4(boolean b) {
    		this.b = b;
    	}

    	public boolean getNotB() {
    		return !this.b;
    	}
    }
}
