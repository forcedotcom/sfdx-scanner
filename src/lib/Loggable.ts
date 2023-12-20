export interface Loggable {
	log(message?: string, ...args: any[]): void;
	logToStderr(message?: string, ...args: any[]): void;
}
