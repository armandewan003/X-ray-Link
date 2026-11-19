#!/usr/bin/env bash
set -euo pipefail

# X-RAY LINK WireGuard VPN Node Bootstrap Script
# Run on Ubuntu 24.04 LTS VPN edge node as root.

WG_IFACE="${WG_IFACE:-wg0}"
WG_PORT="${WG_PORT:-51820}"
WG_NET="${WG_NET:-10.88.0.1/16}"
EXT_NIC="${EXT_NIC:-eth0}"

apt-get update && apt-get install -y wireguard iptables-persistent curl jq

umask 077
mkdir -p /etc/wireguard
if [[ ! -f /etc/wireguard/server_private.key ]]; then
  wg genkey | tee /etc/wireguard/server_private.key | wg pubkey > /etc/wireguard/server_public.key
fi

SERVER_PRIV=$(cat /etc/wireguard/server_private.key)
SERVER_PUB=$(cat /etc/wireguard/server_public.key)

cat > "/etc/wireguard/${WG_IFACE}.conf" <<EOF
[Interface]
Address = ${WG_NET}
ListenPort = ${WG_PORT}
PrivateKey = ${SERVER_PRIV}
PostUp = iptables -A FORWARD -i ${WG_IFACE} -j ACCEPT; iptables -t nat -A POSTROUTING -o ${EXT_NIC} -j MASQUERADE
PostDown = iptables -D FORWARD -i ${WG_IFACE} -j ACCEPT; iptables -t nat -D POSTROUTING -o ${EXT_NIC} -j MASQUERADE
EOF

sysctl -w net.ipv4.ip_forward=1
echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-xraylink-wg.conf

systemctl enable "wg-quick@${WG_IFACE}"
systemctl restart "wg-quick@${WG_IFACE}"

echo "WireGuard node ready. Register this PUBLIC key in X-RAY LINK Admin -> Servers:"
echo "WG_PUBLIC_KEY=${SERVER_PUB}"
