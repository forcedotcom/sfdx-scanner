const fixInstructions =
	`Please verify that Java 1.8 or later is installed on your machine and try again.
If the problem persists, please manually add a 'javaHome' property to your Config.json file, referencing your Java home directory.`;

module.exports = {
	"NoJavaHomeFound": `No Java Home detected.\n${fixInstructions}`,
	"InvalidJavaHome": `Invalid Java Home: %s. Error code: %s.\n${fixInstructions}`,
	"VersionNotFound": `Unable to find Java version.\n${fixInstructions}`,
	"InvalidVersion": "Java version %s not supported. Please install Java 1.8 or later."
};
