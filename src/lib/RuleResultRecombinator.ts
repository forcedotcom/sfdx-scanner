import {OUTPUT_FORMAT} from './RuleManager';
import {SfdxError} from "@salesforce/core";
import {xml2js} from 'xml-js';

export class RuleResultRecombinator {

  public static recombineAndReformatResults(results : [string], format : OUTPUT_FORMAT) : string {
    // TODO: Since we only have the one rule engine right now, we're doing this the quick and dirty way. But once we add
    //  the other engines, we'll need to actually add interesting logic to this class.

    // We need to change the results we were given into the desired final format.
    switch (format) {
      case OUTPUT_FORMAT.CSV:
        return this.constructCsv(results);
      case OUTPUT_FORMAT.XML:
        return this.constructXml(results);
      default:
        throw new SfdxError('Unrecognized output format.');
    }
  }

  private static constructCsv([pmdResults] : [string]) : string {
    // TODO: Eventually, we'll need logic to combine disparate result sets together into a single CSV, but for now we
    //  can proceed with just PMD's results.
    return this.pmdToCsv(pmdResults);
  }

  private static constructXml([pmdResults] : [string]) : string {
    // TODO: Eventually, we'll need logic to actually combine XMLs together and massage them into the format we want to output,
    //  but for now we can just return the XML that was provided to us.
    return pmdResults;
  }

  private static pmdToCsv(pmdResults : string) : string {
    // If the results were just an empty string, we can return it.
    if (pmdResults === '') {
      return '';
    }
    // TODO: This logic will definitely need to get more sophisticated so we can create a CSV with the columns that we
    //  ultimately want. But for now, we'll take the coward's way out and convert the XML into an approximation of the CSV
    //  that would have been produced by PMD if we'd requested it.
    // PMD's results are given to us as an XML contained in a string. To turn the results into a CSV, first we'll need
    // to turn the XML into a JSON that we can easily iterate through.
    const pmdJson = xml2js(pmdResults, {compact: false, ignoreDeclaration: true});
    // We'll gradually build our CSV, starting with these columns.
    let results = '"Problem","File","Priority","Line","Description","Category","Rule"\n';
    let problemCount = 0;

    // The top-level Element just has an 'property', which is a singleton list containing the 'pmd' Element, whose own
    // 'elements' property is a list of 'file' Elements.
    const fileElements = pmdJson.elements[0].elements;
    for (let i = 0; i < fileElements.length; i++) {
      const fileElement = fileElements[i];
      const fileName = fileElement.attributes.name;
      // Each 'file' Element should contain in its 'elements' property a list of 'violation' Elements describing the ways
      // rules were violated.
      const violations = fileElement.elements;
      for (let j = 0; j < violations.length; j++) {
        const violation = violations[j];
        const attrs = violation.attributes;
        // The error message for the violation is a 'text' Element below it.
        const msg = violation.elements[0].text.trim();
        const row = [++problemCount, fileName, attrs.priority, attrs.beginline, msg, attrs.ruleset, attrs.rule];
        results += '"' + row.join('","') + '"\n';
      }
    }
    return results;
  }
}

