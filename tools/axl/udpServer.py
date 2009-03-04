# UDP server example
import time, socket, string

def main():

    port = 9001
    buf = open("random.dat").read()

    svrsocket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    svrsocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    svrsocket.bind(('', port))

    # hostname = socket.gethostname()
    hostname = "localhost"
    ip = socket.gethostbyname(hostname)
    print 'Server is at IP adress: ', ip
    print 'Listening for requests on port %s ...' % port

    data, address = svrsocket.recvfrom(8192)

    count = 0
    while count < 500:
        print 'Sending packet', count, 'to', address[0]
        svrsocket.sendto("%3.3s%s" % (count, buf), address)
        time.sleep(0.08)
        count += 1
        
if __name__ == "__main__":
    main()
