import { test as basetest } from '@salesforce/command/lib/test';
import { CUSTOM_CLASSPATH_REGISTER, CUSTOM_CLASSPATH_REGISTER_TMP } from '../../src/lib/CustomRulePathManager';
import fs = require('fs');


const test = basetest.do(() => {
  if (fs.existsSync(CUSTOM_CLASSPATH_REGISTER)) {
    fs.renameSync(CUSTOM_CLASSPATH_REGISTER, CUSTOM_CLASSPATH_REGISTER_TMP);
  }
})
.finally(ctx => {
  if (fs.existsSync(CUSTOM_CLASSPATH_REGISTER_TMP)) {
    fs.renameSync(CUSTOM_CLASSPATH_REGISTER_TMP, CUSTOM_CLASSPATH_REGISTER);
  }
});

export default test;
