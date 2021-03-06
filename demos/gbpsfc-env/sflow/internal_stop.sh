#!/usr/bin/env bash

if [ ! -f ~/sflow_uuid ]; then
  echo "No sflow_uuid found."
  exit 1
fi

hostnum=${HOSTNAME#"gbpsfc"}
sw="sw$hostnum"

sudo ovs-vsctl remove bridge $sw sflow `cat ~/sflow_uuid`
rm ~/sflow_uuid
