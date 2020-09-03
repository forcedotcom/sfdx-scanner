import {initContainer} from '../../ioc.config';

// runs when the CLI is initialized before a command is found to run
// See https://oclif.io/docs/hooks
const hook = function (): void {
	// Bootstrap the ioc container
	initContainer();
}

export default hook;
