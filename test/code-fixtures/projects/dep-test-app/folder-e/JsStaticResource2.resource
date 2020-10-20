let pangram = 'the quick red fox jumps over the brown lazy dog';
let alphabet = 'abcdefghijklmnopqrstuvwxyz'.split();

for (let letter of alphabet) {
	console.log(`Pangram contains letter ${letter}? ${pangram.indexOf(letter) !== -1}`);
}
