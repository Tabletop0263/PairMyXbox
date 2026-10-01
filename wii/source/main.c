#include <gccore.h>
#include <ogc/lwp_watchdog.h>
#include <wiiuse/wpad.h>
#include <stdio.h>
#include <string.h>
#include "netpad.h"

static void video_init(void) {
    VIDEO_Init();
    GXRModeObj *rmode = VIDEO_GetPreferredMode(NULL);
    void *xfb = MEM_K0_TO_K1(SYS_AllocateFramebuffer(rmode));
    CON_InitEx(rmode, 12, 12, rmode->fbWidth - 24, rmode->xfbHeight - 24);
    VIDEO_Configure(rmode);
    VIDEO_SetNextFramebuffer(xfb);
    VIDEO_SetBlack(FALSE);
    VIDEO_Flush();
    VIDEO_WaitVSync();
    VIDEO_WaitVSync();
}

static void wait_home(void) {
    printf("Press HOME to exit.\n");
    while (1) {
        WPAD_ScanPads();
        if (WPAD_ButtonsDown(0) & WPAD_BUTTON_HOME) return;
        VIDEO_WaitVSync();
    }
}

int main(void) {
    video_init();
    WPAD_Init();

    char ip[16] = {0};
    printf("NetPad: bringing up network...\n");
    int r = netpad_init(ip);
    if (r < 0) {
        printf("Network init failed (%d)\n", r);
        wait_home();
        return 1;
    }
    printf("Wii IP: %s   UDP port 7777\nEnter this IP in PadBridge.\n", ip);

    pad_pkt p;
    memset(&p, 0, sizeof(p));
    u64 last = 0;

    while (1) {
        WPAD_ScanPads();
        if (WPAD_ButtonsDown(0) & WPAD_BUTTON_HOME) break;

        if (netpad_poll(&p)) last = gettime();
        int alive = last && ticks_to_millisecs(diff_ticks(last, gettime())) < 1000;

        printf("\x1b[8;0H");
        if (alive)
            printf("btn=%04X  L(%6d,%6d)  R(%6d,%6d)  LT=%3u RT=%3u   \n",
                   (unsigned)p.buttons, p.lx, p.ly, p.rx, p.ry,
                   (unsigned)p.lt, (unsigned)p.rt);
        else
            printf("no packets from phone...                                    \n");

        VIDEO_WaitVSync();
    }
    return 0;
}
