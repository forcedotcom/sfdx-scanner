# Building

```
cd pmd-cataloger
./gradlew build
```

# Running
```
bin/run scanner:rule:list
```

# Publishing
1. Checkout and pull master. Update package.json and increment the version appropriately, of the form X.Y.Z.
2. Commit and push your change.  This is also a good opportunity to 'yarn upgrade' and commit the new yarn.lock file.
3. git tag v<X.Y.Z>
4. git push origin v<X.Y.Z>
5. CircleCI is triggered to test and deploy a new package with your new version.
