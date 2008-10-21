#ifndef _SIM_LINUXKEYS_H
#define _SIM_LINUXKEYS_H

#include <linux/input.h>

/* ubuntu has these, goobuntu doesn't */
#ifndef KEY_SWITCHVIDEOMODE
# define KEY_SWITCHVIDEOMODE 227
#endif
#ifndef KEY_KBDILLUMTOGGLE
# define KEY_KBDILLUMTOGGLE 228
#endif
#ifndef KEY_KBDILLUMUP
# define KEY_KBDILLUMUP     230
#endif
#ifndef KEY_REPLY
# define KEY_REPLY          232
#endif

#endif /*_SIM_LINUXKEYS_H*/
