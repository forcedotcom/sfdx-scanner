const fixInstructions =
	`Please verify that Java 1.8 or later is installed on your machine and try again.
If the problem persists, please manually add a 'javaHome' property to your Config.json file, referencing your Java home directory.`;

module.exports = {
	"NoJavaHomeFound": `We couldn't find Java Home.\n${fixInstructions}`,
	"InvalidJavaHome": `The Java Home is invalid: %s. Error code: %s.\n${fixInstructions}`,
	"VersionNotFound": `We couldn't find the Java version.\n${fixInstructions}`,
	"InvalidVersion": "Java version %s isn't supported. Install Java 1.8 or later."
};
