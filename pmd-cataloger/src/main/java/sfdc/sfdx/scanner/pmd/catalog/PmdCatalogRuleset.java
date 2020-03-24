package sfdc.sfdx.scanner.pmd.catalog;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sfdc.sfdx.scanner.SfdxScannerException;
import sfdc.sfdx.scanner.EventKey;

import java.util.*;

public class PmdCatalogRuleset {
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
  /**
   * This is going to be a set of all rulesets that reference this one. We'll walk up the dependency chain when we're
   * assigning rules to rulesets.
   */
  private Set<PmdCatalogRuleset> dependentRulesets = new HashSet<>();
  /**
   * This is going to be a set of the paths to every ruleset that this one references. We'll use it when we're building
   * the dependentRulesets set above.
   */
  private Set<String> referencedRulesets = new HashSet<>();

  public PmdCatalogRuleset(Element root, String path) {
    this.name = root.getAttribute("name");
    this.path = path;
    processRoot(root);
  }

  String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof PmdCatalogRuleset) {
      return ((PmdCatalogRuleset) other).getPath().equals(path);
    }
    return false;
  }

  /**
   * If the provided rule is referenced by this ruleset or any rulesets that depend on this one, the rule is added to
   * matching rulesets.
   * @param rule - A rule that may or may not be a part of this ruleset.
   */
  public void processRule(PmdCatalogRule rule) {
    recursivelyProcessRule(rule, null, 0);
  }

  /**
   * Identifies rulesets that reference this one and creates a link to those dependents.
   * @param rulesetsByPath - A map whose keys are paths to ruleset files, and whose values are objects representing those rulesets.
   */
  public void processDependencies(Map<String,PmdCatalogRuleset> rulesetsByPath) {
    for (String ref : referencedRulesets) {
      rulesetsByPath.get(ref).addDependent(this);
    }
  }

  /**
   * Marks the provided ruleset as a dependent of this ruleset.
   * @param ruleset - A ruleset that references rules defined in this ruleset.
   */
  private void addDependent(PmdCatalogRuleset ruleset) {
    dependentRulesets.add(ruleset);
  }

  /**
   * Recursively processes this ruleset and any dependent sets to see if the provided rule is a member.
   * @param rule - A rule that may or may not be a member of this set or a dependent.
   * @param caller - The ruleset that invoked this method. Null for the initial call, non-null for recursive calls.
   * @param recursionDepth - Counter to track how deeply we've recursed, so we know when to give up.
   */
  private void recursivelyProcessRule(PmdCatalogRule rule, PmdCatalogRuleset caller, int recursionDepth) {
    // Before we do anything else, check our recursion depth. Rather than implement any sophisticated logic to check
    // for circular references, we're just going to forcibly exit if we go deeper than 10 layers of recursion, which is
    // way more than anyone could possibly want or need.
    if (recursionDepth > 10) {
      throw new SfdxScannerException(EventKey.ERROR_EXTERNAL_RECURSION_LIMIT, new String[]{caller.getPath(), rule.getFullName()});
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
      for (PmdCatalogRuleset dep : dependentRulesets) {
        dep.recursivelyProcessRule(rule, this, recursionDepth + 1);
      }
    }
  }

  /**
   *
   * @param ruleName - The name of a rule.
   * @param rulePath - The path to the file where the rule is defined.
   * @param singleRefs - A set of rules that are individually included. (either singleCategoryReferences or singleRulesetReferences.)
   * @param bulkRefs - A map of bulk inclusions. (either bulkCategoryReferences or bulkRulesetReferences.)
   * @return - True if the provided map or set reference the provided rule.
   */
  private boolean containsReferenceToRule(String ruleName, String rulePath, Set<String> singleRefs, Map<String,Set<String>> bulkRefs) {
    // If the rule is bulk-referenced, then the path will be a key in the map, and the mapped set won't include the rule's
    // name.
    if (bulkRefs.containsKey(rulePath)) {
      return !bulkRefs.get(rulePath).contains(ruleName);
    }
    // If the rule is single-referenced, its full path (path + name) will be in the set.
    return singleRefs.contains(rulePath + "/" + ruleName);
  }

  /**
   * Invoked by the constructor. Parses all of the rule references nested under the provided root.
   * @param root - The root element in a document model.
   */
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

  /**
   * Processes a single rule node, adding its references to the appropriate map.
   * @param ruleRef - A 'rule'-type node from the document defining this ruleset.
   */
  private void processRuleNode(Element ruleRef) {
    // Nodes are handled differently depending on whether they're individual- or bulk-references.
    boolean isCat = isCategoryReference(ruleRef);
    if (isBulkReference(ruleRef)) {
      // Determine which map we'll want to put these references in.
      Map<String,Set<String>> targetMap = isCat? bulkCategoryReferences : bulkRulesetReferences;
      handleBulkReference(targetMap, ruleRef);
      // If this is a reference to a ruleset, add it to our unique set.
      if (!isCat) {
        referencedRulesets.add(ruleRef.getAttribute("ref"));
      }
    } else {
      // Determine which set we'll want to put this reference in.
      Set<String> targetSet = isCat ? singleCategoryReferences : singleRulesetReferences;
      targetSet.add(ruleRef.getAttribute("ref"));
      // If this is a reference to a rule in a ruleset, add it to our unique set.
      if (!isCat) {
        String ref = ruleRef.getAttribute("ref");
        String path = ref.substring(0, ref.indexOf(".xml") + 4);
        referencedRulesets.add(path);
      }
    }
  }

  /**
   *
   * @param ruleRef - A rule-type node.
   * @return - True if the rule node references a rule defined in a category, else false.
   */
  private boolean isCategoryReference(Element ruleRef) {
    return ruleRef.getAttribute("ref").startsWith("category");
  }

  /**
   *
   * @param ruleRef - A rule-type node.
   * @return - True if the rule node is a bulk reference to multiple rules, else false.
   */
  private boolean isBulkReference(Element ruleRef) {
    return ruleRef.getAttribute("ref").endsWith(".xml");
  }

  /**
   * Adds all exclusions indicated by the given bulk-reference node to the specified reference map.
   * @param targetMap - A map into which bulk references will be placed.
   * @param ruleRef - A bulk-reference rule node.
   */
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
