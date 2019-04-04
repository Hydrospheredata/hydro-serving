#! /usr/env/bin sh

ln -Fs ~/serving_publish_dir_new/$1 ~/serving_publish_dir_new/latest
cp ~/serving_publish_dir_new/latest/paradox.json ~/serving_publish_dir_new/paradox.json
jq 'map(select( . != "'$1'")) | .[. | length] |= . + "'$1'"' ~/serving_publish_dir_new/versions.json > ~/serving_publish_dir_new/versions_new.json
mv ~/serving_publish_dir_new/versions_new.json  ~/serving_publish_dir_new/versions.json
