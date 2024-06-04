import {Flags, SfCommand} from '@salesforce/sf-plugins-core';
import {RunAction} from '../../lib/actions/RunAction';
import {BundleName, getMessage} from '../../lib/messages';

export default class RunCommand extends SfCommand<void> {
	// We don't need the `--json` output for this command.
	public static readonly enableJsonFlag = false;
	public static readonly summary = getMessage(BundleName.RunCommand, 'command.summary');
	public static readonly description = getMessage(BundleName.RunCommand, 'command.description');
	public static readonly examples = [
		getMessage(BundleName.RunCommand, 'command.examples')
	];

	public static readonly flags = {
		// === Flags pertaining to targeting ===
		workspace: Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.workspace.summary'),
			char: 'w',
			multiple: true,
			delimiter: ',',
			default: ['.']
		}),
		'path-start': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.path-start.summary'),
			char: 's',
			multiple: true,
			delimiter: ','
		}),
		// === Flags pertaining to rule selection ===
		'rule-selector': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.rule-selector.summary'),
			char: 'r',
			multiple: true,
			delimiter: ',',
			default: ["recommended"]
		}),
		// === Flags pertaining to output ===
		'severity-threshold': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.severity-threshold.summary'),
			char: 't',
			options: [
				'1', 'critical',
				'2', 'high',
				'3', 'moderate',
				'4', 'low',
				'5', 'info'
			]
		}),
		view: Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.view.summary'),
			char: 'v',
			options: ['table', 'detail'] // TODO: Should probably be enum?
		}),
		'output-file': Flags.string({
			summary: getMessage(BundleName.RunCommand, 'flags.output-file.summary'),
			char: 'f',
			multiple: true,
			delimiter: ','
		}),
		// === Flags pertaining to configuration ===
		'config-file': Flags.file({
			summary: getMessage(BundleName.RunCommand, 'flags.config-file.summary'),
			char: 'c',
			exists: true
		})
	};

	public async run(): Promise<void> {
		const parsedFlags = (await this.parse(RunCommand)).flags;
		const action: RunAction = new RunAction();
		action.execute(parsedFlags);
	}
}

