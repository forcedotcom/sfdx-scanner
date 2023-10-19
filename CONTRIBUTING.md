# Instructions for Internal Contributors
> At the moment, we are not accepting external contributions. Please watch this space to know when we open.

### Building locally
Clone a copy of sfdx-scanner code through git:
```
git clone https://github.com/forcedotcom/sfdx-scanner.git
```

Build first time using yarn:
```
cd sfdx-scanner
yarn --ignore-scripts && yarn build
```

### Running
Run any sfdx scanner command by replacing `sfdx` with `bin/run.js` or `bin/run.cmd` from sfdx-scanner directory. For example, you can invoke `list` command with:
```
bin/run.js scanner:rule:list
```

### Making changes
Checkout a branch copy of `dev` branch. Make your changes and write unit tests in `./test/` directory.
To run tests, use:
```
yarn test
```

Before pushing your changes and creating a PR, run this to make sure the build would remain stable:

```
yarn --ignore-scripts && yarn build && yarn test && yarn lint
```

### Debugging your plugin
We recommend using the Visual Studio Code (VS Code) IDE for your plugin development. Included in the `.vscode` directory of this plugin is a `launch.json` config file, which allows you to attach a debugger to the node process when running your commands.

To debug the `scanner:rule:list` command: 
1. Start the inspector
  
If you linked your plugin to the sfdx cli, call your command with the `dev-suspend` switch: 
```sh-session
$ sfdx scanner:rule:list --dev-suspend
```
  
Alternatively, to call your command using the `bin/run.js` or `bin/run.cmd` script, set the `NODE_OPTIONS` environment variable to `--inspect-brk` when starting the debugger:
```sh-session
$ NODE_OPTIONS=--inspect-brk bin/run.js scanner:rule:list
```

2. Set some breakpoints in your command code
3. Click on the Debug icon in the Activity Bar on the side of VS Code to open up the Debug view.
4. In the upper left hand corner of VS Code, verify that the "Attach to Remote" launch configuration has been chosen.
5. Hit the green play button to the left of the "Attach to Remote" launch configuration window. The debugger should now be suspended on the first line of the program. 
6. Hit the green play button at the top middle of VS Code (this play button will be to the right of the play button that you clicked in step #5).
<br><img src=".images/vscodeScreenshot.png" width="480" height="278"><br>
Congrats, you are debugging!

### Pushing your changes
Create PR with work item name in the title - this would look like:
`@W-1234567@ Descriptive title of work`

Also, add helpful information about your changes so that reviewers can navigate easily and know what to look for.

### Publishing
1. Checkout and pull master. Update package.json and increment the version appropriately, of the form X.Y.Z.
2. Commit and push your change.  This is also a good opportunity to 'yarn upgrade' and commit the new yarn.lock file.
3. git tag v<X.Y.Z>
4. git push origin v<X.Y.Z>
5. CircleCI is triggered to test and deploy a new package with your new version.
