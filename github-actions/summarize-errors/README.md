# summarize-errors github action

## Github action that adds error information to the GHA report

This action is based on the [typescript-action repo](https://github.com/actions/typescript-action).

**Important:** `index.ts` and its dependencies are compiled into a single file `dist/index.js`. Use the following steps to update `dist/index.js` before committing any changes.

```bash 
$ cd github-actions/summarize-errors
$ npm install 
$ npm run all
$ git add .
$ git commit
```
