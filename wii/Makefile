ifeq ($(strip $(DEVKITPPC)),)
$(error Set DEVKITPPC in your environment)
endif
export PATH := $(DEVKITPPC)/bin:$(DEVKITPRO)/tools/bin:$(PATH)

PREFIX  := powerpc-eabi-
LIBOGC  := $(DEVKITPRO)/libogc
MACHDEP := -DGEKKO -mrvl -mcpu=750 -meabi -mhard-float
CFLAGS  := -O2 -Wall $(MACHDEP) -I$(LIBOGC)/include
LDFLAGS := $(MACHDEP) -L$(LIBOGC)/lib/wii
LIBS    := -lwiiuse -lbte -logc -lm
SRC     := source/main.c source/netpad.c

boot.dol: boot.elf
	elf2dol boot.elf boot.dol

boot.elf: $(SRC) source/netpad.h
	$(PREFIX)gcc $(CFLAGS) $(SRC) -o $@ $(LDFLAGS) $(LIBS)

clean:
	rm -f boot.elf boot.dol
