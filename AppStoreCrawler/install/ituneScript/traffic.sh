#!/bin/bash

  eth_name="eth0"
i=0

send_o=`ifconfig $eth_name | grep bytes | awk '{print $6}' | awk -F : '{print $2}'`
recv_o=`ifconfig $eth_name | grep bytes | awk '{print $2}' | awk -F : '{print $2}'`
send_n=$send_o
recv_n=$recv_o

while [ $i -le 100000 ]; do
  send_l=$send_n
  recv_l=$recv_n
  sleep 1
  send_n=`ifconfig $eth_name | grep bytes | awk '{print $6}' | awk -F : '{print $2}'`
  recv_n=`ifconfig $eth_name | grep bytes | awk '{print $2}' | awk -F : '{print $2}'`
  i=`expr $i + 1`
  send_r=`expr $send_n - $send_l`
  recv_r=`expr $recv_n - $recv_l`
  total_r=`expr $send_r + $recv_r`
  send_ra=`expr \( $send_n - $send_o \) / $i`
  recv_ra=`expr \( $recv_n - $recv_o \) / $i`
  total_ra=`expr $send_ra + $recv_ra`
  sendn=`ifconfig $eth_name | grep bytes | awk -F \( '{print $3}' | awk -F \) '{print $1}'`
  recvn=`ifconfig $eth_name | grep bytes | awk -F \( '{print $2}' | awk -F \) '{print $1}'`
#  clear
  send_r=`expr $send_r / 1024`
  recv_r=`expr $recv_r / 1024`
  total_r=`expr $total_r / 1024`
  send_ra=`expr $send_ra / 1024`
  recv_ra=`expr $recv_ra / 1024`
  total_ra=`expr $total_ra / 1024`
  echo  "Last second  :   Send rate: $send_r KB/S  Recv rate: $recv_r KB/S  Total rate: $total_r KB/S"
#  echo  "Average value:   Send rate: $send_ra KB/S Recv rate: $recv_ra KB/S Total rate: $total_ra KB/S"
  #echo  "Total traffic after startup:    Send traffic: $sendn  Recv traffic: $recvn"
done
~                                                                                                                                                            
~              