import https = require('https');
import fs = require('fs');
import unzip = require('node-unzip-2');

const pmdVersion = '6.20.0'; // TODO: get this value from config
const url = `https://github.com/pmd/pmd/releases/download/pmd_releases%2F${pmdVersion}/pmd-bin-${pmdVersion}.zip`;
const distDir = 'dist';
const zipfilename = 'pmd-binary.zip';
const extractedDir = `${distDir}/pmd`;

/**
 * 1. Create distribution directory "./dist" if it doesn't already exist
 * 2. Download PMD binary zip from https://pmd.github.io/ based on the pmdVersion specified.
 * 3. Unzip binary to "./dist/pmd"
 */

export class PmdDownloader {

    static execute() {
        const myDownloader = new PmdDownloader()
        myDownloader.downloadPmd();
    }

    downloadPmd() {
        // clean up dist dir and make a new one
        fs.exists(distDir, function (exists) {
            if (exists) {
                fs.rmdir(distDir, function (err) {
                    console.log(`Could not remove dir ${distDir}: ${err.message}`);
                });
            } else {
                console.log(`${distDir} doesn't exist yet`);
            }
        });

        fs.mkdir(distDir, function (err) {
            console.log(`Could not create dir ${distDir}: ${err.message}`);
        });

        const destination = `${distDir}/${zipfilename}`;

        // download zip file. URL is based on instructions given in https://pmd.github.io/
        this._download(url, destination, function (errMessage) {
            if (errMessage) {
                console.log('Error occurred while downloading PMD: ' + errMessage);
            } else {
                console.log('Successfully downloaded PMD.');
            }
        })

        // extract zip file to dist
        fs.createReadStream(destination).pipe(unzip.Extract({ path: `${extractedDir}` }));

        // delete zip file? (not yet)
    }


    _download(url: string, destination: string, callback: Function) {
        var file = fs.createWriteStream(destination);
        https.get(url, function (response) {
            response.pipe(file);
            file.on('finish', function () { // handle completion
                file.close();
                callback;
            });
        })
            .on('error', function (err) { // handle error
                fs.unlink(destination, callback(err.message));
            });
    }

}