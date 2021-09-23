#!/bin/bash

wpid=$1
revision=`curl -s https://www.wikipathways.org/index.php/Pathway:$wpid | grep "wgCurRevisionId" | cut -d"\"" -f2`
url="https://www.wikipathways.org//wpi/wpi.php?action=downloadFile&type=gpml&pwTitle=Pathway:$wpid&oldid=$revision"

curl -s -o gpml/$wpid.gpml $url

git add gpml/$wpid.gpml
echo "$revision" > gpml/$wpid.gpml.rev
git commit -m "Revision $revision" gpml/$wpid.gpml* >/dev/null 2>&1 || true
