#!/usr/bin/python

import sys
import StringIO
try:
    import Image, ImageDraw, ImageFont
except ImportError, e:
    print str(e)
    print "Are you missing the Python Imaging Library? (apt-get install python-imaging)"
    sys.exit(1)

FONT_PATH = "/usr/share/fonts/truetype/msttcorefonts/arial.ttf"

class Args(object):
    def __init__(self, dest_name, size, circle_color, border_color,
                 letter_color, letter):
        self.dest_name = dest_name
        self.size = size
        self.circle_color = circle_color
        self.border_color = border_color
        self.letter_color = letter_color
        self.letter = letter

def main(args):
    data = process_args(args)
    if data:
        createImage(data)

def process_args(args):
    if not args or len(args) != 6:
        usage()
    return Args(*args)
    
def usage():
    print """Usage: %s <file_name> <size> <circle-color> <border-color> <letter-color> <letter>""" % sys.argv[0]
    sys.exit(1)

def createImage(data):
    zoom = 4
    rmin = -zoom/2
    rmax = zoom/2
    if zoom > 1:
        r = range(-zoom/2, zoom/2+1)
    else:
        r = [ 0 ]
    sz = int(data.size)
    sz4 = sz * zoom
    
    img = Image.new("RGBA", (sz4, sz4), (255,255,255,0))
    draw = ImageDraw.Draw(img)

    draw.ellipse((0, 0, sz4-zoom, sz4-zoom),
                     fill=data.circle_color, outline=None)
    for i in r:
        draw.ellipse((i, i, sz4-i-zoom, sz4-i-zoom),
                     fill=None, outline=data.border_color)

    font = ImageFont.truetype(FONT_PATH, int(sz4 * .75))
    tsx, tsy = draw.textsize(data.letter, font=font)

    ptx = (sz4 - tsx) / 2
    pty = (sz4 - tsy) / 2
    for i in r:
        draw.text((ptx + i, pty), data.letter, font=font, fill=data.letter_color)

    img = img.resize((sz, sz), Image.BICUBIC)
    img.save(data.dest_name, "PNG")
    print "Saved", data.dest_name

if __name__ == "__main__":
    main(sys.argv[1:])
