import { test as basetest } from '@salesforce/command/lib/test';
import { CUSTOM_CLASSPATH_REGISTER, CUSTOM_CLASSPATH_REGISTER_TMP } from '../../src/lib/CustomRulePathManager';
import fs = require('fs');


const test = basetest.do(() => {
  // Before the tests run, we rename the existing custom rule registry so it doesn't interfere with the imminent tests.
  if (fs.existsSync(CUSTOM_CLASSPATH_REGISTER)) {
    fs.renameSync(CUSTOM_CLASSPATH_REGISTER, CUSTOM_CLASSPATH_REGISTER_TMP);
  }
})
.finally(ctx => {
  // Regardless of whether the tests passed or failed, we need to restore the registry to its original state.
  if (fs.existsSync(CUSTOM_CLASSPATH_REGISTER_TMP)) {
    fs.renameSync(CUSTOM_CLASSPATH_REGISTER_TMP, CUSTOM_CLASSPATH_REGISTER);
  }
});

export default test;
