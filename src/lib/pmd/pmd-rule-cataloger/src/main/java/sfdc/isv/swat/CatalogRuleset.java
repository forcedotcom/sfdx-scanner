package sfdc.isv.swat;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;

class CatalogRuleset {
  private String name;
  private String path;
  /**
   * Rulesets can directly include individual rules from either categories or other rulesets.
   */
  private Set<String> singleCategoryReferences = new HashSet<>();
  private Set<String> singleRulesetReferences = new HashSet<>();
  /**
   * Rulesets can reference entire categories or other rulesets, which includes all rules therein except those explicitly
   * excluded. These maps will use the names of bulk-referenced categories/rulesets as keys, and the values will be the
   * set of rules in the that group that are explicitly excluded. A key mapping to an empty set indicates that no rules
   * in that group are excluded.
   */
  private Map<String,Set<String>> bulkCategoryReferences = new HashMap<>();
  private Map<String,Set<String>> bulkRulesetReferences = new HashMap<>();

  private List<CatalogRuleset> dependentRulesets = new ArrayList<>();

  CatalogRuleset(Element root, String path) {
    this.name = root.getAttribute("name");
    this.path = path;
    processRoot(root);
  }

  String getName() {
    return name;
  }

  String getPath() {
    return path;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof CatalogRuleset) {
      return ((CatalogRuleset) other).getPath().equals(path);
    }
    return false;
  }

  void processRule(CatalogRule rule) {
    recursivelyProcessRule(rule, null, 0);
  }

  private void recursivelyProcessRule(CatalogRule rule, CatalogRuleset caller, int recursionDepth) {
    // Before we do anything else, check our recursion depth. Rather than implement any sophisticated logic to check
    // for circular references, we're just going to forcibly exit if we go deeper than 10 layers of recursion, which is
    // way more than anyone could possibly want or need.
    if (recursionDepth > 10) {
      // TODO: Improve the error handling here.
      System.out.println("Recursive processing of rule " + rule.getName() + " went deeper than 10 layers. Your rules are bad and you should feel bad.");
      System.exit(1);
    }
    // Depending on whether this method was invoked by another ruleset, we'll either look for references to the rule's
    // category or references to the ruleset that invoked this method.
    Set<String> singleReferences = caller == null ? singleCategoryReferences : singleRulesetReferences;
    Map<String,Set<String>> bulkReferenceMap = caller == null ? bulkCategoryReferences : bulkRulesetReferences;
    String ruleName = rule.getName();
    String referencePath = caller == null ? rule.getCategoryPath() : caller.getPath();

    // If this ruleset contains a reference to the rule, add it to the rule and make a recursive call on all rulesets
    // depending on this one.
    if (containsReferenceToRule(ruleName, referencePath, singleReferences, bulkReferenceMap)) {
      rule.addRuleset(this);
      for (CatalogRuleset dep : dependentRulesets) {
        dep.recursivelyProcessRule(rule, this, recursionDepth + 1);
      }
    }
  }

  private boolean containsReferenceToRule(String ruleName, String rulePath, Set<String> singleRefs, Map<String,Set<String>> bulkRefs) {
    // If the rule is bulk-referenced, then the path will be a key in the map, and the mapped set won't include the rule's
    // name.
    if (bulkRefs.containsKey(rulePath)) {
      return !bulkRefs.get(rulePath).contains(ruleName);
    }
    // If the rule is single-referenced, its full path (path + name) will be in the set.
    return singleRefs.contains(rulePath + "/" + ruleName);
  }

  private void processRoot(Element root) {
    // First, get all of the Rule-type child nodes.
    NodeList ruleRefs = root.getElementsByTagName("rule");

    // Process all nodes iteratively.
    int nodeCount = ruleRefs.getLength();
    for (int i = 0; i < nodeCount; i++) {
      Element ruleRef = (Element) ruleRefs.item(i);
      processRuleNode(ruleRef);
    }
  }

  private void processRuleNode(Element ruleRef) {
    // Nodes are handled differently depending on whether they're individual- or bulk-references.
    boolean isCat = isCategoryReference(ruleRef);
    if (isBulkReference(ruleRef)) {
      // Determine which map we'll want to put these references in.
      Map<String,Set<String>> targetMap = isCat? bulkCategoryReferences : bulkRulesetReferences;
      handleBulkReference(targetMap, ruleRef);
    } else {
      // Determine which set we'll want to put this reference in.
      Set<String> targetSet = isCat ? singleCategoryReferences : singleRulesetReferences;
      targetSet.add(ruleRef.getAttribute("ref"));
    }
  }

  private boolean isCategoryReference(Element ruleRef) {
    return ruleRef.getAttribute("ref").startsWith("category");
  }

  private boolean isBulkReference(Element ruleRef) {
    return ruleRef.getAttribute("ref").endsWith(".xml");
  }

  private void handleBulkReference(Map<String,Set<String>> targetMap, Element ruleRef) {
    String key = ruleRef.getAttribute("ref");
    Set<String> exclusionSet= new HashSet<>();

    // Get all nodes of type "exclude", and iterate over them, adding their "name" properties to the exclusion set.
    NodeList exclusionNodes = ruleRef.getElementsByTagName("exclude");
    int exclusionCount = exclusionNodes.getLength();
    for (int i = 0; i < exclusionCount; i++) {
      Element exclusion = (Element) exclusionNodes.item(i);
      exclusionSet.add(exclusion.getAttribute("name"));
    }

    targetMap.put(key, exclusionSet);
  }
}
