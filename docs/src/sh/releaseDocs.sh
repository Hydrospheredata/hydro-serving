#! /usr/env/bin sh

ln -Fs ~/serving_docs_new/$1 ~/serving_docs_new/latest
cp ~/serving_docs_new/latest/paradox.json ~/serving_docs_new/paradox.json
jq '.[. | length] |= . + "'$1'"' ~/serving_docs_new/versions.json > ~/serving_docs_new/versions_new.json
mv ~/serving_docs_new/versions_new.json  ~/serving_docs_new/versions.json
