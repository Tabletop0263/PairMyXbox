#ifndef NETPAD_H
#define NETPAD_H
#include <stdint.h>

enum {
    NP_A = 1 << 0,  NP_B = 1 << 1,  NP_X = 1 << 2,  NP_Y = 1 << 3,
    NP_LB = 1 << 4, NP_RB = 1 << 5, NP_BACK = 1 << 6, NP_START = 1 << 7,
    NP_L3 = 1 << 8, NP_R3 = 1 << 9, NP_GUIDE = 1 << 10,
    NP_UP = 1 << 11, NP_DOWN = 1 << 12, NP_LEFT = 1 << 13, NP_RIGHT = 1 << 14
};

/* Wire format, big-endian (native on Wii). Stick Y: up is negative. */
typedef struct {
    uint32_t buttons;
    int16_t  lx, ly, rx, ry;
    uint8_t  lt, rt;
} __attribute__((packed)) pad_pkt;

/* ip_out: buffer of at least 16 bytes. Returns 0 on success. */
int netpad_init(char *ip_out);
/* Drains the socket, keeps the newest packet. Returns 1 if one arrived. */
int netpad_poll(pad_pkt *out);

#endif
