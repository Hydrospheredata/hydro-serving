#!/usr/bin/env python
"""
Very simple HTTP server in python.
Usage::
    ./http_server.py [<port>]
Send a GET request::
    curl http://localhost
Send a HEAD request::
    curl -I http://localhost
Send a POST request::
    curl -d "foo=bar&bin=baz" http://localhost
"""
from http.server import BaseHTTPRequestHandler, HTTPServer
from sys import argv
import json, os, time

class Server(BaseHTTPRequestHandler):
	def do_GET(self):
		self.send_response(200)
		self.send_header('Content-type', 'application/json')
		self.end_headers()
		self.wfile.write("OK".encode())

	def do_POST(self):
		# Doesn't do anything with posted data
		content_length = int(self.headers['Content-Length']) # <--- Gets the size of data
		post_data = self.rfile.read(content_length) # <--- Gets the data itself
		data = json.loads(post_data.decode())
		containerId=os.getenv('CONTAINER_ID', "UNKNOWN")
		if (len(argv) == 3):
			containerId=argv[2]

		for i, val in enumerate(data):
			val[containerId]=int(time.time())

		self.send_response(200)
		self.send_header('Content-type', 'application/json')
		self.end_headers()
		self.wfile.write(json.dumps(data, ensure_ascii=False).encode())

def run(server_class=HTTPServer, handler_class=Server, port=9090):
	server_address = ('', port)
	httpd = server_class(server_address, handler_class)
	print ('Starting httpd...')
	httpd.serve_forever()

if __name__ == "__main__":
	if (len(argv) == 2) or (len(argv) == 3):
		run(port=int(argv[1]))
	else:
		run()