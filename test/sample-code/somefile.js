export default class SmsTemplateForm extends LightningElement {
	@api recordId;
	@api contactId;
	@api phoneNumber;

	#templateIds; // private field causing the issue
	loading = false;
	templateOptions;
}
