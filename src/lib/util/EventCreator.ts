import {OutputProcessor} from '../services/OutputProcessor';
import {AsyncCreatable} from '@salesforce/kit';

export class EventCreator extends AsyncCreatable {
	private outputProcessor: OutputProcessor;
	private initialized: boolean;

	protected async init(): Promise<void> {
		if (this.initialized) {
			return;
		}

		this.outputProcessor = await OutputProcessor.create({});

		this.initialized = true;
	}

	public createUxInfoAlwaysMessage(eventTemplateKey: string, args: string[]): void {
		const event = {
			messageKey: eventTemplateKey,
			args: args,
			type: 'INFO',
			handler: 'UX',
			verbose: false,
			time: Date.now()
		};
		this.outputProcessor.emitEvents([event]);
	}
}
