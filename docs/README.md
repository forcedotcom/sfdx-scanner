# How to run the doc site locally

## Setup Ruby

```
brew install ruby
```

Add the following to your profile:

```
export PATH="/usr/local/opt/ruby/bin:$PATH"
export LDFLAGS="-L/usr/local/opt/ruby/lib"
export CPPFLAGS="-I/usr/local/opt/ruby/include"
```

## Install Jekyll

https://jekyllrb.com/docs/

```
cd docs
gem install jekyll bundler
bundle install
```

## Start the Server

```
bundle exec jekyll serve
```

Navigate to: http://127.0.0.1:4000/sfdx-scanner/

## Troubleshooting Install Issues

1. Compatibility between `listen` and `ruby` version. Example error message:
```
listen-3.2.1 requires ruby version >= 2.2.7, ~> 2.2, which is incompatible with the current version, ruby 3.1.1p18
```
a. Update bundle by running: 
```
bundle update
```
b. Start server again: 
```
bundle exec jekyll serve
```

2. Server start fails with `Cannot load such file` error. Example error message:
```
/usr/local/lib/ruby/gems/3.1.0/gems/jekyll-4.0.1/lib/jekyll/commands/serve/servlet.rb:3:in `require': cannot load such file -- webrick (LoadError)
```
a. Try adding missing dependency to bundle. For example:
```
bundle add webrick
```
b. Start server again: 
```
bundle exec jekyll serve
```


## Relative URLs

When writing docs always use urls relative to the docs folder.

For images it means the url will start with `./images/` like the example below:

```
![My Image](./images/an-image.png)
```

For links the path is relative to the base path (i.e. `/tools/vscode/`). The url also needs to include the language as show in the example:

```
[My Link](./en/getting-started/orgbrowser)
```

## How to generate Change Log

### Option 1: github-changelog-generator
This tool is finnicky. If you can't get it working, try Option 2 instead.
#### Install github-changelog-generator

Instructions: https://github.com/github-changelog-generator/github-changelog-generator/blob/master/README.md

#### Commands

```bash
export CHANGELOG_GITHUB_TOKEN="<<Github Token>>"
github_changelog_generator -u forcedotcom -p sfdx-scanner -f %m-%d-%Y --no-author --exclude-tags-regex "tag-test*" --no-verbose --since-tag vX.Y.Z

```

#### Note:
Copy the information for the release you are making to the top of the release-information.md

### Option 2: pychangelog

#### Prerequisites

- Install [Python3](https://www.python.org/downloads/)
- Install [Poetry](https://python-poetry.org/docs/#installation).
  - If the `curl` script fails with an error about SSL certificate verification:
    - If you're on OSX, you can resolve this by going to `~/Applications/Python 3.11` and running the `Install Certificates.command` located there.
    - Haven't encountered this on Windows yet. If we do, a solution will go here.
- Set up a [Github Access Token](https://github.com/settings/tokens).

#### Using pychangelog

Installation and general usage instructions are found [here](https://github.com/rero/pychangelog).

You'll want to set the following values in the `config.ini` file that already exists in the pychangelog  repo when you clone it:
- `user = forcedotcom`
- `repo = sfdx-scanner`
- `merging_branch = dev`
- `from_tag = [tag used for last published version]`
- `to_tag = [tag to be used for next planned version]`

Running `pychangelog` as per its usage instructions will create a list of all the PRs/Issues that were merged/closed since the last release.
They'll be in a single list, but they're properly ordered, so you can easily copy-paste them into the release notes' "Merged pull requests" and "Closed issues" sections.

NOTE: PR's will have a `(by @author)` suffix. You'll want to delete that.

## Localization (NOT Supported Yet, Ignore this section)

The site is localized in english and japanese. All articles must specify their language (`en` or `ja`) in the front matter:

```

---

title: My Page
lang: en

---

```

When adding new articles, you MUST add them in both the `_articles/en` and `_articles/ja` directories. When creating a new article in english simply copy it exactly to the `_articles/ja` directory so the content is availible when navigating the site in both lagugages. The article will be localized on the next localization pass.

When updating the `_/data/sidebar.yml` file, titles must specify both the `en` and `ja` versions. If you are adding an new item in english simply copy the english value to the japanese value and it will be translated on the next localization pass.

```

```
