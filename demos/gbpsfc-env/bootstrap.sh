#!/bin/bash

# vim: sw=4 ts=4 sts=4 et tw=72 :

echo "---> Updating operating system"
apt update -qq

echo "---> Installing Group Based Policy requirements"
apt install -y software-properties-common -qq
apt install -y python-software-properties -qq
apt install -y python-pip -qq
apt install -y git-core git -qq
apt install -y curl -qq
# docker
curl -sSL https://get.docker.com/ | sh

cat <<EOL > /etc/default/docker
  DOCKER_NETWORK_OPTIONS='--bip=10.250.0.254/24'
EOL

docker pull alagalah/odlpoc_ovs230
# OVS
curl https://raw.githubusercontent.com/yyang13/ovs_nsh_patches/master/start-ovs-deb.sh | bash

# this part is just for local spinup DON'T copy it to releng bootstrap.sh
pip install ipaddr
echo "export PATH=$PATH:/vagrant" >> /home/vagrant/.profile
echo "export ODL="$1 >> /home/vagrant/.profile
usermod -aG docker vagrant

