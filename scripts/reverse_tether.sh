#!/bin/bash
# Copyright 2010 Google Inc.
# All right reserved.
# Author: Szymon Jakubczak <szym@google.com>
#
# Configure the host and the Android phone for "reverse tethering".
# (Route all network traffic via the host.)

# default values
: ${BRIDGE:=usbeth}
: ${LAN_DEV:=eth0}          # LAN uplink on the host
: ${HOST_DEV:=usb0}         # name of the RNDIS interface on the host
: ${PHONE_DEV:=rndis0}        # name of the RNDIS interface on the phone

: ${PHONE_IP:=192.168.77.2} # for NAT and tests
: ${HOST_IP:=192.168.77.1}
: ${NETMASK:=255.255.255.0}

# location of the hwaddr utility
: ${HWADDR:=/home/build/nonconf/google3/experimental/users/szym/moblat/hwaddr/hwaddr-armeabi}
: ${PHONE_HW:=""}  # hardware (Ethernet) address for the interface (bridge only)

# for NAT configuration
: ${DNS1:=8.8.8.8}
: ${DNS2:=8.8.4.4}

# export ADB=/path/to/sdk/adb for custom adb
ADB="${ADB:-adb} ${SERIAL:+-s $SERIAL}"

set -e
trap error ERR

error() {
  echo >&2 "Error occured: $?"
}

usage() {
  echo "Usage: $0 <command>"
  echo "    rndis      -- start RNDIS and test ping the phone"
  echo "    nat        -- use host as NAT"
  echo "    nat+secure -- nat + extra security"
  echo "    bridge     -- use host as bridge"
  echo "    stop       -- switch back to 3G"
  echo "    stop-all   -- clean up everything"
  echo
  echo "Advanced Commands"
  echo "  Host:"
  echo "    nat_start "
  echo "    nat_secure "
  echo "    nat_stop "
  echo "    bridge_start "
  echo "    bridge_add "
  echo "    bridge_stop "
  echo "  Phone:"
  echo "    rndis_start "
  echo "    rndis_stop "
  echo "    rndis_test "
  echo "    route_nat "
  echo "    route_bridge "
  echo "    route_reset "
  echo
  echo "Options and Environment Variables:"
  echo " -h|--help"
  echo " -b bridge_name                 BRIDGE=$BRIDGE"
  echo " -s serial_number               SERIAL=$SERIAL"
  echo " -u host_usb_device             HOST_DEV=$HOST_DEV"
  echo " -l host_lan_device             LAN_DEV=$LAN_DEV"
  echo " -d dns1 dns2                   DNS1=$DNS1"
  echo "                                DNS2=$DNS2"
  echo " -p phone_ip                    PHONE_IP=$PHONE_IP"
  echo " -a host_ip                     HOST_IP=$HOST_IP"
  echo " -m netmask                     NETMASK=$NETMASK"
  echo " -e hardware_addr               PHONE_HW=$PHONE_HW"
  echo
  echo " HWADDR=$HWADDR"
  echo " ADB=$ADB"
}

##################################
### PHONE configuration routines
##################################
rndis_start() {
  echo "Starting RNDIS..."
  $ADB wait-for-device
  $ADB shell "svc usb setFunction rndis"
  $ADB wait-for-device
  $ADB shell "ifconfig $PHONE_DEV down"
  if [[ -n "$PHONE_HW" ]]; then
    $ADB push $HWADDR /data/local/hwaddr  # TODO(szym) handle failures?
    $ADB shell "/data/local/hwaddr $PHONE_DEV $PHONE_HW"
    $ADB shell "/data/local/hwaddr $PHONE_DEV"
  fi
}

rndis_stop() {
  $ADB shell "svc usb setFunction" #empty to clear
}

rndis_test() {
  # configure some IPs, so that we can ping
  $ADB shell "ifconfig $PHONE_DEV $PHONE_IP netmask $NETMASK up"
  sudo ifconfig $HOST_DEV $HOST_IP netmask $NETMASK up
  echo "Pinging the phone..."
  ping -q -c 1 -W 1 $PHONE_IP
  echo "Success!"
}

update_dns() {
  $ADB shell 'setprop net.dnschange $((`getprop net.dnschange`+1))'
}

default_routes() {
  $ADB shell 'cat /proc/net/route' | awk '{ if ($2==00000000) print $1 }'
}

route_none() {
  $ADB shell "svc data disable"
  $ADB shell "svc wifi disable"
  # kill all default route interfaces (just in case something remains)
  for dev in `default_routes`; do
    $ADB shell "ifconfig $dev down"
  done
}
route_nat() {
  echo "Setting up phone routes and DNS..."
  route_none
  $ADB shell "ifconfig $PHONE_DEV $PHONE_IP netmask $NETMASK up"
  $ADB shell "route add default gw $HOST_IP dev $PHONE_DEV"
  $ADB shell "setprop net.dns1 $DNS1"
  $ADB shell "setprop net.dns2 $DNS2"
  update_dns
}
route_bridge() {
  echo "Running DHCP on the phone..."
  route_none
  $ADB shell "ifconfig $PHONE_DEV up"
  $ADB shell "netcfg $PHONE_DEV dhcp"
  $ADB shell "ifconfig $PHONE_DEV"  # for diagnostics

  DNS1=`$ADB shell getprop net.${PHONE_DEV}.dns1`
  $ADB shell "setprop net.dns1 $DNS1"
  DNS2=`$ADB shell getprop net.${PHONE_DEV}.dns2`
  $ADB shell "setprop net.dns2 $DNS2"
  update_dns
}
route_reset() {
  route_none
  $ADB shell "svc data enable"
}

#################################
### HOST configuration routines
#################################
nat_start() {
  echo "Configuring NAT..."
  sudo sysctl -w net.ipv4.ip_forward=1
  sudo iptables -F
  sudo iptables -t nat -F
  sudo iptables -t nat -A POSTROUTING -o $LAN_DEV -j MASQUERADE
  sudo iptables -P FORWARD ACCEPT
  sudo ifconfig $HOST_DEV $HOST_IP netmask $NETMASK up
}
nat_secure() {
  echo "Making your NAT secure..."
  sudo iptables -A FORWARD -m state --state RELATED,ESTABLISHED -j ACCEPT
  sudo iptables -A FORWARD -m state --state NEW -i $HOST_DEV -j ACCEPT
  sudo iptables -P FORWARD DROP
  sudo ifconfig usb0 $HOST_IP netmask $NETMASK up
}
nat_stop() {
  sudo sysctl -w net.ipv4.ip_forward=0
  sudo iptables -F
  sudo iptables -t nat -F
}

bridge_start() {
  echo "Configuring bridge..."
  sudo brctl addbr $BRIDGE || return 0 # all good
  sudo brctl setfd $BRIDGE 0
  sudo ifconfig $LAN_DEV 0.0.0.0
  sudo brctl addif $BRIDGE $LAN_DEV
  sudo dhclient $BRIDGE || {
    echo "DHCP failed. Recovering..."
    bridge_stop
    false
  }
}
bridge_add() {
  echo "Adding usb0 to the bridge"
  sudo brctl delif $BRIDGE $HOST_DEV 2>/dev/null || true # ignore
  sudo ifconfig $HOST_DEV 0.0.0.0
  sudo brctl addif $BRIDGE $HOST_DEV
}
bridge_stop() {
  sudo ifconfig $BRIDGE down || true # ignore errors
  sudo brctl delbr $BRIDGE || true
  sudo dhclient $LAN_DEV
}

### command-line interpreter
if [ $# == "0" ]; then
  usage
fi

while (( $# )); do
case $1 in
--help|-h)
  usage
  exit
  ;;

-b) shift; BRIDGE=$1 ;;
-s) shift; SERIAL=$1 ;;
-u) shift; HOST_DEV=$1 ;;
-l) shift; LAN_DEV=$1 ;;
-d) shift; DNS1=$1; shift; DNS2=$1 ;;
-p) shift; PHONE_IP=$1 ;;
-a) shift; HOST_IP=$1 ;;
-m) shift; NETMASK=$1 ;;
-e) shift; PHONE_HW=$1 ;;

rndis)
  rndis_start
  rndis_test
  ;;

bridge)
  ifconfig $HOST_DEV >/dev/null || $0 rndis
  bridge_start
  bridge_add
  route_bridge
  ;;

nat)
  ifconfig $HOST_DEV >/dev/null || $0 rndis
  nat_start
  route_nat
  ;;

nat+secure)
  $0 nat
  nat_secure
  ;;

stop)
  route_reset
  ;;

stop-all)
  bridge_stop
  nat_stop
  route_reset
  rndis_stop
  ;;

*) # execute 'advanced command' by function name
  $1
  ;;
esac
shift
done
