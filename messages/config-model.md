# template.last-calculated-as
Last calculated by the config command as: %s

# template.modified-from
Modified from: %s

# template.yaml.remove-empty-object
Remove this empty object {} when you are ready to specify your first rule override

# annotation.config_root
The absolute folder path to which all other path values in this configuration may be relative to.
If not specified or if equal to null, then the value is automatically chosen to be the parent folder of your Code Analyzer
configuration file if it exists, or the current working directory otherwise.

# annotation.log_folder
Folder where to store log files. May be an absolute path or a path relative to config_root.
If not specified or if equal to null, then the value is automatically chosen to be your machine's default temporary directory

# annotation.custom_engine_plugin_modules
(Use at your own risk. This is an experimental property that is not officially supported.)
List of EnginePlugin module paths to dynamically add to Code Analyzer. Each path may be absolute or relative to config_root.

# annotation.rules
Rule override settings of the format rules.<engine_name>.<rule_name>.<property_name> = <override_value> where
  * <engine_name> is the name of the engine containing the rule that you want to override
  * <rule_name> is the name of the rule that you want to override
  * <property_name> can either be:
     'severity' - [Optional] The severity level value that you want to use to override the default severity level for the rule
                  Possible values: 1 or 'Critical', 2 or 'High', 3 or 'Moderate', 4 or 'Low', 5 or 'Info'
     'tags'     - [Optional] The string array of tag values that you want to use to override the default tags for the rule

[Example usage]:
rules:
  eslint:
    sort-vars:
      severity: Info
      tags: ["Recommended", "Suggestion"]
  
