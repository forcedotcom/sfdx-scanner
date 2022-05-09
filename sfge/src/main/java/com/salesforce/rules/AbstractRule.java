package com.salesforce.rules;

public abstract class AbstractRule {
    public enum CATEGORY {
        BEST_PRACTICES("Best Practices"),
        INTERNAL_DEBUGGING("Internal Debugging"),
        INTERNAL_TESTING("Internal Testing"),
        PERFORMANCE("Performance"),
        SECURITY("Security");

        public String name;

        CATEGORY(String name) {
            this.name = name;
        }
    }

    // TODO: Define/Refine a system for severity. (e.g.: What's the range? Are higher numbers
    // worse?)
    public enum SEVERITY {
        HIGH(1),
        MODERATE(2),
        LOW(3);

        public int code;

        SEVERITY(int code) {
            this.code = code;
        }
    }

    public Descriptor getDescriptor() {
        return new Descriptor(this);
    }

    // TODO: Eventually, we want to be pulling this data from a file, not from the rule itself. At
    // that point, we'll
    //  probably un-abstract this method.
    protected abstract int getSeverity();

    // TODO: Eventually, we want to be pulling this data from a file, not from the rule itself. At
    // that point, we'll
    //  probably un-abstract this method.
    protected abstract String getDescription();

    // TODO: Eventually, we want to be pulling this data from a file, not from the rule itself. At
    // that point, we'll
    //  probably un-abstract this method.
    protected abstract String getCategory();

    public static class Descriptor {
        private final String name;
        private final String description;
        private final String category;

        private Descriptor(AbstractRule rule) {
            this.name = rule.getClass().getSimpleName();
            this.description = rule.getDescription();
            this.category = rule.getCategory();
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }
    }
}
