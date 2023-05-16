trigger AccountBefore on Account (before insert, before update) {
	if (Trigger.isInsert) {
		System.debug('asdfasdf');
	}
}
