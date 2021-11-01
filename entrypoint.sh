# update iptables configuration
apt-get install iptables -y
iptables -P FORWARD DROP
iptables -A INPUT -m state --state INVALID -j DROP
iptables -A INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT
iptables -A INPUT -i lo -j ACCEPT
iptables -A INPUT -s 172.18.0.2 -j ACCEPT
iptables -P INPUT DROP

cd /sustainKG-API && sbt ~jetty:start
