package com.salesforce.rules.usewithsharingondatabaseoperation;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.vertex.UserClassVertex;

public class SharingPolicyUtil {

    public enum InheritanceType {
        // inherited sharing policy from a parent class
        PARENT,
        // inherited sharing policy from the calling class
        CALLING;

        @Override
        public String toString() {
            return name().toLowerCase() + " class";
        }
    }

    public static boolean hasWithOrInheritedSharing(UserClassVertex v) {
        return v.getSharingPolicy().equals(ASTConstants.SharingPolicy.WITH_SHARING)
                || v.getSharingPolicy().equals(ASTConstants.SharingPolicy.INHERITED_SHARING);
    }

    public static boolean hasInheritedSharing(UserClassVertex v) {
        return v.getSharingPolicy().equals(ASTConstants.SharingPolicy.INHERITED_SHARING);
    }

    public static boolean hasWithSharing(UserClassVertex v) {
        return v.getSharingPolicy().equals(ASTConstants.SharingPolicy.WITH_SHARING);
    }

    public static boolean hasNoSharingPolicy(UserClassVertex v) {
        return v.getSharingPolicy().equals(ASTConstants.SharingPolicy.OMITTED_DECLARATION);
    }
}
