import {OUTPUT_FORMAT} from './RuleManager';
import {SfdxError} from "@salesforce/core";


export class RuleResultRecombinator {

  public static recombineAndReformatResults([pmdResults] : [string], format : OUTPUT_FORMAT) : string {
    // TODO: Since we only have the one rule engine right now, we're doing this the quick and dirty way. But once we add
    //  the other engines, we'll need to actually add interesting logic to this class.

    // We need to change the results we were given into the desired final format.
    switch (format) {
      case OUTPUT_FORMAT.CSV:
        // TODO: Add CSV support.
        throw new SfdxError('CSV conversion is not currently supported.');
      case OUTPUT_FORMAT.XML:
        return pmdResults;
      default:
        throw new SfdxError('Unrecognized output format.');
    }
  }
}
