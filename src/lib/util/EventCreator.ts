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

	public async createUxInfoAlwaysMessage(eventTemplateKey: string, args: string[]): Promise<void> {
		const event = {
			messageKey: eventTemplateKey,
			args: args,
			type: 'INFO',
			handler: 'UX',
			verbose: false,
			time: Date.now()
		};
		await this.outputProcessor.emitEvents([event]);
	}

	public async createUxErrorMessage(eventTemplateKey: string, args: string[]): Promise<void> {
		const event = {
			messageKey: eventTemplateKey,
			args: args,
			type: 'ERROR',
			handler: 'UX',
			verbose: false,
			time: Date.now()
		};
		await this.outputProcessor.emitEvents([event]);
	}
}
