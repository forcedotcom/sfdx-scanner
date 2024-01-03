import {Display} from "../../src/lib/Display";
import {Ux} from "@salesforce/sf-plugins-core";

export class FakeDisplay implements Display {
	private outputs: string[] = [];
	private confirmationPromptResponse: boolean = true;
	private lastTableColumns: Ux.Table.Columns<Ux.Table.Data>;
	private lastTableData: Ux.Table.Data[];

	public getOutputArray(): string[] {
		return this.outputs;
	}

	public getOutputText(): string {
		return this.outputs.join("\n");
	}

	public setConfirmationPromptResponse(tf: boolean) {
		this.confirmationPromptResponse = tf;
	}

	public getLastTableColumns(): Ux.Table.Columns<Ux.Table.Data> {
		return this.lastTableColumns;
	}

	public getLastTableData(): Ux.Table.Data[] {
		return this.lastTableData;
	}


	displayConfirmationPrompt(msg: string): Promise<boolean> {
		this.outputs.push(msg);
		return Promise.resolve(this.confirmationPromptResponse);
	}

	displayInfo(msg: string): void {
		this.outputs.push("[Info]: " + msg);
	}

	displayVerboseInfo(msg: string): void {
		this.outputs.push("[VerboseInfo]: " + msg);
	}

	displayWarning(msg: string): void {
		this.outputs.push("[Warning]: " + msg);
	}

	displayVerboseWarning(msg: string): void {
		this.outputs.push("[VerboseWarning]: " + msg);
	}

	displayStyledHeader(headerText: string): void {
		this.outputs.push("[StyledHeader]: " + headerText);
	}

	displayTable<R extends Ux.Table.Data>(data: R[], columns: Ux.Table.Columns<R>): void {
		this.lastTableColumns = columns;
		this.lastTableData = data;
		this.outputs.push("[Table][" + JSON.stringify(columns) + "]: " + JSON.stringify(data));
	}

	spinnerStart(msg: string, status?: string): void {
		const statusText = status ? "[" + status + "]" : "";
		this.outputs.push("[SpinnerStart]" + statusText + ": " + msg)
	}

	spinnerStop(msg: string): void {
		this.outputs.push("[SpinnerStop]: " + msg);
	}

	spinnerUpdate(status: string): void {
		this.outputs.push("[SpinnerUpdate]: " + status);
	}

	spinnerWait(): void {
		this.outputs.push("[SpinnerWait]");
	}

}
