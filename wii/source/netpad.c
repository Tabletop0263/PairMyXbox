#include <network.h>
#include <string.h>
#include <stdbool.h>
#include <fcntl.h>
#ifndef F_SETFL
#define F_SETFL 4
#endif
#ifndef IOS_O_NONBLOCK
#define IOS_O_NONBLOCK 0x04
#endif
#include "netpad.h"

#define NETPAD_PORT 7777

static s32 sock = -1;

int netpad_init(char *ip_out) {
    char mask[16], gw[16];
    if (if_config(ip_out, mask, gw, true, 20) < 0) return -1;

    sock = net_socket(AF_INET, SOCK_DGRAM, IPPROTO_IP);
    if (sock < 0) return -2;

    struct sockaddr_in a;
    memset(&a, 0, sizeof(a));
    a.sin_len = sizeof(a);
    a.sin_family = AF_INET;
    a.sin_port = htons(NETPAD_PORT);
    a.sin_addr.s_addr = INADDR_ANY;
    if (net_bind(sock, (struct sockaddr *)&a, sizeof(a)) < 0) return -3;

    net_fcntl(sock, F_SETFL, IOS_O_NONBLOCK);
    return 0;
}

int netpad_poll(pad_pkt *out) {
    pad_pkt p;
    int got = 0;
    while (net_recv(sock, &p, sizeof(p), 0) == (s32)sizeof(p)) {
        *out = p;   /* keep newest, drop stale */
        got = 1;
    }
    return got;
}
