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

Navigate to: http://127.0.0.1:4000/


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
